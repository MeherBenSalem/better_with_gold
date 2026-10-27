#!/usr/bin/env python3
"""Store-page gallery: scenes composed for 16:9 on the 1920x1080 client (display :1, 60 degree field of view),
run at normal speed; every scene is a burst of full-resolution screenshots to pick from.

  python3 gallery.py <scene>...      (the scenes below, or any shot of shots.py / stills.py)
"""
import math
import os
import subprocess
import sys
import threading
import time

import stills  # noqa: F401  (patches shots.Rig for the landscape display)
import shots

OUT = '/home/user/fsreel/gallery'
os.makedirs(OUT, exist_ok=True)
FX, FY, FZ = shots.FX, shots.FY, shots.FZ
P = shots.P
BRIGHT = 11800  # warm but still lit, for the feature shots
battle = shots.battle


shots.SLOW = float(os.environ.get('GSLOW', '1'))  # 10 with the game on the slowed clock (restart.sh 0.1)


def cap(self, name, game_seconds, every=0.2):
    if shots.SLOW > 1:  # every rendered frame, lossless, to pick the exact moment from
        self.capproc = subprocess.Popen(['ffmpeg', '-loglevel', 'error', '-y', '-f', 'x11grab', '-video_size', '1920x1080',
                                         '-framerate', '15', '-draw_mouse', '0', '-i', ':1', '-t', str(game_seconds * shots.SLOW),
                                         '-threads', '2', '-c:v', 'libx264rgb', '-preset', 'ultrafast', '-crf', '0',
                                         f'/home/user/fsreel/raw/gal_{name}.mkv'])
        time.sleep(1.0)
        return

    def run():
        t0, k = time.time(), 0
        while time.time() - t0 < game_seconds:
            subprocess.run(['import', '-window', 'root', f'{OUT}/{name}_{k:02d}.png'], env=dict(os.environ, DISPLAY=':1'))
            k += 1
            time.sleep(every)
    self.capproc = threading.Thread(target=run)
    self.capproc.start()
    time.sleep(0.2)


def wait_cap(self):
    if self.capproc:
        self.capproc.wait() if hasattr(self.capproc, 'wait') else self.capproc.join()
        self.capproc = None


shots.Rig.cap = cap
shots.Rig.wait_cap = wait_cap
_hud, _view = shots.Rig.hud, shots.Rig.view


def hud(self, visible):
    """F1 only reaches a focused window: an unfocused press left the rig's idea of the HUD inverted."""
    self.focus()
    _hud(self, visible)


def view(self, mode):
    self.focus()
    _view(self, mode)


shots.Rig.hud = hud
shots.Rig.view = view


def g_storm(s):
    """Hero: low behind the warband's flank, the sun going down beyond the raiders; the charge and the Arc Storm."""
    battle(s, 'g_storm', {'pos': (FX + 9.5, FY + 2.0, FZ + 5.5), 'look': (FX - 6, FY + 2.4, FZ - 0.5)}, seconds=10, enemies=(6, 3))


def g_storm2(s):
    """Hero, closer: just south of the warband's line, the raiders and the sun beyond; the Arc Storm lands on the pack."""
    battle(s, 'g_storm2', {'pos': (FX + 3.0, FY + 2.3, FZ + 5.2), 'look': (FX - 7, FY + 2.0, FZ - 0.3)}, seconds=9, enemies=(6, 3))


def g_storm_side(s):
    """Above the raiders' south flank: the pack as the storm forks through it, the warband crashing in from the right."""
    battle(s, 'g_storm_side', {'pos': (FX - 4.5, FY + 4.0, FZ + 7.0), 'look': (FX - 6.5, FY + 0.8, FZ)}, seconds=9, enemies=(6, 3))


def g_taunt(s):
    """Beside the Bulwark as it reaches the raiders and Challenges them."""
    battle(s, 'g_taunt', {'pos': (FX + 0.5, FY + 2.0, FZ + 5.0), 'look': (FX - 5, FY + 1.2, FZ - 0.8)}, seconds=8,
           order=('ranger', 'skirmisher', 'bulwark', 'arcanist', 'mender'))


def g_volley(s):
    """Low behind the Ranger at the south end of the line: three arrows at once."""
    battle(s, 'g_volley', {'pos': (FX + 9.0, FY + 1.6, FZ + 5.6), 'look': (FX - 6, FY + 1.6, FZ + 0.5)}, seconds=8,
           order=('skirmisher', 'bulwark', 'arcanist', 'mender', 'ranger'))


def g_aggro(s):
    """Close on the raiders' flank with the labels on: who each one is after, TAUNTED once the Bulwark Challenges."""
    battle(s, 'g_aggro', {'pos': (FX - 3.0, FY + 2.6, FZ + 6.0), 'look': (FX - 5.5, FY + 1.6, FZ)}, seconds=9, hud=True,
           enemies=(4, 2))
    s.hud(False)


def g_mend(s):
    """Low and close at sunset: the Bulwark takes blows, the Mender's light arcs into it."""
    s.clear_field()
    s.reset(12150)
    s.fresh()
    s.cmd(f'effect clear {s.ally("bulwark")}')
    s.place('bulwark', FX - 0.2, FZ - 0.9, -90)
    s.place('mender', FX + 1.3, FZ + 1.1, -130)
    for cls, z in (('skirmisher', -9), ('ranger', -11), ('arcanist', -13)):
        s.place(cls, FX + 9, FZ + z, 90)
    s.order('hold')
    s.tp(FX + 4.15, FY + 1.6, FZ - 2.6, facing=(FX + 0.55, FY + 1.2, FZ + 0.1))  # side on, into the sunset
    s.hud(False)
    s.g(1.2)
    s.cap('g_mend', 9)
    for k in range(3):
        s.g(0.4)
        s.cmd(f'damage {s.ally("bulwark")} 9 minecraft:generic')
        s.g(2.6)
    s.wait_cap()
    s.fresh()


def g_downed(s):
    """Over the shoulder, in third person: the Skirmisher is cut down in front of us, we crouch, it gets back up."""
    s.clear_field()
    s.reset(12150, mode='survival')
    s.fresh()
    s.place('skirmisher', FX - 0.6, FZ + 0.9, -90)
    s.place('bulwark', FX - 4.5, FZ - 1.5, 90)
    s.place('ranger', FX - 4.0, FZ + 4.5, 90)
    s.place('arcanist', FX + 5, FZ - 4, 90)
    s.place('mender', FX + 40, FZ + 30, 90)  # the Mender would revive it before us
    s.order('hold')
    s.cmd(f'data merge entity {s.ally("skirmisher")} {{NoAI:1b}}')
    s.tp(FX + 0.8, FY + 1, FZ - 0.3, 31, 28)  # the Skirmisher ahead and to the right of the player
    s.view(1)
    s.hud(True)
    s.clear_chat()
    s.g(1.5)
    s.cap('g_downed', 7.5)
    s.g(0.6)
    s.cmd(f'damage {s.ally("skirmisher")} 500 minecraft:mob_attack')
    s.g(1.4)
    s.x('keydown', 'shift')
    s.g(3.8)
    s.x('keyup', 'shift')
    s.wait_cap()
    s.cmd(f'data merge entity {s.ally("skirmisher")} {{NoAI:0b}}')
    s.view(0)
    s.fresh()


def g_lineup(s, name='g_lineup', tod=12150, hud=True):
    """The five, close and at eye level, their nameplates up, the Warbanner raised behind them, the sun going down."""
    s.clear_field()
    s.reset(tod, mode='survival')
    s.fresh()
    s.formation('line')
    s.order('hold')
    s.me('fellowship banner lift')
    s.cmd(f'gamemode survival {P}', f'item replace entity {P} weapon.mainhand with better_with_gold:warbanner')
    s.tp(FX - 1.6, FY + 1, FZ + 0.75, 90, 39)
    s.focus()
    s.g(0.6)
    s.x('click', '3')
    s.g(0.6)
    s.cmd(f'clear {P}', f'gamemode spectator {P}')
    s.line_up(FX - 1.2, z0=-3.0, step=1.5, yaw=-90, order=('ranger', 'skirmisher', 'bulwark', 'arcanist', 'mender'))
    s.order('hold')
    s.g(1.0)
    # frozen where they stand, each one turned towards the camera
    for k, cls in enumerate(('ranger', 'skirmisher', 'bulwark', 'arcanist', 'mender')):
        z = FZ - 3.0 + 1.5 * k
        yaw = math.degrees(math.atan2(-(FX + 3.6 - (FX - 1.2)), FZ + 0.1 - z))
        s.cmd(f'data merge entity {s.ally(cls)} {{NoAI:1b}}', f'tp {s.ally(cls)} {FX - 1.2:.2f} {FY + 1} {z:.2f} {yaw:.1f} 0',
              f'data merge entity {s.ally(cls)} {{Rotation:[{yaw:.1f}f,0f],yHeadRot:{yaw:.1f}f,yBodyRot:{yaw:.1f}f}}')
    s.tp(FX + 3.6, FY + 1.05, FZ + 0.1, facing=(FX - 1.2, FY + 2.15, FZ))
    s.hud(hud)
    s.g(2.0)
    s.cap(name, 3, every=0.5)
    s.wait_cap()
    s.cmd('execute as @e[tag=cast] run data merge entity @s {NoAI:0b}')
    s.hud(False)


def g_lineup_clean(s):
    g_lineup(s, 'g_lineup_clean', hud=False)


def g_hire(s):
    """At the village bell, gold in hand: a sellsword's nameplate up close, one click, the oath."""
    s.dismiss_extras()
    s.reset(shots.ST.get('morning', 1500), 'survival')
    s.cmd('tag @e[tag=hired] remove hired')
    for k, (cls, _) in enumerate(shots.CAST):  # the warband waits out on the field, out of shot
        s.place(cls, FX + 2 * k, FZ, 90)
    s.order('hold')
    s.cmd(f'item replace entity {P} weapon.mainhand with minecraft:gold_ingot 48')
    hx, hy, hz = shots.ST['hire_from']
    x, y, z = hx - 3.9, hy, hz + 0.4
    s.tp(hx, hy, hz, shots.ST['hire_yaw'], 4)
    s.g(0.6)
    s.me('fellowship sellsword bulwark 3')
    s.g(0.4)
    s.cmd(f'execute as {P} at {P} run tag @e[type={shots.ALLY},tag=!cast,limit=1,sort=nearest] add hired')
    s.cmd(f'data merge entity @e[tag=hired,limit=1] {{NoAI:1b}}')
    s.cmd(f'tp @e[tag=hired,limit=1] {x:.2f} {y} {z:.2f} -90 0',
          f'attribute {P} minecraft:player.entity_interaction_range base set 6')
    s.tp(hx, hy, hz, facing=(x, y + 1.2, z + 0.32))  # the sellsword a little right of centre: its nameplate clears the party frames
    s.hud(True)
    print('hired at', s.cmd('data get entity @e[tag=hired,limit=1] Pos'), flush=True)
    s.clear_chat()
    s.g(1.0)
    s.cap('g_hire', 4.5, every=0.3)
    s.g(1.4)
    s.x('click', '3')
    s.g(0.4)
    s.cmd('data merge entity @e[tag=hired,limit=1] {NoAI:0b}')
    s.wait_cap()
    s.cmd(f'attribute {P} minecraft:player.entity_interaction_range base set 3')
    s.hud(False)


def g_formations(s):
    """The band at its Warbanner from almost straight above: Shield Wall, Wedge, Circle, Line."""
    s.clear_field()
    s.reset(BRIGHT, mode='survival')
    s.fresh()
    s.formation('line')
    s.order('follow')
    shots.plant_banner(s, FX + 4)
    s.cmd(f'clear {P}', f'gamemode spectator {P}')
    s.tp(FX + 6.0, FY + 10.0, FZ + 0.01, facing=(FX + 2.6, FY, FZ))
    s.hud(False)
    s.g(3)
    for f in ('shield_wall', 'wedge', 'circle', 'line'):
        s.formation(f)
        s.g(3.5)
        s.cap(f'g_form_{f}', 1.2, every=0.4)
        s.wait_cap()
    s.me('fellowship banner lift')


def g_guard(s):
    """Three-quarter view at sunset: the band rings its planted Warbanner."""
    s.clear_field()
    s.reset(12150, mode='survival')
    s.fresh()
    s.formation('circle')
    s.order('follow')
    shots.plant_banner(s, FX + 4)
    s.cmd(f'clear {P}', f'gamemode spectator {P}')
    s.tp(FX + 6.8, FY + 2.7, FZ + 3.0, facing=(FX + 2.6, FY + 1.0, FZ))
    s.hud(False)
    s.g(4)
    print('banner:', s.cmd('execute if entity @e[type=minecraft:marker]'), s.me('fellowship'), flush=True)
    s.cap('g_guard', 2, every=0.5)
    s.wait_cap()
    s.me('fellowship banner lift')


def g_regalia(s):
    """The leader in Reinforced Golden Armour with a Reinforced Greater Golden Hammer, the warband behind, at sunset."""
    s.clear_field()
    s.reset(12150, mode='survival')
    s.fresh()
    s.line_up(FX + 0.5, z0=-3.0, step=1.5, yaw=-90, order=('ranger', 'skirmisher', 'bulwark', 'arcanist', 'mender'))
    s.order('hold')
    for slot, item in (('armor.head', 'reinforced_golden_armor_helmet'), ('armor.chest', 'reinforced_golden_armor_chestplate'),
                       ('armor.legs', 'reinforced_golden_armor_leggings'), ('armor.feet', 'reinforced_golden_armor_boots'),
                       ('weapon.mainhand', 'reinforced_greater_golden_hammer')):
        s.cmd(f'item replace entity {P} {slot} with better_with_gold:{item}')
    s.tp(FX + 3.2, FY + 1, FZ, -90, 8)  # facing east: the front-view camera stands east of us, the sun behind us
    s.view(2)
    s.hud(False)
    s.g(2.0)
    s.cap('g_regalia', 2, every=0.5)
    s.wait_cap()
    s.view(0)
    s.cmd(f'clear {P}')


SCENES = {f.__name__: f for f in (g_downed, g_storm, g_storm2, g_storm_side, g_taunt, g_volley, g_aggro, g_mend, g_lineup, g_lineup_clean, g_hire,
                                  g_formations, g_guard, g_regalia)}
shots.SHOTS.update(SCENES)

if __name__ == '__main__':
    rig = shots.Rig()
    for name in sys.argv[1:]:
        os.makedirs(f'{OUT}/old', exist_ok=True)
        for f in os.listdir(OUT):
            if f.startswith(name + '_') or (name == 'g_formations' and f.startswith('g_form_')):
                os.replace(os.path.join(OUT, f), os.path.join(OUT, 'old', f'{int(time.time())}_{f}'))
        if name in SCENES:
            SCENES[name](rig)
        else:  # a reel or stills scene, captured into the gallery folder
            shots.SHOTS[name](rig)
        print(name, 'done', flush=True)
