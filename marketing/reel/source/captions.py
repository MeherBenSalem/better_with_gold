"""Word-timed caption chunks: script words (exact spelling) timed by the Whisper word timestamps."""
import json, re
T = json.load(open("/home/user/fsreel/edit/timeline.json"))
W = json.load(open("/home/user/fsreel/edit/words.json"))
SKIP = {11}                       # "It's called Remnants Fellowship" is the title card
MAXW = {0: 4, 7: 2, 8: 3}
chunks = []
for line in T["lines"]:
    toks = line["cap"].upper().replace("...", "…").split()
    ws = [w for w in W if w["start"] >= line["start"] - 0.25 and w["end"] <= line["end"] + 0.35]
    if len(ws) == len(toks):
        times = [(max(w["start"], line["start"]), w["end"]) for w in ws]  # Whisper can start a word early
    else:  # spread the line's duration by character count
        total = sum(len(t) for t in toks); t0 = line["start"]; times = []
        for t in toks:
            d = (line["end"] - line["start"]) * len(t) / total; times.append((t0, t0 + d)); t0 += d
    if line["i"] in SKIP:
        continue
    words = [{"text": t, "start": round(a, 3), "end": round(b, 3)} for t, (a, b) in zip(toks, times)]
    n = MAXW.get(line["i"], 3)
    # break into chunks of up to n words, preferring breaks after punctuation
    cur = []
    for k, w in enumerate(words):
        cur.append(w)
        nxt = words[k + 1]["text"] if k + 1 < len(words) else ""
        closes_next = bool(re.search(r"[.,?!…]$", nxt))
        if re.search(r"[.,?!…]$", w["text"]) or (len(cur) >= n and not (closes_next and len(cur) < 4)):
            chunks.append(cur); cur = []
    if cur:
        chunks.append(cur)
    # never end a chunk on a dangling function word
    WEAK = {"OF", "TO", "THE", "A", "YOUR", "WHO", "THAT", "IT", "AND", "WITH"}
    for k in range(len(chunks) - 1):
        if len(chunks[k]) > 1 and chunks[k][-1]["text"] in WEAK and len(chunks[k + 1]) < 4 and chunks[k + 1][0]["start"] < line["end"]:
            chunks[k + 1].insert(0, chunks[k].pop())
out = []
for i, c in enumerate(chunks):
    nxt = chunks[i + 1][0]["start"] if i + 1 < len(chunks) else c[-1]["end"] + 0.6
    end = min(c[-1]["end"] + 0.45, nxt)
    # never run into the next voice line, even one shown as a graphic
    later = [l["start"] for l in T["lines"] if l["start"] > c[-1]["end"]]
    if later:
        end = min(end, later[0] - 0.03)
    out.append({"start": c[0]["start"], "end": round(end, 3), "words": c})
# the closing question stays up with the comment prompt until just before the loop
out[-1]["end"] = round(T["duration"] - 0.15, 3)
json.dump(out, open("/home/user/fsreel/edit/captions.json", "w"), indent=1)
for c in out:
    print(f'{c["start"]:6.2f}-{c["end"]:6.2f}  ' + " ".join(w["text"] for w in c["words"]))
