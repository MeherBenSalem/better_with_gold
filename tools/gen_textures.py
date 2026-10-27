#!/usr/bin/env python3
"""Generates Remnants | Fellowship's original textures: 20 ally skins (5 classes x 4 variants, 64x64 player layout,
both layers) and the Warbanner and Gold Upgrade item icons. Deterministic: the same script always writes the same
pixels.

    python3 tools/gen_textures.py

How the skins are built: every box face is painted with a material (cloth, leather, chainmail, plate, skin...), then
details (faces, trims, emblems, straps) go on top, then one lighting pass shades every face as if lit from above and
in front (hue-shifted: shadows lean cool, highlights warm), adds a soft vertical gradient and darkens the seams.
Gear on the outer layer (helmets, hoods, pauldrons, cloaks, tabards, greaves) sits one pixel off the body in game,
so it reads as real armour and cloth.
"""
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "shared-resources/assets/better_with_gold/textures"

# Player skin layout (64x64, 4-pixel arms). Each box: UV origin and size (w, h, d).
BOXES = {
    "head": ((0, 0), (8, 8, 8)), "hat": ((32, 0), (8, 8, 8)),
    "body": ((16, 16), (8, 12, 4)), "jacket": ((16, 32), (8, 12, 4)),
    "rarm": ((40, 16), (4, 12, 4)), "rsleeve": ((40, 32), (4, 12, 4)),
    "larm": ((32, 48), (4, 12, 4)), "lsleeve": ((48, 48), (4, 12, 4)),
    "rleg": ((0, 16), (4, 12, 4)), "rpants": ((0, 32), (4, 12, 4)),
    "lleg": ((16, 48), (4, 12, 4)), "lpants": ((0, 48), (4, 12, 4)),
}
OVERLAY = {"hat", "jacket", "rsleeve", "lsleeve", "rpants", "lpants"}
SIDES = ("front", "back", "left", "right")
ALL = SIDES + ("top", "bottom")
ARMS = (("rarm", "rsleeve"), ("larm", "lsleeve"))
LEGS = (("rleg", "rpants"), ("lleg", "lpants"))

SKIN_TONES = [(245, 212, 184), (233, 188, 150), (206, 150, 108), (166, 110, 74), (122, 80, 52), (88, 58, 40)]
HAIR = {"black": (34, 28, 30), "brown": (92, 60, 36), "chestnut": (128, 70, 38), "auburn": (150, 62, 36),
        "blond": (214, 170, 92), "ash": (176, 168, 158), "white": (222, 220, 214), "red": (176, 70, 40)}
EYES = {"brown": (92, 58, 34), "blue": (58, 104, 168), "green": (70, 128, 70), "hazel": (128, 98, 50),
        "grey": (110, 118, 128), "violet": (190, 120, 255)}


def faces(box):
    (u, v), (w, h, d) = BOXES[box]
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}


def clamp(c):
    return tuple(max(0, min(255, int(round(x)))) for x in c[:3])


def mix(a, b, t):
    return tuple(a[i] * (1 - t) + b[i] * t for i in range(3))


def lit(c, k):
    """Hue-shifted shading: darker means cooler (bluer), brighter means warmer."""
    r, g, b = c[:3]
    if k < 1:
        t = 1 - k
        return (r * k * (1 - 0.10 * t), g * k * (1 - 0.03 * t), b * k * (1 + 0.12 * t) + 5 * t)
    t = k - 1
    return (r * k + 14 * t, g * k + 8 * t, b * k - 6 * t)


class Skin:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rng = random.Random(seed)
        self.seed = seed
        self.metal = set()   # pixels that get a specular glint
        self.glow = set()    # pixels that are not shaded (embroidery that glows)

    # ---- raw access -------------------------------------------------------------------------------------------
    def xy(self, part, face, i, j):
        x, y, w, h = faces(part)[face]
        if 0 <= i < w and 0 <= j < h:
            return x + i, y + j
        return None

    def set(self, part, face, i, j, color, alpha=255, metal=False, glow=False):
        p = self.xy(part, face, i, j)
        if p:
            self.px[p] = clamp(color) + (alpha,)
            (self.metal.add if metal else self.metal.discard)(p)
            (self.glow.add if glow else self.glow.discard)(p)

    def get(self, part, face, i, j):
        p = self.xy(part, face, i, j)
        return self.px[p] if p else (0, 0, 0, 0)

    def clear(self, part, face, i, j):
        p = self.xy(part, face, i, j)
        if p:
            self.px[p] = (0, 0, 0, 0)
            self.metal.discard(p)
            self.glow.discard(p)

    def size(self, part, face):
        return faces(part)[face][2:]

    def noise(self, x, y, scale=2):
        """Smooth-ish value noise in [-1, 1], stable per skin."""
        def h(a, b):
            return random.Random(self.seed * 7919 + a * 104729 + b * 1299709).uniform(-1, 1)
        gx, gy = x / scale, y / scale
        x0, y0 = math.floor(gx), math.floor(gy)
        tx, ty = gx - x0, gy - y0
        top = h(x0, y0) * (1 - tx) + h(x0 + 1, y0) * tx
        bot = h(x0, y0 + 1) * (1 - tx) + h(x0 + 1, y0 + 1) * tx
        return top * (1 - ty) + bot * ty

    # ---- painting ---------------------------------------------------------------------------------------------
    def paint(self, part, mat, face_list=ALL, rows=None, cols=None):
        """Paints a material on faces; rows/cols limit the band (side faces share rows; top/bottom take all)."""
        for face in face_list:
            w, h = self.size(part, face)
            for j in range(h):
                if rows and face in SIDES and not (rows[0] <= j < rows[1]):
                    continue
                for i in range(w):
                    if cols and face in ("front", "back") and not (cols[0] <= i < cols[1]):
                        continue
                    x, y = self.xy(part, face, i, j)
                    col = mat(self, part, face, i, j, w, h, x, y)
                    if col is None:
                        continue
                    if len(col) == 2 and isinstance(col[1], dict):
                        col, flags = col
                    else:
                        flags = {}
                    self.set(part, face, i, j, col, **flags)

    def band(self, part, rows, mat, face_list=SIDES):
        self.paint(part, mat, face_list, rows=rows)

    # ---- lighting ---------------------------------------------------------------------------------------------
    def light(self):
        base = {"top": 1.10, "front": 1.0, "right": 0.90, "left": 0.80, "back": 0.84, "bottom": 0.62}
        for part in BOXES:
            over = part in OVERLAY
            for face, (fx, fy, w, hh) in faces(part).items():
                for j in range(hh):
                    for i in range(w):
                        x, y = fx + i, fy + j
                        r, g, b, a = self.px[x, y]
                        if a == 0 or (x, y) in self.glow:
                            continue
                        k = base[face] + (0.04 if over else 0)
                        if face in SIDES:
                            if part in ("head", "hat"):
                                k *= 1.03 - 0.06 * j / (hh - 1)
                            elif part in ("rleg", "lleg", "rpants", "lpants"):
                                k *= (0.86 if j == 0 else 0.93 if j == 1 else 1.0) * (1.02 - 0.08 * j / (hh - 1))
                            else:
                                k *= 1.05 - 0.12 * j / (hh - 1)
                            if i == 0 or i == w - 1:
                                k *= 0.93
                        c = lit((r, g, b), k)
                        if (x, y) in self.metal and face in ("front", "top", "right"):
                            # a glint on the lit side of metal
                            if (face == "front" and i == 1) or (face == "top" and j == 1) or (face == "right" and i == w - 2):
                                c = mix(c, (255, 250, 236), 0.35)
                        self.px[x, y] = clamp(c) + (a,)

    def drop_shadow(self, over, base, face_list=SIDES):
        """Darkens the base-layer pixel just below where an overlay band ends, so gear casts a shadow."""
        for face in face_list:
            w, h = self.size(over, face)
            for i in range(w):
                for j in range(h - 1):
                    if self.get(over, face, i, j)[3] and not self.get(over, face, i, j + 1)[3]:
                        r, g, b, a = self.get(base, face, i, j + 1)
                        if a:
                            self.set(base, face, i, j + 1, lit((r, g, b), 0.8))


# ---- materials: functions (skin, part, face, i, j, w, h, x, y) -> colour ---------------------------------------
def solid(color, noise=3, lf=4):
    def m(s, part, face, i, j, w, h, x, y):
        n = s.rng.uniform(-noise, noise) + lf * s.noise(x, y, 2)
        return tuple(c + n for c in color)
    return m


def cloth(color, noise=4, weave=3, folds=0):
    def m(s, part, face, i, j, w, h, x, y):
        n = s.rng.uniform(-noise, noise) + 5 * s.noise(x, y, 3) + (weave if (i + j) % 2 else -weave) * 0.6
        k = 1.0
        if folds and face in ("front", "back") and i % folds == folds - 1:
            k = 0.9
        return tuple(c * k + n for c in color)
    return m


def leather(color):
    def m(s, part, face, i, j, w, h, x, y):
        n = s.rng.uniform(-6, 6) + 9 * s.noise(x, y, 2)
        return tuple(c + n for c in color)
    return m


def chainmail(color=(128, 132, 142)):
    def m(s, part, face, i, j, w, h, x, y):
        ring = (i + j) % 2 == 0
        k = 1.05 if ring else 0.9
        if j % 2 == 1:
            k = 0.98 if ring else 0.93
        return (tuple(c * k + s.rng.uniform(-4, 4) for c in color), {"metal": ring})
    return m


def plate(color=(150, 156, 168)):
    def m(s, part, face, i, j, w, h, x, y):
        k = 1.0 + 0.04 * s.noise(x, y, 4)
        return (tuple(c * k + s.rng.uniform(-2, 2) for c in color), {"metal": True})
    return m


def skin_mat(color):
    def m(s, part, face, i, j, w, h, x, y):
        return tuple(c + s.rng.uniform(-2, 2) + 2 * s.noise(x, y, 3) for c in color)
    return m


# ---- heads ------------------------------------------------------------------------------------------------------
def head(s, look):
    skin, hair, eyes = look["skin"], look["hair"], look["eyes"]
    s.paint("head", skin_mat(skin))
    hair_m = solid(hair, noise=5, lf=8)
    length = look.get("length", "short")
    back_rows = {"short": 4, "medium": 6, "long": 8, "bald": 0}[length]
    side_rows = {"short": 2, "medium": 4, "long": 7, "bald": 0}[length]
    if length != "bald":
        s.paint("head", hair_m, ("top",))
        s.band("head", (0, back_rows), hair_m, ("back",))
        for side in ("left", "right"):
            s.band("head", (0, side_rows), hair_m, (side,))
            # sideburn at the front edge of each side
            front_col = 7 if side == "right" else 0
            for j in range(side_rows, 4):
                s.set("head", side, front_col, j, hair)
        # hair strands: a darker streak pattern
        for i in range(8):
            if (i * 3 + s.seed) % 4 == 0:
                s.set("head", "top", i, 2 + i % 3, lit(hair, 0.82))
    else:
        s.set("head", "top", 2, 2, lit(skin, 1.15))
        s.set("head", "top", 3, 2, lit(skin, 1.1))
    fringe = look.get("fringe", "parted")
    rows = {"parted": [(0, range(8)), (1, (0, 1, 2, 5, 6, 7))],
            "swept": [(0, range(8)), (1, range(0, 5)), (2, (0,))],
            "short": [(0, range(8)), (1, (0, 7))],
            "bangs": [(0, range(8)), (1, range(8)), (2, (0, 7))],
            "none": []}[fringe if length != "bald" else "none"]
    for j, cols in rows:
        for i in cols:
            s.set("head", "front", i, j, lit(hair, 1.0 - 0.06 * j))
    brow = lit(hair, 0.72) if length != "bald" else lit(skin, 0.7)
    for i in (1, 2, 5, 6):
        s.set("head", "front", i, 3, brow)
    # eyes: white, iris with a catchlight row above
    s.set("head", "front", 1, 4, (236, 236, 232))
    s.set("head", "front", 6, 4, (236, 236, 232))
    glow = look.get("glow_eyes", False)
    s.set("head", "front", 2, 4, eyes, glow=glow)
    s.set("head", "front", 5, 4, eyes, glow=glow)
    # nose and cheeks
    s.set("head", "front", 3, 5, lit(skin, 0.92))
    s.set("head", "front", 4, 5, lit(skin, 0.86))
    s.set("head", "front", 1, 5, mix(skin, (220, 110, 100), 0.12))
    s.set("head", "front", 6, 5, mix(skin, (220, 110, 100), 0.12))
    # mouth
    lips = mix(lit(skin, 0.7), (150, 60, 60), 0.25)
    s.set("head", "front", 3, 6, lips)
    s.set("head", "front", 4, 6, lips)
    if look.get("smile"):
        s.set("head", "front", 2, 6, lit(skin, 0.82))
        s.set("head", "front", 5, 6, lit(skin, 0.82))
    for i in range(1, 7):
        s.set("head", "front", i, 7, lit(skin, 0.95))
    # ears
    for side in ("left", "right"):
        s.set("head", side, 3, 4, lit(skin, 0.9))
        s.set("head", side, 4, 4, lit(skin, 0.9))
        s.set("head", side, 3, 5, lit(skin, 0.75))
    beard = look.get("beard")
    bc = look.get("beard_color", hair)
    if beard == "full":
        for i in range(8):
            for j in (6, 7):
                if not (j == 6 and i in (3, 4)):
                    s.set("head", "front", i, j, lit(bc, 1.0 - 0.05 * (j - 6) + s.rng.uniform(-0.05, 0.05)))
        for i in (0, 1, 6, 7):
            s.set("head", "front", i, 5, bc)
        s.set("head", "front", 3, 6, lit(lips, 0.9))
        s.set("head", "front", 4, 6, lit(lips, 0.9))
        for side in ("left", "right"):
            for j in range(4, 8):
                s.set("head", side, 7 if side == "right" else 0, j, bc)
            for i in range(8):
                s.set("head", side, i, 7, lit(bc, 0.9)) if (side == "right" and i > 4) or (side == "left" and i < 3) else None
        for i in range(8):
            s.set("head", "bottom", i, 0, bc)
    elif beard == "stubble":
        for i in range(1, 7):
            for j in (6, 7):
                if (i + j) % 2 == 0 and not (j == 6 and i in (3, 4)):
                    s.set("head", "front", i, j, mix(skin, bc, 0.45))
    elif beard == "goatee":
        for (i, j) in ((3, 7), (4, 7), (2, 6), (5, 6)):
            s.set("head", "front", i, j, bc)
    if look.get("scar"):
        for (i, j) in ((5, 3), (6, 4), (6, 5)):
            s.set("head", "front", i, j, mix(skin, (210, 120, 120), 0.45))
        s.set("head", "front", 6, 4, (236, 236, 232))


def hood(s, color, trim=None, opening=(1, 6, 2, 7), up=True):
    """A hood on the hat layer: full cover with a shadowed face opening."""
    s.paint("hat", cloth(color, weave=2))
    x0, x1, y0, y1 = opening
    for i in range(8):
        for j in range(8):
            if x0 <= i <= x1 and y0 <= j <= y1:
                s.clear("hat", "front", i, j)
            elif x0 - 1 <= i <= x1 + 1 and y0 - 1 <= j <= y1:
                s.set("hat", "front", i, j, lit(color, 0.72) if trim is None else trim)
    for i in range(8):
        for j in range(8):
            s.clear("hat", "bottom", i, j)
    # deep shadow just inside the opening on the head itself
    for i in range(x0, x1 + 1):
        r, g, b, a = s.get("head", "front", i, y0)
        s.set("head", "front", i, y0, lit((r, g, b), 0.62))
    for j in range(y0, y1 + 1):
        for i in (x0, x1):
            r, g, b, a = s.get("head", "front", i, j)
            s.set("head", "front", i, j, lit((r, g, b), 0.78))
    # a peak at the back of the hood
    for i in range(2, 6):
        s.set("hat", "back", i, 0, lit(color, 1.08))
    for j in range(8):
        s.set("hat", "back", 3, j, lit(color, 0.86))


# ---- shared pieces -----------------------------------------------------------------------------------------------
def boots(s, color, rows=(8, 12), cuff=None):
    for leg, _ in LEGS:
        s.band(leg, rows, leather(color))
        s.paint(leg, leather(lit(color, 0.85)), ("bottom",))
        if cuff:
            s.band(leg, (rows[0], rows[0] + 1), leather(cuff))
        for face in ("front",):
            s.set(leg, face, 1, rows[1] - 1, lit(color, 1.2))


def belt(s, color, row, buckle=(214, 172, 70), part="body"):
    s.band(part, (row, row + 1), leather(color))
    s.set(part, "front", 3, row, buckle, metal=True)
    s.set(part, "front", 4, row, lit(buckle, 0.8), metal=True)


def emblem_diamond(s, part, face, cx, cy, gold):
    """A solid gold diamond: darker rim, bright core."""
    for i in range(-2, 3):
        for j in range(-2, 3):
            d = abs(i) + abs(j)
            if d == 2:
                s.set(part, face, cx + i, cy + j, lit(gold, 0.78), metal=True)
            elif d == 1:
                s.set(part, face, cx + i, cy + j, gold, metal=True)
    s.set(part, face, cx, cy, mix(gold, (255, 245, 200), 0.55), metal=True)


# ---- classes -------------------------------------------------------------------------------------------------------
GOLD = (222, 174, 62)


def bulwark(s, look, v):
    steel = (140, 146, 158)
    blue = (52, 96, 170)
    head(s, look)
    helm = ("none", "nasal", "bascinet", "kettle")[v]
    if helm == "nasal":
        s.paint("hat", plate(steel), ("top",))
        s.band("hat", (0, 3), plate(steel), ("left", "right", "back"))
        s.band("hat", (0, 2), plate(steel), ("front",))
        for j in range(2, 6):
            s.set("hat", "front", 3, j, steel, metal=True)
            s.set("hat", "front", 4, j, lit(steel, 0.85), metal=True)
        for i in range(8):
            s.set("hat", "front", i, 2 if i not in (3, 4) else 5, GOLD if i in (0, 7) else lit(steel, 0.75), metal=True)
    elif helm == "bascinet":
        s.paint("hat", plate(steel), ("top",))
        s.band("hat", (0, 2), plate(steel), SIDES)
        s.band("hat", (2, 8), chainmail(), ("left", "right", "back"))
        for j in range(2, 8):
            for i in (0, 7):
                s.set("hat", "front", i, j, chainmail()(s, "hat", "front", i, j, 8, 8, 0, j)[0], metal=True)
        for i in range(8):
            s.set("hat", "front", i, 7, chainmail()(s, "hat", "front", i, 7, 8, 8, i, 7)[0], metal=True)
        for i in range(3, 5):
            s.clear("hat", "front", i, 7)
        for i in range(8):
            s.set("hat", "front", i, 1, GOLD, metal=True)
    elif helm == "kettle":
        s.paint("hat", plate(steel), ("top",))
        s.band("hat", (0, 3), plate(steel), SIDES)
        s.band("hat", (2, 3), plate(lit(steel, 1.1)), SIDES)
        for i in range(8):
            s.set("hat", "top", i, 0, lit(steel, 1.15), metal=True)
    # torso: chainmail under a blue tabard with a gold border and diamond
    s.paint("body", chainmail())
    s.paint("jacket", cloth(blue, weave=2), ("front", "back"), cols=(1, 7))
    for face in ("front", "back"):
        for j in range(12):
            s.set("jacket", face, 1, j, GOLD, metal=True)
            s.set("jacket", face, 6, j, GOLD, metal=True)
        for i in range(1, 7):
            s.set("jacket", face, i, 11, GOLD, metal=True)
    for i in (0, 7):
        for j in range(12):
            s.clear("jacket", "front", i, j)
            s.clear("jacket", "back", i, j)
    emblem_diamond(s, "jacket", "front", 3, 4, GOLD)
    emblem_diamond(s, "jacket", "back", 4, 4, GOLD)
    belt(s, (74, 50, 32), 8, part="jacket")
    for face in ("left", "right"):
        s.set("jacket", face, 0, 8, (74, 50, 32))
        s.set("jacket", face, 3, 8, (74, 50, 32))
        s.set("jacket", face, 1, 8, (74, 50, 32))
        s.set("jacket", face, 2, 8, (74, 50, 32))
    # arms: mail with plate pauldrons and gauntlets
    for arm, sleeve in ARMS:
        s.paint(arm, chainmail())
        s.band(sleeve, (0, 4), plate(steel))
        s.paint(sleeve, plate(lit(steel, 1.08)), ("top",))
        s.band(sleeve, (3, 4), solid(GOLD, 2, 2))
        for face in SIDES:
            s.set(sleeve, face, 1, 1, lit(steel, 1.25), metal=True)
        s.band(arm, (9, 12), plate(lit(steel, 0.92)))
        s.band(sleeve, (8, 9), solid(lit(GOLD, 0.9), 2, 2))
        s.drop_shadow(sleeve, arm)
    # legs: mail, tabard skirt flaps, plate greaves and steel-capped boots
    for leg, pants in LEGS:
        s.paint(leg, chainmail())
        s.band(pants, (0, 3), cloth(blue, weave=2), ("front", "back"))
        s.band(pants, (2, 3), solid(GOLD, 2, 2), ("front", "back"))
        s.band(pants, (5, 11), plate(steel))
        s.band(pants, (5, 6), plate(lit(steel, 1.15)))
        s.drop_shadow(pants, leg)
    boots(s, (66, 48, 34), rows=(10, 12))


def skirmisher(s, look, v):
    red = (176, 52, 44)
    shirt = (58, 52, 56)
    dark = (44, 34, 32)
    brass = (206, 160, 70)
    head(s, look)
    if v == 0:      # headband with tails
        s.band("hat", (1, 3), cloth(red, weave=2))
        for j in range(3, 7):
            s.set("hat", "back", 5, j, lit(red, 0.95 - 0.04 * j))
        s.set("hat", "back", 6, 4, lit(red, 0.85))
        s.set("hat", "back", 4, 5, lit(red, 0.8))
    elif v == 1:    # eyepatch and a bandana over the whole crown
        s.paint("hat", cloth(red, weave=2), ("top",))
        s.band("hat", (0, 3), cloth(red, weave=2), SIDES)
        for j in range(3, 6):
            s.set("hat", "back", 3, j, lit(red, 0.9))
            s.set("hat", "back", 4, j, lit(red, 0.8))
        s.set("head", "front", 5, 4, (24, 20, 22))
        s.set("head", "front", 6, 4, (24, 20, 22))
        s.set("head", "front", 5, 3, (30, 26, 28))
        for i in (0, 7):
            s.set("head", "front", i, 3, (30, 26, 28))
    elif v == 2:    # topknot
        for i in (3, 4):
            for j in (3, 4):
                s.set("hat", "top", i, j, lit(look["hair"], 1.1))
        s.set("hat", "back", 3, 0, look["hair"])
        s.set("hat", "back", 4, 0, look["hair"])
        s.band("hat", (0, 1), cloth(red), ("back",))
    else:           # face mask
        s.band("hat", (5, 8), cloth(lit(red, 0.85), weave=2), SIDES)
        for i in range(8):
            s.set("hat", "front", i, 5, lit(red, 0.75))
    # torso: dark shirt, red leather jerkin with lacing, crossed baldrics
    s.paint("body", cloth(shirt))
    s.paint("jacket", leather(red), ("front", "back", "left", "right", "top"), rows=(0, 10))
    for j in range(1, 7):
        s.set("jacket", "front", 3, j, lit(red, 0.62) if j % 2 else (196, 170, 120))
        s.set("jacket", "front", 4, j, (196, 170, 120) if j % 2 else lit(red, 0.62))
    for j in range(10):
        a = min(7, j * 7 // 9)
        s.set("jacket", "front", a, j, dark)
        s.set("jacket", "front", 7 - a, j, dark)
        s.set("jacket", "back", a, j, dark)
    s.set("jacket", "front", 3, 4, brass, metal=True)
    belt(s, (30, 24, 24), 8, buckle=brass, part="jacket")
    s.band("jacket", (9, 10), leather(lit(red, 0.8)))
    for j in range(9, 12):     # sash on the right hip
        s.set("jacket", "front", 6, j, lit(red, 1.05 - 0.05 * j))
    s.set("jacket", "front", 5, 11, lit(red, 0.75))
    # arms: rolled shirt sleeves, bare forearms, leather bracers and fingerless gloves
    for arm, sleeve in ARMS:
        s.band(arm, (0, 5), cloth(shirt))
        s.paint(arm, cloth(shirt), ("top",))
        s.band(arm, (4, 5), cloth(lit(shirt, 1.25)))
        s.band(arm, (5, 12), skin_mat(look["skin"]))
        s.paint(arm, skin_mat(look["skin"]), ("bottom",))
        s.band(sleeve, (7, 10), leather((112, 74, 46)))
        s.band(sleeve, (8, 9), leather((70, 46, 30)))
        s.band(arm, (11, 12), leather(dark))
        s.band(sleeve, (0, 3), leather(red))
        s.drop_shadow(sleeve, arm)
    for leg, pants in LEGS:
        s.paint(leg, cloth((84, 60, 44)))
        s.band(pants, (5, 7), cloth((190, 168, 128), weave=2))
        s.set(pants, "front", 1, 6, (150, 128, 96))
        s.drop_shadow(pants, leg)
    boots(s, (34, 28, 28), rows=(8, 12), cuff=(58, 46, 40))


def ranger(s, look, v):
    green = (58, 104, 54)
    tunic = (98, 128, 60)
    brown = (110, 76, 46)
    head(s, look)
    hooded = v in (0, 1, 3)
    if hooded:
        hood(s, green)
    else:   # hood down: folds at the neck
        s.band("hat", (6, 8), cloth(green, weave=2), ("back", "left", "right"))
        for i in range(8):
            s.set("hat", "back", i, 5, lit(green, 0.9))
    # torso: tunic, leather vest edges, cloak on the back and shoulders, quiver strap and quiver
    s.paint("body", cloth(tunic, folds=4))
    for j in range(12):
        for i in (0, 7):
            s.set("body", "front", i, j, lit(brown, 0.95))
    s.paint("jacket", cloth(green, weave=2, folds=3), ("back", "left", "right", "top"))
    for j in range(12):
        s.set("jacket", "front", 0, j, lit(green, 1.0))
        s.set("jacket", "front", 7, j, lit(green, 0.92))
    s.set("jacket", "front", 1, 0, GOLD, metal=True)
    s.set("jacket", "front", 6, 0, GOLD, metal=True)
    for i in range(1, 7):
        s.set("jacket", "front", i, 0, lit(green, 0.85)) if i not in (1, 6) else None
    for j in range(12):          # strap across the chest
        i = 6 - min(6, j * 6 // 11)
        s.set("jacket", "front", i, j, lit(brown, 0.9))
        if j == 5:
            s.set("jacket", "front", i, j, (200, 170, 90), metal=True)
    for j in range(0, 10):       # quiver on the back
        for i in (5, 6):
            s.set("jacket", "back", i, j, leather(brown)(s, "jacket", "back", i, j, 8, 12, i, j))
    for i, col in ((5, (236, 232, 222)), (6, (196, 60, 52))):
        s.set("jacket", "back", i, 0, col)
    s.set("jacket", "back", 5, 1, (236, 232, 222))
    belt(s, (82, 56, 34), 8)
    for arm, sleeve in ARMS:
        s.paint(arm, cloth(tunic))
        s.band(arm, (7, 11), leather(brown))
        s.band(arm, (8, 9), leather(lit(brown, 0.7)))
        s.band(arm, (11, 12), leather((60, 44, 30)))
        s.paint(arm, leather((60, 44, 30)), ("bottom",))
        s.band(sleeve, (0, 4), cloth(green, weave=2))
        s.paint(sleeve, cloth(green), ("top",))
        s.drop_shadow(sleeve, arm)
    for leg, pants in LEGS:
        s.paint(leg, cloth((88, 72, 50)))
    boots(s, (78, 56, 36), rows=(7, 12), cuff=(126, 96, 64))


def mender(s, look, v):
    robe = (232, 226, 210)
    stole = (246, 240, 226)
    brown = (120, 88, 56)
    head(s, look)
    if v in (0, 2):
        hood(s, (226, 220, 204), trim=GOLD)
    elif v == 1:    # circlet with a green gem
        s.band("hat", (2, 3), solid(GOLD, 2, 2), SIDES)
        s.set("hat", "front", 3, 2, (90, 200, 120), glow=True)
        s.set("hat", "front", 4, 2, (60, 160, 96), glow=True)
        for face in SIDES:
            for i in range(8):
                s.set("hat", face, i, 2, GOLD, metal=True) if s.get("hat", face, i, 2)[3] and face != "front" else None
    else:           # hair bun
        for i in (3, 4):
            for j in (0, 1):
                s.set("hat", "back", i, j, lit(look["hair"], 1.05))
        for i in (3, 4):
            for j in (5, 6):
                s.set("hat", "top", i, j, look["hair"])
    s.paint("body", cloth(robe, weave=2, folds=4))
    for face in ("front", "back"):
        for j in range(12):
            for i in (2, 5):
                s.set("jacket", face, i, j, GOLD if j % 3 else lit(GOLD, 1.15), metal=True)
            for i in (3, 4):
                s.set("jacket", face, i, j, stole)
    # a gold leaf sigil on the stole
    for (i, j) in ((3, 2), (4, 2), (3, 3), (4, 3), (3, 4)):
        s.set("jacket", "front", i, j, (140, 196, 110) if (i + j) % 2 else (96, 160, 90))
    s.set("jacket", "front", 4, 4, GOLD, metal=True)
    s.band("jacket", (7, 8), solid((200, 170, 110), 4, 3))       # rope belt
    s.set("jacket", "front", 2, 8, (200, 170, 110))
    s.set("jacket", "front", 2, 9, (200, 170, 110))
    for j in range(7, 11):                                         # satchel
        for i in (6, 7):
            s.set("jacket", "front", i, j, leather(brown)(s, "jacket", "front", i, j, 8, 12, i, j))
    s.set("jacket", "front", 6, 8, (90, 170, 90))
    for arm, sleeve in ARMS:
        s.paint(arm, cloth(robe, weave=2))
        s.band(arm, (10, 12), skin_mat(look["skin"]))
        s.paint(arm, skin_mat(look["skin"]), ("bottom",))
        s.band(sleeve, (7, 10), cloth(robe, weave=2))
        s.band(sleeve, (9, 10), solid(GOLD, 2, 2))
        s.drop_shadow(sleeve, arm)
    for leg, pants in LEGS:
        s.paint(leg, cloth(robe, weave=2))
        s.band(pants, (0, 10), cloth(robe, weave=2, folds=3))
        s.band(pants, (9, 10), solid(GOLD, 2, 2))
        s.drop_shadow(pants, leg)
    boots(s, (110, 80, 52), rows=(10, 12))


def arcanist(s, look, v):
    robe = (78, 46, 126)
    deep = (46, 26, 80)
    inner = (128, 92, 190)
    rune = (196, 166, 255)
    cyan = (120, 230, 255)
    head(s, look)
    if v in (0, 2):
        hood(s, deep)
        for i in range(2, 6):
            s.set("hat", "top", i, 3, lit(deep, 1.15))
    elif v == 1:
        s.band("hat", (2, 3), solid(GOLD, 2, 2), SIDES)
        s.set("hat", "front", 3, 2, cyan, glow=True)
        s.set("hat", "front", 4, 2, lit(cyan, 0.8), glow=True)
    s.paint("body", cloth(robe, weave=2, folds=4))
    for j in range(12):
        s.set("body", "front", 3, j, inner)
        s.set("body", "front", 4, j, lit(inner, 0.9))
    # high collar and gold-edged opening
    s.band("jacket", (0, 2), cloth(deep, weave=2), SIDES)
    for i in range(2, 6):
        s.clear("jacket", "front", i, 0)
        s.clear("jacket", "front", i, 1)
    for j in range(0, 12):
        s.set("jacket", "front", 2, j, GOLD, metal=True)
        s.set("jacket", "front", 5, j, GOLD, metal=True)
    s.set("jacket", "front", 3, 2, cyan, glow=True)                # amulet
    s.set("jacket", "front", 4, 2, lit(cyan, 0.75), glow=True)
    s.band("jacket", (6, 7), solid(GOLD, 2, 2))                     # sash
    s.set("jacket", "front", 6, 7, GOLD)
    s.set("jacket", "front", 6, 8, lit(GOLD, 0.8))
    for (i, j) in ((1, 3), (6, 4), (1, 9), (6, 10), (0, 6)):      # glowing embroidery
        s.set("body", "front", i, j, rune, glow=True)
    for (i, j) in ((2, 3), (5, 8), (3, 10), (6, 2)):
        s.set("body", "back", i, j, rune, glow=True)
    for arm, sleeve in ARMS:
        s.paint(arm, cloth(robe, weave=2))
        s.band(arm, (10, 12), skin_mat(look["skin"]))
        s.paint(arm, skin_mat(look["skin"]), ("bottom",))
        s.band(sleeve, (6, 10), cloth(lit(robe, 1.1), weave=2))
        s.band(sleeve, (9, 10), solid(GOLD, 2, 2))
        s.set(sleeve, "front", 1, 7, rune, glow=True)
        s.drop_shadow(sleeve, arm)
    for leg, pants in LEGS:
        s.paint(leg, cloth(robe, weave=2, folds=3))
        s.band(leg, (10, 11), solid(GOLD, 2, 2))
        s.band(leg, (11, 12), leather((36, 24, 50)))
        s.paint(leg, leather((36, 24, 50)), ("bottom",))
    s.set("rleg", "front", 1, 4, rune, glow=True)
    s.set("lleg", "front", 2, 7, rune, glow=True)


CLASSES = {"bulwark": bulwark, "skirmisher": skirmisher, "ranger": ranger, "mender": mender, "arcanist": arcanist}

# Four looks per class: skin tone, hair colour, length, fringe, eyes, beard, extras.
LOOKS = {
    "bulwark": [dict(skin=1, hair="brown", length="short", fringe="short", eyes="blue", beard="full"),
                dict(skin=3, hair="black", length="short", fringe="parted", eyes="brown", beard="stubble"),
                dict(skin=0, hair="red", length="medium", fringe="swept", eyes="green", scar=True),
                dict(skin=4, hair="ash", length="short", fringe="short", eyes="hazel", beard="full", beard_color="ash")],
    "skirmisher": [dict(skin=2, hair="black", length="short", fringe="swept", eyes="brown", scar=True, smile=True),
                   dict(skin=0, hair="auburn", length="medium", fringe="bangs", eyes="green", beard="goatee"),
                   dict(skin=4, hair="black", length="short", fringe="none", eyes="hazel"),
                   dict(skin=1, hair="blond", length="medium", fringe="parted", eyes="blue")],
    "ranger": [dict(skin=1, hair="chestnut", length="medium", fringe="parted", eyes="green"),
               dict(skin=3, hair="brown", length="short", fringe="short", eyes="hazel", beard="full"),
               dict(skin=0, hair="blond", length="long", fringe="swept", eyes="blue", smile=True),
               dict(skin=5, hair="black", length="medium", fringe="bangs", eyes="brown")],
    "mender": [dict(skin=2, hair="brown", length="medium", fringe="parted", eyes="hazel", smile=True),
               dict(skin=0, hair="blond", length="long", fringe="parted", eyes="blue", smile=True),
               dict(skin=1, hair="white", length="short", fringe="short", eyes="grey", beard="full", beard_color="white"),
               dict(skin=4, hair="black", length="medium", fringe="swept", eyes="brown", smile=True)],
    "arcanist": [dict(skin=0, hair="ash", length="long", fringe="parted", eyes="violet", glow_eyes=True),
                 dict(skin=3, hair="black", length="long", fringe="bangs", eyes="violet", glow_eyes=True),
                 dict(skin=1, hair="white", length="medium", fringe="swept", eyes="grey", beard="full", beard_color="white"),
                 dict(skin=2, hair="auburn", length="short", fringe="swept", eyes="green")],
}


def skins():
    out = OUT / "entity/ally"
    out.mkdir(parents=True, exist_ok=True)
    for ci, (name, paint) in enumerate(CLASSES.items()):
        for v, look in enumerate(LOOKS[name]):
            s = Skin(seed=ci * 100 + v)
            look = dict(look)
            look["skin"] = SKIN_TONES[look["skin"]]
            look["hair"] = HAIR[look["hair"]]
            look["eyes"] = EYES[look["eyes"]]
            if "beard_color" in look:
                look["beard_color"] = HAIR[look["beard_color"]]
            paint(s, look, v)
            s.light()
            s.img.save(out / f"{name}_{v}.png")


# ---- items --------------------------------------------------------------------------------------------------------
def outline(img, k=0.32):
    """Draws a dark outline on the transparent pixels around the sprite, tinted by the colour it borders, the way
    vanilla item art is outlined."""
    px = img.load()
    w, h = img.size
    marks = {}
    for x in range(w):
        for y in range(h):
            if px[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and px[nx, ny][3]:
                    marks[(x, y)] = px[nx, ny][:3]
                    break
    for (x, y), c in marks.items():
        px[x, y] = clamp(mix(lit(c, k), (24, 16, 12), 0.35)) + (255,)


def warbanner_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    def put(x, y, c):
        px[x, y] = clamp(c) + (255,)
    wood, wood_hi = (106, 70, 40), (162, 114, 66)
    cloth_c = (30, 28, 40)
    gold, gold_hi, gold_lo = (232, 182, 62), (255, 238, 156), (172, 120, 38)
    # pole with a gold cap
    for y in range(3, 15):
        put(2, y, wood_hi)
        put(3, y, wood)
    put(2, 1, gold_hi)
    put(3, 1, gold)
    # gold crossbar with finials
    for x in range(1, 15):
        put(x, 2, gold_hi if x in (1, 14) else gold if x % 3 else lit(gold, 1.1))
    # cloth with folds, a gold border and a swallowtail
    for x in range(6, 14):
        for y in range(3, 13):
            fold = 1.0 + (0.22 if x in (8, 11) else -0.14 if x in (7, 12) else 0)
            put(x, y, lit(cloth_c, fold + 0.06 * (12 - y) / 9))
    for x in (6, 7, 8, 11, 12, 13):
        put(x, 13, lit(cloth_c, 0.92))
    for x in (6, 7, 12, 13):
        put(x, 14, lit(cloth_c, 0.86))
    for y in range(3, 15):
        if px[6, y][3]:
            put(6, y, gold_lo)
        if px[13, y][3]:
            put(13, y, gold_lo)
    for x in range(6, 14):
        put(x, 3, gold)
    put(8, 13, gold_lo)
    put(11, 13, gold_lo)
    # the diamond emblem: gold rim, darker gold body, bright core
    rows = {5: (9, 10), 6: (8, 11), 7: (7, 12), 8: (7, 12), 9: (8, 11), 10: (9, 10)}
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            rim = x in (x0, x1)
            put(x, y, gold if rim else gold_lo)
    for x, y in ((9, 7), (10, 7), (9, 8), (10, 8)):
        put(x, y, gold_hi)
    put(9, 6, gold_hi)
    outline(img)
    out = OUT / "item"
    out.mkdir(parents=True, exist_ok=True)
    img.save(out / "warbanner.png")


def upgrade_template_icon():
    """A slate smithing tablet with a gold frame and two gold chevrons (the Gold Upgrade)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    def put(x, y, c):
        px[x, y] = clamp(c) + (255,)
    slate = (62, 66, 80)
    gold, gold_hi, gold_lo = (232, 182, 62), (255, 238, 156), (172, 120, 38)
    for x in range(2, 14):
        for y in range(1, 15):
            if x in (2, 13) and y in (1, 14):
                continue
            k = 1.0 + 0.14 * (1 - (x + y) / 26) + random.Random(x * 31 + y).uniform(-0.05, 0.05)
            put(x, y, lit(slate, k))
    for x in range(3, 13):
        put(x, 2, gold_hi if x < 8 else gold)
        put(x, 13, gold_lo)
    for y in range(3, 13):
        put(3, y, gold)
        put(12, y, gold_lo)

    def chevron(y0, top, body):
        for dy, xs in enumerate(((7, 8), (6, 7, 8, 9), (5, 6, 9, 10), (4, 5, 10, 11))):
            for x in xs:
                put(x, y0 + dy, top if dy < 2 else body)
    chevron(4, gold_hi, gold)
    chevron(8, gold, gold_lo)
    outline(img)
    img.save(OUT / "item" / "gold_upgrade.png")


if __name__ == "__main__":
    skins()
    warbanner_icon()
    upgrade_template_icon()
    print("textures written to", OUT)
