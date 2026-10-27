#!/bin/bash
# Downloads the reel's sound effects from Mixkit (Mixkit Sound Effects Free License: free for commercial use in
# videos, no attribution required) into $1 (default ./sfxlib), as WAV. sfx_pro.py reads them from there.
# A few files are only served as MP3 previews; those are converted to WAV. Nothing here is synthesized.
set -e
D=${1:-sfxlib}; mkdir -p "$D"
for id in 2780 2289 2785 461 2175 459 2918 2908 788 2900 2901 2762 2763 2764 2765 2794 2795 2792 2775 2774 1993 1989 3187 \
          2171 2784 2771 2767 1491 2769 1300 1283 1286 2599 2345 658 873 2583 2352 1492 2903 498 2768 490 2584 930 2629 1143 \
          1469 2343 2790; do
  f="$D/$id.wav"; [ -s "$f" ] && continue
  curl -sf --max-time 60 -o "$f" "https://assets.mixkit.co/active_storage/sfx/$id/$id.wav" || rm -f "$f"
  if ! head -c 4 "$f" 2>/dev/null | grep -q RIFF; then
    curl -sf --max-time 60 -o "$D/$id.mp3" "https://assets.mixkit.co/active_storage/sfx/$id/$id-preview.mp3"
    ffmpeg -loglevel error -y -i "$D/$id.mp3" -c:a pcm_s16le "$f" && rm "$D/$id.mp3"
  fi
done
ls "$D" | wc -l
