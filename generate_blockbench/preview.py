import argparse, json, math, tempfile
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw

parser = argparse.ArgumentParser(description='Render preview images for a generated portal model.')
parser.add_argument('model', nargs='?', default='GoblinPortal', choices=['GoblinPortal', 'PocketPortal'])
parser.add_argument('--out', type=Path, default=Path(tempfile.gettempdir()) / 'portal_previews')
args = parser.parse_args()

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'src' / 'main' / 'resources' / 'Common' / 'Resources' / 'Items' / args.model
OUT = args.out / args.model
OUT.mkdir(parents=True, exist_ok=True)
model = json.load(open(BASE / f'{args.model}.blockymodel'))
tex = Image.open(BASE / f'{args.model}_Texture.png').convert('RGBA')


def qmat(q):
    x, y, z, w = q['x'], q['y'], q['z'], q['w']
    n = math.sqrt(x * x + y * y + z * z + w * w)
    x, y, z, w = x / n, y / n, z / n, w / n
    return np.array([
        [1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
        [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
        [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]])


# face: normal, u-axis, v-axis (v goes "down" in texture)
FACES = {
    'front': (np.array([0, 0, 1.]), np.array([1, 0, 0.]), np.array([0, -1, 0.])),
    'back': (np.array([0, 0, -1.]), np.array([-1, 0, 0.]), np.array([0, -1, 0.])),
    'right': (np.array([1., 0, 0]), np.array([0, 0, -1.]), np.array([0, -1, 0.])),
    'left': (np.array([-1., 0, 0]), np.array([0, 0, 1.]), np.array([0, -1, 0.])),
    'top': (np.array([0, 1., 0]), np.array([1, 0, 0.]), np.array([0, 0, 1.])),
    'bottom': (np.array([0, -1., 0]), np.array([1, 0, 0.]), np.array([0, 0, -1.])),
}


def collect(node, R, T, quads, skip_body_rot):
    Rl = qmat(node['orientation'])
    if skip_body_rot and node['name'] == 'Body':
        Rl = np.eye(3)
    p = node['position']
    T2 = T + R @ np.array([p['x'], p['y'], p['z']])
    R2 = R @ Rl
    s = node['shape']
    if s['type'] == 'box':
        sz = s['settings']['size']
        size = np.array([sz['x'], sz['y'], sz['z']])
        off = np.array([s['offset'][k] for k in 'xyz'])
        for fname, fl in s['textureLayout'].items():
            n, u, v = FACES[fname]
            uw = abs(u @ size)
            vh = abs(v @ size)
            origin = off + n * (abs(n @ size) / 2) - u * uw / 2 - v * vh / 2
            ox, oy = fl['offset']['x'], fl['offset']['y']
            for i in range(int(math.ceil(uw))):
                for j in range(int(math.ceil(vh))):
                    a0, a1 = i, min(i + 1, uw)
                    b0, b1 = j, min(j + 1, vh)
                    corners = [origin + u * a + v * b for a, b in ((a0, b0), (a1, b0), (a1, b1), (a0, b1))]
                    wc = [T2 + R2 @ c for c in corners]
                    col = tex.getpixel((min(63, ox + i), min(63, oy + j)))
                    quads.append((wc, R2 @ n, col, s['shadingMode']))
    for c in node.get('children', []):
        collect(c, R2, T2, quads, skip_body_rot)


def render(yaw, pitch, name, scale=14, size=520):
    quads = []
    for n in model['nodes']:
        collect(n, np.eye(3), np.zeros(3), quads, True)
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rx = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]])
    V = Rx @ Ry
    light = np.array([0.4, 0.8, 0.6])
    light /= np.linalg.norm(light)
    items = []
    for wc, nrm, col, shade in quads:
        if col[3] == 0:
            continue
        cn = V @ nrm
        if cn[2] <= 0:
            continue
        pts = [V @ (c - np.array([0, 15, 0])) for c in wc]
        depth = sum(p[2] for p in pts) / 4
        k = 1.0 if shade == 'fullbright' else 0.55 + 0.45 * max(0, nrm @ light)
        c = tuple(int(min(255, ch * k)) for ch in col[:3])
        items.append((depth, [(size / 2 + p[0] * scale, size / 2 - p[1] * scale) for p in pts], c))
    items.sort(key=lambda t: t[0])
    img = Image.new('RGB', (size, size), (24, 30, 44))
    d = ImageDraw.Draw(img)
    for _, poly, c in items:
        d.polygon(poly, fill=c, outline=c)
    img.save(OUT / f'{name}.png')
    return img


views = [render(-30, 15, 'pv_front34'), render(0, 0, 'pv_front'), render(180, 0, 'pv_back'), render(90, 10, 'pv_side')]
sheet = Image.new('RGB', (1040, 1040))
for k, im in enumerate(views):
    sheet.paste(im, ((k % 2) * 520, (k // 2) * 520))
sheet.save(OUT / 'preview_sheet.png')
tex.resize((512, 512), Image.NEAREST).save(OUT / 'texture_big.png')
