"""Exercise the real server, export two settled states, and retain a test report."""
import json
from controller import complete
from test_server import *
from export_litematic import export

def pulse(on):
    block(-5,6,3,'stone_button[face=floor,facing=north,powered='+str(on).lower()+']')
    # /setblock does not send the support-neighbour update performed by ButtonBlock.use.
    block(-5,4,4,'glass');block(-5,4,4,'air')
def closed():return all(isblock(x,5,z,'stone_bricks') for x in (0,1) for z in (0,1))
def opened():return all(isblock(x,y,z,'air') for x in (0,1) for z in (0,1) for y in range(6))
def main():
    complete();setup();step(220);assert closed()
    cmd('save-all flush');export(SERVER/'world','floor_hatch_2x2_1.21.4','2x2 Flush Floor Hatch / Closed / Java 1.21.4')
    block(-10,1,-14,'lever[face=wall,facing=east,powered=true]');step(220);assert opened()
    cmd('save-all flush');export(SERVER/'world','floor_hatch_2x2_BUILD_OPEN','2x2 Flush Floor Hatch / Assembly / Maintenance ON')
    block(-10,1,-14,'lever[face=wall,facing=east,powered=false]');step(220);assert closed()
    report={'minecraft':'vanilla Java 1.21.4','data_version':4189,'cycles':[],'maintenance_open_and_close':True}
    for cycle in range(20):
        pulse(True);step(20);pulse(False);step(30);assert opened(),f'cycle {cycle}: not open'
        step(60);assert opened(),f'cycle {cycle}: closed early'
        step(110);assert closed(),f'cycle {cycle}: not closed'
        report['cycles'].append({'cycle':cycle+1,'pass':True})
    # Repeated clicks, including clicks while the hatch is closing.
    for gap in (22,40,80,110,125,140,150):
        pulse(True);step(20);pulse(False);step(gap-20);pulse(True);step(20);pulse(False);step(240)
        assert closed(),f'retrigger gap {gap}: failed to recover'
    report['retrigger_gaps_game_ticks']=[22,40,80,110,125,140,150]
    (ROOT/'tests'/'integration.json').write_text(json.dumps(report,indent=2))
    print('PASS: 20 cycles + 7 retrigger tests + maintenance switch')
    cmd('tick rate 20');cmd('tick unfreeze')

if __name__=='__main__':main()
