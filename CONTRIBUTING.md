# Building and contributing

## Local dependencies

The addon is deliberately pinned to the inspected Minecraft 1.21.1 / Fabric / Cobblemon / Battle Tower stack. Obtain the matching mods through their legitimate distribution channels. Do not commit or attach the mods to issues, pull requests, or releases.

```sh
python3 tools/prepare_dependencies.py "/absolute/path/to/your/mods"
./gradlew test build
```

`docs/dependency-pins.json` records the five locally supplied build dependencies. Fabric Loader, Fabric API, Minecraft mappings and other public build dependencies are declared in Gradle. Prepared JARs and nested support libraries stay in ignored `libs/`.

Output: `build/libs/cobbleverse-competitive-tower-0.3.0-experimental.jar`. The runtime and source JARs include the addon license. The source ZIP explicitly excludes private operations, full personal mod inventories, installed mods, and decompilations.

## Verification

Run the existing test suite for changes to selection, mode state or packets. Check server-side authority and packet contents when changing previews. Closed team sheets must not serialize hidden opponent data. Exercise ordinary Doubles as well as Doubles VGC after changing shared hooks.

The optional test-only harness builds with:

```sh
./gradlew -PvisualSmoke remapVisualSmoke
```

The harness source under `tools/visual-smoke/` expects a disposable localhost server on port 25577 and a test client named `TowerUITest`. Enabling its test properties creates/replaces that test player's party and invokes Tower battles in the disposable world. Use it only in an isolated development instance. It is not part of the production addon JAR. Exact test-instance launch setup is environment-specific; see the recorded checks and limits in `docs/VERIFICATION.md`.

## Source-only publishing check

After staging changes, run:

```sh
python3 tools/check_repository.py
```

The check examines Git-tracked paths and text, rejects dependency/build/game-state files and obvious secret/private-machine patterns, and allows only the Apache-licensed Gradle wrapper JAR. It is a guardrail, not a substitute for reviewing staged changes.

Keep local deployment scripts and account-specific server records outside version control. Do not publish private keys, access tokens, player saves, chat logs, or third-party implementation. New code contributions are intended to use the repository's MIT license; existing third-party notices must be preserved.
