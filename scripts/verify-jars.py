"""Check the full release matrix and resource references before client launches."""
import argparse
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parent.parent
MATRIX = [(mc, loader) for mc, loaders in (("1.20.1", ("fabric", "forge")),
          ("1.21.1", ("fabric", "forge", "neoforge")), ("26.2", ("fabric", "neoforge")),
          ("26.3", ("fabric", "neoforge"))) for loader in loaders]


def verify(jar):
    with zipfile.ZipFile(jar) as archive:
        files = set(archive.namelist())
        assert "logo.png" in files, f"Missing icon: {jar.name}"
        models = [n for n in files if n.startswith("assets/better_with_gold/models/item/") and n.endswith(".json")]
        assert len(models) >= 17, f"Missing item models: {jar.name}"
        assert not any(n.startswith("assets/minecraft/textures/models/armor/") and not n.endswith("/") for n in files), f"Vanilla armor texture override: {jar.name}"
        for layer in (1, 2):
            assert f"assets/better_with_gold/textures/models/armor/gold_amyth_layer_{layer}.png" in files
        if "-26." in jar.name:
            equipment = json.loads(archive.read("assets/better_with_gold/equipment/reinforced_golden_armor.json"))
            for layer_type, layers in equipment["layers"].items():
                for layer in layers:
                    namespace, texture = layer["texture"].split(":", 1)
                    assert f"assets/{namespace}/textures/entity/equipment/{layer_type}/{texture}.png" in files
        for name in models:
            model = json.loads(archive.read(name))
            for texture in model.get("textures", {}).values():
                if texture.startswith("better_with_gold:"):
                    expected = "assets/better_with_gold/textures/" + texture.split(":", 1)[1] + ".png"
                    assert expected in files, f"Missing texture {expected} in {jar.name}"
        if "fabric.mod.json" in files:
            metadata = json.loads(archive.read("fabric.mod.json"))
            assert "jauml" not in metadata["depends"], f"Unused dependency in {jar.name}"
            assert "fabric-api" in metadata["depends"], f"Fabric API dependency missing in {jar.name}"
        assert not any("ClientBootAgent" in n for n in files), f"Test observer leaked into {jar.name}"
    print("PASS resources", jar.name)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", required=True)
    args = parser.parse_args()
    for mc, loader in MATRIX:
        verify(ROOT / "all-jars" / f"better_with_gold-{loader}-{mc}-{args.version}.jar")
