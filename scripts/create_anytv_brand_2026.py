#!/usr/bin/env python3
"""Build approved red/black AnyTV assets, without touching Android identity."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "assets"
OUT.mkdir(exist_ok=True)
RED = (247, 9, 53)
WHITE = (247, 247, 248)

def color_ramp(width, height):
    img = Image.new("RGB", (width, height))
    p = img.load()
    for y in range(height):
        for x in range(width):
            t = min(1., .68 * x / width + .32 * y / height)
            p[x,y] = (int(255 - 41*t),int(24 - 18*t),int(64 - 31*t))
    return img

def mark(canvas, left, top, width):
    height = int(width*.62)
    thick = int(width*.105)
    mask=Image.new("L",canvas.size,0)
    d=ImageDraw.Draw(mask)
    d.rounded_rectangle((left,top,left+width,top+height),
                        radius=int(width*.16),fill=255)
    d.rounded_rectangle((left+thick,top+thick,left+width-thick,top+height-thick),
                        radius=int(width*.095),fill=0)
    d.rectangle((left+int(width*.36),top+height-thick,
                 left+int(width*.72),top+height),fill=0)
    d.polygon([(left+int(width*.36),top+int(height*.23)),
               (left+int(width*.36),top+int(height*.77)),
               (left+int(width*.68),top+int(height*.5))],fill=255)
    canvas.paste(color_ramp(canvas.width,canvas.height),(0,0),mask)

def font(size):
    for n in ["C:/Windows/Fonts/seguisb.ttf","C:/Windows/Fonts/arialbd.ttf",
              "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"]:
        if Path(n).is_file():return ImageFont.truetype(n,size)
    return ImageFont.load_default()

def wordmark(img,y,size,x=None):
    d=ImageDraw.Draw(img); f=font(size)
    aw=d.textbbox((0,0),"Any",font=f)[2]
    tw=d.textbbox((0,0),"TV",font=f)[2]
    start=(img.width-aw-tw)//2 if x is None else x
    d.text((start,y),"Any",font=f,fill=WHITE)
    d.text((start+aw,y),"TV",font=f,fill=RED)

icon=Image.new("RGB",(512,512),(6,6,9))
ImageDraw.Draw(icon).rounded_rectangle((13,13,498,498),radius=102,
                                       outline=(52,41,47),width=3)
mark(icon,79,158,354)
icon.save(OUT/"anytv_icon.png","PNG",optimize=True)

logo=Image.new("RGB",(768,595),(0,0,0))
mark(logo,190,112,388)
wordmark(logo,368,112)
logo.save(OUT/"anytv_logo.png","PNG",optimize=True)

horizontal=Image.new("RGB",(850,190),(0,0,0))
mark(horizontal,15,39,215)
ImageDraw.Draw(horizontal).line((258,33,258,157),fill=(110,102,106),width=2)
wordmark(horizontal,43,102,x=294)
horizontal.save(OUT/"anytv_horizontal.png","PNG",optimize=True)
print("ANYTV_RED_BRAND_ASSETS_READY")
