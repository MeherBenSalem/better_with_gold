#!/usr/bin/env python3
"""Pixel-art GUI icons for Remnants | Fellowship, drawn in code so every Minecraft version shares them.

    python3 tools/gen_icons.py [preview.png]

Writes 1.21.1/common/src/main/java/tn/nightbeam/better_with_gold/client/Icons.java (copied to the other versions by
tools/sync-shared.sh). Each icon is 16x16; 'c', 'C' and 'x' take the tint passed when drawing (a class colour), the
other letters are fixed colours. With a file name it also writes a preview sheet at 8x.
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "1.21.1/common/src/main/java/tn/nightbeam/better_with_gold/client/Icons.java"

PALETTE = {
    "o": 0x1B1410, "k": 0x3A3C44, "K": 0x5C606C, "s": 0x8C93A2, "S": 0xD8DCE4,
    "g": 0xA8782A, "G": 0xE8B63E, "H": 0xFFE89A, "w": 0x6E4A2C, "W": 0xA8784A,
    "r": 0xB8352A, "R": 0xFF6B5A, "e": 0x3A9A48, "E": 0x8EE07A, "b": 0x3468C8, "B": 0x8EC2FF,
    "p": 0x7446BE, "P": 0xC8A6FF, "y": 0x58E0FF, "f": 0xF4F1E8, "n": 0xE2D6BC, "N": 0xA89C84, "d": 0x262230,
}
TINT = {"c": 1.0, "C": 1.45, "x": 0.6}

ICONS = {
    # ---- roles (class emblems) ----
    "tank": [
        "................",
        "..oooooooooooo..",
        ".oSSSSSSSSSSSSo.",
        ".oSccccccccccso.",
        ".oSccccHHccccso.",
        ".oScccHGGgcccso.",
        ".oSccHGGGGgccso.",
        ".oScccgGGgcccso.",
        ".oSccccggccccso.",
        ".oSccccccccccso.",
        "..oSccccccccso..",
        "..oSxccccccxso..",
        "...oSxccccxso...",
        "....oSxccxso....",
        ".....oSxxso.....",
        "......oooo......",
    ],
    "melee": [
        "oo............oo",
        "oSo..........oSo",
        "osSo........oSso",
        ".osSo......oSso.",
        "..osSo....oSso..",
        "...osSo..oSso...",
        "....osSooSso....",
        ".....osSSso.....",
        ".....oSssSo.....",
        "....oSsooSso....",
        ".o.oSso..osSo.o.",
        "oGooso....osooGo",
        ".oGGo......oGGo.",
        ".owoGo....oGowo.",
        "owo.oo....oo.owo",
        "oo............oo",
    ],
    "ranged": [
        "........oooo....",
        ".......oWWwo....",
        "......oWwoo.....",
        ".....oWwo.......",
        "....oWwo.....oo.",
        "...oWwo.....oHGo",
        "...oWo.....oGGo.",
        "..oWwo....onfo..",
        "..oWo....onfo...",
        "..oWo...onfo....",
        "..oWwo.onfo.....",
        "...oWoonfo......",
        "...oWwofo.......",
        "....oWwoo.......",
        ".....oWWwo......",
        "......oooo......",
    ],
    "healer": [
        "..........oo....",
        ".........oEeo...",
        "........oEEeo.H.",
        ".......oEEeeoHGH",
        "......oEEeeeo.H.",
        ".....oEEeeeeo...",
        "....oEEeEeeeo...",
        "...oEEeEeeeeo...",
        "...oEeEeeeeo....",
        "..oEeEeeeeeo....",
        "..oEEeeeeeo.....",
        "..oEeeeeeo......",
        ".oweoeeoo.......",
        ".owooo..........",
        "owo.............",
        "oo..............",
    ],
    "caster": [
        ".......oo.......",
        ".......oPo......",
        "......oPPo......",
        "......oPpo......",
        "..o...oPpo...o..",
        "..oPo.oPpo.oPo..",
        "...oPPoPpoPPo...",
        "oooooPPyyPpooooo",
        "oPPPPpyffypppppo",
        "oooooppyypPooooo",
        "...oPpoPpoPPo...",
        "..oPo.oPpo.oPo..",
        "..o...oPpo...o..",
        "......oPpo......",
        "......oppo......",
        ".......oo.......",
    ],
    # ---- orders ----
    "follow": [
        "................",
        ".......oo.......",
        "......oHGo......",
        ".....oHGGgo.....",
        "....oHGoogGo....",
        "...oHGo..ogGo...",
        "..oHGo....ogGo..",
        "..ooo..oo..ooo..",
        ".......HG.......",
        "......oHGo......",
        ".....oHGGgo.....",
        "....oHGoogGo....",
        "...oHGo..ogGo...",
        "..oHGo....ogGo..",
        "..ooo......ooo..",
        "................",
    ],
    "charge": [
        "...........oooo.",
        "..........oSSSo.",
        ".........oSSso..",
        "........oSSso...",
        ".......oSSso....",
        "......oSSso.....",
        ".o...oSSso......",
        "..o.oSSso.......",
        "...oSSso........",
        "..ooGso.........",
        ".oGGGo..........",
        "..owGo..........",
        ".owo.oo.........",
        "owo.............",
        "Go..............",
        "................",
    ],
    "focus": [
        "......oooo......",
        "....ooRrrRoo....",
        "...oRo.oo.oRo...",
        "..oRo..rr..oRo..",
        ".oRo...rr...oRo.",
        ".oro........oro.",
        "oRo..........oRo",
        "oRrrr..RR..rrrRo",
        "oRrrr..RR..rrrRo",
        "oRo..........oRo",
        ".oro........oro.",
        ".oRo...rr...oRo.",
        "..oRo..rr..oRo..",
        "...oRo.oo.oRo...",
        "....ooRrrRoo....",
        "......oooo......",
    ],
    "hold": [
        ".......oo.......",
        "......oSSo......",
        "......oSso......",
        "......oSso......",
        "....oooSsooo....",
        "...oSSSSssssSo..",
        "....oooSsooo....",
        "......oSso......",
        "......oSso......",
        "..oo..oSso..oo..",
        ".oSo..oSso..oso.",
        ".oSso.oSso.osso.",
        "..oSsooSsoossso.",
        "...oSSSSssssso..",
        "....ooosssooo...",
        ".......oo.......",
    ],
    "fall_back": [
        "................",
        "................",
        "....oo..........",
        "...oBo..........",
        "..oBBooooooo....",
        ".oBBBBBBBBBBoo..",
        "oBBBBBBBBBBBbbo.",
        ".obBBoooooooobbo",
        "..obBo......obbo",
        "...obo......obbo",
        "....oo......obbo",
        "...........obbbo",
        ".....oooooobbbo.",
        ".....obbbbbbbo..",
        ".....oooooooo...",
        "................",
    ],
    "guard_banner": [
        "..o.............",
        ".oHo............",
        "oGGGGGGGGGGGGGGo",
        ".oWwoGGGGGGGGGo.",
        ".oWwogddddddgo..",
        ".oWwogddHGddgo..",
        ".oWwogdHGGgdgo..",
        ".oWwogdGGGgdgo..",
        ".oWwogddggddgo..",
        ".oWwogddddddgo..",
        ".oWwogdddddddo..",
        ".oWwogddoodddo..",
        ".oWwoggdo.oddo..",
        ".oWwo.ooo..ooo..",
        ".oWwo...........",
        ".oooo...........",
    ],
    "formation": [
        "................",
        "......oooo......",
        ".....oHHGgo.....",
        ".....oHGGgo.....",
        ".....oGGggo.....",
        "......oooo......",
        "................",
        "................",
        "................",
        ".oooo......oooo.",
        "oSSSso....oSSSso",
        "oSSsso....oSSsso",
        "osssso....osssso",
        ".oooo......oooo.",
        "................",
        "................",
    ],
    "recall": [
        "................",
        "................",
        "..ooo...........",
        ".oGHGo..........",
        ".oGddGo.........",
        ".ogddGWo........",
        "..ogGWWWoo......",
        "...oWWWWwwoo....",
        "....oWWWwwwwoo..",
        ".....ooWWwwwwgo.",
        ".......ooWwwGHo.",
        ".........oowGgo.",
        "...........ooo..",
        "................",
        "................",
        "................",
    ],
    # ---- stats and bars ----
    "skull": [
        "................",
        ".....oooooo.....",
        "....onnnnnno....",
        "...onffffnnno...",
        "..onffffffnnno..",
        "..onfffffnnnNo..",
        "..onoofnnooNNo..",
        "..onoofnnooNNo..",
        "..onfffonnnnNo..",
        "...onffnnnnNo...",
        "....onnooonNo...",
        "....onononoNo...",
        ".....oNNNNNo....",
        "......ooooo.....",
        "................",
        "................",
    ],
    "crown": [
        "................",
        "................",
        "................",
        "..o....oo....o..",
        ".oHo..oHGo..oGo.",
        ".oGGo.oGGo.oGgo.",
        ".oGGGoGrGgoGGgo.",
        ".oGGGGGGGGGGGgo.",
        ".oGHGGbGGrGGGgo.",
        ".oGGGGGGGGGGGgo.",
        ".ogggggggggggo..",
        "..ooooooooooo...",
        "................",
        "................",
        "................",
        "................",
    ],
    "heart": [
        "................",
        "................",
        "..ooo....ooo....",
        ".oRRro..oRrro...",
        "oRffRroorRRrro..",
        "oRfRRRrrRRRrro..",
        "oRRRRRRRRRRrro..",
        "oRRRRRRRRRRrro..",
        ".oRRRRRRRRrro...",
        "..oRRRRRRrro....",
        "...oRRRRrro.....",
        "....oRRrro......",
        ".....oRro.......",
        "......oo........",
        "................",
        "................",
    ],
    "star": [
        "................",
        ".......oo.......",
        "......oHGo......",
        "......oHGo......",
        ".....oHHGgo.....",
        "oooooHHGGGgooooo",
        "oHHHHHHGGGGGGGgo",
        ".oGGGGGGGGGGGgo.",
        "..oGGGGGGGGGgo..",
        "...oGGGGGGGgo...",
        "...oGGGgGGGgo...",
        "..oGGgoooGGGgo..",
        "..oGgo...ogGgo..",
        ".oGgo.....ogGo..",
        ".ooo.......ooo..",
        "................",
    ],
    "xp": [
        "................",
        ".......oo.......",
        "......oBbo......",
        "......oBbo......",
        ".....oBBbbo.....",
        "oooooBBffbbooooo",
        "oBBBBBffffbbbbbo",
        ".obBBBBffbbbbbo.",
        "..obBBBBbbbbbo..",
        "...obBBBbbbbo...",
        "...obBBbbbbbo...",
        "..obBbooobBbbo..",
        "..obbo...obbbo..",
        ".obbo.....obbo..",
        ".ooo.......ooo..",
        "................",
    ],
    "flame": [
        "......o.........",
        ".....oGo........",
        ".....oGGo.......",
        "....oGHGo..o....",
        "....oGHGGooGo...",
        "...oGHHGGGoGGo..",
        "..oRGHHGGGGGGo..",
        "..oRGHfHGGGGRo..",
        ".oRGHffHGGGGRro.",
        ".oRGHffHHGGGRro.",
        ".oRGGHfHHGGRRro.",
        ".oRRGHHHGGGRrro.",
        "..oRRGGGGRRrro..",
        "...oRRRRRrrro...",
        "....oooooooo....",
        "................",
    ],
    "down": [
        "................",
        "......oooo......",
        "......oRro......",
        "......oRro......",
        "......oRro......",
        "......oRro......",
        "..ooooORroooo...",
        "..oRRRRRrrrro...",
        "...oRRRRrrro....",
        "....oRRrrro.....",
        ".....oRrro......",
        "......oro.......",
        ".......o........",
        "................",
        "................",
        "................",
    ],
    "revive": [
        "................",
        ".......o........",
        "......oEo.......",
        ".....oEEeo......",
        "....oEEEeeo.....",
        "...oEEEeeeeo....",
        "..oooEEeeooo....",
        "....oEEeeo..oHo.",
        "....oEEeeo.oHGHo",
        "....oEEeeo..oHo.",
        "....oEEeeo...o..",
        "....oEEeeo......",
        "....oooooo......",
        "................",
        "................",
        "................",
    ],
    "coin": [
        "................",
        "................",
        ".....oooooo.....",
        "....oHHGGGgo....",
        "...oHGGGGGGgo...",
        "...oHGGgGGGgo...",
        "...oGGgHgGGgo...",
        "...oGgHGGgGgo...",
        "...oGGgGggGgo...",
        "...oGGGggGGgo...",
        "...ogGGGGGggo...",
        "....oggggggo....",
        ".....oooooo.....",
        "................",
        "................",
        "................",
    ],
    "camp": [
        "................",
        "................",
        ".......oo.......",
        "......oWWo......",
        ".....oWwwWo.....",
        "....oWwddwWo....",
        "...oWwdddwwWo...",
        "..oWwddddwwwWo..",
        ".oWwdddGddwwwWo.",
        "oWwddoGHGowwwwWo",
        "oooooooooooooooo",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
}


def _converge():
    g = [["."] * 16 for _ in range(16)]

    def put(x, y, ch):
        if 0 <= x < 16 and 0 <= y < 16:
            g[y][x] = ch
    for sx, sy in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        def at(i, j, ch):
            put(i if sx > 0 else 15 - i, j if sy > 0 else 15 - j, ch)
        for t in range(5):
            at(1 + t, 1 + t, "B")
            at(2 + t, 1 + t, "b")
        for i, j in ((3, 6), (4, 6), (5, 6), (6, 6), (6, 5), (6, 4), (6, 3)):
            at(i, j, "B")
    for i, j, ch in ((7, 6, "H"), (8, 6, "G"), (6, 7, "H"), (7, 7, "H"), (8, 7, "G"), (9, 7, "g"), (6, 8, "G"), (7, 8, "G"),
                     (8, 8, "g"), (9, 8, "g"), (7, 9, "g"), (8, 9, "g")):
        put(i, j, ch)
    out = [row[:] for row in g]
    for y in range(16):
        for x in range(16):
            if g[y][x] == ".":
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and g[ny][nx] != ".":
                        out[y][x] = "o"
                        break
    return ["".join(r) for r in out]


ICONS["recall"] = _converge()


def check():
    for name, rows in ICONS.items():
        assert len(rows) == 16, (name, len(rows))
        for i, r in enumerate(rows):
            assert len(r) == 16, (name, i, len(r), r)
            for ch in r:
                assert ch == "." or ch in PALETTE or ch in TINT or ch == "O", (name, ch)


def preview(path, tint=0x3E6FB8):
    from PIL import Image, ImageDraw
    S = 8
    names = list(ICONS)
    cols = 8
    rows = (len(names) + cols - 1) // cols
    img = Image.new("RGBA", (cols * (16 * S + 16) + 16, rows * (16 * S + 34) + 16), (20, 22, 32, 255))
    d = ImageDraw.Draw(img)
    for k, name in enumerate(names):
        ox = 16 + (k % cols) * (16 * S + 16)
        oy = 16 + (k // cols) * (16 * S + 34)
        for j, row in enumerate(ICONS[name]):
            for i, ch in enumerate(row):
                col = color(ch, tint)
                if col is None:
                    continue
                d.rectangle((ox + i * S, oy + j * S, ox + i * S + S - 1, oy + j * S + S - 1), fill=((col >> 16) & 255, (col >> 8) & 255, col & 255, 255))
        d.text((ox, oy + 16 * S + 4), name, fill=(220, 220, 220, 255))
    img.save(path)


def color(ch, tint):
    if ch == ".":
        return None
    if ch == "O":
        ch = "o"
    if ch in PALETTE:
        return PALETTE[ch]
    k = TINT[ch]
    r, g, b = (tint >> 16) & 255, (tint >> 8) & 255, tint & 255
    if k > 1:
        t = k - 1
        r, g, b = r + (255 - r) * t * 0.6, g + (255 - g) * t * 0.6, b + (255 - b) * t * 0.6
    else:
        r, g, b = r * k, g * k, b * k
    return (int(r) << 16) | (int(g) << 8) | int(b)


def java():
    lines = []
    for name, rows in ICONS.items():
        const = name.upper()
        body = ",\n\t\t\t".join('"' + r.replace("O", "o") + '"' for r in rows)
        lines.append(f"\tpublic static final Icon {const} = new Icon(\n\t\t\t{body});\n")
    pal = "\n".join(f"\t\t\tcase '{k}' -> 0x{v:06X};" for k, v in PALETTE.items())
    src = f'''package tn.nightbeam.better_with_gold.client;

/**
 * Pixel-art icons for the Fellowship screens and HUD, drawn as coloured squares on a Canvas so they look the same
 * on every Minecraft version. Generated by tools/gen_icons.py; edit the art there.
 */
public final class Icons {{
	/** A 16x16 icon. 'c', 'C' and 'x' are the tint (normal, light, dark); the other letters are fixed colours. */
	public record Icon(String... rows) {{
	}}

{chr(10).join(lines)}
	private Icons() {{
	}}

	/** The emblem for a class role: TANK, MELEE, RANGED, HEALER or CASTER. */
	public static Icon role(String role) {{
		return switch (role == null ? "" : role.toUpperCase(java.util.Locale.ROOT)) {{
			case "TANK" -> TANK;
			case "MELEE" -> MELEE;
			case "RANGED" -> RANGED;
			case "HEALER" -> HEALER;
			default -> CASTER;
		}};
	}}

	/** The icon for an order id (follow, charge, focus, hold, fall_back, guard_banner) or wheel action. */
	public static Icon order(String id) {{
		return switch (id == null ? "" : id) {{
			case "charge" -> CHARGE;
			case "focus" -> FOCUS;
			case "hold" -> HOLD;
			case "fall_back" -> FALL_BACK;
			case "guard_banner" -> GUARD_BANNER;
			case "formation" -> FORMATION;
			case "recall" -> RECALL;
			default -> FOLLOW;
		}};
	}}

	/** Draws an icon with its top-left at (x, y), each icon pixel {{@code px}} GUI units wide. */
	public static void draw(Canvas c, Icon icon, float x, float y, float px, int tint, float alpha) {{
		String[] rows = icon.rows();
		for (int j = 0; j < rows.length; j++) {{
			String row = rows[j];
			int i = 0;
			while (i < row.length()) {{
				char ch = row.charAt(i);
				if (ch == '.') {{
					i++;
					continue;
				}}
				// merge runs of the same colour into one quad
				int end = i + 1;
				while (end < row.length() && row.charAt(end) == ch) {{
					end++;
				}}
				c.fill(x + i * px, y + j * px, x + end * px, y + (j + 1) * px, Canvas.alpha(color(ch, tint), alpha));
				i = end;
			}}
		}}
	}}

	public static void draw(Canvas c, Icon icon, float x, float y, float size) {{
		draw(c, icon, x, y, size / 16f, 0xC8A04A, 1);
	}}

	private static int color(char ch, int tint) {{
		return switch (ch) {{
{pal}
			case 'C' -> Canvas.mix(tint, 0xFFFFFF, 0.27F);
			case 'x' -> Canvas.mix(tint, 0x000000, 0.4F);
			default -> tint & 0xFFFFFF;
		}};
	}}
}}
'''
    JAVA.write_text(src)


if __name__ == "__main__":
    check()
    if len(sys.argv) > 1:
        preview(sys.argv[1])
    java()
    print("wrote", JAVA)
