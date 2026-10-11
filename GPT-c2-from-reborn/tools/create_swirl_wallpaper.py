#!/usr/bin/env python3
"""Generate a 240x320 slate-grey Nokia-style swirl wallpaper. Deterministic."""
from PIL import Image, ImageDraw, ImageFilter
import math, sys
from pathlib import Path

S=3
W,H=240*S,320*S
im=Image.new("RGB",(W,H))
pix=im.load()
for y in range(H):
    for x in range(W):
        u=x/W;v=y/H
        glow=54*math.exp(-(((u-.40)/.57)**2+((v-.57)/.42)**2))
        edge=15*((u-.5)**2+(v-.5)**2)
        tone=int(42+glow-23*v-edge)
        pix[x,y]=(tone+2,tone+3,tone+5)
bands=Image.new("RGBA",(W,H),(0,0,0,0))
d=ImageDraw.Draw(bands)
def path(points,width,rgba):
    # smooth quadratic Bezier interpolation
    p=[]
    for a,b,c in points:
        for j in range(44):
            t=j/43
            px=(1-t)**2*a[0]+2*(1-t)*t*b[0]+t*t*c[0]
            py=(1-t)**2*a[1]+2*(1-t)*t*b[1]+t*t*c[1]
            p.append((int(px*S),int(py*S)))
    d.line(p,fill=rgba,width=int(width*S),joint="curve")
path([((-50,56),(132,23),(193,150)),((193,150),(205,210),(254,29))],2,(206,214,221,67))
path([((-54,70),(127,31),(183,154)),((183,154),(213,226),(278,36))],10,(139,147,156,28))
path([((-39,251),(95,168),(187,217)),((187,217),(240,245),(290,122))],22,(205,213,220,23))
path([((-55,260),(100,165),(194,226)),((194,226),(239,256),(300,131))],2,(221,224,229,72))
path([((-65,290),(84,176),(141,231)),((141,231),(188,310),(302,302))],3,(227,233,239,56))
path([((159,-40),(275,70),(178,200)),((178,200),(121,261),(257,332))],4,(225,231,236,47))
im=Image.alpha_composite(im.convert("RGBA"),bands.filter(ImageFilter.GaussianBlur(1.0*S))).convert("RGB")
dest=Path(sys.argv[1]) if len(sys.argv)>1 else Path("screen_wallpaper.png")
dest.parent.mkdir(parents=True,exist_ok=True)
im.resize((240,320),Image.Resampling.LANCZOS).save(dest,format="PNG",optimize=True)
assert Image.open(dest).size==(240,320)
print(f"Generated {dest}: {dest.stat().st_size} bytes, 240x320 RGB PNG")
