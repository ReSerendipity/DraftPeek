# G34R·红弦 PNG rasterizer — supersampled line rendering with round caps
import json, os, math
from PIL import Image, ImageDraw

OUT = r'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/outputs/DraftPeek-icon-export'
geo = json.load(open(os.path.join(OUT, 'geometry.json')))
SS = 4  # supersample factor

def render(size, rounded=False):
    px = size * SS
    img = Image.new('RGB', (px, px), geo['BG'])
    dr = ImageDraw.Draw(img)
    s = px / 100.0  # 100-grid → px
    for ch in geo['chords']:
        w = geo['SW'] * s
        x1, y1, x2, y2 = ch['x1'] * s, ch['y1'] * s, ch['x2'] * s, ch['y2'] * s
        col = geo['RED'] if ch['red'] else geo['INK']
        dr.line([x1, y1, x2, y2], fill=col, width=round(w))
        r = w / 2
        for (cx, cy) in [(x1, y1), (x2, y2)]:
            dr.ellipse([cx - r, cy - r, cx + r, cy + r], fill=col)
    img = img.resize((size, size), Image.LANCZOS)
    if rounded:
        mask = Image.new('L', (size, size), 0)
        md = ImageDraw.Draw(mask)
        md.rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * 0.22), fill=255)
        out = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        out.paste(img, (0, 0), mask)
        return out
    return img.convert('RGB')

targets = [(512, 'ic_launcher-playstore.png', False),
           (512, 'ic_launcher-rounded-512.png', True),
           (192, 'ic_launcher_192.png', True),
           (144, 'ic_launcher_144.png', True),
           (96, 'ic_launcher_96.png', True),
           (72, 'ic_launcher_72.png', True),
           (48, 'ic_launcher_48.png', True)]
for size, name, rnd in targets:
    im = render(size, rnd)
    p = os.path.join(OUT, 'store-png', name)
    im.save(p)
    print('saved', name)
print('PNG set done')
