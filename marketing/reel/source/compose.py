"""Renders the base video (the footage, no text) from edl.json.

The footage was captured with the game's clock slowed 10x, so every rendered frame is in the capture.
Each output frame asks for a source *game time*; the two rendered frames around it are cross-faded
(no repeated or skipped frames, so no judder at any speed), and fast passages average a few samples
across half a frame interval (a 180 degree shutter) so speed ramps read as motion blur instead of jumps.

A shot with a "zoomwarp" entry follows a measured zoom curve instead (kept from the studio's compositor; this reel
does not use it).
"""
import bisect
import json
import math
import subprocess
import sys

import cv2
import numpy as np
from scipy.interpolate import PchipInterpolator

E = "/home/user/fsreel/edit"
EDL = json.load(open(f"{E}/edl.json"))
FPS = EDL["fps"]
W, H = EDL["size"]
DUR = json.load(open(f"{E}/cues.json"))["duration"]
NF = int(round(DUR * FPS))
SHUTTER = 0.5  # fraction of a frame interval the blur spans
UI_SHOTS = set()  # every shot here is gameplay


def ease(x):
    x = min(1.0, max(0.0, x))
    return x * x * (3 - 2 * x)


def ease_out(x):
    x = min(1.0, max(0.0, x))
    return 1 - (1 - x) ** 3


class Source:
    """Unique rendered frames of one capture, read sequentially with a small cache."""

    def __init__(self, name):
        j = json.load(open(f"{E}/idx/{name}.json"))
        self.name, self.idx, self.t = name, j["idx"], j["t"]
        self.k_of = {c: k for k, c in enumerate(self.idx)}
        self.cap, self.pos, self.cache = None, 0, {}

    def frame(self, k):
        k = max(0, min(len(self.idx) - 1, k))
        if k in self.cache:
            return self.cache[k]
        want = self.idx[k]
        if self.cap is None or want < self.pos:
            self.cap = cv2.VideoCapture(f"/home/user/fsreel/raw/{self.name}.mkv")
            self.pos = 0
        while self.pos <= want:
            ok, f = self.cap.read()
            if not ok:
                raise RuntimeError(f"{self.name}: ran out of frames at {self.pos}")
            kk = self.k_of.get(self.pos)
            if kk is not None and kk >= k - 6:
                self.cache[kk] = f
            self.pos += 1
        for kk in [x for x in self.cache if x < k - 6 or x > k + 6]:
            del self.cache[kk]
        return self.cache[k]

    def sample(self, g):
        """Frame at game time g, cross-fading into the next render as g approaches it."""
        t = self.t
        if g <= t[0]:
            return self.frame(0).astype(np.float32)
        i = bisect.bisect_right(t, g) - 1
        if i >= len(t) - 1:
            return self.frame(len(t) - 1).astype(np.float32)
        d = min(t[i + 1] - t[i], 0.0275)  # a held (static) screen cross-fades in its last render interval and no sooner
        w = (g - (t[i + 1] - d)) / d
        if w <= 0.002:
            return self.frame(i).astype(np.float32)
        if w >= 0.998:
            return self.frame(i + 1).astype(np.float32)
        return cv2.addWeighted(self.frame(i), 1 - w, self.frame(i + 1), w, 0, dtype=cv2.CV_32F)


def cam_at(shot, t):
    keys = shot["cam"]
    if t <= keys[0][0]:
        return tuple(keys[0][1:])
    for a, b in zip(keys, keys[1:]):
        if a[0] <= t <= b[0]:
            u = (t - a[0]) / (b[0] - a[0])
            mode = shot.get("camEase")
            u = u * u if mode == "in" else 1 - (1 - u) ** 2 if mode == "out" else u
            return tuple(a[i] + (b[i] - a[i]) * u for i in (1, 2, 3))
    return tuple(keys[-1][1:])


def grade(img):
    f = img * (1.07 / 255.0) + (0.5 - 0.5 * 1.07 + 0.01)
    gray = f.mean(axis=2, keepdims=True)
    f = gray + (f - gray) * 1.14
    return np.clip(f * 255.0, 0, 255).astype(np.uint8)


def scale_about_center(img, s):
    m = np.array([[s, 0, W / 2 - s * W / 2], [0, s, H / 2 - s * H / 2]], dtype=np.float32)
    return cv2.warpAffine(img, m, (W, H), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE)


class ZoomWarp:
    def __init__(self, shot, src):
        zw = shot["zoomwarp"]
        track = json.load(open(f"{E}/{zw['track']}"))
        self.tz = [p[0] for p in track]
        self.lz = [p[1] for p in track]
        self.k = [src.t.index(min(src.t, key=lambda x: abs(x - tt))) for tt in self.tz]
        self.t0, self.t1, self.p = zw["t0"], zw["t1"], zw["ease"]
        self.src = src
        # The HUD (header, ability bar, RPG panel, buttons, minimap) is drawn over the tree on translucent
        # panels, so whole panel rectangles keep the unscaled render; their borders hide the edge.
        hud = np.zeros((H, W), bool)
        for x0, y0, x1, y1 in [(0, 0, W, 95), (16, 1642, 308, 1766), (14, 1784, 472, 1906), (624, 1594, 792, 1896),
                               (799, 1639, 1061, 1901)]:
            # also cover where the zoomed-in copy of the panel lands (the remainder never exceeds 6%)
            xs = [W / 2 + (x - W / 2) * 1.06 for x in (x0, x1)] + [x0, x1]
            ys = [H / 2 + (y - H / 2) * 1.06 for y in (y0, y1)] + [y0, y1]
            hud[max(0, int(min(ys))):int(math.ceil(max(ys))), max(0, int(min(xs))):int(math.ceil(max(xs)))] = True
        self.hud = hud[..., None]

    def active(self, t):
        return self.t0 <= t <= self.t1

    def render(self, t):
        x = (t - self.t0) / (self.t1 - self.t0)
        target = self.lz[0] + (self.lz[-1] - self.lz[0]) * (1 - (1 - x) ** self.p)
        j = next((i for i, l in enumerate(self.lz) if l <= target + 1e-9), len(self.lz) - 1)
        raw = self.src.frame(self.k[j])
        s = math.exp(target - self.lz[j])  # >= 1: zoom the further-out render in by the remainder
        out = scale_about_center(raw, s) if s > 1.0005 else raw
        return np.where(self.hud, raw, out).astype(np.float32)


def build_map(shot):
    xs = [p[0] for p in shot["map"]]
    ys = [p[1] for p in shot["map"]]
    if shot["interp"] == "smooth":
        f = PchipInterpolator(xs, ys)
        return lambda u: float(f(min(max(u, xs[0]), xs[-1])))
    return lambda u: float(np.interp(u, xs, ys))


def main():
    part = None
    if len(sys.argv) > 1:  # optional: render a time range for checks, e.g. compose.py 8 15
        part = (float(sys.argv[1]), float(sys.argv[2]))
    out_path = f"{E}/base.mp4" if part is None else f"{E}/base_part.mp4"
    sources, maps, warps = {}, {}, {}
    ff = subprocess.Popen(["ffmpeg", "-loglevel", "error", "-y", "-f", "rawvideo", "-pix_fmt", "bgr24", "-s", f"{W}x{H}",
                           "-r", str(FPS), "-i", "-", "-c:v", "libx264", "-preset", "medium", "-crf", "12",
                           "-pix_fmt", "yuv420p", out_path], stdin=subprocess.PIPE)
    shots = EDL["shots"]
    for fno in range(NF):
        t = fno / FPS
        if part and not (part[0] <= t < part[1]):
            continue
        shot = next((s for s in shots if s["start"] <= t < s["end"]), shots[-1])
        name = shot["name"]
        src = sources.setdefault(shot["src"], Source(shot["src"]))
        fmap = maps.setdefault(name, build_map(shot))
        local = t - shot["start"]
        if "zoomwarp" in shot and name not in warps:
            warps[name] = ZoomWarp(shot, src)
        if name in warps and warps[name].active(t):
            img = warps[name].render(t)
        else:
            g = fmap(local)
            v = abs(fmap(local + 0.004) - fmap(local - 0.004)) / 0.008
            span = 0.0 if name in UI_SHOTS else SHUTTER * v / FPS
            n = 1 if span < 0.006 else min(9, int(math.ceil(span / 0.006)) + 1)
            if n == 1:
                img = src.sample(g)
            else:
                img = None
                for k in range(n):
                    s_k = src.sample(g + span * (k / (n - 1) - 0.5))
                    img = s_k if img is None else img + s_k
                img /= n
        s, cx, cy = cam_at(shot, t)
        if shot.get("punch", 0) and local < 0.22:
            # push in on the cut, in the same direction as the camera drift, so the two never cancel into a still
            s *= 1 - shot["punch"] * (1 - ease_out(local / 0.22))
        dx = dy = 0.0
        for at, amp, dur in shot.get("shake", []):
            if at <= t < at + dur:
                k = (1 - (t - at) / dur) ** 2
                dx += amp * k * math.sin(t * 91.0) / s
                dy += amp * k * math.cos(t * 77.0) / s
        cx, cy = cx + dx, cy + dy
        # keep the view inside the frame: zoom in a touch rather than show a border
        cx = min(max(cx, 1.0), W - 1.0)
        cy = min(max(cy, 1.0), H - 1.0)
        s = max(s, 1.0, W / (2 * min(cx, W - cx)), H / (2 * min(cy, H - cy)))
        m = np.array([[s, 0, W / 2 - s * cx], [0, s, H / 2 - s * cy]], dtype=np.float32)
        out = cv2.warpAffine(img, m, (W, H), flags=cv2.INTER_LINEAR, borderMode=cv2.BORDER_REPLICATE)
        ff.stdin.write(grade(out).tobytes())
        if fno % 90 == 0:
            print(f"frame {fno}/{NF} {name}", flush=True)
    ff.stdin.close()
    ff.wait()
    print(out_path, "done")


main()
