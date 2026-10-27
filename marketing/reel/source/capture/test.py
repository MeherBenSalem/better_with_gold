"""Dry run of shots at normal speed: the capture becomes a burst of screenshots (every 0.4 s) to check framing."""
import os, subprocess, sys, time, threading
os.environ['SLOW'] = '1'
import shots

OUT = '/tmp/claude-0/-home-user/63821b54-1434-56b1-a049-c4e9f2acaa5b/scratchpad/test'
os.makedirs(OUT, exist_ok=True)


def cap(self, name, game_seconds):
    def run():
        t0 = time.time(); k = 0
        while time.time() - t0 < game_seconds:
            subprocess.run(['import', '-window', 'root', '-resize', '25%', f'{OUT}/{name}_{k:02d}.png'], env=dict(os.environ, DISPLAY=':2'))
            k += 1
            time.sleep(0.25)
    self.capproc = threading.Thread(target=run); self.capproc.start(); time.sleep(0.3)


def wait_cap(self):
    if self.capproc:
        self.capproc.join(); self.capproc = None


shots.Rig.cap = cap
shots.Rig.wait_cap = wait_cap
rig = shots.Rig()
for name in sys.argv[1:]:
    for f in os.listdir(OUT):
        if f.startswith(name + '_'):
            os.remove(os.path.join(OUT, f))
    shots.SHOTS[name](rig)
    n = len([f for f in os.listdir(OUT) if f.startswith(name + '_')])
    subprocess.run(f'montage {OUT}/{name}_*.png -tile 10x -geometry +2+2 {OUT}/sheet_{name}.png', shell=True)
    print(name, n, 'frames')
