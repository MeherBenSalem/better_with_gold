#!/bin/bash
# grab_cmds.sh -- starts the 1.21.1 Fabric dev server and QA client once through Gradle, saves their java command
# lines (one argument per line) to server_cmd.txt / client_cmd.txt for game.sh, then stops them.
set -u
source /home/user/env.sh
cd /home/user/better_with_gold
export QA_LOGS=/home/user/qa-logs/reel; mkdir -p $QA_LOGS; rm -f $QA_LOGS/*.log
tools/qa/devrun.sh start-server 1.21.1 fabric </dev/null >/dev/null 2>&1
for i in $(seq 1 300); do grep -q "RCON running" $QA_LOGS/fabric-server.log 2>/dev/null && break; sleep 2; done
for pid in $(pgrep -x java); do
  c=$(tr '\0' '\n' < /proc/$pid/cmdline)
  if echo "$c" | grep -q "fabric.dli.env=server"; then echo "$c" > /home/user/fsreel/server_cmd.txt; fi
done
tools/qa/devrun.sh start-client 1.21.1 fabric </dev/null >/dev/null 2>&1
for i in $(seq 1 300); do grep -q "joined the game" $QA_LOGS/fabric-server.log 2>/dev/null && break; sleep 2; done
for pid in $(pgrep -x java); do
  c=$(tr '\0' '\n' < /proc/$pid/cmdline)
  if echo "$c" | grep -q "fabric.dli.env=client"; then echo "$c" > /home/user/fsreel/client_cmd.txt; fi
done
tools/qa/devrun.sh stop-client; tools/qa/devrun.sh stop-server
wc -l /home/user/fsreel/server_cmd.txt /home/user/fsreel/client_cmd.txt
