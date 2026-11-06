# Better With Gold 1.2.3

Startup compatibility fixes for all supported loaders.

- Fixed the `Item id not set` crash while registering items on Minecraft 26.2 and 26.3.
- Fixed premature registry access during Forge and NeoForge startup.
- Removed the unused JAuml dependency. Fabric builds still require Fabric API.
- Restored the original item models, textures, translations, and project icon omitted by the earlier MultiLoader conversion.
- Fixed equipped reinforced armor using vanilla gold textures on newer versions and Fabric 1.20.1.

Supported builds: Fabric/Forge 1.20.1; Fabric/Forge/NeoForge 1.21.1; Fabric/NeoForge 26.2 and 26.3.
