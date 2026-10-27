#!/bin/bash
# tryseed.sh <seed> -- fresh reel world at normal speed; prints the nearest plains village and spawn
S=$1
cd /home/user/fsreel/server_run
pkill -f "fabric.dli.env=server" 2>/dev/null; sleep 2
rm -rf reel logs
sed -i "s/^level-seed=.*/level-seed=$S/" server.properties
nohup /home/user/fsreel/edit/capture/game.sh server 1 > /home/user/fsreel/server.log 2>&1 &
until grep -q "RCON running" /home/user/fsreel/server.log; do sleep 1; grep -q "Exception in server tick\|Failed to start" /home/user/fsreel/server.log && exit 1; done
python3 /home/user/fsreel/edit/capture/rc.py "locate structure minecraft:village_plains" "execute positioned 0 64 0 run locate biome minecraft:plains" "setworldspawn ~ ~ ~" 2>&1 | head -5
