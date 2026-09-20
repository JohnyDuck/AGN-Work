import sys,json
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
from test_server import *
import nbtlib as n
from litemapy import Schematic
src=ROOT/'floor_hatch_2x2_1.21.4.litematic';sch=Schematic.load(src);reg=sch.regions['Hatch'];raw=n.load(src)['Regions']['Hatch'];pal=raw['BlockStatePalette'];keys={str(v):i for i,v in enumerate(pal)}
# Decode independently with Litemapy, then use the vanilla structure loader.
entries=[];tes={(int(t['x']),int(t['y']),int(t['z'])):t for t in raw['TileEntities']}
for y in range(7):
 for z in range(30):
  for x in range(22):
   v=reg[x,y,z];tag=v.to_nbt();idx=next(i for i,a in enumerate(pal) if a==tag)
   ent=n.Compound({'pos':n.List[n.Int]([x,y,z]),'state':n.Int(idx)})
   if (x,y,z) in tes:ent['nbt']=tes[x,y,z]
   entries.append(ent)
pack=SERVER/'world/datapacks/hatch_test';(pack/'data/trapdoor/structure').mkdir(parents=True,exist_ok=True)
(pack/'pack.mcmeta').write_text(json.dumps({'pack':{'pack_format':61,'description':'Temporary hatch import integration test'}}))
n.File({'DataVersion':n.Int(4189),'size':n.List[n.Int]([22,7,30]),'palette':pal,'blocks':n.List[n.Compound](entries),'entities':n.List[n.Compound]([])},gzipped=True).save(pack/'data/trapdoor/structure/imported.nbt')
cmd('tick rate 1000');cmd('tick freeze');cmd('forceload add 48 -32 96 16');print(cmd('reload'));step(20)
print(cmd('place template trapdoor:imported 53 60 -21'));step(100)
assert all(isblock(x+64,5,z,'stone_bricks') for x in (0,1) for z in (0,1))
block(59,6,3,'stone_button[face=floor,facing=north,powered=true]');block(59,4,4,'glass');block(59,4,4,'air');step(20)
block(59,6,3,'stone_button[face=floor,facing=north,powered=false]');block(59,4,4,'glass');block(59,4,4,'air');step(35)
assert all(isblock(x+64,y,z,'air') for x in (0,1) for z in (0,1) for y in range(6))
step(200);assert all(isblock(x+64,5,z,'stone_bricks') for x in (0,1) for z in (0,1))
print('PASS: .litematic independently decoded -> vanilla structure placement -> complete cycle')
(ROOT/'tests/import.json').write_text(json.dumps({'independent_decoder':'litemapy 0.11.0b0','dimensions':[22,7,30],'vanilla_structure_import_cycle':'pass'},indent=2))
