"""Music bed for the Remnants | Fellowship reel: Mixkit "Epical Drums 04" (id 679, Mixkit Stock Music Free License),
cut from bar 34 for exactly the reel's 76 beats (19 bars), so the end flows back into the start when the reel loops.
Short dips before the two big strikes and the title give them room. Writes music.wav (48 kHz stereo)."""
import json
import subprocess

import numpy as np
import soundfile as sf

E = "/home/user/fsreel/edit"
C = json.load(open(f"{E}/cues.json"))
SR = 48000
D = C["duration"]
subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-ss", f"{C['music_in']:.4f}", "-i", "/home/user/musiclib/679.mp3",
                "-t", f"{D + 0.05:.4f}", "-ar", str(SR), "-ac", "2", f"{E}/music_raw.wav"], check=True)
m, sr = sf.read(f"{E}/music_raw.wav", dtype="float32")
N = int(round(D * SR))
m = m[:N]
if len(m) < N:
    m = np.pad(m, ((0, N - len(m)), (0, 0)))
# a 15 ms equal-power seam at the loop point
k = int(0.015 * SR)
w = np.linspace(0, 1, k)[:, None]
m[:k] = m[:k] * np.sqrt(w) + m[-k:] * np.sqrt(1 - w)
m[-k:] *= np.sqrt(1 - w)


def dip(at, length, depth_db=-9):
    i0, i1 = int((at - length) * SR), int(at * SR)
    g = 10 ** (depth_db / 20)
    f = int(0.04 * SR)
    env = np.ones(i1 - i0)
    env[:f] = np.linspace(1, g, f)
    env[f:] = g
    m[i0:i1] *= env[:, None]


ev = C["events"]
dip(ev["storm"], 0.22)
dip(C["title"][0], 0.25)
dip(ev["plant"], 0.15, -6)
sf.write(f"{E}/music.wav", (m / max(1e-6, np.abs(m).max()) * 0.9).astype(np.float32), SR)
print("music.wav", D)
