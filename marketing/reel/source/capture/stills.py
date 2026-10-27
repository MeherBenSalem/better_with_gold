#!/usr/bin/env python3
"""Store-page screenshots: the reel's staged scenes, replayed at normal speed on a 1920x1080 client (display :1),
each capture turned into a burst of full-resolution screenshots to pick from.

  python3 stills.py <shot>...      (the shots of shots.py, plus the extra stills scenes below)
"""
import os
import subprocess
import sys
import threading
import time

os.environ['SLOW'] = '1'
import shots  # noqa: E402

OUT = '/home/user/fsreel/stills'


def cap(self, name, game_seconds, every=0.35):
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
        self.capproc.join()
        self.capproc = None


shots.Rig.cap = cap
shots.Rig.wait_cap = wait_cap
_init = shots.Rig.__init__


def init(self):
    _init(self)
    self.env = dict(os.environ, DISPLAY=':1')


shots.Rig.__init__ = init

FX, FY, FZ = shots.FX, shots.FY, shots.FZ


def regalia(s):
    """The leader in Reinforced Golden Armour with a Reinforced Greater Golden Hammer, the warband behind, at sunset."""
    s.clear_field()
    s.reset(mode='survival')
    s.fresh()
    s.line_up(FX - 1, z0=-3.2, step=1.6, yaw=-90, order=('ranger', 'skirmisher', 'bulwark', 'arcanist', 'mender'))
    s.order('hold')
    for slot, item in (('armor.head', 'reinforced_golden_armor_helmet'), ('armor.chest', 'reinforced_golden_armor_chestplate'),
                       ('armor.legs', 'reinforced_golden_armor_leggings'), ('armor.feet', 'reinforced_golden_armor_boots'),
                       ('weapon.mainhand', 'reinforced_greater_golden_hammer')):
        s.cmd(f'item replace entity {shots.P} {slot} with better_with_gold:{item}')
    s.tp(FX + 2.5, FY + 1, FZ, 90, 6)
    s.view(2)
    s.hud(False)
    s.g(2.0)
    s.cap('regalia', 3)
    s.wait_cap()
    s.view(0)
    s.cmd(f'clear {shots.P}')


def roster_land(s):
    """The roster on the 1920x1080 client (GUI scale 3): pick allies from the list."""
    s.reset(mode='survival')
    s.hud(True)
    s.clear_chat()
    s.tp(FX + 8, FY + 1, FZ, 90, 0)
    s.g(1.0)
    s.key('u')
    s.g(1.0)
    s.glide(-500, -309, 0.4, steps=20)
    for k in range(5):
        s.x('click', '1')
        s.g(0.8)
        s.cap(f'roster_land{k}', 0.5)
        s.wait_cap()
        s.glide(0, 78, 0.25, steps=10)
    s.key('Escape')


shots.SHOTS.update({'regalia': regalia, 'roster_land': roster_land})

if __name__ == '__main__':
    rig = shots.Rig()
    for name in sys.argv[1:]:
        for f in os.listdir(OUT):
            if f.startswith(name + '_'):
                os.remove(os.path.join(OUT, f))
        shots.SHOTS[name](rig)
        print(name, 'done', flush=True)
