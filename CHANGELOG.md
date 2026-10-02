# Changelog

## 1.2.3

- Assign registry IDs before constructing items on Minecraft 26.2 and 26.3, fixing the Fabric `Item id not set` startup crash.
- Register items and creative tabs during the registry event on every Forge and NeoForge version, avoiding premature reads of deferred entries.
- Remove the unused JAuml startup dependency; Better With Gold does not use its API. Fabric API remains required for Fabric builds.
- Restore the original item models, textures, translations, and project icon omitted by the earlier MultiLoader conversion.
- Connect equipped reinforced armor to its custom textures on all supported versions.
- Add isolated client boot verification for the actual packaged jars on all nine supported Minecraft/loader combinations.

## 1.2.2

- Add Minecraft 26.3 support (Fabric + NeoForge), matching the existing 26.2 version root.

## 1.2.1

- Fix Fabric startup failure: replace invalid `"fabric": "*"` dependency with `"fabric-api": "*"` in all Fabric `fabric.mod.json` files (1.20.1, 1.21.1, 26.2). `"fabric"` is not a mod id and prevented the game from launching.

## 1.2.0

- Relicensed under Apache License 2.0
- Standardized Apache OSS repo layout with root build aggregator
- Confirmed MultiLoader support: Fabric + Forge (1.20.1), Fabric + NeoForge + Forge (1.21.1), Fabric + NeoForge (26.2)
- Removed template and examplemod clutter
