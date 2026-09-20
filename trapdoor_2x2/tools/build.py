"""2x2 flush floor hatch for Java 1.21.4. Coordinates use floor Y=5."""
from pathlib import Path
import json
B = {}

def put(x,y,z,name,**props):
    p = f'minecraft:{name}'
    if props: p += '[' + ','.join(f'{k}={str(v).lower()}' for k,v in sorted(props.items())) + ']'
    B[x,y,z] = p

def solid(x,y,z,name='stone_bricks'): put(x,y,z,name)
def wire(x,y,z):
    solid(x,y-1,z,'light_blue_concrete'); put(x,y,z,'redstone_wire')
def repeater(x,y,z,facing,delay=1):
    solid(x,y-1,z,'light_blue_concrete');put(x,y,z,'repeater',facing={'north':'south','south':'north','east':'west','west':'east'}[facing],delay=delay,locked=False,powered=False)
def line(x1,z1,x2,z2,y):
    assert x1==x2 or z1==z2
    for x in range(min(x1,x2),max(x1,x2)+1):
        for z in range(min(z1,z2),max(z1,z2)+1): wire(x,y,z)

def mechanism():
    B.clear()
    # Eight lateral pistons carry four blocks + four lift pistons.
    for x,face,moved in [(-2,'east',-1),(3,'west',2)]:
        for z in (0,1):
            for y in (3,4): put(x,y,z,'sticky_piston',facing=face,extended=False)
            put(moved,3,z,'sticky_piston',facing='up',extended=False)
            solid(moved,4,z)
    # Independent powered blocks for the four lift pistons, at their CLOSED positions.
    for x in (0,1):
        for z,rz,face in [(-1,-2,'south'),(2,3,'north')]:
            solid(x,3,z);repeater(x,3,rz,face)
    # Lift wiring runs two levels beneath the lateral circuit (isolated crossings).
    line(0,-5,8,-5,1);line(0,6,8,6,1);line(8,-5,8,6,1)
    repeater(4,1,-5,'west');repeater(4,1,6,'west');repeater(8,1,1,'south')
    for x in (0,1):
        wire(x,2,-4);wire(x,3,-3);wire(x,2,5);wire(x,3,4)
    # Strongly powered blocks drive the upper pistons and QC-power the lower pair.
    for x,rx,bx,direction in [(-5,-4,-3,'east'),(6,5,4,'west')]:
        line(x,-6,x,1,4)
        for z in (0,1):
            repeater(rx,4,z,direction);solid(bx,4,z,'lime_concrete')
    line(-5,-6,6,-6,4)
    # Floor: all machinery is below Y=5. Opening is x=0..1, z=0..1.
    for x in range(-6,10):
        for z in range(-7,9):
            if not (x in (0,1) and z in (0,1)): solid(x,5,z)
    return B

if __name__=='__main__':
    mechanism();print(len(B))
