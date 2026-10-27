#!/bin/bash
# cap.sh <name> <real_seconds> -- grabs the portrait display at 15 fps (every frame the slowed game renders)
ffmpeg -loglevel error -y -f x11grab -video_size 1080x1920 -framerate 15 -draw_mouse 0 -i :2 -t $2 -threads 2 -c:v libx264 -preset ultrafast -crf 13 -pix_fmt yuv420p /home/user/fsreel/raw/$1.mkv
