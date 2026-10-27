#!/bin/bash
# game.sh server|client [clock factor, default 0.1] [client run dir]
# Relaunches the Remnants | Fellowship 1.21.1 Fabric dev server or client (command lines grabbed from a normal dev run
# into server_cmd.txt / client_cmd.txt) on a clock slowed 10x with libfaketime, so the software renderer keeps
# up with game time. The client opens a 1080x1920 window on the portrait display :2.
F=${2:-0.1}
J=/usr/lib/jvm/java-21-openjdk-amd64/bin/java
if [ "$F" != 1 ]; then
  export FAKETIME="+0 x$F" FAKETIME_DONT_FAKE_MONOTONIC=0 LD_PRELOAD=/usr/lib/x86_64-linux-gnu/faketime/libfaketimeMT.so.1
fi
if [ "$1" = server ]; then
  cd /home/user/fsreel/server_run
  mapfile -t CMD < /home/user/fsreel/server_cmd.txt
  CMD[0]=$J
  exec "${CMD[@]}"
else
  cd "${3:-/home/user/fsreel/client_run}"
  mapfile -t CMD < /home/user/fsreel/client_cmd.txt
  CMD[0]=$J
  n=${#CMD[@]}
  if [ -n "${LANDSCAPE:-}" ]; then CMD[$n-3]=1920; CMD[$n-1]=1080; DISP=:1; else CMD[$n-3]=1080; CMD[$n-1]=1920; DISP=:2; fi
  export DISPLAY=$DISP LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe MESA_GL_VERSION_OVERRIDE=4.5 MESA_GLSL_VERSION_OVERRIDE=450
  exec "${CMD[@]}"
fi
