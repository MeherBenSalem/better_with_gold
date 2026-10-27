"""Contact sheet of a prepped clip by game time: sheet.py <clip> [t0 t1 step] -> sheets/<clip>.jpg, every tile labelled."""
import json, sys
import cv2
import numpy as np

E = "/home/user/fsreel/edit"
name = sys.argv[1]
J = json.load(open(f"{E}/idx/{name}.json"))
t0 = float(sys.argv[2]) if len(sys.argv) > 2 else 0.0
t1 = float(sys.argv[3]) if len(sys.argv) > 3 else J["t"][-1]
step = float(sys.argv[4]) if len(sys.argv) > 4 else 0.25
want = []
t = t0
while t <= t1 + 1e-6:
    k = min(range(len(J["t"])), key=lambda i: abs(J["t"][i] - t))
    want.append((J["idx"][k], J["t"][k]))
    t += step
need = {i for i, _ in want}
cap = cv2.VideoCapture(f"/home/user/fsreel/raw/{name}.mkv")
frames, n = {}, 0
while len(frames) < len(need):
    ok, f = cap.read()
    if not ok:
        break
    if n in need:
        frames[n] = cv2.resize(f, (216, 384), interpolation=cv2.INTER_AREA)
    n += 1
tiles = []
for i, tt in want:
    f = frames.get(i, np.zeros((384, 216, 3), np.uint8)).copy()
    cv2.rectangle(f, (0, 0), (216, 26), (0, 0, 0), -1)
    cv2.putText(f, f"{tt:.2f}", (6, 20), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (80, 220, 255), 2)
    tiles.append(f)
cols = 10
while len(tiles) % cols:
    tiles.append(np.zeros((384, 216, 3), np.uint8))
rows = [np.hstack(tiles[r:r + cols]) for r in range(0, len(tiles), cols)]
cv2.imwrite(f"{E}/sheets/{name}.jpg", np.vstack(rows), [cv2.IMWRITE_JPEG_QUALITY, 80])
print(f"sheets/{name}.jpg: {len(want)} tiles {t0:.2f}-{t1:.2f}")
