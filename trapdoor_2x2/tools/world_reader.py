"""Read modern Anvil chunks without depending on a Minecraft client."""
import io, math, struct, zlib
from pathlib import Path
import nbtlib

class World:
    def __init__(self, path): self.path=Path(path);self.cache={}
    def chunk(self,cx,cz):
        if (cx,cz) not in self.cache:
            p=self.path/'region'/f'r.{cx//32}.{cz//32}.mca'
            with p.open('rb') as f:
                f.seek(4*((cx%32)+(cz%32)*32));v=int.from_bytes(f.read(4),'big');f.seek((v>>8)*4096)
                length=int.from_bytes(f.read(4),'big');compression=f.read(1)[0];raw=f.read(length-1)
            assert compression==2
            self.cache[cx,cz]=nbtlib.File.parse(io.BytesIO(zlib.decompress(raw)))
        return self.cache[cx,cz]
    def state(self,x,y,z):
        c=self.chunk(x//16,z//16)
        sec=next((s for s in c['sections'] if int(s['Y'])==y//16),None)
        if sec is None or 'block_states' not in sec: return {'Name':'minecraft:air'}
        bs=sec['block_states'];pal=bs['palette']
        if len(pal)==1: return pal[0].unpack()
        bits=max(4,(len(pal)-1).bit_length());per=64//bits
        i=(y%16)*256+(z%16)*16+x%16
        value=(int(bs['data'][i//per]) >> ((i%per)*bits)) & ((1<<bits)-1)
        return pal[value].unpack()
    def region(self,lo,hi,yoffset=0):
        return {(x,y,z):self.state(x,y+yoffset,z) for x in range(lo[0],hi[0]+1) for y in range(lo[1],hi[1]+1) for z in range(lo[2],hi[2]+1)}
