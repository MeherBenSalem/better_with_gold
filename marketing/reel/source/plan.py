"""Single source of truth for the Remnants | Fellowship reel: voiceover placement, shots, time maps and every cue.

Every shot lasts a whole number of beats on the 125 BPM grid of the music bed (Mixkit "Epical Drums 04"); the reel
is 76 beats (19 bars) long and starts on a beat, so the music loops with the picture. Each voice line is pinned to
its shot or to a word anchor (the Arc Storm strikes on "storm", the formation cuts land on "Wedge" and "Charge",
the title on "Remnants"); if a line does not fit before the next one, or a shot asks for footage past the end of
its clip, the plan stops and says which. Narration is never sped up. Footage never plays slower than real time.
Writes timeline_plan.json, edl.json and cues.json.
"""
import json
import sys

import numpy as np

E = "/home/user/fsreel/edit"
BEAT = 0.47997          # measured from the track: 125.007 BPM
MUSIC_IN = 69.648       # the bed starts at bar 34 of the track

BEATS = {
    "hook": 5, "hire": 4, "march": 3, "taunt": 4, "volley": 5, "storm": 5, "mend": 5, "wheel": 6,
    "form_shield": 0, "form_wedge": 0, "form_charge": 0,  # one 6-beat block, cut on the words (below)
    "downed": 8, "plant": 3, "guard": 3, "roster": 5, "title": 6, "cta": 8,
}
FORM_BEATS = 6

LW = {l["i"]: l for l in json.load(open(f"{E}/lineword.json"))}
lines = json.load(open(f"{E}/lines.json"))
CLIPS = {}


def clip_len(name):
    if name not in CLIPS:
        CLIPS[name] = json.load(open(f"{E}/idx/{name}.json"))["t"][-1]
    return CLIPS[name]


def word_at(i, word):
    for w in LW[i]["words"]:
        if w["w"].lower().strip(".,!?").startswith(word):
            return w["s"]
    raise KeyError(f"line {i}: no word {word!r} in {[w['w'] for w in LW[i]['words']]}")


# ------------------------------------------------------------------ the grid
STARTS, t = {}, 0.0
for name, n in BEATS.items():
    if name.startswith("form_"):
        if name == "form_shield":
            STARTS["formations"] = (round(t, 4), round(t + FORM_BEATS * BEAT, 4))
            t += FORM_BEATS * BEAT
        continue
    STARTS[name] = (round(t, 4), round(t + n * BEAT, 4))
    t += n * BEAT
DURATION = round(t, 4)


def S(name):
    return STARTS[name][0]


def L(name):
    return STARTS[name][1] - STARTS[name][0]


# ------------------------------------------------------------------ voiceover (start times) and word anchors
VO_START = {
    0: 0.12,
    1: round(S("hire") + 0.05, 3),
    2: round(S("taunt") + 0.04, 3),
    3: round(S("volley") + 0.18, 3),
    5: round(S("mend") + 0.04, 3),
    6: round(S("wheel") + 0.04, 3),
    7: round(S("formations") + 0.04, 3),
    8: round(S("downed") + 0.06, 3),
    9: round(S("plant") + 0.04, 3),
    10: round(S("roster") + 0.04, 3),
    12: round(S("cta") + 0.16, 3),
}
VO_START[4] = round(S("storm") + 0.04, 3)
STORM_WORD = round(VO_START[4] + word_at(4, "storm"), 4)        # the strike lands on "storm"
TITLE_WORD = round(S("title") + BEAT, 4)                         # the name lands one beat into the title
VO_START[11] = round(TITLE_WORD - word_at(11, "remnants"), 3)
WEDGE = round(VO_START[7] + word_at(7, "wedge") - 0.04, 4)       # formation cuts just before each word
CHARGE = round(VO_START[7] + word_at(7, "charge") - 0.04, 4)
CROUCH = round(VO_START[8] + word_at(8, "crouch"), 4)
GUARD_CUT = round(VO_START[9] + word_at(9, "and") - 0.02, 4)     # "...and they hold the ground"
STARTS["form_shield"] = (S("formations"), WEDGE)
STARTS["form_wedge"] = (WEDGE, CHARGE)
STARTS["form_charge"] = (CHARGE, STARTS["formations"][1])
VO_START = dict(sorted(VO_START.items()))

problems = []
for i in range(len(lines) - 1):
    end = VO_START[i] + LW[i]["dur"]
    if end + 0.06 > VO_START[i + 1]:
        problems.append(f"line {i} ends {end:.2f} but line {i + 1} starts {VO_START[i + 1]:.2f}")
last = VO_START[12] + LW[12]["dur"]
if last > DURATION - 1.2:
    problems.append(f"the closing line ends {last:.2f}, too close to the loop at {DURATION:.2f}")

# ------------------------------------------------------------------ shots
# map: [[local_out_time, src_game_time], ...] (piecewise linear). No shot plays slower than real time.
shots = []


def shot(name, src, mp, cam, punch=0.05, shake=(), start=None, end=None, **kw):
    a, z = (start, end) if start is not None else STARTS[name]
    n = z - a
    xs = [p[0] for p in mp]
    ys = [p[1] for p in mp]
    for (x0, y0), (x1, y1) in zip(mp, mp[1:]):
        if (y1 - y0) < (x1 - x0) * 0.999:
            problems.append(f"{name}: plays slower than real time between {x0:.2f} and {x1:.2f}")
    if abs(xs[-1] - n) > 1e-3:
        problems.append(f"{name}: map ends at {xs[-1]:.3f}, shot lasts {n:.3f}")
    if ys[-1] > clip_len(src) + 1e-3 or ys[0] < 0:
        problems.append(f"{name}: needs {src} up to {ys[-1]:.2f}, the clip ends at {clip_len(src):.2f}")
    s = {"name": name, "src": src, "start": round(a, 4), "end": round(z, 4), "map": mp, "interp": "linear",
         "cam": [[a + u * n, sc, cx, cy] for u, sc, cx, cy in cam], "punch": punch, "shake": [list(x) for x in shake]}
    s.update(kw)
    shots.append(s)
    return s


def run(name, src, s0, speed=1.0, anchor=None, **kw):
    """A straight run through a clip at a constant speed (>= 1). anchor=(out_time, src_time) pins a moment."""
    a, z = kw.pop("start", None), kw.pop("end", None)
    if a is None:
        a, z = STARTS[name]
    n = z - a
    if anchor:
        at, src_t = anchor
        s0 = src_t - (at - a) * speed
    return shot(name, src, [[0, round(s0, 4)], [round(n, 4), round(s0 + n * speed, 4)]], kw.pop("cam"),
                start=a, end=z, **kw)


SRC = json.load(open(f"{E}/src_times.json"))   # moments found on the contact sheets (game seconds in each clip)

HOOK_HIT = round(S("hook") + 0.22, 4)
run("hook", "clash_front", 0, 1.0, anchor=(HOOK_HIT, SRC["front_storm"]), cam=[[0, 1.1, 540, 900], [1, 1.2, 540, 920]],
    punch=0.0, shake=[(HOOK_HIT, 26, 0.6)])
HIRE_CLICK = round(S("hire") + 0.95, 4)  # the villager walks in after; the oath bursts under "of sellswords"
run("hire", "hire", 0, 1.0, anchor=(HIRE_CLICK, SRC["hire_click"]), cam=[[0, 1.04, 540, 1000], [1, 1.1, 540, 1000]])
run("march", "march", SRC["march_in"], 1.1, cam=[[0, 1.05, 540, 980], [1, 1.12, 540, 980]])
TAUNT = round(S("taunt") + 0.62, 4)
run("taunt", SRC["taunt_clip"], 0, 1.0, anchor=(TAUNT, SRC["taunt_ring"]), cam=[[0, 1.12, 600, 1000], [1, 1.24, 620, 1000]],
    shake=[(TAUNT, 10, 0.3)])
VOLLEY = round(S("volley") + 0.5, 4)
run("volley", "volley", 0, 1.0, anchor=(VOLLEY, SRC["volley_loose"]), cam=[[0, 1.15, 560, 980], [1, 1.28, 560, 980]])
run("storm", "storm", 0, 1.0, anchor=(STORM_WORD, SRC["storm_strike"]), cam=[[0, 1.08, 540, 900], [1, 1.22, 540, 880]],
    shake=[(STORM_WORD, 30, 0.7)])
run("mend", "mend", SRC["mend_in"], 1.0, cam=[[0, 1.05, 540, 1000], [1, 1.16, 540, 1000]])
run("wheel", "wheel", SRC["wheel_in"], 1.0, cam=[[0, 1.02, 540, 1040], [1, 1.13, 560, 1000]], punch=0.0)
f0, f1 = STARTS["form_shield"], STARTS["form_wedge"]
run("form_shield", "formations", 0, 1.25, anchor=(f0[0] + 0.25, SRC["form_shield"]), start=f0[0], end=f0[1],
    cam=[[0, 1.08, 540, 960], [1, 1.12, 540, 960]])
run("form_wedge", "formations", 0, 1.6, anchor=(f1[0] + 0.1, SRC["form_wedge"]), start=f1[0], end=f1[1],
    cam=[[0, 1.12, 540, 960], [1, 1.14, 540, 960]])
f2 = STARTS["form_charge"]
run("form_charge", "formations", 0, 1.0, anchor=(f2[0] + 0.05, SRC["form_charge"]), start=f2[0], end=f2[1],
    cam=[[0, 1.14, 540, 960], [1, 1.26, 540, 940]])
# downed: the fall, then the crouch on "crouch" and the revive (played at up to 1.5x so it completes in the line)
d0, d1 = STARTS["downed"]
DOWN_HIT = round(d0 + 0.35, 4)
fall = SRC["downed_hit"]
crouch = SRC["downed_crouch"]
revived = SRC["downed_revived"]
mp = [[0, round(fall - 0.35, 4)], [round(CROUCH - d0, 4), round(crouch, 4)]]
sp = max(1.0, (revived - crouch) / max(0.1, (d1 - 0.5) - CROUCH))
mp.append([round(d1 - d0, 4), round(crouch + (d1 - CROUCH) * sp, 4)])
if mp[1][1] - mp[0][1] < mp[1][0]:  # never slower than real time before the crouch
    mp[0][1] = round(mp[1][1] - mp[1][0], 4)
shot("downed", "downed", mp, [[0, 1.05, 540, 1000], [1, 1.14, 540, 1000]], shake=[(DOWN_HIT, 12, 0.35)])
PLANT = round(S("plant") + 0.4, 4)
run("plant", "banner_plant", 0, 1.0, anchor=(PLANT, SRC["plant_click"]), end=GUARD_CUT, start=S("plant"),
    cam=[[0, 1.04, 540, 1000], [1, 1.12, 540, 1000]], shake=[(PLANT, 14, 0.4)])
run("guard", "banner_guard", SRC["guard_in"], 1.3, start=GUARD_CUT, end=STARTS["guard"][1],
    cam=[[0, 1.05, 540, 960], [1, 1.14, 540, 960]])
run("roster", "roster", SRC["roster_in"], 1.0, cam=[[0, 1.1, 540, 960], [1, 1.16, 540, 960]], punch=0.0)
run("title", SRC["title_clip"], SRC["title_in"], 1.0, cam=[[0, 1.05, 540, 940], [1, 1.2, 540, 940]], punch=0.0)
run("cta", "cta", SRC["cta_in"], 1.0, cam=[[0, 1.0, 540, 960], [1, 1.1, 540, 950]], punch=0.0)

if problems:
    print("PLAN DOES NOT FIT:\n  " + "\n  ".join(problems))
    sys.exit(1)

# ------------------------------------------------------------------ cues
CLASS = {"bulwark": "#4F8FE0", "skirmisher": "#D9483B", "ranger": "#5DBB63", "mender": "#F2C94C", "arcanist": "#A06BE0"}
GOLD = "#F2C94C"
LABELS = [
    ("hire", "Sellswords", "Hired with <b>gold</b> at the village bell", "coin", GOLD),
    ("taunt", "Bulwark", "<b>Challenge</b> pulls every enemy onto it", "tank", CLASS["bulwark"]),
    ("volley", "Ranger", "<b>Volley</b>: three arrows at once", "ranged", CLASS["ranger"]),
    ("storm", "Arcanist", "<b>Arc Storm</b> strikes the whole pack", "caster", CLASS["arcanist"]),
    ("mend", "Mender", "<b>Mend</b> heals and revives", "healer", CLASS["mender"]),
    ("wheel", "Command Wheel", "Hold <b>G</b> · eight orders", "charge", GOLD),
    ("formations", "Formations", "Line · Wedge · Shield Wall · Circle", "formation", GOLD),
    ("downed", "Downed, not dead", "<b>30 s</b> to pull them back up", "revive", "#E5534B"),
    ("plant", "Warbanner", "Plant it · they guard it", "guard_banner", GOLD),
    ("roster", "Chronicle", "Every deed, written down", "star", GOLD),
]
labels = []
for key, nm, sub, icon, color in LABELS:
    a, z = STARTS[key] if key != "plant" else (S("plant"), STARTS["guard"][1])
    labels.append({"t0": a, "t1": z, "name": nm, "sub": sub, "icon": icon, "color": color})

ev = {
    "hook_hit": HOOK_HIT, "hire_click": HIRE_CLICK, "taunt": TAUNT, "volley": VOLLEY, "storm": STORM_WORD,
    "mend_heals": [], "wheel_open": None, "wheel_pick": None, "wheel_release": None,
    "shield": S("formations"), "wedge": WEDGE, "charge": CHARGE, "down": DOWN_HIT, "crouch": CROUCH,
    "plant": PLANT, "guard": GUARD_CUT, "roster": S("roster"), "title": S("title"), "cta": S("cta"),
    "cuts": sorted({round(s["start"], 4) for s in shots}),
}


def out_time(name, src_t):
    s = next(x for x in shots if x["name"] == name)
    xs, ys = zip(*s["map"])
    return round(s["start"] + float(np.interp(src_t, ys, xs)), 4)


ev["mend_heals"] = [out_time("mend", t_) for t_ in SRC.get("mend_heals", []) if t_ <= shots[6]["map"][-1][1]]
for k in ("wheel_open", "wheel_pick", "wheel_release"):
    if k in SRC:
        ev[k] = out_time("wheel", SRC[k])
if "downed_revived" in SRC:
    ev["revived"] = out_time("downed", SRC["downed_revived"])

TITLE0, TITLE1 = STARTS["title"]
cues = {
    "duration": DURATION, "beat": BEAT, "music_in": MUSIC_IN, "events": ev, "labels": labels,
    "flashes": [[HOOK_HIT, 0.55], [STORM_WORD, 0.45], [PLANT, 0.3], [TITLE0, 0.55]],
    "capHigh": [],
    "capLow": [[S("wheel"), S("formations")], [S("downed"), STARTS["guard"][1]], [S("roster"), S("title")]],
    "hook": [0.0, 0.0, 0.0],
    "title": [TITLE0, TITLE1],
    "titleWord": TITLE_WORD,
    "comment": [round(min(VO_START[12] + LW[12]["dur"] + 0.1, DURATION - 1.6), 3), round(DURATION - 0.12, 3)],
}

timeline = [{"i": i, "start": VO_START[i], "cap": lines[i]["cap"]} for i in range(len(lines))]
json.dump({"duration": DURATION, "lines": timeline}, open(f"{E}/timeline_plan.json", "w"), indent=1)
json.dump({"fps": 30, "size": [1080, 1920], "shots": shots}, open(f"{E}/edl.json", "w"), indent=1)
json.dump(cues, open(f"{E}/cues.json", "w"), indent=1)
print(f"duration {DURATION:.3f} s = {round(DURATION / BEAT)} beats")
for s in shots:
    print(f'{s["start"]:7.3f}-{s["end"]:7.3f} {s["end"] - s["start"]:5.2f}s  {s["name"]:12s} {s["src"]:13s} '
          f'src {s["map"][0][1]:.2f}->{s["map"][-1][1]:.2f}')
for i in range(len(lines)):
    print(f'VO {i:2d} {VO_START[i]:6.2f}-{VO_START[i] + LW[i]["dur"]:6.2f} {lines[i]["cap"]}')
print(json.dumps(ev))
