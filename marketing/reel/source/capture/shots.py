#!/usr/bin/env python3
"""Records the Remnants | Fellowship reel footage on a dev client whose clock (and the server's) runs 10x slow.

  python3 shots.py setup            # once: rules, the warband (tagged by class), gear
  python3 shots.py <shot> [<shot>...]

Every wait below is in game seconds and is multiplied by SLOW. The screen is grabbed at 15 fps, which catches
every frame the software renderer draws. Stage (stage.json): `field` is the grass level at the centre of an open
plains field next to the village (the battles face west, into the sunset), `bell` the village bell.
Everything on screen is the mod running: the fights are real fights between the warband and a pillager band,
abilities fire on their own, and the wheel, the Warbanner, crouch-reviving and hiring are real key presses.
"""
import json
import math
import os
import re
import subprocess
import sys
import time

sys.path.insert(0, '/home/user/better_with_gold/tools/qa')
from rcon import Rcon  # noqa: E402

SLOW = float(os.environ.get('SLOW', '10'))  # 1 for a quick framing test at normal speed
P = 'Builder'
ALLY = 'better_with_gold:ally'
STATE = '/home/user/fsreel/rigstate.json'
ST = json.load(open('/home/user/fsreel/stage.json'))
FX, FY, FZ = ST['field']          # grass block at the field centre; entities stand on FY + 1
BX, BY, BZ = ST['bell']           # the village bell block
CAST = [('bulwark', 14), ('skirmisher', 12), ('ranger', 11), ('mender', 10), ('arcanist', 13)]
GOLDEN = ST.get('golden', 12250)  # time of day for the field shots


class Rig:
    def __init__(self):
        self.r = Rcon(port=25575, password='qa', timeout=900)
        self.env = dict(os.environ, DISPLAY=':2')
        self.st = json.load(open(STATE)) if os.path.exists(STATE) else {'view': 0, 'hud_hidden': False}
        self.capproc = None

    def save(self):
        json.dump(self.st, open(STATE, 'w'))

    def cmd(self, *cs):
        out = ''
        for c in cs:
            out = self.r.command(c)
        return out

    def me(self, c):
        return self.cmd(f'execute as {P} at {P} run {c}')

    def tp(self, x, y, z, yaw=0.0, pitch=0.0, facing=None, eye=1.62):
        if facing:
            dx, dy, dz = facing[0] - x, facing[1] - (y + eye), facing[2] - z
            yaw = math.degrees(math.atan2(-dx, dz))
            pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
        self.cmd(f'tp {P} {x:.2f} {y:.2f} {z:.2f} {yaw:.2f} {pitch:.2f}')

    @staticmethod
    def g(seconds):
        time.sleep(seconds * SLOW)

    def x(self, *args):
        subprocess.run(['xdotool', *args], env=self.env, check=False)

    def focus(self):
        out = subprocess.run(['xdotool', 'search', '--name', 'Minecraft'], env=self.env, capture_output=True, text=True).stdout.split()
        if out:
            self.x('windowfocus', '--sync', out[-1])

    def key(self, k):
        self.x('key', k)

    def view(self, mode):
        """0 through the eyes, 1 behind, 2 in front (F5 cycles)."""
        while self.st['view'] != mode:
            self.key('F5')
            self.st['view'] = (self.st['view'] + 1) % 3
            time.sleep(1.5)
        self.save()

    def hud(self, visible):
        if visible == self.st['hud_hidden']:
            self.key('F1')
            self.st['hud_hidden'] = not visible
            time.sleep(1.5)
        self.save()

    def clear_chat(self):
        self.x('keydown', 'F3')
        self.x('key', 'd')
        self.x('keyup', 'F3')
        time.sleep(0.5)

    def glide(self, dx, dy, real_seconds, steps=60):
        """Moves the mouse by (dx, dy) pixels in small steps spread over real_seconds (a smooth game-time move)."""
        done_x = done_y = 0
        for i in range(1, steps + 1):
            u = i / steps
            e = u * u * (3 - 2 * u)
            tx, ty = round(dx * e), round(dy * e)
            self.x('mousemove_relative', '--', str(tx - done_x), str(ty - done_y))
            done_x, done_y = tx, ty
            time.sleep(real_seconds / steps)

    def cap(self, name, game_seconds):
        self.capproc = subprocess.Popen(['/home/user/fsreel/edit/capture/cap.sh', name, str(game_seconds * SLOW)])
        time.sleep(1.0)  # ffmpeg up and grabbing

    def wait_cap(self):
        if self.capproc:
            self.capproc.wait()
            self.capproc = None

    # ---------------------------------------------------------------- world helpers
    def clear_field(self, r=26):
        # never the villagers, their golem and cats: the village is next door
        keep = ','.join(f'type=!{t}' for t in ('minecraft:player', ALLY, 'minecraft:villager', 'minecraft:iron_golem',
                                                'minecraft:cat', 'minecraft:marker'))
        self.cmd(f'execute positioned {FX} {FY} {FZ} run kill @e[{keep},distance=..{r}]',
                 'kill @e[type=minecraft:item]', 'kill @e[type=minecraft:arrow]', 'kill @e[type=minecraft:experience_orb]')

    def reset(self, tod=None, mode='spectator'):
        self.dismiss_extras()
        self.me('fellowship banner lift')
        self.cmd(f'clear {P}', f'effect clear {P}', f'effect give {P} minecraft:resistance infinite 4 true',
                 f'time set {tod or GOLDEN}', f'gamemode {mode} {P}', 'weather clear')
        self.view(0)
        self.hud(False)
        self.focus()

    def ally(self, cls):
        return f'@e[type={ALLY},tag={cls},limit=1]'

    def fresh(self):
        """Every ally standing, healed, no target; resistance keeps the staged fights from downing anyone."""
        if 'count: 5' not in self.cmd('execute if entity @e[tag=cast]'):
            # recall onto the field, with the leader standing on it (a flying leader would drop them from the sky)
            self.cmd(f'gamemode survival {P}', f'tp {P} {FX + 8} {FY + 1} {FZ} 90 0')
            self.me('fellowship recall')
            time.sleep(1.5 * SLOW)
            self.cmd(f'gamemode spectator {P}')
        for cls, _ in CAST:
            self.cmd(f'data merge entity {self.ally(cls)} {{FellowshipDowned:0}}')
        self.cmd(f'effect clear @e[type={ALLY}]', f'effect give @e[type={ALLY}] minecraft:instant_health 1 10 true',
                 f'effect give @e[type={ALLY}] minecraft:resistance 600 1 true')

    def place(self, cls, x, z, yaw):
        self.cmd(f'tp {self.ally(cls)} {x:.2f} {FY + 1} {z:.2f} {yaw:.1f} 0')

    def line_up(self, x, z0=-4.0, step=2.0, yaw=90.0, order=('skirmisher', 'bulwark', 'arcanist', 'ranger', 'mender')):
        for k, cls in enumerate(order):
            self.place(cls, x, FZ + z0 + k * step, yaw)

    def raiders(self, x, z, vindicators=5, pillagers=3, spread=1.6):
        """A packed pillager band at (x, z): the Arcanist's storm wants them close together."""
        k = 0
        for kind, n in (('vindicator', vindicators), ('pillager', pillagers)):
            for i in range(n):
                a = k * 2.399963  # golden-angle spiral: packed, never on top of each other
                rr = spread * math.sqrt(k + 0.5) / 1.6
                k += 1
                self.cmd(f'summon minecraft:{kind} {x + rr * math.cos(a):.2f} {FY + 1} {z + rr * math.sin(a):.2f} '
                         f'{{PersistenceRequired:1b,Rotation:[-90f,0f],PatrolLeader:0b}}')

    def dismiss_extras(self):
        """Releases every hired ally that is not one of the cast (the band stays at five) and clears waiting sellswords."""
        for _ in range(8):
            out = self.cmd(f'data get entity @e[type={ALLY},tag=!cast,limit=1] Fellowship')
            m = re.search(r'\\?"name\\?":\\?"([^"\\]+)', out)
            if not m:
                break
            self.me(f'fellowship dismiss {m.group(1)}')
            self.cmd(f'kill @e[type={ALLY},tag=!cast,limit=1]')
        self.cmd(f'kill @e[type={ALLY},tag=!cast]')

    def order(self, o):
        self.me(f'fellowship order {o}')

    def formation(self, f):
        self.me(f'fellowship formation {f}')


# ------------------------------------------------------------------ setup
def setup(s):
    """Rules, a clear sky, and the warband: one of each class, tagged by class, at the field."""
    s.cmd('gamerule doDaylightCycle false', 'gamerule doWeatherCycle false', 'gamerule doMobSpawning false',
          'gamerule doPatrolSpawning false', 'gamerule doTraderSpawning false', 'gamerule doInsomnia false',
          'gamerule sendCommandFeedback false', 'gamerule showDeathMessages false', 'gamerule announceAdvancements false',
          f'recipe give {P} *', 'gamerule keepInventory true', 'gamerule doImmediateRespawn true', 'gamerule fallDamage false', 'weather clear', f'op {P}', 'difficulty normal', f'time set {GOLDEN}')
    s.tp(FX + 8, FY + 1, FZ, 90, 10)
    s.cmd(f'gamemode survival {P}', f'effect give {P} minecraft:resistance 99999 4 true')
    s.g(1)
    if 'joins your warband' not in s.cmd(f'data get entity {s.ally("bulwark")}'):
        s.cmd(f'kill @e[type={ALLY}]')
    for cls, lvl in CAST:
        out = s.me(f'fellowship recruit {cls} {lvl}')
        print(out.strip(), flush=True)
        s.g(0.5)
        s.cmd(f'tag @e[type={ALLY},tag=!cast] add {cls}', f'tag @e[tag={cls}] add cast')
    s.formation('line')
    s.order('follow')
    print(s.me('fellowship').strip())


# ------------------------------------------------------------------ battles
def battle(s, name, cam, seconds=10.0, charge_at=0.6, enemies=(5, 3), ex=-7.0, line_x=5.0, view=None, hud=False,
           order=('skirmisher', 'bulwark', 'arcanist', 'ranger', 'mender')):
    """The warband (in a line at FX + line_x, facing west) charges a pillager band at FX + ex."""
    s.clear_field()
    s.reset()
    s.fresh()
    s.formation('line')
    s.line_up(FX + line_x, z0=-3.2, step=1.6, yaw=90, order=order)
    s.order('hold')
    s.raiders(FX + ex, FZ, *enemies)
    s.tp(*cam['pos'], facing=cam['look'])
    if view is not None:
        s.view(view)
    s.hud(hud)
    s.g(1.2)
    s.cap(name, seconds)
    s.g(charge_at)
    s.order('charge')
    s.wait_cap()
    s.order('hold')


def clash_back(s):
    """Low behind the line: backs, the raiders, the village and the setting sun beyond; the charge runs away from us."""
    battle(s, 'clash_back', {'pos': (FX + 13.5, FY + 2.8, FZ + 0.6), 'look': (FX - 6, FY + 1.2, FZ)}, seconds=11)


def clash_front(s):
    """Behind the raiders, looking back at the warband as it charges straight at the camera."""
    battle(s, 'clash_front', {'pos': (FX - 11.5, FY + 1.6, FZ + 1.0), 'look': (FX + 5, FY + 1.5, FZ)}, seconds=10)


def taunt(s):
    """Close behind the Bulwark as the raiders turn on the others and it Challenges them."""
    battle(s, 'taunt', {'pos': (FX + 1.5, FY + 2.6, FZ + 3.8), 'look': (FX - 6, FY + 1.0, FZ - 0.6)}, seconds=11)


def taunt2(s):
    """Right behind the Bulwark's charge: it Challenges a few blocks in front of us, the ring bursts out, the
    raiders turn on it (their labels read TAUNTED)."""
    battle(s, 'taunt2', {'pos': (FX + 5.0, FY + 1.9, FZ - 0.7), 'look': (FX - 3, FY + 1.1, FZ - 1.8)}, seconds=8)


def volley(s):
    """Over the Ranger's shoulder at the south end of the line: it keeps its range and looses three arrows at once."""
    battle(s, 'volley', {'pos': (FX + 8.5, FY + 1.9, FZ + 5.0), 'look': (FX - 6, FY + 1.5, FZ + 1.0)}, seconds=11,
           order=('skirmisher', 'bulwark', 'arcanist', 'mender', 'ranger'))


def storm(s):
    """Above the raiders' pack, from the warband's side: the Arcanist's storm forks through all of them."""
    battle(s, 'storm', {'pos': (FX - 2.5, FY + 6.5, FZ + 4.5), 'look': (FX - 7.2, FY + 0.5, FZ)}, seconds=8, enemies=(6, 3))


def mend(s):
    """Close on the Mender and the Bulwark against the sunset: the Bulwark takes blows, the Mender's light pours in."""
    s.clear_field()
    s.reset()
    s.fresh()
    s.cmd(f'effect clear {s.ally("bulwark")}')
    s.place('bulwark', FX - 0.2, FZ - 0.9, -90)
    s.place('mender', FX + 1.3, FZ + 1.1, -130)
    for cls, z in (('skirmisher', -9), ('ranger', -11), ('arcanist', -13)):
        s.place(cls, FX + 9, FZ + z, 90)
    s.order('hold')
    s.tp(FX + 4.6, FY + 1.75, FZ + 0.3, facing=(FX + 0.4, FY + 1.35, FZ + 0.1))
    s.hud(False)
    s.g(1.2)
    s.cap('mend', 9)
    for k in range(3):
        s.g(0.4)
        s.cmd(f'damage {s.ally("bulwark")} 9 minecraft:generic')
        s.g(2.6)
    s.wait_cap()
    s.fresh()


# ------------------------------------------------------------------ hire, wheel, formations, downed, banner
def hire(s):
    """Through the player's eyes at the village bell, gold in hand: a sellsword for hire, one click, the oath."""
    s.dismiss_extras()
    s.reset(ST.get('morning', 1500), 'survival')
    s.cmd(f'item replace entity {P} weapon.mainhand with minecraft:gold_ingot 48')
    hx, hy, hz = ST['hire_from']
    s.tp(hx, hy, hz, ST['hire_yaw'], 4)
    s.g(0.6)
    s.me('fellowship sellsword bulwark 3')
    s.g(0.4)
    s.cmd(f'execute as {P} at {P} run data merge entity @e[type={ALLY},tag=!cast,limit=1,sort=nearest] {{NoAI:1b}}')
    s.cmd(f'execute as {P} at {P} run tag @e[type={ALLY},tag=!cast,limit=1,sort=nearest] add hired')
    # stand it 4.3 blocks out, beside the bell, facing us, so its whole nameplate (name, class, level, price) fits;
    # a longer reach (vanilla attribute) and a hidden Speed effect (a wider field of view) make that work
    x, y, z = hx - 4.3, hy, hz + 0.5
    s.cmd(f'tp @e[tag=hired,limit=1] {x:.2f} {y} {z:.2f} -90 0',
          f'attribute {P} minecraft:player.entity_interaction_range base set 6',
          f'effect give {P} minecraft:speed 60 1 true')
    s.tp(hx, hy, hz, facing=(x, y + 1.05, z))
    s.hud(True)
    s.clear_chat()
    s.g(1.0)
    s.cap('hire', 5.5)
    s.g(1.6)
    s.x('click', '3')
    s.g(0.4)
    s.cmd('data merge entity @e[tag=hired,limit=1] {NoAI:0b}')
    s.wait_cap()
    s.cmd(f'effect clear {P} minecraft:speed', f'attribute {P} minecraft:player.entity_interaction_range base set 3')
    s.hud(False)


def march(s):
    """The warband follows in a wedge, straight at the camera, which backs away ahead of it at walking pace."""
    s.clear_field()
    s.reset()
    s.fresh()
    s.formation('wedge')
    s.line_up(FX - 12, z0=-3.2, step=1.6, yaw=-90)
    s.order('follow')
    # the formation trails the leader's direction of travel: step east beforehand so it forms up to the west
    s.tp(FX - 9, FY + 1.4, FZ, -90, 6)
    s.g(0.6)
    s.tp(FX - 6, FY + 1.4, FZ, -90, 6)
    s.g(0.6)
    s.tp(FX - 3, FY + 1.4, FZ, -90, 6)
    s.g(3.5)
    s.tp(FX - 3, FY + 1.4, FZ, 90, 8)  # turn round to face them
    s.focus()
    if not s.st.get('slow_fly'):
        for _ in range(8):  # spectator flight down to about walking pace
            s.x('click', '5')
            time.sleep(0.2)
        s.st['slow_fly'] = True
        s.save()
    s.g(1.0)
    s.cap('march', 8)
    s.x('keydown', 's')
    s.g(7.6)
    s.x('keyup', 's')
    s.wait_cap()


def wheel(s):
    """Through the player's eyes among the warband: hold G, the wheel opens, point at Charge, let go; they charge the raiders."""
    s.clear_field()
    s.reset(mode='survival')
    s.cmd(f'effect give {P} minecraft:resistance 99999 4 true')
    s.fresh()
    s.formation('line')
    s.line_up(FX + 5, z0=-3.2, step=1.6, yaw=90)
    s.order('hold')
    s.tp(FX + 9.5, FY + 1, FZ + 0.2, 90, 4)
    s.raiders(FX - 7, FZ, 4, 2)
    s.hud(True)
    s.g(1.0)
    s.cap('wheel', 7)
    s.g(0.6)
    s.x('keydown', 'g')
    s.g(0.5)
    s.glide(150, -150, 0.45 * SLOW)  # towards Charge (up-right of the hub; slices run clockwise from Follow)
    s.g(0.9)
    s.x('keyup', 'g')
    s.wait_cap()
    s.order('hold')
    s.hud(False)


def plant_banner(s, x, pitch=55):
    """The player plants the Warbanner on the ground in front of it (a real right-click with the item)."""
    s.me('fellowship banner lift')
    s.cmd(f'gamemode survival {P}', f'item replace entity {P} weapon.mainhand with better_with_gold:warbanner')
    s.tp(x, FY + 1, FZ, 90, pitch)
    s.focus()
    s.g(0.6)
    s.x('click', '3')
    s.g(0.6)


def formations(s):
    """From behind and above the band guarding its Warbanner: Shield Wall, then Wedge, then the charge."""
    s.clear_field()
    s.reset(mode='survival')
    s.fresh()
    s.formation('line')
    s.order('follow')
    plant_banner(s, FX + 6)
    s.cmd(f'clear {P}', f'gamemode spectator {P}')
    s.raiders(FX - 9, FZ, 4, 1)
    s.tp(FX + 10, FY + 5.2, FZ + 3.6, facing=(FX + 3.2, FY + 0.6, FZ))
    s.g(4)
    s.cap('formations', 11.5)
    s.g(0.8)
    s.formation('shield_wall')
    s.g(3.4)
    s.formation('wedge')
    s.g(3.4)
    s.order('charge')
    s.wait_cap()
    s.order('hold')
    s.me('fellowship banner lift')


def downed(s):
    """Over the shoulder: the Skirmisher is cut down beside us, collapses, and we crouch and pull it back up."""
    s.clear_field()
    s.reset(mode='survival')
    s.fresh()
    s.place('skirmisher', FX - 0.6, FZ + 0.9, -90)
    s.place('bulwark', FX - 4, FZ - 3, 90)
    s.place('ranger', FX - 3.5, FZ + 4, 90)
    s.place('arcanist', FX + 5, FZ - 4, 90)
    s.place('mender', FX + 40, FZ + 30, 90)  # the Mender would revive it before us
    s.order('hold')
    s.cmd(f'data merge entity {s.ally("skirmisher")} {{NoAI:1b}}')  # stays exactly where it falls, in reach
    s.tp(FX + 0.8, FY + 1, FZ - 0.3, 112, 32)
    s.view(1)
    s.hud(True)
    s.clear_chat()
    s.g(1.5)
    s.cap('downed', 7.5)
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


def banner_plant(s):
    """Through the player's eyes, the Warbanner in hand, the sun going down: plant it; the gold pillar rises."""
    s.clear_field()
    s.reset(mode='survival')
    s.fresh()
    s.me('fellowship banner lift')
    s.formation('circle')
    s.line_up(FX + 13, z0=-3.2, step=1.6, yaw=90)
    s.order('hold')
    s.cmd(f'item replace entity {P} weapon.mainhand with better_with_gold:warbanner')
    s.tp(FX + 5.2, FY + 1, FZ, 90, 25)
    s.hud(True)
    s.clear_chat()
    s.g(1.2)
    s.cap('banner_plant', 4.5)
    s.g(0.9)
    s.x('click', '3')
    s.wait_cap()
    s.cmd(f'clear {P}')


def banner_guard(s):
    """Wide: the band runs in from all sides and forms a ring around the planted Warbanner (run after banner_plant)."""
    s.cmd(f'gamemode spectator {P}', f'effect clear @e[type={ALLY}]')
    for cls, (dx, dz) in zip(('bulwark', 'skirmisher', 'ranger', 'mender', 'arcanist'),
                             ((-7, -5), (-6, 6), (7, -6), (8, 5), (1, -9))):
        s.place(cls, FX + 2 + dx, FZ + dz, 0)
    s.order('hold')
    s.tp(FX + 7.5, FY + 3.4, FZ + 5.2, facing=(FX + 1.8, FY + 0.9, FZ))
    s.hud(False)
    s.g(1.0)
    s.cap('banner_guard', 6)
    s.g(0.5)
    s.order('guard_banner')
    s.wait_cap()


def roster(s):
    """The roster (client run at GUI scale 2): open it, pick allies from the list, their portraits and Chronicles."""
    s.reset(mode='survival')
    s.hud(True)
    s.clear_chat()
    s.tp(FX + 8, FY + 1, FZ, 90, 0)
    s.g(1.0)
    s.cap('roster', 8)
    s.g(0.4)
    s.key('u')
    s.g(0.9)
    s.glide(-400, -232, 0.4 * SLOW, steps=40)  # onto the top row of the list
    s.x('click', '1')
    s.g(1.4)
    for _ in range(3):
        s.glide(0, 52, 0.25 * SLOW, steps=24)
        s.x('click', '1')
        s.g(1.3)
    s.wait_cap()
    s.key('Escape')


def cta(s):
    """The warband in a line at sunset, facing us, the Warbanner raised behind them."""
    s.clear_field()
    s.reset(mode='survival')
    s.fresh()
    s.formation('line')
    s.order('hold')
    plant_banner(s, FX - 1.5, pitch=39)  # lands about two blocks ahead, behind the line
    s.cmd(f'clear {P}', f'gamemode spectator {P}')
    s.line_up(FX - 1.2, z0=-2.8, step=1.4, yaw=-90, order=('ranger', 'skirmisher', 'bulwark', 'arcanist', 'mender'))
    s.order('hold')
    s.tp(FX + 4.6, FY + 1.25, FZ + 0.1, facing=(FX - 1.2, FY + 1.75, FZ))
    s.hud(False)
    s.g(2.5)
    s.cap('cta', 9)
    s.wait_cap()


def overview(s):
    """Framing check: the whole fight from straight above."""
    battle(s, 'overview', {'pos': (FX - 1, FY + 15, FZ), 'look': (FX - 1.01, FY, FZ)}, seconds=14)


SHOTS = {f.__name__: f for f in (overview, setup, clash_back, clash_front, taunt, taunt2, volley, storm, mend, hire, march, wheel,
                                 formations, downed, banner_plant, banner_guard, roster, cta)}

if __name__ == '__main__':
    rig = Rig()
    for name in sys.argv[1:]:
        t0 = time.time()
        SHOTS[name](rig)
        print(f'{name} done in {time.time() - t0:.0f}s', flush=True)
