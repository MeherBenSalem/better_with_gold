#!/usr/bin/env bash
# Copies the version-independent Fellowship sources from the 1.21.1 reference root to other version roots:
#   tools/sync-shared.sh 1.20.1 26.2 26.3
# Version-specific files (Compat, the entities, the Warbanner item, renderers, screens, keys, the HUD hook, the
# creative tabs, items and the loader modules) are maintained per version and are never overwritten here.
set -euo pipefail
cd "$(dirname "$0")/.."
SRC=1.21.1/common/src/main/java/tn/nightbeam/better_with_gold
SHARED=(
  core
  ally/goal
  ally/AllyCombat.java
  ally/Fellowship.java
  ally/Fx.java
  ally/RasBridge.java
  ally/Sellswords.java
  ally/Threat.java
  ally/WarbandState.java
  ally/Warbands.java
  command/FellowshipCommands.java
  net/Net.java
  platform/services/IPlatformHelper.java
  registry/ModEntities.java
  client/Canvas.java
  client/ClientFellowship.java
  client/Icons.java
  client/Ui.java
  client/PartyHud.java
  client/RosterView.java
  client/WheelView.java
)
for root in "$@"; do
  DST=$root/common/src/main/java/tn/nightbeam/better_with_gold
  for path in "${SHARED[@]}"; do
    mkdir -p "$(dirname "$DST/$path")"
    if [ -d "$SRC/$path" ]; then
      rm -rf "${DST:?}/$path"
      cp -r "$SRC/$path" "$DST/$path"
    else
      cp "$SRC/$path" "$DST/$path"
    fi
  done
  mkdir -p "$root/common/src/test/java/tn/nightbeam/better_with_gold/core"
  cp 1.21.1/common/src/test/java/tn/nightbeam/better_with_gold/core/*.java "$root/common/src/test/java/tn/nightbeam/better_with_gold/core/"
  echo "synced shared sources into $root"
done
