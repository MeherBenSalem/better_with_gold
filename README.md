# Better With Gold

Better With Gold is a lightweight Minecraft mod that expands gold tools, armor, and
special items with upgrade templates, reinforced variants, and unique weapon effects.

## Supported versions

| Version root | Minecraft | Loaders | Java |
| --- | --- | --- | --- |
| `1.20.1/` | 1.20.1 | Fabric, Forge | 17 |
| `1.21.1/` | 1.21.1 | Fabric, NeoForge, Forge | 21 |
| `26.2/` | 26.2 | Fabric, NeoForge | 25 |
| `26.3/` | 26.3 | Fabric, NeoForge | 25 |

Each version root is an independent Gradle project.

## Features

- Gold upgrade templates via smithing table
- Golden scythe, great axe, impact blade, and reinforced variants
- Reinforced golden armor set
- Golden fish food item

## Building

```powershell
$env:JAVA_HOME_17 = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
$env:JAVA_HOME_21 = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
$env:JAVA_HOME_25 = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot"
$env:JAVA_HOME = $env:JAVA_HOME_21
.\gradlew.bat buildAll --no-daemon
```

Built mod JARs are collected under `all-jars/`.

## Release verification

With the JDKs above and Python `minecraft-launcher-lib` installed:

```powershell
python scripts/verify-jars.py --version 1.2.3
python scripts/smoke-client.py --version 1.2.3
node scripts/upload_platforms.mjs --version 1.2.3 --changelog-file Better-With-Gold-1.2.3-PatchNotes.md
```

The smoke test loads the actual packaged jars in isolated instances under
`~/.cache/better-with-gold-tests`. It checks all 17 item registry IDs and the
current title screen or an inspected world remains loaded for ten seconds. It also
checks all four reinforced armor texture bindings. A separate test observer closes
the client normally after verification; it is never included in the released mod.
Publishing requires a passing report for every jar with a matching SHA-512 hash.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Security

See [.github/SECURITY.md](.github/SECURITY.md).

## License

Apache License 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).
