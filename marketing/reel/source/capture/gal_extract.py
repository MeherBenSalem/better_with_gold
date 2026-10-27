#!/usr/bin/env python3
"""gal_extract.py <name> [keep]: pulls the distinct renders out of raw/gal_<name>.mkv into gallery/<name>_NNN.png
(about `keep` of them, evenly spread), then deletes the capture."""
import os, sys
import cv2
import numpy as np
name = sys.argv[1]
keep = int(sys.argv[2]) if len(sys.argv) > 2 else 90
src = f'/home/user/fsreel/raw/gal_{name}.mkv'
cap = cv2.VideoCapture(src)
prev, frames = None, []
while True:
    ok, f = cap.read()
    if not ok:
        break
    g = cv2.resize(cv2.cvtColor(f, cv2.COLOR_BGR2GRAY), (240, 135)).astype(np.int16)
    if prev is None or np.abs(g - prev).max() > 6:
        frames.append(f)
    prev = g
step = max(1, len(frames) // keep)
out = '/home/user/fsreel/gallery'
for k, f in enumerate(frames[::step]):
    cv2.imwrite(f'{out}/{name}_{k:03d}.png', f)
print(name, len(frames), 'renders, kept', len(frames[::step]))
os.remove(src)
