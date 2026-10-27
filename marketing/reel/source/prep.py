"""Indexes a slowed-clock capture: which captured frames are new renders, and the game time of each.

The game ran with its clock slowed 10x and the screen was grabbed at 15 fps, so every rendered frame
appears in the capture at least once. A frame counts as new when any pixel visibly changes from the
previous capture (encoding noise stays below 5 levels, real changes are far above)."""
import json, sys
import cv2
import numpy as np

SLOW, CAP_FPS, THRESH = 0.1, 15.0, 8
name = sys.argv[1]
cap = cv2.VideoCapture(f"/home/user/fsreel/raw/{name}.mkv")
prev, n, idx, luma, motion = None, 0, [], [], []
while True:
    ok, f = cap.read()
    if not ok:
        break
    g = cv2.resize(cv2.cvtColor(f, cv2.COLOR_BGR2GRAY), (270, 480), interpolation=cv2.INTER_AREA).astype(np.int16)
    d = None if prev is None else np.abs(g - prev)
    if d is None or d.max() > THRESH:
        idx.append(n)
        luma.append(round(float(g.mean()), 2))
        motion.append(0.0 if d is None else round(float(d.mean()), 3))
    prev, n = g, n + 1
times = [round(i / CAP_FPS * SLOW, 5) for i in idx]
gaps = np.diff(times) if len(times) > 1 else np.array([1 / 60])
dmed = float(np.median(gaps))
json.dump({"name": name, "frames": n, "game_len": round(n / CAP_FPS * SLOW, 4), "idx": idx, "t": times,
           "luma": luma, "motion": motion, "dmed": round(dmed, 5)}, open(f"/home/user/fsreel/edit/idx/{name}.json", "w"))
print(f"{name}: {len(idx)} renders over {n / CAP_FPS * SLOW:.2f} game s, median gap {dmed * 1000:.1f} ms "
      f"(p90 {np.percentile(gaps, 90) * 1000:.1f} ms)")
