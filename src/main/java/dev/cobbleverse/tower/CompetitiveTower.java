package dev.cobbleverse.tower;

import battle.tower.battle.BattleTowerManager;
import battle.tower.data.*;
import dev.cobbleverse.tower.mixin.TowerRecovery;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.gitlab.srcmc.rctapi.api.trainer.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.Registries;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static net.minecraft.server.command.CommandManager.*;

public final class CompetitiveTower implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("competitive-tower");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("cobbleverse-competitive-tower.json");
    private static Config config = new Config();
    private static final Map<UUID, Pending> pending = new HashMap<>();
    private static final Set<UUID> recovery = new HashSet<>();
    private static Path journal;
    private static MinecraftServer currentServer;
    private static boolean storageReady;
    private static final ThreadLocal<Request> request = new ThreadLocal<>();
    private static Pending launching;

    public static final class Config {
        public boolean enabled = false;
        public int selectionTimeoutSeconds = 120;
        public Set<UUID> optedInPlayers = new HashSet<>();
    }
    private record Request(ServerPlayerEntity player, BlockPos terminal) {}
    private static final class Pending {
        final UUID owner;
        final String token = UUID.randomUUID().toString();
        final long deadline;
        final BlockPos terminal;
        final String dimension;
        final int floor;
        final long snapshotTime;
        final String settings;
        final String forcedTeam;
        final List<UUID> partyIds;
        final List<String> partyState;
        final List<PreviewNetwork.Entry> partyPreview;
        final BattleTowerTeams.TeamResult opponent;
        final int[] npcSlots = Selection.randomFour(new Random());
        int[] playerSlots;
        final List<net.minecraft.entity.Entity> spawned = new ArrayList<>();
        Pending(ServerPlayerEntity p, BlockPos pos, BattleTowerTeams.TeamResult opponent) {
            owner = p.getUuid(); terminal = pos.toImmutable();
            dimension = p.getWorld().getRegistryKey().getValue().toString();
            var data = TowerDataManager.getPlayerData(owner);
            floor = data.getCurrentFloor(); snapshotTime = data.getTeamSnapshot().getSnapshotTime();
            settings = data.getBattleSettings().toNbt().toString(); forcedTeam = data.getForcedTeamName();
            var party = party(p); partyIds = ids(party); partyState = state(p, party); partyPreview = publicEntries(party);
            this.opponent = opponent;
            deadline = System.nanoTime() + config.selectionTimeoutSeconds * 1_000_000_000L;
        }
    }
    @Override public void onInitialize() {
        PreviewNetwork.register();
        TowerModeNetwork.register();
        // Resolve all version-specific hooks at startup, rather than on the first player's challenge.
        try { Class.forName("battle.tower.battle.BattleTowerManager", false, getClass().getClassLoader()); }
        catch (ClassNotFoundException e) { throw new IllegalStateException("Pinned Tower manager missing",e); }
        try {
            if (Files.exists(CONFIG)) config = JSON.fromJson(Files.readString(CONFIG), Config.class);
            if (config == null || config.optedInPlayers == null || config.selectionTimeoutSeconds < 10 || config.selectionTimeoutSeconds > 600)
                throw new IOException("Invalid config; timeout must be 10–600 seconds.");
            if (!Files.exists(CONFIG)) write(CONFIG, config);
        } catch (Exception e) { config = new Config(); LOG.error("Competitive mode disabled: invalid configuration", e); }
        // A client JVM can host several singleplayer worlds in succession.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> resetWorldState());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> resetWorldState());
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            currentServer = server;
            journal = server.getSavePath(WorldSavePath.ROOT).resolve("competitive-tower-pending.json");
            try {
                recovery.clear();
                if (Files.exists(journal)) recovery.addAll(Arrays.asList(JSON.fromJson(Files.readString(journal), UUID[].class)));
                storageReady = true;
            } catch (Exception e) { storageReady = false; LOG.error("Competitive mode disabled: unreadable recovery journal", e); }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (recovery.contains(handler.player.getUuid())) cancel(handler.player, "Recovered an interrupted preview; the Tower run was forfeited.");
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (pending.containsKey(handler.player.getUuid())) cancel(handler.player, "Preview disconnected; run forfeited.");
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (Pending p : List.copyOf(pending.values())) {
                var player = server.getPlayerManager().getPlayer(p.owner);
                if (player != null && (System.nanoTime() >= p.deadline || !sameRun(player, p)))
                    cancel(player, "Preview expired or run changed; run forfeited.");
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (UUID id : List.copyOf(pending.keySet())) {
                var player = server.getPlayerManager().getPlayer(id);
                if (player != null) cancel(player, "Server stopping; preview run forfeited.");
            }
            pending.clear(); request.remove(); launching = null;
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
            literal("ctower")
                .then(literal("on").executes(c -> preference(c.getSource().getPlayerOrThrow(), true)))
                .then(literal("off").executes(c -> preference(c.getSource().getPlayerOrThrow(), false)))
                .then(literal("cancel").executes(c -> {
                    var player = c.getSource().getPlayerOrThrow();
                    if (pending.containsKey(player.getUuid())) cancel(player, "Preview canceled; run forfeited.");
                    else tell(player, "No competitive preview is pending.");
                    return 1;
                }))
                .then(literal("preview").executes(c -> {
                    var player = c.getSource().getPlayerOrThrow(); var p = pending.get(player.getUuid());
                    if (p != null) show(player, p); else tell(player, "No preview. Enable with /ctower on, then lock six Pokémon in Tower Doubles and start.");
                    return 1;
                }))
                .then(literal("pick").then(argument("token", StringArgumentType.word())
                    .then(argument("first", IntegerArgumentType.integer(1,6))
                    .then(argument("second", IntegerArgumentType.integer(1,6))
                    .then(argument("third", IntegerArgumentType.integer(1,6))
                    .then(argument("fourth", IntegerArgumentType.integer(1,6)).executes(c -> choose(
                        c.getSource().getPlayerOrThrow(), StringArgumentType.getString(c,"token"),
                        IntegerArgumentType.getInteger(c,"first"), IntegerArgumentType.getInteger(c,"second"),
                        IntegerArgumentType.getInteger(c,"third"), IntegerArgumentType.getInteger(c,"fourth")))))))))));
        LOG.info("Competitive Tower experimental addon loaded; enabled={}", config.enabled);
    }
    private static void resetWorldState() {
        pending.clear();
        recovery.clear();
        request.remove();
        launching = null;
        journal = null;
        currentServer = null;
        storageReady = false;
    }
    public static boolean modeAvailable() { return config.enabled && storageReady; }
    public static boolean legacyPreference(UUID player) { return config.optedInPlayers.contains(player); }
    public static boolean hasSixHealthy(ServerPlayerEntity player) { return healthySix(party(player)); }
    private static int preference(ServerPlayerEntity player, boolean enabled) {
        if (TowerDataManager.getPlayerData(player.getUuid()).isPartyLockedIn() || BattleTowerManager.hasActiveSession(player.getUuid())) {
            tell(player, "Finish or forfeit your current Tower run before changing mode."); return 0;
        }
        if (enabled && (!config.enabled || !storageReady)) { tell(player, "Competitive mode is disabled by the server."); return 0; }
        var previous = new HashSet<>(config.optedInPlayers);
        if (enabled) config.optedInPlayers.add(player.getUuid()); else config.optedInPlayers.remove(player.getUuid());
        try { write(CONFIG, config); }
        catch (IOException e) { config.optedInPlayers = previous; tell(player, "Could not save preference."); return 0; }
        tell(player, enabled ? "Competitive selection enabled for solo Tower Doubles. Bring six healthy Pokémon. Cancel/timeout forfeits the run." : "Original Tower 6v6 mode restored.");
        return 1;
    }
    public static BattleTowerManager.BattleTowerSession start(ServerPlayerEntity player, BlockPos terminal,
            Operation<BattleTowerManager.BattleTowerSession> original) {
        Pending p = pending.get(player.getUuid());
        if (p != null && launching != p) { show(player, p); return null; }
        Request before = request.get(); request.set(new Request(player, terminal));
        try { return original.call(player, terminal); }
        finally { if (before == null) request.remove(); else request.set(before); }
    }
    /** Called at solo entry, after the Tower's own validation but before teleport or NPC creation. */
    public static boolean pause() {
        Request r = request.get();
        if (r == null || launching != null) return false;
        var player = r.player; var data = TowerDataManager.getPlayerData(player.getUuid());
        if (!TowerModes.competitive(player.getUuid())) return false;
        if (!config.enabled || !storageReady || !healthySix(party(player)) || !validTerminal(player, r.terminal)) {
            tell(player, "Competitive mode needs six healthy Pokémon and a nearby Holo Tower. Run forfeited.");
            BattleTowerManager.forfeitRun(player); return true;
        }
        try {
            recovery.add(player.getUuid()); saveJournal();
            var settings = data.getBattleSettings();
            BattleTowerTeams.TeamResult team = data.getForcedTeamName() == null ? null : BattleTowerTeams.getTeamByName(data.getCurrentFloor(), data.getForcedTeamName());
            if (team == null) team = data.getForcedTeamName() == null && settings.getTeamMode() == BattleSettings.TeamMode.RANDOM
                ? BattleTowerTeams.getRandomTeamForFloor(data.getCurrentFloor(), player.getUuid(), settings.isAllowLegendaries())
                : BattleTowerTeams.getTeamForFloor(data.getCurrentFloor(), player.getUuid());
            if (team == null || !healthySix(Arrays.asList(team.team))) throw new IllegalStateException("Tower did not generate six healthy opponents");
            Pending p = new Pending(player, r.terminal, team); pending.put(player.getUuid(), p); show(player, p);
        } catch (Exception e) { LOG.error("Unable to prepare competitive preview", e); cancel(player, "Preview failed; run forfeited."); }
        return true;
    }
    private static int choose(ServerPlayerEntity player, String token, int... slots) {
        Pending p = pending.get(player.getUuid());
        if (p == null || !p.token.equals(token)) { tell(player, "No matching preview. Use /ctower preview."); return 0; }
        try { slots = Selection.validate(slots); } catch (IllegalArgumentException e) { tell(player, e.getMessage()); return 0; }
        var party = party(player);
        if (!Selection.accepts(p.owner, player.getUuid(), p.token, token, p.deadline, System.nanoTime(), p.partyIds, ids(party))
            || !sameRun(player,p) || !healthySix(party) || !p.partyState.equals(state(player,party))
            || BattleRegistry.getBattleByParticipatingPlayer(player) != null
            || !validTerminal(player,p.terminal) || !p.dimension.equals(player.getWorld().getRegistryKey().getValue().toString())) {
            cancel(player, "Party, location or run changed, or preview expired; run forfeited."); return 0;
        }
        if (TowerDataManager.getPlayerData(p.owner).isOnBattleInitiationCooldown()) {
            tell(player,"Tower initiation cooldown is still active. Try the same selection again shortly."); return 0;
        }
        p.playerSlots = slots; launching = p;
        var returnWorld = player.getServerWorld(); var returnPos = player.getPos();
        float returnYaw = player.getYaw(), returnPitch = player.getPitch();
        Map<UUID,Integer> levels = new HashMap<>(), experience = new HashMap<>();
        for (var pokemon : party) { levels.put(pokemon.getUuid(),pokemon.getLevel()); experience.put(pokemon.getUuid(),pokemon.getExperience()); }
        boolean succeeded = false;
        try {
            var result = BattleTowerManager.startBattle(player, p.terminal);
            if (result == null) {
                for (var entity : p.spawned) if (!entity.isRemoved()) entity.discard();
                cancel(player, "Tower could not start the battle; run forfeited."); return 0;
            }
            pending.remove(p.owner); recovery.remove(p.owner); saveJournal();
            PreviewNetwork.status(player,p.token,true,"Battle starting.");
            succeeded = true;
            tell(player, "Competitive doubles started: four per side; your first two selections lead.");
            return 1;
        } catch (Exception e) {
            LOG.error("Competitive Tower start failed", e);
            TowerRecovery.competitive$recover(player);
            cancel(player, "Battle start failed; Tower cleanup requested.");
            for (var entity : p.spawned) if (!entity.isRemoved()) entity.discard();
            return 0;
        } finally {
            try {
                if (!succeeded) {
                    for (var pokemon : party(player)) if (levels.containsKey(pokemon.getUuid())) {
                        if (pokemon.getLevel() != levels.get(pokemon.getUuid())) pokemon.setLevel(levels.get(pokemon.getUuid()));
                        if (pokemon.getExperience() != experience.get(pokemon.getUuid())) pokemon.setExperienceAndUpdateLevel(experience.get(pokemon.getUuid()));
                    }
                    player.teleport(returnWorld,returnPos.x,returnPos.y,returnPos.z,returnYaw,returnPitch);
                }
            } finally { launching = null; }
        }
    }
    public static BattleTowerTeams.TeamResult cachedTeam() {
        return launching != null && request.get() != null && request.get().player.getUuid().equals(launching.owner) ? launching.opponent : null;
    }
    public static Pokemon[] roster(Trainer trainer, Pokemon[] original) {
        Pending p = launching;
        if (p == null || cachedTeam() == null) return original;
        if (trainer instanceof TrainerPlayer player && player.getPlayer().getUuid().equals(p.owner))
            return Selection.pick(Arrays.asList(original), p.playerSlots).toArray(Pokemon[]::new);
        if (trainer instanceof TrainerNPC && ids(Arrays.asList(original)).equals(ids(Arrays.asList(p.opponent.team))))
            return Selection.pick(Arrays.asList(original), p.npcSlots).toArray(Pokemon[]::new);
        return original;
    }
    public static void trackSpawn(UUID owner, net.minecraft.entity.Entity entity) {
        if (launching != null && launching.owner.equals(owner)) launching.spawned.add(entity);
    }
    private static boolean sameRun(ServerPlayerEntity player, Pending p) {
        var data = TowerDataManager.getPlayerData(p.owner);
        return data.isPartyLockedIn() && data.isRunActive() && data.getCurrentFloor() == p.floor
            && data.getTeamSnapshot().getSnapshotTime() == p.snapshotTime
            && data.getBattleSettings().toNbt().toString().equals(p.settings)
            && Objects.equals(data.getForcedTeamName(), p.forcedTeam);
    }
    private static boolean validTerminal(ServerPlayerEntity player, BlockPos pos) {
        return player.getBlockPos().isWithinDistance(pos, 8)
            && Registries.BLOCK.getId(player.getWorld().getBlockState(pos).getBlock()).toString().equals("cobblemon_battle_tower:holo_battle_tower");
    }
    public static void forget(UUID owner) {
        Pending ended = pending.remove(owner); recovery.remove(owner);
        if (ended != null && currentServer != null) PreviewNetwork.status(currentServer.getPlayerManager().getPlayer(owner),ended.token,true,"Preview ended.");
        try { saveJournal(); } catch (IOException e) { storageReady = false; LOG.error("Cannot update recovery journal", e); }
    }
    private static void cancel(ServerPlayerEntity player, String message) {
        // Keep recovery journal until the original forfeit has saved the run's state.
        BattleTowerManager.forfeitRun(player);
        forget(player.getUuid()); tell(player,message);
    }
    public static void submitUi(ServerPlayerEntity player, PreviewNetwork.Submit submission) {
        Pending p = pending.get(player.getUuid());
        if (p == null || !p.token.equals(submission.token())) {
            PreviewNetwork.status(player,submission.token(),true,"This preview has expired."); return;
        }
        int result = choose(player,submission.token(),submission.first(),submission.second(),submission.third(),submission.fourth());
        if (result == 0 && pending.get(player.getUuid()) == p)
            PreviewNetwork.status(player,p.token,false,"Selection not accepted. Check four distinct slots; if valid, wait for the Tower cooldown and retry.");
    }
    public static void cancelUi(ServerPlayerEntity player, String token) {
        Pending p = pending.get(player.getUuid());
        if (p != null && p.token.equals(token)) cancel(player,"Preview canceled; run forfeited.");
        else PreviewNetwork.status(player,token,true,"This preview has expired.");
    }
    private static List<PreviewNetwork.Entry> publicEntries(List<Pokemon> team) {
        return team.stream().map(p -> new PreviewNetwork.Entry(p.getSpecies().getResourceIdentifier().toString(),p.getSpecies().getName(),p.getForm().getName())).toList();
    }
    private static void show(ServerPlayerEntity player, Pending p) {
        tell(player,"Opponent's six (species / form only): " + labels(Arrays.asList(p.opponent.team)));
        tell(player,"Your slots: " + labels(party(player)));
        tell(player,"Pick four in order; first two lead. NPC choice is hidden. Cancel, timeout or disconnect forfeits this run.");
        if (PreviewNetwork.available(player)) {
            int remaining = (int)Math.clamp((p.deadline-System.nanoTime())/1_000_000L,0,600_000);
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,new PreviewNetwork.Preview(
                p.token,p.floor,remaining,publicEntries(Arrays.asList(p.opponent.team)),p.partyPreview));
        }
        String command = "/ctower pick " + p.token + " 1 2 3 4";
        player.sendMessage(Text.literal("[Click to edit your four slots] " + command)
            .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))), false);
    }
    private static String labels(List<Pokemon> team) {
        List<String> labels = new ArrayList<>();
        for (int i=0;i<team.size();i++) labels.add((i+1)+": "+team.get(i).getSpecies().getName()+" / "+team.get(i).getForm().getName());
        return String.join("; ",labels);
    }
    private static List<Pokemon> party(ServerPlayerEntity player) {
        List<Pokemon> list = new ArrayList<>(); Cobblemon.INSTANCE.getStorage().getParty(player).forEach(list::add); return list;
    }
    private static List<UUID> ids(List<Pokemon> team) { return team.stream().map(Pokemon::getUuid).toList(); }
    private static List<String> state(ServerPlayerEntity player, List<Pokemon> team) {
        return team.stream().map(p -> p.saveToNBT(player.getRegistryManager(),new net.minecraft.nbt.NbtCompound()).toString()).toList();
    }
    private static boolean healthySix(List<Pokemon> team) { return team.size()==6 && team.stream().allMatch(p -> p != null && !p.isFainted()); }
    private static void tell(ServerPlayerEntity player, String text) { player.sendMessage(Text.literal(text),false); }
    private static void saveJournal() throws IOException { if (journal != null) write(journal,recovery); }
    private static void write(Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent()); Path temp = path.resolveSibling(path.getFileName()+".tmp");
        Files.writeString(temp,JSON.toJson(value));
        Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
}
