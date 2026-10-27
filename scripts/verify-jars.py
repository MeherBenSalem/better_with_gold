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
        fellowship(jar, archive, files)
    print("PASS resources", jar.name)


PKG = "tn/nightbeam/better_with_gold/"
CLASSES = ["ally/AllyEntity", "ally/ThreatLabelEntity", "ally/Warbands", "ally/Fellowship", "ally/Fx", "ally/AllyCombat",
           "command/FellowshipCommands", "compat/Compat", "core/ClassCatalog", "item/WarbannerItem",
           "client/RosterView", "client/WheelView", "client/PartyHud", "client/Icons", "client/Ui", "client/GuiCanvas",
           "client/AllyRenderer"]
LANG_KEYS = ["entity.better_with_gold.ally", "item.better_with_gold.warbanner", "key.better_with_gold.command_wheel",
             "key.better_with_gold.roster", "item_group.better_with_gold.fellowship"]


def fellowship(jar, archive, files):
    """Remnants | Fellowship content: classes, skins, GUI art, translations, the Warbanner's model and recipe."""
    for name in CLASSES:
        assert f"{PKG}{name}.class" in files, f"Missing class {name} in {jar.name}"
    assert not any("CoreRegression" in n for n in files), f"Test class leaked into {jar.name}"
    for cls in ("bulwark", "skirmisher", "ranger", "mender", "arcanist"):
        for v in range(4):
            assert f"assets/better_with_gold/textures/entity/ally/{cls}_{v}.png" in files, f"Missing skin {cls}_{v} in {jar.name}"
    for tex in ("wheel_sector", "wheel_sector_hover", "wheel_hub", "wheel_ring", "wheel_pointer"):
        assert f"assets/better_with_gold/textures/gui/{tex}.png" in files, f"Missing GUI texture {tex} in {jar.name}"
        assert f"assets/better_with_gold/textures/gui/{tex}.png.mcmeta" in files, f"Missing GUI filter {tex} in {jar.name}"
    lang = json.loads(archive.read("assets/better_with_gold/lang/en_us.json"))
    for key in LANG_KEYS:
        assert key in lang, f"Missing translation {key} in {jar.name}"
    assert "assets/better_with_gold/textures/item/warbanner.png" in files
    assert "assets/better_with_gold/models/item/warbanner.json" in files
    if "-26." in jar.name:
        assert "assets/better_with_gold/items/warbanner.json" in files, f"Missing item model definition in {jar.name}"
    recipe = "data/better_with_gold/recipes/warbanner.json" if "-1.20.1-" in jar.name else "data/better_with_gold/recipe/warbanner.json"
    assert recipe in files, f"Missing Warbanner recipe {recipe} in {jar.name}"
    if "fabric.mod.json" in files:
        metadata = json.loads(archive.read("fabric.mod.json"))
        assert metadata["entrypoints"].get("client"), f"Fabric client entrypoint missing in {jar.name}"
    if "-forge-1.21.1-" in jar.name:
        mixins = json.loads(archive.read("better_with_gold.forge.mixins.json"))
        assert "GuiMixin" in mixins.get("client", []), f"Party frame HUD mixin missing in {jar.name}"
        assert f"{PKG}mixin/GuiMixin.class" in files


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", required=True)
    args = parser.parse_args()
    for mc, loader in MATRIX:
        verify(ROOT / "all-jars" / f"better_with_gold-{loader}-{mc}-{args.version}.jar")
