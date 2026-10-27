#!/usr/bin/env python3
"""Generates Fellowship's original textures: 20 ally skins (5 classes x 4 variants, 64x64 player layout) and the
Warbanner item icon. Deterministic: the same script always writes the same pixels.

    python3 tools/gen_textures.py
"""
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "shared-resources/assets/better_with_gold/textures"

SKIN = [(242, 206, 175), (222, 170, 126), (172, 116, 80), (106, 70, 46)]
HAIR = [(66, 44, 28), (28, 22, 20), (201, 158, 86), (146, 62, 32), (176, 176, 170)]

# Player skin layout (64x64, 4-pixel arms). Each box: origin and size (w, h, d) as in the vanilla UV map.
BOXES = {
    "head": ((0, 0), (8, 8, 8)),
    "hat": ((32, 0), (8, 8, 8)),
    "body": ((16, 16), (8, 12, 4)),
    "jacket": ((16, 32), (8, 12, 4)),
    "rarm": ((40, 16), (4, 12, 4)),
    "rsleeve": ((40, 32), (4, 12, 4)),
    "larm": ((32, 48), (4, 12, 4)),
    "lsleeve": ((48, 48), (4, 12, 4)),
    "rleg": ((0, 16), (4, 12, 4)),
    "rpants": ((0, 32), (4, 12, 4)),
    "lleg": ((16, 48), (4, 12, 4)),
    "lpants": ((0, 48), (4, 12, 4)),
}


def faces(box):
    """The six faces of a box as (x, y, w, h)."""
    (u, v), (w, h, d) = BOXES[box]
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


class Painter:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rng = random.Random(seed)

    def put(self, x, y, color, alpha=255):
        r, g, b = color[:3]
        self.px[x, y] = (max(0, min(255, int(r))), max(0, min(255, int(g))), max(0, min(255, int(b))), alpha)

    def shade(self, color, k):
        return tuple(c * k for c in color[:3])

    def fill(self, rect, color, noise=6, alpha=255):
        x, y, w, h = rect
        for i in range(w):
            for j in range(h):
                n = self.rng.uniform(-noise, noise)
                self.put(x + i, y + j, tuple(c + n for c in color), alpha)

    def box(self, name, color, noise=6, side_shade=0.82, alpha=255):
        f = faces(name)
        for key, rect in f.items():
            k = {"top": 1.08, "bottom": 0.7, "front": 1.0, "back": 0.9, "right": side_shade, "left": side_shade}[key]
            self.fill(rect, self.shade(color, k), noise, alpha)

    def rows(self, name, face, y0, y1, color, noise=5, alpha=255):
        x, y, w, h = faces(name)[face]
        self.fill((x, y + y0, w, y1 - y0), color, noise, alpha)

    def around(self, name, y0, y1, color, noise=5, alpha=255, faces_list=("front", "back", "left", "right")):
        for face in faces_list:
            self.rows(name, face, y0, y1, color, noise, alpha)

    def dot(self, name, face, x, y, color, alpha=255):
        fx, fy, w, h = faces(name)[face]
        self.put(fx + x, fy + y, color, alpha)

    def edge_shadow(self, name, face, k=0.8):
        fx, fy, w, h = faces(name)[face]
        for j in range(h):
            for i in (0, w - 1):
                r, g, b, a = self.px[fx + i, fy + j]
                if a:
                    self.put(fx + i, fy + j, (r * k, g * k, b * k), a)


def head(p, skin, hair, hood=None, style=0):
    p.box("head", skin, noise=4)
    # Hair: top, back, sides, fringe.
    p.rows("head", "top", 0, 8, hair)
    p.rows("head", "back", 0, 7 if style != 2 else 8, hair)
    for side in ("left", "right"):
        p.rows("head", side, 0, 3 if style != 2 else 6, hair)
    p.rows("head", "front", 0, 2, hair)
    for x in (0, 7):
        p.dot("head", "front", x, 2, hair)
    if style == 1:
        for x in (1, 2):
            p.dot("head", "front", x, 2, hair)
    dark = p.shade(hair, 0.7)
    # Brows, eyes, nose, mouth.
    for x in (1, 2, 5, 6):
        p.dot("head", "front", x, 3, dark)
    p.dot("head", "front", 1, 4, (238, 238, 238))
    p.dot("head", "front", 2, 4, (40, 70, 110) if style != 1 else (70, 50, 30))
    p.dot("head", "front", 5, 4, (40, 70, 110) if style != 1 else (70, 50, 30))
    p.dot("head", "front", 6, 4, (238, 238, 238))
    for x in (3, 4):
        p.dot("head", "front", x, 5, p.shade(skin, 0.86))
        p.dot("head", "front", x, 6, p.shade(skin, 0.66))
    if hood:
        hood_dark = p.shade(hood, 0.75)
        p.box("hat", hood, noise=6)
        hx, hy, w, h = faces("hat")["front"]
        for i in range(w):
            for j in range(h):
                inside = 1 <= i <= 6 and 2 <= j <= 7
                if inside:
                    p.px[hx + i, hy + j] = (0, 0, 0, 0)
                elif j >= 1 and (i in (1, 6) or j == 1):
                    p.put(hx + i, hy + j, hood_dark)
        bx, by, w, h = faces("hat")["bottom"]
        for i in range(w):
            for j in range(h):
                p.px[bx + i, by + j] = (0, 0, 0, 0)


def legs(p, trousers, boots):
    for leg in ("rleg", "lleg"):
        p.box(leg, trousers)
        p.around(leg, 8, 12, boots, noise=4)
        p.rows(leg, "bottom", 0, 4, p.shade(boots, 0.7))


def arms(p, sleeve, cuff, hand):
    for arm in ("rarm", "larm"):
        p.box(arm, sleeve)
        p.around(arm, 8, 10, cuff, noise=3)
        p.around(arm, 10, 12, hand, noise=3)
        p.rows(arm, "bottom", 0, 4, hand)


def bulwark(p, skin, hair, v):
    steel = (136, 141, 150)
    tabard = (63, 111, 181)
    gold = (227, 179, 65)
    head(p, skin, hair, style=v % 3)
    if v % 2 == 1:
        # Half-helm on the hat layer.
        p.rows("hat", "front", 0, 2, steel, noise=8)
        for side in ("left", "right", "back"):
            p.rows("hat", side, 0, 3, steel, noise=8)
        p.rows("hat", "top", 0, 8, steel, noise=8)
        p.dot("hat", "front", 3, 2, steel)
        p.dot("hat", "front", 4, 2, steel)
        p.dot("hat", "front", 3, 3, p.shade(steel, 0.7))
        p.dot("hat", "front", 4, 3, p.shade(steel, 0.7))
    p.box("body", steel, noise=10)
    p.rows("body", "front", 0, 12, tabard)
    p.rows("body", "back", 0, 12, tabard)
    for y in range(12):
        p.dot("body", "front", 0, y, gold)
        p.dot("body", "front", 7, y, gold)
    for x in range(2, 6):
        p.dot("body", "front", x, 4, gold)
    for y in range(2, 8):
        p.dot("body", "front", 3, y, gold)
        p.dot("body", "front", 4, y, gold)
    p.around("body", 8, 9, (74, 52, 34), noise=3)
    p.dot("body", "front", 3, 8, gold)
    p.dot("body", "front", 4, 8, gold)
    arms(p, steel, (90, 66, 44), skin)
    p.around("rarm", 0, 3, p.shade(steel, 1.15), noise=4)
    p.around("larm", 0, 3, p.shade(steel, 1.15), noise=4)
    legs(p, (58, 61, 68), (60, 44, 30))


def skirmisher(p, skin, hair, v):
    leather = (168, 50, 42)
    dark = (40, 34, 34)
    head(p, skin, hair, style=v % 3)
    band = (150, 34, 30)
    p.rows("hat", "front", 1, 2, band)
    for side in ("left", "right", "back"):
        p.rows("hat", side, 1, 2, band)
    p.dot("hat", "back", 3, 2, band)
    p.dot("hat", "back", 4, 3, band)
    if v % 2 == 0:
        p.dot("head", "front", 6, 5, p.shade(skin, 0.6))
        p.dot("head", "front", 6, 6, p.shade(skin, 0.6))
    p.box("body", (52, 46, 44))
    p.rows("body", "front", 0, 10, leather)
    p.rows("body", "back", 0, 10, leather)
    for y in range(0, 10, 3):
        for x in (2, 5):
            p.dot("body", "front", x, y + 1, p.shade(leather, 0.6))
    p.around("body", 8, 10, dark, noise=3)
    p.dot("body", "front", 6, 9, (200, 170, 60))
    arms(p, skin, (90, 60, 40), skin)
    p.around("rarm", 6, 10, (110, 74, 48), noise=4)
    p.around("larm", 6, 10, (110, 74, 48), noise=4)
    p.around("rarm", 0, 3, leather, noise=5)
    p.around("larm", 0, 3, leather, noise=5)
    legs(p, (88, 62, 43), (36, 30, 28))


def ranger(p, skin, hair, v):
    cloak = (62, 107, 58)
    tunic = (94, 125, 58)
    leather = (107, 74, 46)
    head(p, skin, hair, hood=cloak if v != 3 else None, style=v % 3)
    p.box("body", tunic)
    p.box("jacket", cloak, noise=8)
    jx, jy, w, h = faces("jacket")["front"]
    for i in range(2, 6):
        for j in range(0, 12):
            p.px[jx + i, jy + j] = (0, 0, 0, 0)
    for j in range(12):
        x = min(7, j * 7 // 11)
        p.dot("body", "front", x, j, leather)
        p.dot("body", "back", 7 - x, j, leather)
    p.around("body", 8, 9, p.shade(leather, 0.8), noise=3)
    arms(p, tunic, leather, skin)
    p.around("rarm", 0, 4, cloak, noise=6)
    p.around("larm", 0, 4, cloak, noise=6)
    legs(p, (78, 64, 44), (58, 42, 28))


def mender(p, skin, hair, v):
    robe = (232, 226, 210)
    gold = (217, 178, 60)
    head(p, skin, hair, hood=(222, 214, 194) if v in (1, 3) else None, style=v % 3)
    p.box("body", robe, noise=5)
    for y in range(12):
        p.dot("body", "front", 3, y, gold)
        p.dot("body", "front", 4, y, gold)
    p.around("body", 7, 8, gold, noise=3)
    p.dot("body", "front", 2, 2, (200, 60, 60))
    p.dot("body", "front", 5, 2, (200, 60, 60))
    arms(p, robe, gold, skin)
    for leg in ("rleg", "lleg"):
        p.box(leg, robe, noise=5)
        p.around(leg, 10, 12, (110, 80, 52), noise=3)
    for leg in ("rpants", "lpants"):
        p.around(leg, 0, 8, robe, noise=5)
        p.around(leg, 7, 8, gold, noise=2)


def arcanist(p, skin, hair, v):
    robe = (91, 58, 140)
    trim = (123, 85, 181)
    rune = (201, 162, 255)
    head(p, skin, hair, hood=(61, 37, 99) if v in (0, 2) else None, style=v % 3)
    p.box("body", robe, noise=6)
    for y in range(12):
        p.dot("body", "front", 0, y, trim)
        p.dot("body", "front", 7, y, trim)
    p.around("body", 6, 7, (40, 28, 60), noise=2)
    for (x, y) in ((2, 2), (5, 3), (3, 9), (5, 10), (2, 11)):
        p.dot("body", "front", x, y, rune)
    arms(p, robe, trim, skin)
    for leg in ("rleg", "lleg"):
        p.box(leg, robe, noise=6)
        p.around(leg, 10, 12, (40, 28, 60), noise=3)
    p.dot("rleg", "front", 1, 4, rune)
    p.dot("lleg", "front", 2, 7, rune)


CLASSES = {"bulwark": bulwark, "skirmisher": skirmisher, "ranger": ranger, "mender": mender, "arcanist": arcanist}


def skins():
    out = OUT / "entity/ally"
    out.mkdir(parents=True, exist_ok=True)
    for ci, (name, paint) in enumerate(CLASSES.items()):
        for v in range(4):
            p = Painter(seed=ci * 100 + v)
            skin = SKIN[(v + ci) % 4]
            hair = HAIR[(v * 3 + ci) % len(HAIR)]
            paint(p, skin, hair, v)
            for part in ("head", "body", "rarm", "larm", "rleg", "lleg"):
                for face in ("front", "back"):
                    p.edge_shadow(part, face, 0.86)
            p.img.save(out / f"{name}_{v}.png")


def warbanner_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    pole = (120, 88, 50, 255)
    pole_hi = (160, 120, 70, 255)
    cloth = (24, 22, 28, 255)
    cloth_hi = (44, 40, 50, 255)
    gold = (232, 186, 64, 255)
    gold_dark = (176, 132, 40, 255)
    for y in range(1, 16):
        px[3, y] = pole
        px[4, y] = pole_hi
    for x in range(2, 14):
        px[x, 1] = gold_dark
    px[2, 0] = gold
    px[13, 0] = gold
    for x in range(5, 13):
        for y in range(2, 12):
            px[x, y] = cloth if (x + y) % 5 else cloth_hi
    for x in range(5, 13):
        px[x, 2] = gold
    for y in range(2, 12):
        px[12, y] = gold
    for y in (12, 13):
        for x in range(5, 13):
            if (x - 5 + (y - 12)) % 3 != 2:
                px[x, y] = gold if y == 12 else gold_dark
    for (x, y) in ((8, 5), (9, 5), (7, 6), (10, 6), (7, 7), (10, 7), (8, 8), (9, 8), (8, 6), (9, 7)):
        px[x, y] = gold
    out = OUT / "item"
    out.mkdir(parents=True, exist_ok=True)
    img.save(out / "warbanner.png")


if __name__ == "__main__":
    skins()
    warbanner_icon()
    print("textures written to", OUT)
