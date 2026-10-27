#!/usr/bin/env python3
"""End-to-end Fellowship checks on a running dev server and QA client (see devrun.sh).

Drives the server over RCON and the client with xdotool, prints PASS/FAIL per check and saves screenshots to
the folder given with --shots. Run with the client already in the world:

    python3 tools/qa/fellowship_checks.py --shots /tmp/fs-qa/shots
"""
from __future__ import annotations

import argparse
import re
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from client import PLAYER, Session  # noqa: E402

RESULTS = []


def check(name, ok, detail=''):
    RESULTS.append((name, ok, detail))
    print(('PASS ' if ok else 'FAIL ') + name + (': ' + detail if detail else ''), flush=True)


def as_player(s, command):
    return s.cmd(f'execute as {PLAYER} at {PLAYER} run {command}')


def ally_count(s, extra=''):
    return count(s, f'@e[type=better_with_gold:ally{extra}]')


def count(s, selector):
    out = s.cmd(f'execute if entity {selector}')
    m = re.search(r'count: (\d+)', out, re.IGNORECASE)
    return int(m.group(1)) if m else 0


def clear(s, selector):
    """Kills everything matching selector. A killed slime splits into smaller ones when its death animation ends
    (20 ticks later), so this repeats, past each animation, until nothing is left."""
    for _ in range(6):
        if not count(s, selector):
            return
        s.cmd(f'kill {selector}')
        time.sleep(1.6)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--shots', default='/tmp/fs-qa/shots')
    ap.add_argument('--display', default=':1')
    args = ap.parse_args()
    shots = Path(args.shots)
    s = Session(display=args.display)
    # 26.x renamed the game rules (doMobSpawning is spawn_mobs, and monsters have their own spawn_monsters); each
    # server rejects the spellings it does not know.
    s.cmd(f'op {PLAYER}', 'gamerule doDaylightCycle false', 'gamerule advance_time false', 'time set 6000', 'weather clear',
          'gamerule doMobSpawning false', 'gamerule spawn_mobs false', 'gamerule spawn_monsters false',
          f'gamemode survival {PLAYER}', f'effect give {PLAYER} minecraft:resistance 99999 4 true', f'tp {PLAYER} 0 -60 0 0 10')
    clear(s, '@e[type=!minecraft:player]')  # the flat world fills with slimes while the server starts
    time.sleep(2)

    # Recruit one of each class.
    for cls, lvl in (('bulwark', 6), ('skirmisher', 4), ('ranger', 5), ('mender', 5), ('arcanist', 4)):
        out = as_player(s, f'fellowship recruit {cls} {lvl}')
        check(f'recruit {cls}', 'joins your warband' in out, out.strip())
    time.sleep(3)
    check('five allies in the world', ally_count(s) == 5, str(ally_count(s)))
    out = as_player(s, 'fellowship')
    check('warband summary', 'in the world' in out, out.strip())

    # Formation and orders.
    as_player(s, 'fellowship formation wedge')
    as_player(s, 'fellowship order follow')
    s.cmd(f'tp {PLAYER} 0 -60 12 0 10')
    near = 0
    for _ in range(12):
        time.sleep(1)
        near = ally_count(s, ',distance=..8')
        if near >= 4:
            break
    s.tp(0, -60, 12, yaw=180, pitch=15)
    time.sleep(1)
    s.hud(True)
    s.shot(shots / 'formation.png')
    check('allies follow into formation', near >= 4, f'{near} within 8 blocks')

    # Combat: zombies against the warband, aggro labels.
    s.cmd('difficulty normal')
    for i in range(4):
        s.cmd(f'summon minecraft:zombie {6 + i} -60 26 {{PersistenceRequired:1b}}')
    seen_labels = 0
    # The zombies start about 15 blocks out; a label appears once an ally has hit one (or been hit), which on a slow
    # software-rendered run can take most of 10 seconds.
    for _ in range(25):
        time.sleep(0.6)
        seen_labels = max(seen_labels, count(s, '@e[type=better_with_gold:aggro_label]'))
        if seen_labels:
            s.shot(shots / 'combat.png')
            break
    check('aggro labels ride engaged enemies', seen_labels > 0, str(seen_labels))
    for _ in range(30):
        if count(s, '@e[type=minecraft:zombie]') == 0:
            break
        time.sleep(1)
    zombies = count(s, '@e[type=minecraft:zombie]')
    check('warband kills the zombies', zombies == 0, f'{zombies} left')

    # After the fight the band follows the leader again (a formation goal that stopped re-pathing once froze some).
    as_player(s, 'fellowship order follow')
    s.cmd(f'tp {PLAYER} 0 -60 -8 180 10')
    after = 0
    for _ in range(15):
        time.sleep(1)
        m = re.search(r'count: (\d+)', s.cmd(f'execute at {PLAYER} if entity @e[type=better_with_gold:ally,distance=..8]'), re.IGNORECASE)
        after = int(m.group(1)) if m else 0
        if after == 5:
            break
    check('allies follow again after the fight', after == 5, f'{after} within 8 blocks')
    s.cmd(f'tp {PLAYER} 0 -60 12 0 10')
    time.sleep(3)

    # Downed and revived.
    # Down one ally (tagged, so the revive check reads the same one) and keep the Mender away so the player revives.
    s.cmd('effect clear @e[type=better_with_gold:ally]', 'tag @e[type=better_with_gold:ally] remove qa_downed', 'tag @e[type=better_with_gold:ally] remove qa_skip')
    as_player(s, 'fellowship order hold')
    for _ in range(5):
        record = s.cmd(f'execute as {PLAYER} at {PLAYER} run data get entity @e[type=better_with_gold:ally,limit=1,sort=nearest,tag=!qa_skip] Fellowship')
        target = '@e[type=better_with_gold:ally,limit=1,sort=nearest,tag=!qa_skip]'
        s.cmd(f'execute at {PLAYER} run tag {target} add ' + ('qa_skip' if '\\"classId\\":\\"mender\\"' in record or '"classId":"mender"' in record else 'qa_downed'))
        if count(s, '@e[tag=qa_downed]'):
            break
    s.cmd('damage @e[tag=qa_downed,limit=1] 500 minecraft:mob_attack')
    time.sleep(1)
    downed = s.cmd('data get entity @e[tag=qa_downed,limit=1] FellowshipDowned')
    check('ally is downed instead of killed', ally_count(s) == 5 and 'has the following entity data: 0' not in downed, downed.strip())
    s.shot(shots / 'downed.png')
    # Walk to the downed ally and crouch to revive.
    s.cmd(f'execute as {PLAYER} at @e[tag=qa_downed,limit=1] run tp {PLAYER} ~1 ~ ~')
    s.focus()
    s.xdo('keydown', 'shift')
    time.sleep(4.5)
    s.xdo('keyup', 'shift')
    time.sleep(1)
    after = s.cmd('data get entity @e[tag=qa_downed,limit=1] FellowshipDowned')
    check('crouching revives the ally', after.strip().endswith(': 0'), after.strip())
    as_player(s, 'fellowship order follow')

    # Roster screen and command wheel.
    s.key('u')
    time.sleep(2.5)
    s.shot(shots / 'roster.png')
    s.key('Escape')
    time.sleep(0.5)
    s.focus()
    s.xdo('keydown', 'g')
    time.sleep(1.2)
    s.move(960, 760)
    time.sleep(0.6)
    s.shot(shots / 'wheel.png')
    s.xdo('keyup', 'g')
    time.sleep(1)
    summary = as_player(s, 'fellowship')
    check('wheel gives an order', 'order' in summary, summary.strip())

    # Warbanner: plant it on the ground in front of the player, allies form up around it. Guarding allies still
    # fight enemies near the banner, so anything left over is counted (the log shows it) and cleared first.
    strays = '@e[type=!minecraft:player,type=!better_with_gold:ally,type=!better_with_gold:aggro_label]'
    kinds = {k: count(s, f'@e[type=minecraft:{k}]') for k in ('slime', 'item', 'arrow', 'experience_orb', 'zombie')}
    print(f'note: {count(s, strays)} other entities cleared before the Warbanner check {kinds}', flush=True)
    clear(s, strays)
    s.cmd(f'give {PLAYER} better_with_gold:warbanner', f'item replace entity {PLAYER} weapon.mainhand with better_with_gold:warbanner')
    s.tp(0, -60, 30, yaw=0, pitch=60)
    time.sleep(1)
    s.click(3)
    time.sleep(6)
    summary = as_player(s, 'fellowship')
    check('warbanner planted', 'banner planted' in summary, summary.strip())
    near_banner = ally_count(s, ',x=0,y=-61,z=31,distance=..7')
    s.tp(0, -59, 22, yaw=0, pitch=10)
    time.sleep(1.5)
    s.shot(shots / 'banner.png')
    check('allies guard the banner', near_banner >= 4, f'{near_banner} within 7 blocks')

    print()
    failed = [r for r in RESULTS if not r[1]]
    print(f'{len(RESULTS) - len(failed)}/{len(RESULTS)} checks passed')
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main())
