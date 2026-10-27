import sys
sys.path.insert(0, '/home/user/better_with_gold/tools/qa')
from rcon import Rcon
r = Rcon(port=25575, password='qa', timeout=600)
for c in sys.argv[1:]:
    out = r.command(c)
    if out.strip():
        print(out)
