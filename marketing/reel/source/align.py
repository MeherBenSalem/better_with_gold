import json
from faster_whisper import WhisperModel
m = WhisperModel("small.en", device="cpu", compute_type="int8")
import soundfile as sf
audio, _ = sf.read("/home/user/fsreel/edit/vo16.wav", dtype="float32")
segs, info = m.transcribe(audio, word_timestamps=True, beam_size=5, vad_filter=False)
words = []
for s in segs:
    for w in s.words:
        words.append({"w": w.word.strip(), "start": round(w.start, 3), "end": round(w.end, 3), "p": round(w.probability, 2)})
json.dump(words, open("/home/user/fsreel/edit/words.json", "w"), indent=1)
print(" ".join(w["w"] for w in words))
print("low confidence:", [(w["w"], w["p"]) for w in words if w["p"] < 0.6])
