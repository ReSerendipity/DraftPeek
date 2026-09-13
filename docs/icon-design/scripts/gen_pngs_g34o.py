# G34O final PNG renderer — supersampled chords (round caps) + eye dot on light background
import json, os
from PIL import Image, ImageDraw

OUT = r'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/outputs/DraftPeek-icon-export-g34o'
geo = json.load(open(os.path.join(OUT, 'geometry.json')))
SS = 4

def render(size, rounded=False):
    px = size * SS
    img = Image.new('RGB', (px, px), geo['bg'])
    dr = ImageDraw.Draw(img)
    s = px / 100.0
    w = geo['sw'] * s
    for c in geo['chords']:
        x1, y1, x2, y2 = c['x1'] * s, c['y1'] * s, c['x2'] * s, c['y2'] * s
        dr.line([x1, y1, x2, y2], fill=c['col'], width=round(w))
        r = w / 2
        for cx, cy in [(x1, y1), (x2, y2)]:
            dr.ellipse([cx - r, cy - r, cx + r, cy + r], fill=c['col'])
    e = geo['eye']
    er = e['r'] * s
    dr.ellipse([e['cx'] * s - er, e['cy'] * s - er, e['cx'] * s + er, e['cy'] * s + er], fill=e['col'])
    img = img.resize((size, size), Image.LANCZOS)
    if rounded:
        mask = Image.new('L', (size, size), 0)
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * 0.22), fill=255)
        out = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        out.paste(img, (0, 0), mask)
        return out
    return img.convert('RGB')

for size, name, rnd in [(512, 'ic_launcher-playstore.png', False), (512, 'ic_launcher-rounded-512.png', True),
                        (192, 'ic_launcher_192.png', True), (144, 'ic_launcher_144.png', True),
                        (96, 'ic_launcher_96.png', True), (72, 'ic_launcher_72.png', True), (48, 'ic_launcher_48.png', True)]:
    render(size, rnd).save(os.path.join(OUT, 'store-png', name))
    print('saved', name)
print('G34O PNG set done')
