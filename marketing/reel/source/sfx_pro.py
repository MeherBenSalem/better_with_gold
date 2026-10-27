"""Sound design for the Remnants | Fellowship reel: real recorded sound effects, timed from cues.json.

Every sound is a professional recording from Mixkit (Mixkit Sound Effects Free License: free for commercial use in
videos, no attribution required; https://mixkit.co/license/), listed in SFX below by its Mixkit id and title. No
synthesized effects. Each cue places a recording so its main hit (its loudest 10 ms, measured per file) lands on the
frame it belongs to (the Arc Storm strikes, the Challenge ring, the coins of the hire, the Warbanner); whooshes and
risers peak into the cut or hit they lead to. Anything that starts before 0 wraps to the end of the reel, so the
swell into the opening strike plays under the closing shot and carries the loop. Writes sfx.wav (48 kHz stereo).

fetch_sfx.sh downloads the library from Mixkit by id; it is not stored in the repository.
"""
import json
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, resample_poly, sosfilt

E = "/home/user/fsreel/edit"
LIB = os.environ.get("SFX_LIB", "/home/user/sfxlib")
SR = 48000
C = json.load(open(f"{E}/cues.json"))
EV = C["events"]
DUR = C["duration"]
N = int(round(DUR * SR))

SFX = {
    1286: "Cinematic impact thunder", 1300: "Strong close thunder explosion", 1283: "Cinematic thunder",
    2918: "Epic movie trailer whoosh impact", 2171: "Fighting man's voice", 2763: "Sword slashes in battle",
    1993: "Clinking coins", 3187: "Bag of coins touch", 930: "Bell of promise", 658: "Choir magic shine",
    461: "Big army crowd marching", 2795: "Heavy sword smashes metal", 2599: "Heavy electric shockwave impact",
    2771: "Arrow shot through air", 2767: "Arrow whizz in battle", 1491: "Arrow whoosh", 2769: "Metal arrow hit",
    2901: "Epic movie impact", 2345: "Magic spell mystery whoosh", 2792: "Fast sword whoosh",
    2765: "Sword strikes armor", 873: "Light spell", 2352: "Magic glitter or particles",
    2343: "Cinematic whoosh magic gust", 2629: "Magic astral sweep effect", 2289: "Warfare horn",
    2775: "Hitting metal armor", 2774: "Axe hits to a plate", 2768: "Human fighter pain scream",
    498: "Deep heartbeat impact", 490: "Human single heart beat", 2583: "Magical light transition",
    2908: "Movie trailer epic impact", 2784: "Drums of war", 1469: "Flying fast swoosh",
    1492: "Cinematic whoosh fast transition", 2584: "Magical light moving", 788: "Big cinematic impact",
    2900: "Movie logo intro impact", 2903: "Movie whoosh impact presentation", 459: "Male crowd cheering short",
    2785: "War horn ambience", 2762: "Sword blade attack in medieval battle",
}

_cache = {}


def load(iid):
    """A recording at 48 kHz stereo, normalized to 0 dBFS peak, and the time of its main hit."""
    if iid not in _cache:
        a, sr = sf.read(os.path.join(LIB, f"{iid}.wav"), dtype="float32", always_2d=True)
        if a.shape[1] == 1:
            a = np.repeat(a, 2, axis=1)
        a = a[:, :2]
        if sr != SR:
            a = np.stack([resample_poly(a[:, c], SR, sr) for c in range(2)], axis=1).astype(np.float32)
        a /= max(1e-6, np.abs(a).max())
        m = a.mean(1)
        win = int(0.01 * SR)
        env = np.sqrt(np.convolve(m ** 2, np.ones(win) / win, "same"))
        _cache[iid] = (a, int(np.argmax(env)) / SR)
    return _cache[iid]


out = np.zeros((N, 2), np.float32)


def place(iid, at, db=0.0, align="peak", start=0.0, length=None, fade_in=0.0, fade_out=0.08, hp=None, lp=None):
    """Places recording iid so its `align` point ("peak", "start" or "end") lands at `at` seconds."""
    assert iid in SFX, iid
    a, peak = load(iid)
    seg = a[int(start * SR): int((start + length) * SR) if length else None].copy()
    if hp:
        seg = sosfilt(butter(2, hp, "high", fs=SR, output="sos"), seg, axis=0).astype(np.float32)
    if lp:
        seg = sosfilt(butter(2, lp, "low", fs=SR, output="sos"), seg, axis=0).astype(np.float32)
    n = len(seg)
    if fade_in:
        k = min(n, int(fade_in * SR))
        seg[:k] *= np.linspace(0, 1, k)[:, None]
    if fade_out:
        k = min(n, int(fade_out * SR))
        seg[n - k:] *= np.linspace(1, 0, k)[:, None]
    ref = {"peak": peak - start, "start": 0.0, "end": n / SR}[align]
    i0 = int(round((at - ref) * SR))
    idx = (np.arange(n) + i0) % N  # wraps around the loop
    np.add.at(out, idx, seg * 10 ** (db / 20))


# ---------------------------------------------------------------- the opening strike (and the loop into it)
HOOK = EV["hook_hit"]
place(2918, HOOK, db=-4)                         # trailer whoosh-impact: its swell plays under the end of the reel
place(1286, HOOK, db=-1)                         # thunder impact on the Arc Storm
place(1300, HOOK + 0.02, db=-5, fade_out=1.2)    # the close thunder rolling out
place(2171, HOOK + 0.75, db=-15)                 # a war cry in the melee
place(2763, HOOK + 1.35, db=-15, fade_out=0.3)   # blades

# ---------------------------------------------------------------- hiring at the bell, the march
HIRE = EV["hire_click"]
place(3187, HIRE - 0.05, db=-9)                  # the purse
place(1993, HIRE + 0.02, db=-5)                  # coins change hands
place(930, HIRE + 0.12, db=-9)                   # the oath rings out
place(658, HIRE + 0.2, db=-15)
place(461, C["events"]["cuts"][2], db=-12, align="start", length=1.6, fade_in=0.15, fade_out=0.4)  # the band marching

# ---------------------------------------------------------------- Bulwark, Ranger, Arcanist, Mender
place(2171, EV["taunt"] - 0.12, db=-11)          # the Challenge shout
place(2795, EV["taunt"], db=-4)                  # shield on steel
place(2599, EV["taunt"] + 0.02, db=-9)           # the ring bursts out
place(2771, EV["volley"], db=-5)                 # three arrows loosed
place(2767, EV["volley"] + 0.07, db=-9)
place(1491, EV["volley"] + 0.16, db=-10)
place(2769, EV["volley"] + 0.75, db=-12)
place(2792, EV["storm"] - 0.42, db=-10)          # the Skirmisher's whirl just before
place(2345, EV["storm"], db=-11, align="end", fade_in=0.4)  # the spell gathering into the strike
place(1300, EV["storm"], db=-2, fade_out=1.0)    # the Arc Storm
place(2901, EV["storm"] + 0.01, db=-6)
place(2765, EV["mend_heals"][0] - 0.25 if EV["mend_heals"] else EV["cuts"][6] + 0.2, db=-10)  # a blow lands
for t in EV["mend_heals"]:
    place(873, t, db=-8)                         # the Mender's light
    place(2352, t + 0.15, db=-15)

# ---------------------------------------------------------------- the command wheel
place(2343, EV["wheel_open"], db=-9)             # the wheel opens
place(2629, EV["wheel_pick"], db=-12)            # Charge lights up
place(2289, EV["wheel_release"], db=-5)          # the horn: charge
place(1286, EV["wheel_release"] + 0.55, db=-8)   # the storm that follows

# ---------------------------------------------------------------- formations
place(2775, EV["shield"] + 0.05, db=-6)          # shields lock
place(2792, EV["wedge"], db=-6)
place(2774, EV["wedge"] + 0.1, db=-10)
place(2918, EV["charge"], db=-6)                 # the charge
place(461, EV["charge"], db=-13, align="start", length=1.0, fade_out=0.3)
place(1283, EV["charge"] + 0.12, db=-9)

# ---------------------------------------------------------------- downed and revived
place(2768, EV["down"] - 0.05, db=-11)           # cut down
place(498, EV["down"], db=-4)
for k in range(3):
    place(490, EV["down"] + 0.55 + 0.5 * k, db=-10 - 2 * k)  # the heartbeat while bleeding out
place(2583, EV["crouch"] + 0.1, db=-13, align="start", fade_out=0.4)  # the revive ring filling
place(658, EV["revived"], db=-5)                 # back on their feet
place(2352, EV["revived"] + 0.1, db=-12)

# ---------------------------------------------------------------- the Warbanner
place(2908, EV["plant"], db=-3)                  # planted
place(2784, EV["plant"] + 0.05, db=-12, align="start", length=1.4, fade_out=0.5)
place(1469, EV["guard"], db=-13)
place(461, EV["guard"], db=-14, align="start", length=1.3, fade_out=0.4)

# ---------------------------------------------------------------- roster, title, closing question
place(1492, EV["roster"], db=-10)
place(2584, EV["roster"] + 0.2, db=-18, align="start", length=2.0, fade_in=0.3, fade_out=0.6)
place(2903, EV["title"], db=-10, align="end", fade_in=0.4)  # into the title
place(788, EV["title"], db=-2)
place(2900, C["titleWord"], db=-6)
place(2785, EV["cta"] + 0.1, db=-15, align="start", fade_in=0.4, fade_out=0.8)
place(459, EV["cta"] + 1.9, db=-17)

peak = np.abs(out).max()
out *= 0.9 / max(1e-6, peak)
sf.write(f"{E}/sfx.wav", out, SR)
print(f"sfx.wav: {len(SFX)} recordings, {DUR:.2f} s, peak normalized from {20 * np.log10(peak):.1f} dBFS")
