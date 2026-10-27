# Remnants | Fellowship: store assets

Everything the CurseForge and Modrinth pages need, made from the mod itself: real in-game screenshots, the mod's own
pixel art, and two OFL fonts. Nothing here is AI-generated, and every number in the copy comes from the code or the
generated config (`docs/reference/`).

| Path | What it is |
| --- | --- |
| `curseforge/summary.txt` | The one-line summary (CurseForge "summary", Modrinth "description") |
| `curseforge/description.md` | The page body, Markdown with absolute image links; paste it into either store |
| `images/icon.png` | Project icon, 512×512: the Warbanner over the five class emblems, inside a gold diamond |
| `images/banner.png` | Top-of-page banner, 1600×520: the name over an Arc Storm landing on a pillager band at sunset |
| `images/card-*.png` | Feature cards, 1600×900, one per section of the page |
| `images/header-*.png`, `images/divider.png` | Section headers (1400×120) and the divider, transparent |
| `images/gifs/command-wheel.gif`, `images/gifs/revive.gif` | 480 and 440 px loops from the Reel's footage, 4 to 5 MB each |
| `images/screenshots/*.png` | The gallery: 1920×1080 captures, listed below with captions |
| `reel/` | The promotional Reel (see `reel/README.md`) |
| `source/render.cjs` | Renders the icon, banner, cards, headers and divider |

## Gallery

Upload these to the store gallery in this order, with `battle-storm.png` as the featured image. Titles and descriptions are ready to
paste.

| File | Title | Description |
| --- | --- | --- |
| `battle-storm.png` | Arc Storm | The Arcanist's Arc Storm lands on a pillager band as the Ranger's volley flies in. |
| `warband.png` | Your warband | One of each class at sunset: Mender, Arcanist, Bulwark, Skirmisher and Ranger. |
| `hire-sellsword.png` | For hire | A sellsword near the village bell: name, class, level and price over its head. |
| `hire-oath.png` | The oath | One click with gold in hand and the sellsword swears the oath. |
| `command-wheel.png` | The command wheel | Hold G, point, let go. Party frames on the left. |
| `challenge.png` | Challenge and Whirl | The Bulwark's Challenge ring spreads out while the Skirmisher whirls through the raiders. |
| `taunt.png` | Bulwark | The Bulwark pulls the raiders off everyone else. |
| `whirl.png` | Skirmisher | Whirl hits everything within 3 blocks. |
| `volley.png` | Ranger | Volley: three arrows at once. |
| `arc-storm.png` | Arcanist | Arc Storm: lightning on the pack and a rune circle around it. |
| `mend.png` | Mender | The Mender keeps the Bulwark standing. |
| `aggro-labels.png` | Aggro | A label over every engaged enemy shows who it is after. |
| `downed.png` | Downed | At zero health an ally collapses instead of dying. |
| `revive.png` | Revived | Crouch beside it for 3 seconds to pull it back up. |
| `march.png` | On the march | The warband follows in a Wedge. |
| `warbanner-planted.png` | The Warbanner | Planted: the band holds this ground. |
| `warbanner-guard.png` | Guarded | The band rings its Warbanner in a Circle. |
| `roster.png` | Roster and Chronicle | Press U: live portraits, stats, traits and a Chronicle written from what happened. |
| `skirmish.png` | In the thick of it | Whirl and arcane bolts in the same second. |
| `charge.png` | Charge | The Bulwark and the Skirmisher run in, the Arcanist behind them. |
| `regalia.png` | Gilded Regalia | The leader in Reinforced Golden Armour with a Reinforced Greater Golden Hammer. |

## How the screenshots were made

Every screenshot is an unedited capture of the 1.21.1 Fabric development client of this repository (1920×1080, a
60° field of view, fancy graphics, all particles), joined to its development server, in seed `8675309` beside the
plains village at (-304, -320). The fights are real fights between a warband recruited with `/fellowship recruit` and
pillagers and vindicators; the abilities fire on their own; hiring, the wheel, crouch-reviving and planting the
Warbanner are real key presses and clicks. `reel/source/capture/gallery.py` (with `stills.py`) stages each scene. Action scenes ran on a
clock slowed 10× (as for the Reel) and were grabbed losslessly, so the frame with the Arc Storm's lightning could be
picked; `reel/source/capture/gal_extract.py` pulls the distinct frames out. Nothing was retouched.

## Rebuilding the artwork

From the repository root, with the `playwright` package and its Chromium:

```sh
NODE_PATH="$(npm root -g)" node marketing/source/render.cjs            # everything
NODE_PATH="$(npm root -g)" node marketing/source/render.cjs card-command  # one asset
```

`render.cjs` reads the screenshots above, `source/backdrop.jpg` (the card background, `warband.png` scaled down),
`source/warbanner.png` and `source/icons/` (the mod's own textures), and `source/fonts/` (Cinzel and EB Garamond, both
under the SIL Open Font License). The formation tiles on the command card are drawn from the slot offsets in
`common/.../core/Formation.java`; the configuration card prints the real `docs/reference/fellowship.json` and
`classes/bulwark.json`. The supported versions and loaders are one list at the top of `render.cjs`.

## Posting checklist

- **Icon:** `images/icon.png` on both stores.
- **CurseForge:** summary from `summary.txt`, body from `description.md`, gallery as above. Categories: Adventure and
  RPG, Mobs, Armor/Tools/Weapons.
- **Modrinth:** same body and gallery, `battle-storm.png` as the featured image. Tags: adventure, mobs, game-mechanics,
  equipment. Environment: required on client and server.
- **Reel:** see `reel/README.md`.
