"""Word offsets inside each voice line (Whisper on each line on its own), for placing words on beats."""
import json
import numpy as np
import soundfile as sf
from faster_whisper import WhisperModel
from scipy.signal import resample_poly

E = "/home/user/fsreel/edit"
m = WhisperModel("small.en", device="cpu", compute_type="int8")
lines = json.load(open(f"{E}/lines.json"))
out = []
for i, line in enumerate(lines):
    a, sr = sf.read(f"{E}/vo/line_{i:02d}.wav", dtype="float32")
    segs, _ = m.transcribe(resample_poly(a, 16000, sr).astype(np.float32), word_timestamps=True, beam_size=5)
    ws = [{"w": w.word.strip(), "s": round(w.start, 3), "e": round(w.end, 3)} for s in segs for w in s.words]
    out.append({"i": i, "dur": round(len(a) / sr, 3), "words": ws})
    print(i, out[-1]["dur"], " ".join(f'{w["w"]}@{w["s"]}' for w in ws))
json.dump(out, open(f"{E}/lineword.json", "w"), indent=1)
