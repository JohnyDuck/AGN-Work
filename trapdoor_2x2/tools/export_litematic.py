"""Export actual, settled vanilla world block states to Litematica v6 (1.21.4)."""
import json,math,time
from collections import Counter
from pathlib import Path
import nbtlib as n
from world_reader import World
ROOT=Path(__file__).resolve().parents[1]
LO=(-11,0,-21); HI=(10,6,8); SIZE=(22,7,30)

def vec(v):return n.Compound(dict(zip(('x','y','z'),map(n.Int,v))))
def nstate(s):
    d={'Name':n.String(s['Name'])}
    if s.get('Properties'):d['Properties']=n.Compound({k:n.String(v) for k,v in s['Properties'].items()})
    return n.Compound(d)
def export(world_path,name,title):
    w=World(world_path);states=w.region(LO,HI,yoffset=60)
    assert all(s['Name']!='minecraft:moving_piston' for s in states.values()), 'Do not export while moving'
    palette=[{'Name':'minecraft:air'}];lookup={json.dumps(palette[0],sort_keys=True):0};ids=[]
    for y in range(LO[1],HI[1]+1):
        for z in range(LO[2],HI[2]+1):
            for x in range(LO[0],HI[0]+1):
                s=states[x,y,z];key=json.dumps(s,sort_keys=True)
                if key not in lookup:lookup[key]=len(palette);palette.append(s)
                ids.append(lookup[key])
    bits=max(2,(len(palette)-1).bit_length());longs=[0]*math.ceil(len(ids)*bits/64)
    for i,value in enumerate(ids):
        word,shift=divmod(i*bits,64)
        longs[word] |= (value << shift) & ((1<<64)-1)
        if shift+bits>64:longs[word+1] |= value >> (64-shift)
    # Independent bitwise read-back check, including indices straddling a long boundary.
    for i,expected in enumerate(ids):
        offset=i*bits;actual=sum(((longs[(offset+j)//64]>>((offset+j)%64))&1)<<j for j in range(bits))
        assert actual==expected
    longs=[v-(1<<64) if v>=1<<63 else v for v in longs]
    tes=[]
    for c in w.cache.values():
        for te in c.get('block_entities',[]):
            x,y,z=int(te['x']),int(te['y'])-60,int(te['z'])
            if all(LO[i]<=v<=HI[i] for i,v in enumerate((x,y,z))):
                copy=n.Compound(te);copy['x']=n.Int(x-LO[0]);copy['y']=n.Int(y-LO[1]);copy['z']=n.Int(z-LO[2]);tes.append(copy)
    count=sum(i!=0 for i in ids);timestamp=int(time.time()*1000)
    region=n.Compound({'Position':vec((0,0,0)),'Size':vec(SIZE),'BlockStatePalette':n.List[n.Compound]([nstate(s) for s in palette]),'BlockStates':n.LongArray(longs),'TileEntities':n.List[n.Compound](tes),'Entities':n.List[n.Compound]([]),'PendingBlockTicks':n.List[n.Compound]([]),'PendingFluidTicks':n.List[n.Compound]([])})
    f=n.File({'Version':n.Int(6),'SubVersion':n.Int(1),'MinecraftDataVersion':n.Int(4189),'Metadata':n.Compound({'Name':n.String(title),'Author':n.String('Arena / custom build'),'Description':n.String('Java 1.21.4. 2x2 flush floor hatch; stone button, 4.5 s clear passage. Tested on vanilla server. Floor Y=5; hole X=11..12 Z=21..22. See README_RU.md.'),'EnclosingSize':vec(SIZE),'TimeCreated':n.Long(timestamp),'TimeModified':n.Long(timestamp),'TotalBlocks':n.Int(count),'TotalVolume':n.Int(len(ids)),'RegionCount':n.Int(1)}),'Regions':n.Compound({'Hatch':region})},gzipped=True)
    f.save(ROOT/f'{name}.litematic')
    saved=n.load(ROOT/f'{name}.litematic');assert int(saved['Metadata']['TotalBlocks'])==count
    (ROOT/'tests'/f'{name}.blocks.json').write_text(json.dumps([{'pos':[x-LO[0],y-LO[1],z-LO[2]],**s} for (x,y,z),s in states.items() if s['Name']!='minecraft:air'],ensure_ascii=False,separators=(',',':')))
    print(name,'blocks',count,'palette',len(palette),'bits',bits,'tile_entities',len(tes))
    return states

if __name__=='__main__':
    import os
    export(os.environ['MC_WORLD'],'floor_hatch_2x2_1.21.4','2x2 Flush Floor Hatch / Button')
