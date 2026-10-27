#!/usr/bin/env python3
"""Renders the command wheel's textures (smooth, anti-aliased, 512x512) into
shared-resources/assets/better_with_gold/textures/gui/. Deterministic.

    python3 tools/gen_gui.py [preview.png]

wheel_sector / wheel_sector_hover: one 45-degree slice of the ring, pointing up (it is drawn rotated eight times).
wheel_hub: the centre medallion. wheel_ring: the outer ring with a diamond between every slice. wheel_pointer: the
gold arrowhead that turns toward the highlighted order.
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "shared-resources/assets/better_with_gold/textures/gui"
N = 512          # output size
SS = 4           # supersampling
R_IN, R_OUT = 0.36, 0.90     # slice radii as a fraction of the half size
GAP = 1.1        # degrees left empty on each side of a slice
GOLD = (232, 182, 62)
GOLD_HI = (255, 232, 154)
GOLD_LO = (150, 104, 30)


def canvas():
    return Image.new("RGBA", (N * SS, N * SS), (0, 0, 0, 0))


def finish(img, name):
    img = img.resize((N, N), Image.LANCZOS)
    OUT.mkdir(parents=True, exist_ok=True)
    img.save(OUT / f"{name}.png")
    return img


def polar(r, deg):
    """Image coordinates of a point at radius r (fraction) and angle deg (0 = up, clockwise)."""
    c = N * SS / 2
    a = math.radians(deg)
    return c + math.sin(a) * r * c, c - math.cos(a) * r * c


def slice_mask(r0, r1, a0, a1, steps=180):
    m = Image.new("L", (N * SS, N * SS), 0)
    pts = [polar(r1, a0 + (a1 - a0) * k / steps) for k in range(steps + 1)]
    pts += [polar(r0, a1 - (a1 - a0) * k / steps) for k in range(steps + 1)]
    ImageDraw.Draw(m).polygon(pts, fill=255)
    return m


def radial(inner, outer, r0, r1, alpha0, alpha1):
    """A radial gradient image between radii r0 and r1."""
    img = Image.new("RGBA", (N * SS, N * SS), (0, 0, 0, 0))
    px = img.load()
    c = N * SS / 2
    step = 2
    for y in range(0, N * SS, step):
        for x in range(0, N * SS, step):
            r = math.hypot(x + 0.5 - c, y + 0.5 - c) / c
            t = max(0.0, min(1.0, (r - r0) / (r1 - r0)))
            col = tuple(int(inner[i] + (outer[i] - inner[i]) * t) for i in range(3))
            a = int(alpha0 + (alpha1 - alpha0) * t)
            for dy in range(step):
                for dx in range(step):
                    px[x + dx, y + dy] = col + (a,)
    return img


def arc(draw, r, a0, a1, width, color):
    c = N * SS / 2
    box = (c - r * c, c - r * c, c + r * c, c + r * c)
    # PIL angles: 0 = 3 o'clock, counter-clockwise is negative; ours: 0 = up, clockwise
    draw.arc(box, a0 - 90, a1 - 90, fill=color, width=int(width * SS))


def sector(name, hover):
    a0, a1 = -22.5 + GAP, 22.5 - GAP
    mask = slice_mask(R_IN, R_OUT, a0, a1)
    if hover:
        body = radial((122, 88, 24), (52, 34, 10), R_IN, R_OUT, 236, 244)
    else:
        body = radial((34, 40, 64), (12, 14, 24), R_IN, R_OUT, 214, 232)
    img = canvas()
    img.paste(body, (0, 0), mask)
    d = ImageDraw.Draw(img)
    rim = GOLD_HI if hover else GOLD
    rim_lo = GOLD if hover else GOLD_LO
    # outer and inner rims, side edges
    arc(d, R_OUT - 0.004, a0, a1, 3.2, rim + (255,))
    arc(d, R_OUT - 0.022, a0 + 1.0, a1 - 1.0, 1.2, rim_lo + (150,))
    arc(d, R_IN + 0.004, a0, a1, 2.2, rim_lo + (255,))
    for a in (a0, a1):
        d.line([polar(R_IN, a), polar(R_OUT, a)], fill=rim_lo + (230,), width=int(1.6 * SS))
    # a soft highlight along the top of the slice
    shine = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(shine).polygon([polar(R_OUT - 0.03, a0 + 2), polar(R_OUT - 0.03, a1 - 2), polar(R_OUT - 0.16, a1 - 4),
                                   polar(R_OUT - 0.16, a0 + 4)], fill=(255, 255, 255, 22))
    shine = shine.filter(ImageFilter.GaussianBlur(10 * SS))
    if hover:
        img.alpha_composite(Image.composite(shine, Image.new("RGBA", img.size, (0, 0, 0, 0)), mask))
    if hover:
        glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
        arc(ImageDraw.Draw(glow), R_OUT + 0.012, a0, a1, 7, GOLD_HI + (210,))
        glow = glow.filter(ImageFilter.GaussianBlur(5 * SS))
        img = Image.alpha_composite(glow, img)
    return finish(img, name)


def hub():
    img = canvas()
    r = 0.33
    body = radial((30, 34, 54), (10, 11, 18), 0.0, r, 246, 250)
    m = Image.new("L", img.size, 0)
    c = N * SS / 2
    ImageDraw.Draw(m).ellipse((c - r * c, c - r * c, c + r * c, c + r * c), fill=255)
    img.paste(body, (0, 0), m)
    d = ImageDraw.Draw(img)
    for rr, w, col in ((r - 0.004, 3.4, GOLD + (255,)), (r - 0.032, 1.2, GOLD_LO + (200,)), (r * 0.62, 1.0, GOLD_LO + (90,))):
        d.ellipse((c - rr * c, c - rr * c, c + rr * c, c + rr * c), outline=col, width=int(w * SS))
    for a in (0, 90, 180, 270):
        x, y = polar(r - 0.004, a)
        s = 0.022 * c
        d.polygon([(x, y - s), (x + s, y), (x, y + s), (x - s, y)], fill=GOLD_HI + (255,), outline=GOLD_LO + (255,))
    return finish(img, "wheel_hub")


def ring():
    img = canvas()
    d = ImageDraw.Draw(img)
    c = N * SS / 2
    r = 0.955
    d.ellipse((c - r * c, c - r * c, c + r * c, c + r * c), outline=GOLD_LO + (170,), width=int(1.4 * SS))
    for k in range(64):
        a = k * 360 / 64
        if k % 8 == 4:
            continue
        x0, y0 = polar(r - 0.012, a)
        x1, y1 = polar(r + 0.006, a)
        d.line([(x0, y0), (x1, y1)], fill=GOLD + (110,), width=int(1.0 * SS))
    for k in range(8):
        a = 22.5 + k * 45
        x, y = polar(r, a)
        s = 0.026 * c
        d.polygon([(x, y - s), (x + s, y), (x, y + s), (x - s, y)], fill=GOLD + (255,), outline=GOLD_LO + (255,))
        x, y = polar(R_IN - 0.02, a)
        s = 0.012 * c
        d.polygon([(x, y - s), (x + s, y), (x, y + s), (x - s, y)], fill=GOLD_LO + (220,))
    return finish(img, "wheel_ring")


def pointer():
    img = canvas()
    d = ImageDraw.Draw(img)
    tip, base, half = 0.42, 0.345, 7
    pts = [polar(tip, 0), polar(base, half), polar(base + 0.02, 0), polar(base, -half)]
    glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(glow).polygon(pts, fill=GOLD_HI + (200,))
    glow = glow.filter(ImageFilter.GaussianBlur(4 * SS))
    img.alpha_composite(glow)
    d.polygon(pts, fill=GOLD_HI + (255,), outline=GOLD_LO + (255,))
    return finish(img, "wheel_pointer")


def main():
    imgs = [sector("wheel_sector", False), sector("wheel_sector_hover", True), hub(), ring(), pointer()]
    if len(sys.argv) > 1:
        # composite preview: the full wheel with slice 1 highlighted
        bg = Image.new("RGBA", (N, N), (70, 90, 70, 255))
        bg.alpha_composite(imgs[3])
        for k in range(8):
            s = imgs[1] if k == 1 else imgs[0]
            bg.alpha_composite(s.rotate(-k * 45, resample=Image.BICUBIC))
        bg.alpha_composite(imgs[2])
        bg.alpha_composite(imgs[4].rotate(-45, resample=Image.BICUBIC))
        bg.save(sys.argv[1])
    print("gui textures written to", OUT)


if __name__ == "__main__":
    main()
