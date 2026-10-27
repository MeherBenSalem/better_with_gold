# Notes for Claude

## This repository

Remnants | Fellowship (mod id `better_with_gold`, formerly Better With Gold). Version-independent code is edited in
`1.21.1/` and copied with `tools/sync-shared.sh 1.20.1 26.2 26.3`; every Minecraft call that differs between
versions lives in that version's `compat/Compat.java`. Run `./gradlew :common:coreRegression` in a version root for
the plain-Java checks, and `tools/qa/` (dev server + Xvfb client + `fellowship_checks.py`) for a real play-through.
Every number in docs and store copy comes from the code or the generated config (`tools/gen_docs.py`).

Wording: call the mod **Remnants | Fellowship** (never "Fellowship" alone), say "aggro" (Remnants: RPG Mobs already
uses "threat"), never write "first" or "only", and never use another game's name, logo or art.

## Videos (reels, trailers, shorts): the owner's standing rules

### Voiceover: always the NightBeam channel voice

Owner, 2026-10-27: "save that voice for all future videos i ask for".

- Every voiceover uses the channel voice "gamer": Chatterbox (MIT) cloning `voice/channel_ref.wav` with the runner
  `voice/chatterbox_tts.py`, both in the private repo `MeherBenSalem/moneymaker` (its `CHANNEL.md` → "The channel
  voice"). Attach that repo read-only to get them. The settings are fixed: `exaggeration=0.55, cfg_weight=0.3,
  temperature=0.7`, seed `1000 × take + line number`. Never copy the reference clip into a public repository.
- Never Kokoro (rejected twice: "way too AI generated", then "sleepy"), ElevenLabs or any paid TTS, or a real
  person's voice. Never speed up or time-stretch narration: time the edit around the real line lengths instead
  (Chatterbox reads about 1.3× longer than Kokoro).
- Energetic, never sleepy: punchy lines, exclamation marks on hype lines, numbers written out ("three thousand and
  one"), invented names spelled so they are read right ("Chrono Fall"). One-word lines often come out garbled, so give
  them more takes.
- Generate 4 takes of every line, run [`marketing/voice/pick_takes.py`](marketing/voice/pick_takes.py) (right words,
  right pitch, then the most energetic read), and generate takes 5–8 only for lines it reports with no good take.
- Setup: `python3 -m venv /home/user/ttsvenv && /home/user/ttsvenv/bin/pip install torch==2.6.0 torchaudio==2.6.0
  --index-url https://download.pytorch.org/whl/cpu && /home/user/ttsvenv/bin/pip install chatterbox-tts faster-whisper`.
  About 70 s of CPU per line: generate with the game closed.
- Mix: the voice gets only the channel narration chain,
  `highpass=f=75,equalizer=f=3500:t=q:w=1:g=1.5,deesser=i=0.35,acompressor=threshold=-20dB:ratio=2.5:attack=10:release=120`,
  and the music is sidechain-ducked under it.
- A worked example (plan with a fit check, takes, mix) is the Chronofall reel in `MeherBenSalem/TheYear3001`,
  `marketing/reel/`.

### Sound: production quality, never placeholders

Owner, 2026-10-27, on synthesized effects: "they feel way to gimmicky ... put better quality audio production ready
and not placeholders". Use real recorded sound effects from a library whose licence allows commercial use in videos
(the Chronofall reel uses Mixkit's free sound effects: no attribution required; `marketing/reel/source/fetch_sfx.sh`
and `sfx_pro.py` in `MeherBenSalem/TheYear3001` show the method). Layer cinematic impacts, explosions and whooshes,
skip anything labelled arcade, 8-bit or cartoon, measure each recording's hit and land it on its frame, and mix in
stereo under the voice. Never synthesize effects as stand-ins.

### Footage: always smooth

Owner: "make sure always video smooth no lag". Record with both game processes on a clock slowed 10× (libfaketime) so
every rendered frame is kept, sample by game time with cross-fades, never play gameplay slower than real time
(particles update on ticks and would judder), and check the final video has no repeated frames. The pipeline is in
`marketing/reel/README.md` and `marketing/reel/source/` (here and in the Chronofall reel).
