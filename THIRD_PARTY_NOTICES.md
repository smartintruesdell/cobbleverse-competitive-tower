# Third-party notices and license scope

The MIT license in `LICENSE` applies to the original Cobbleverse Competitive Tower addon code and original documentation. It does not relicense third-party dependencies, game assets, trademarks, or screenshots' embedded third-party artwork.

## Gradle wrapper

`gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` are upstream Gradle wrapper components under Apache License 2.0. Their existing headers and the JAR's embedded license are retained. A copy is provided at `licenses/Apache-2.0.txt`.

Upstream: https://github.com/gradle/gradle

## Required mods and game components

Minecraft, Fabric, Cobblemon, Cobblemon Battle Tower, RCT API, Architectury, Wild Battle API, and other runtime mods retain their respective licenses and terms. None of their implementation, binaries, decompiled source, or standalone game assets is bundled in this repository or the addon's runtime JAR.

The dependency pin file contains filenames, versions, IDs and checksums only. Developers must supply their own matching local dependencies. Build tools may download their own public dependencies under the relevant upstream terms.

The inspected Cobblemon Battle Tower metadata declares **All Rights Reserved**, with authors Raguto & Yemagh. This addon uses version-specific interoperability hooks. Its MIT license grants rights only to the original addon material; it does not grant permission to redistribute or modify the upstream mod or settle whether separate authorization is required.

## Screenshots and names

`docs/screenshots/` contains development screenshots with Minecraft, Cobblemon, Pokémon, and Battle Tower interface artwork. That underlying artwork and branding remain with their respective owners and are excluded from the MIT grant. Screenshots document the integration; they are not a reusable MIT-licensed game-art pack. This is an unofficial addon with no claimed endorsement from upstream authors or rights holders.

MIT reference: https://spdx.org/licenses/MIT.html
