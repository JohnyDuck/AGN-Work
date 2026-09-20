"""RCON helpers for the disposable vanilla 1.21.4 integration-test world.
Never point these scripts at a valuable world: setup() clears the test area.
Dependencies: mcrcon, nbtlib. Server must set pause-when-empty-seconds=-1.
"""
import os,time
from pathlib import Path
from mcrcon import MCRcon
from build import B
ROOT=Path(__file__).resolve().parents[1]
SERVER=Path(os.environ.get('MC_TEST_SERVER','/home/user/.cache/trapdoor/server'))
Y=60
r=MCRcon('127.0.0.1',os.environ.get('MC_RCON_PASSWORD','local-mechanism-test'));r.connect()
def cmd(s): return r.command(s)
def step(ticks=20):
    cmd(f'tick step {ticks}');time.sleep(ticks/1000+.08)
def block(x,y,z,b): return cmd(f'setblock {x} {y+Y} {z} {b}')
def isblock(x,y,z,b):return 'Test passed' in cmd(f'execute if block {x} {y+Y} {z} {b}')
def setup():
    print(cmd('forceload add -48 -64 64 32'))
    cmd('gamerule doMobSpawning false');cmd('gamerule doDaylightCycle false');cmd('time set noon');cmd('tick rate 1000');cmd('tick freeze')
    # Let in-flight piston block events from a prior test finish before clearing again.
    cmd('fill -14 59 -30 20 67 10 air');step(30);cmd('fill -14 59 -30 20 67 10 air');cmd('kill @e[type=item]')
    for (x,y,z),b in sorted(B.items(),key=lambda i:i[0][1]):
        out=block(x,y,z,b)
        if 'Incorrect' in out or 'Unknown' in out:raise RuntimeError(out)
    step(20)
