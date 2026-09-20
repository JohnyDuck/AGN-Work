from build import *

def comparator(x,y,z,direction):
    solid(x,y-1,z,'orange_concrete')
    put(x,y,z,'comparator',facing={'north':'south','south':'north','east':'west','west':'east'}[direction],mode='compare',powered=False)

def complete():
    mechanism()
    # Six-comparator analog pulse extender. Two parallel rows, opposite directions.
    for x in (-9,-5):
        for z in (-17,-16): wire(x,1,z)
    for x in (-8,-7,-6):
        comparator(x,1,-17,'east');comparator(x,1,-16,'west')
    repeater(-4,1,-16,'east');wire(-3,1,-16)
    # ABBA controller. Side channel is delayed 8 redstone ticks.
    for x in (-2,-1):repeater(x,1,-16,'east',4)
    wire(0,1,-16)
    for x in (1,2):repeater(x,1,-16,'east',4)
    wire(3,1,-16)
    # Up channel: direct pulse OR its 16-redstone-tick delayed copy.
    line(-3,-19,-3,-16,1);line(-3,-19,8,-19,1)
    repeater(2,1,-19,'east')
    line(8,-19,8,-9,1);repeater(8,1,-13,'south')
    wire(3,1,-15);repeater(4,1,-15,'east');line(5,-15,8,-15,1)
    repeater(8,1,-8,'south');solid(8,1,-7,'yellow_concrete')
    put(8,1,-6,'redstone_wall_torch',facing='south',lit=True)
    # Side pulse rises to the higher circuit through a three-step staircase.
    line(0,-16,0,-13,1)
    wire(0,2,-12);wire(0,3,-11);wire(0,4,-10)
    repeater(0,4,-9,'south');solid(0,4,-8,'lime_concrete')
    put(0,4,-7,'redstone_wall_torch',facing='south',lit=True)
    # Button next to the hatch, signal routed around the western side.
    solid(-5,5,3)
    put(-5,6,3,'stone_button',face='floor',facing='north',powered=False)
    wire(-5,4,3);wire(-6,4,3);wire(-7,3,3);wire(-8,2,3)
    line(-9,-15,-9,3,1)
    repeater(-9,1,-5,'north');repeater(-9,1,-15,'north')
    # Hidden maintenance switch: enables safe assembly with every piston retracted.
    solid(-11,1,-14,'orange_concrete')
    put(-10,1,-14,'lever',face='wall',facing='east',powered=False)
    # Roof / finished floor, controller fully below it.
    for x in range(-11,11):
        for z in range(-21,9):
            if not (x in (0,1) and z in (0,1)):solid(x,5,z)
    # Solid shaft lining below the machinery; four side faces remain open to allow motion.
    for y in (0,1,2):
        for x,z in [(-1,0),(-1,1),(2,0),(2,1),(0,-1),(1,-1),(0,2),(1,2)]:solid(x,y,z)
    return B

if __name__=='__main__':
    complete();print(len(B))
