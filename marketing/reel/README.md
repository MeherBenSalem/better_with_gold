# Reel: "Never fight alone again!"

| File | Use |
| --- | --- |
| `fellowship-reel.mp4` | The Reel: 1080×1920, 30 fps, 36.5 s, H.264 + AAC, about −14 LUFS. Not tracked in git. Rebuild it with the steps below |
| `cover.jpg` | Cover image. The title sits inside the centre 3:4 area, so it survives the profile-grid crop. Not tracked in git |
| `caption.txt` | Post caption with hashtags |

## Structure

Short-form retention practice: the payoff in the opening frames (an Arc Storm strikes a pillager band), one idea per
shot, a label on every class and feature, word-timed captions for sound-off viewers, and an ending that loops into the
opening frame. Every cut, voice line and sound sits on the 125 BPM grid of the music bed; the Reel is 76 beats (19 bars)
long and starts on a beat, so the music loops with the picture.

| Time | Shot | Voiceover |
| --- | --- | --- |
| 0.0–2.4 | From behind a pillager band: the Arcanist's Arc Storm strikes them, their aggro labels flash up | "Never fight alone again!" |
| 2.4–5.8 | Through the player's eyes at the village bell: a sellsword's nameplate (name, class, level, price), one click with gold in hand, the oath; then the warband marching at sunset | "Hire a warband of sellswords!" |
| 5.8–7.7 | Behind the Bulwark as it charges: its Challenge ring bursts out among the raiders | "Bulwarks taunt, and hold the line!" |
| 7.7–10.1 | Over the Ranger's shoulder: a three-arrow volley into the band | "Rangers rain arrows!" |
| 10.1–12.5 | Above the raiders: the Skirmisher's whirl, then the Arc Storm on "storm" | "Arcanists call down the storm!" |
| 12.5–14.9 | The Mender's light pours into the Bulwark as it takes blows | "Menders keep everyone standing!" |
| 14.9–17.8 | Through the player's eyes: hold G, the command wheel, Charge; the warband charges | "Hold G, and command them all!" |
| 17.8–20.6 | The band at its Warbanner: Shield Wall, Wedge, then the charge, cut on each word | "Shield wall! Wedge! Charge!" |
| 20.6–24.5 | Over the shoulder: the Skirmisher is cut down, DOWNED, we crouch, the revive ring fills, it stands | "Someone goes down? Crouch, and pull them back up!" |
| 24.5–27.4 | Planting the Warbanner; the band runs in and rings it | "Plant the Warbanner, and they hold the ground!" |
| 27.4–29.8 | The roster: live portraits, stats, and the Chronicle written from the fights just filmed | "Every ally writes their own chronicle." |
| 29.8–32.6 | Title card over the warband walking out of the sunset; the name lands on "Remnants" | "It's called Remnants Fellowship." |
| 32.6–36.5 | The warband lined up at sunset, the Warbanner behind; a "comment below" prompt; the strike that opens the Reel follows | "Who joins your warband?" |

## How it was made

- **Footage:** unedited captures of the 1.21.1 Fabric development client of this repository, in a 1080×1920 window on
  a portrait virtual display, joined to its development server. Everything on screen is the mod running: the fights
  are real fights between the warband (`/fellowship recruit`, one of each class) and pillagers and vindicators; the
  abilities fire on their own; hiring, the command wheel, crouch-reviving and planting the Warbanner are real key
  presses and clicks. `source/capture/shots.py` stages and records every shot (`setup` once, then one function per
  shot). The world is seed `8675309`, with a levelled field east of the plains village at (-304, -320); the battles face
  west into the sunset (time 12300).
- **Smooth playback:** both game processes ran on a clock slowed 10× with libfaketime (`source/capture/game.sh`), and
  the screen was grabbed at 15 fps, so every capture holds about 125 rendered frames per game second.
  `source/prep.py` indexes the unique frames and their game time; `source/compose.py` asks for a game time per output
  frame and cross-fades the two renders around it, so no frame repeats or skips and nothing plays slower than real
  time; fast passages get a 180° shutter blur. `source/verify.py` checks the result for repeated frames and judder.
- **Voice:** the NightBeam channel voice ("gamer"): [Chatterbox](https://github.com/resemble-ai/chatterbox) (MIT)
  cloning the channel's reference clip with the channel's fixed settings (exaggeration 0.55, cfg_weight 0.3,
  temperature 0.7, seed 1000 × take + line number), run with the channel's own runner. The reference clip lives in the
  private channel repository, not here. Every line was generated in 4 takes (`source/voice/script.json`) and picked with
  [`marketing/voice/pick_takes.py`](../voice/pick_takes.py) (right words, right pitch, then the most energetic read);
  two lines had no good take and got takes 5 to 8 (`source/voice/picks.json`). Nothing is sped up or time-stretched:
  `source/plan.py` gives each shot the beats its line needs and refuses a plan where a line would not fit. Word offsets
  (`source/lineword.py`) put the Arc Storm on "storm", the formation cuts on "Wedge" and "Charge", and the title on
  "Remnants". The voice gets nothing but the channel narration chain (low cut at 75 Hz, +1.5 dB at 3.5 kHz, de-ess, 2.5:1
  compression) and the music is sidechain-ducked under it (`source/final.sh`). Captions are timed per word from a
  Whisper transcript of the finished voiceover (`source/align.py`, `source/captions.py`).
- **Sound effects:** 43 professional recordings from [Mixkit](https://mixkit.co/free-sound-effects/) under the Mixkit
  Sound Effects Free License (free for commercial use in videos, no attribution required), placed by
  `source/sfx_pro.py`: cinematic thunder impacts on the Arc Storms, coins, a bell and a choir on the hire, a shield on
  steel and an electric shockwave on the Challenge, arrows on the volley, a war horn on the wheel's Charge, armour and
  sword whooshes on the formations, a cry and heartbeats when the Skirmisher goes down, a choir when it stands, a
  trailer impact and war drums on the Warbanner, and a movie-logo hit on the title. Each recording's main hit is
  measured and lands on the frame of the event it belongs to (found on the contact sheets of `source/sheet.py`,
  `source/src_times.json`); the whoosh-impact into the opening strike wraps to the end of the Reel and carries the
  loop. `source/fetch_sfx.sh` downloads them by their Mixkit ids (the files are not stored here).
- **Music:** "Epical Drums 04" from [Mixkit](https://mixkit.co/free-stock-music/) (Mixkit Stock Music Free License),
  bars 34 to 52 (`source/audio.py`), with short dips before the two Arc Storms and the title.
- **Edit:** `source/plan.py` is the single source of truth (beat grid, shots, time maps, cues). `source/overlay.html`
  is rendered frame by frame with Playwright (`source/render_overlay.cjs`) in the listing's look: Cinzel and
  EB Garamond (both OFL, in `marketing/source/fonts/`), gold on dark, the mod's class colours and its own pixel icons.

## Posting checklist

- Upload the MP4 as a Reel and set `cover.jpg` as the cover. Keep the original audio; a trending track on top would
  bury the voiceover.
- Paste `caption.txt`. Put the Modrinth link in your bio before posting, since the caption points there.
- Alt text, for example: "Minecraft gameplay: a warband of five hired allies fights pillagers at sunset; lightning
  strikes, a knight taunts, an archer fires three arrows, a healer heals, a command wheel, a fallen ally is revived, a
  banner is planted."
- Pin a comment that answers the question yourself ("a Bulwark, always") so replies start.
- Post when your audience is most active and reply to early comments quickly. Cross-post the same file to TikTok and
  YouTube Shorts.

## Rebuilding

The scripts were run from a scratch working folder (`E` at the top of each script; the raw captures, the voice lines
and the rendered frames live there, not in git). Copy `source/` there with `marketing/source/fonts/` as `fonts/`,
`marketing/source/icons/` as `icons/` and `marketing/images/icon.png`, then:

1. `capture/grab_cmds.sh`, `capture/tryseed.sh 8675309`, the field (see `shots.py`), `capture/game.sh` for the server
   and the client on the slowed clock, `capture/shots.py setup`, then `capture/shots.py <shot>...` (the roster with
   the GUI-scale-2 client).
2. `prep.py <clip>` per clip and `sheet.py <clip>` to read the event times into `src_times.json`.
3. The channel runner over `voice/script.json` (takes 1 to 4), `marketing/voice/pick_takes.py`, then `lineword.py`.
4. `plan.py`, `build_vo.py`, `align.py`, `captions.py`, `audio.py`, `fetch_sfx.sh` then `sfx_pro.py`.
5. `compose.py`, `node render_overlay.cjs`, `final.sh`, and `verify.py reel.mp4 edl.json`.
