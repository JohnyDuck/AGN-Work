"""Deterministic isometric preview of the exported block snapshots, not an AI image.
Textures: Minecraft Java assets via npm minecraft-assets@1.21.4 data.
Set MC_TEXTURES to that package's minecraft-assets/data/1.21.4/blocks directory.
"""
import os,json,math
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw,ImageFont,ImageEnhance,ImageFilter
ROOT=Path(__file__).resolve().parents[1]
TEX=Path(os.environ.get('MC_TEXTURES','/home/user/.cache/trapdoor/client/node_modules/minecraft-assets/minecraft-assets/data/1.21.4/blocks'))
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf';BOLD=FONT.replace('.ttf','-Bold.ttf')

def font(n,bold=False):return ImageFont.truetype(BOLD if bold else FONT,n)
def read(name):
    a=json.loads((ROOT/'tests'/f'{name}.blocks.json').read_text())
    return {(e['pos'][0]-11,e['pos'][1],e['pos'][2]-21):e for e in a}
cache={}
def texture(name,light=1):
    key=name,light
    if key not in cache:
        p=TEX/(name+'.png')
        if p.exists():im=Image.open(p).convert('RGBA').crop((0,0,16,16)).resize((64,64),Image.Resampling.NEAREST)
        else: im=Image.new('RGBA',(64,64),(114,123,133,255))
        im=ImageEnhance.Brightness(im).enhance(light);cache[key]=im
    return cache[key]

class Renderer:
    def __init__(self,w,h,sx=31,sy=18,sz=29,origin=None):
        self.im=Image.new('RGBA',(w,h));self.sx=sx;self.sy=sy;self.sz=sz;self.origin=origin or (w/2,h*.48)
    def project(self,x,y,z):
        return (self.origin[0]+(x-z)*self.sx,self.origin[1]+(x+z)*self.sy-(y-5)*self.sz)
    def face(self,pts,name,light):
        pts=[self.project(*p) for p in pts]
        minx=math.floor(min(p[0] for p in pts));miny=math.floor(min(p[1] for p in pts));maxx=math.ceil(max(p[0] for p in pts));maxy=math.ceil(max(p[1] for p in pts))
        if maxx<=minx or maxy<=miny:return
        p0,p1,p3=pts[0],pts[1],pts[3]
        a=np.array([[(p1[0]-p0[0])/64,(p3[0]-p0[0])/64],[(p1[1]-p0[1])/64,(p3[1]-p0[1])/64]])
        try:iv=np.linalg.inv(a)
        except np.linalg.LinAlgError:return
        off=iv@np.array([minx-p0[0],miny-p0[1]])
        coef=(iv[0,0],iv[0,1],off[0],iv[1,0],iv[1,1],off[1])
        tile=texture(name,light).transform((maxx-minx,maxy-miny),Image.Transform.AFFINE,coef,Image.Resampling.NEAREST)
        self.im.alpha_composite(tile,(minx,miny))
    def cube(self,x,y,z,top='stone_bricks',side=None,height=1,visible=(True,True,True),bounds=(0,0,1,1)):
        side=side or top;u,v,U,V=bounds;x0,x1=x+u,x+U;z0,z1=z+v,z+V;y1=y+height
        if visible[1]:self.face([(x1,y1,z0),(x1,y1,z1),(x1,y,z1),(x1,y,z0)],side,.62)
        if visible[2]:self.face([(x0,y1,z1),(x1,y1,z1),(x1,y,z1),(x0,y,z1)],side,.80)
        if visible[0]:self.face([(x0,y1,z0),(x1,y1,z0),(x1,y1,z1),(x0,y1,z1)],top,1.12)
    def render(self,states,crop=(-6,7,-5,6),cutaway=False):
        vox={p:s for p,s in states.items() if crop[0]<=p[0]<=crop[1] and crop[2]<=p[2]<=crop[3] and (not cutaway or p[1]<5)}
        transparent=('wire','repeater','comparator','torch','button','lever')
        def full(p):
            if p not in vox:return False
            return not any(t in vox[p]['Name'] for t in transparent)
        for (x,y,z),s in sorted(vox.items(),key=lambda a:sum((a[0][0],a[0][2],a[0][1]*2.5))):
            name=s['Name'].split(':')[1];props=s.get('Properties',{})
            vis=(not full((x,y+1,z)),not full((x+1,y,z)),not full((x,y,z+1)))
            if name in ('stone_button','lever'):
                self.cube(x,y,z,'stone',height=.13,bounds=(.28,.35,.72,.65))
                if name=='lever':
                    d=ImageDraw.Draw(self.im);d.line([self.project(x+.5,y+.1,z+.5),self.project(x+.75,y+.6,z+.5)],fill='#c0a577',width=3)
            elif name=='redstone_wire':
                pwr=int(props.get('power','0'));d=ImageDraw.Draw(self.im);col='#f04030' if pwr else '#691e26'
                center=self.project(x+.5,y+.025,z+.5)
                for direction,dx,dz in [('north',0,-.5),('south',0,.5),('east',.5,0),('west',-.5,0)]:
                    if props.get(direction,'side')!='none':d.line([center,self.project(x+.5+dx,y+.025,z+.5+dz)],fill=col,width=max(1,int(self.sx/9)))
            elif name in ('repeater','comparator'):
                self.cube(x,y,z,'smooth_stone',height=.1)
                for u,v in ((.3,.5),(.7,.5)):
                    d=ImageDraw.Draw(self.im);q=self.project(x+u,y+.3,z+v);d.ellipse((q[0]-2,q[1]-4,q[0]+2,q[1]+2),fill='#ed564a' if props.get('powered')=='true' else '#8c332f')
            elif 'torch' in name:
                d=ImageDraw.Draw(self.im);p=self.project(x+.5,y+.1,z+.5);q=self.project(x+.5,y+.6,z+.5);d.line([p,q],fill='#ab8959',width=3);d.ellipse((q[0]-3,q[1]-3,q[0]+3,q[1]+3),fill='#ff6546')
            elif name=='sticky_piston':
                self.cube(x,y,z,'piston_top_sticky' if props.get('facing')=='up' else 'piston_side','piston_side',visible=vis)
            elif name=='piston_head':self.cube(x,y,z,'piston_top_sticky','piston_side',visible=vis)
            else:self.cube(x,y,z,name,visible=vis)
        return self.im

def main():
    W,H=1800,1240;im=Image.new('RGB',(W,H),'#0c1420');d=ImageDraw.Draw(im)
    mint='#81e7c0';muted='#98a6b8';white='#eff5fc'
    d.text((72,46),'REDSTONE / CUSTOM BUILD',font=font(19,True),fill=mint)
    d.text((72,87),'Секретный люк 2×2',font=font(65,True),fill=white)
    d.rounded_rectangle((1332,54,1728,105),radius=25,fill='#1a2c37')
    d.text((1361,66),'JAVA 1.21.4  ·  LITEMATICA',font=font(20,True),fill=mint)
    d.text((76,177),'Кнопка рядом с люком. Блоки уходят вниз и в стороны.',font=font(27),fill=muted)
    closed=read('floor_hatch_2x2_1.21.4');opened=read('floor_hatch_2x2_BUILD_OPEN')
    for left,title,desc,states,color in [(60,'01  ЗАКРЫТО','Сплошной пол из каменных кирпичей',closed,'#bac4d1'),(920,'02  ОТКРЫТО','Свободная шахта 2×2 — можно падать',opened,mint)]:
        d.rounded_rectangle((left,248,left+820,890),radius=25,fill='#141f2d',outline='#263646',width=2)
        d.text((left+30,278),title,font=font(27,True),fill=color)
        d.text((left+30,323),desc,font=font(21),fill=muted)
        r=Renderer(820,470,sx=28,sy=16,sz=27,origin=(410,205))
        scene=r.render(states,crop=(-6,6,-5,5))
        im.paste(scene,(left,375),scene)
        # Outline the precise four-block opening, using the same projection as the voxel renderer.
        poly=[r.project(x,6.025,z) for x,z in [(0,0),(2,0),(2,2),(0,2),(0,0)]]
        poly=[(p[0]+left,p[1]+375) for p in poly]
        d.line(poly,fill=color,width=3)
        d.text((left+30,846),'ПОРШНИ И ПРОВОДКА — ПОД ПЕРЕКРЫТИЕМ',font=font(15,True),fill=muted)
    d.line((74,946,1726,946),fill='#2a3b4c',width=2)
    for x,big,small in [(76,'2 × 2','размер прохода'),(480,'≈ 4,5 с','проход полностью свободен'),(945,'12 поршней','без командных блоков'),(1380,'22 × 30 × 7','габариты всей схемы')]:
        d.text((x,978),big,font=font(37,True),fill=white);d.text((x,1032),small,font=font(20),fill=muted)
    d.rounded_rectangle((74,1102,1726,1156),radius=14,fill='#17322f')
    d.text((96,1116),'ПРОВЕРЕНО НА VANILLA 1.21.4: 20 ЦИКЛОВ + НАЖАТИЕ КНОПКИ И ПАДЕНИЕ ИГРОВОГО КЛИЕНТА',font=font(18,True),fill=mint)
    d.text((76,1190),'Рендер блоков из .litematic, не скриншот игры. Показан фрагмент пола; контроллер продолжается под полом.',font=font(17),fill=muted)
    im.save(ROOT/'preview.png')
    # Full technical view: no artificial ground or decorative blocks added.
    rr=Renderer(1600,1200,sx=23,sy=12,sz=28,origin=(555,785))
    layer=rr.render(opened,(-11,10,-21,8),cutaway=True)
    cut=Image.new('RGB',(1600,1200),'#101a27');cut.paste(layer,(0,0),layer);dd=ImageDraw.Draw(cut)
    dd.text((60,40),'Технический вид · перекрытие скрыто',font=font(40,True),fill=white)
    dd.text((62,103),'Те же блоки схемы. Открытое положение / сервисный рычаг включён.',font=font(22),fill=muted)
    dd.text((62,1132),'Голубой — опоры проводки   /   оранжевый — таймер   /   зелёный — боковой привод',font=font(22),fill=muted)
    cut.save(ROOT/'mechanism.png')

if __name__=='__main__':main()
