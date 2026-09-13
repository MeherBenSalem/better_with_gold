# Changelog

## 1.2.1

- Fix Fabric startup failure: replace invalid `"fabric": "*"` dependency with `"fabric-api": "*"` in all Fabric `fabric.mod.json` files (1.20.1, 1.21.1, 26.2). `"fabric"` is not a mod id and prevented the game from launching.

## 1.2.0

- Relicensed under Apache License 2.0
- Standardized Apache OSS repo layout with root build aggregator
- Confirmed MultiLoader support: Fabric + Forge (1.20.1), Fabric + NeoForge + Forge (1.21.1), Fabric + NeoForge (26.2)
- Removed template and examplemod clutter
