package dev.cobbleverse.tower.client;

import dev.cobbleverse.tower.PreviewNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TowerClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        TowerModeClient.register();
        ClientPlayNetworking.registerGlobalReceiver(PreviewNetwork.Preview.ID,(p,c) -> c.client().execute(() -> {
            if(c.client().world==null) return;
            if(c.client().currentScreen instanceof PreviewScreen screen && screen.token().equals(p.token())) screen.refresh(p);
            else c.client().setScreen(new PreviewScreen(p));
        }));
        ClientPlayNetworking.registerGlobalReceiver(PreviewNetwork.Status.ID,(p,c) -> c.client().execute(() -> {
            if(c.client().currentScreen instanceof PreviewScreen screen && screen.token().equals(p.token())) screen.status(p);
        }));
    }
}
