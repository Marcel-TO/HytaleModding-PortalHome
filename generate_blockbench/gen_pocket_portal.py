import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / 'src' / 'main' / 'resources' / 'Common' / 'Resources' / 'Items' / 'PocketPortal'
TEX_W, TEX_H = 64, 64


def hx(s):
    s = s.lstrip('#')
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


OBSIDIAN = [hx('07050d'), hx('100b1c'), hx('1a1229'), hx('251a3a'), hx('35264f'), hx('4b3970')]
GOLD = [hx('5b350c'), hx('8c5b16'), hx('b9821f'), hx('dca72f'), hx('f5d35a'), hx('fff1a6')]
RUNE = [hx('3d1d8f'), hx('6f3cff'), hx('a56bff'), hx('d9bcff')]
CRYSTAL = [hx('25105b'), hx('3f1b8f'), hx('6230d0'), hx('8b55ff'), hx('b98cff'), hx('eadcff')]
PORTAL = [hx('050318'), hx('10094a'), hx('1d168a'), hx('3332d0'), hx('5e6eff'), hx('8f6dff'), hx('c5a5ff'), hx('f1e8ff')]


class Atlas:
    def __init__(self, w, h):
        self.img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        self.w, self.h = w, h
        self.used = [[False] * w for _ in range(h)]
        self.cache = {}
        self.max_y = 0

    def alloc(self, w, h):
        for y in range(self.h - h + 1):
            for x in range(self.w - w + 1):
                if all(not self.used[y + j][x + i] for j in range(h) for i in range(w)):
                    for j in range(h):
                        for i in range(w):
                            self.used[y + j][x + i] = True
                    self.max_y = max(self.max_y, y + h)
                    return x, y
        raise RuntimeError(f'atlas full allocating {w}x{h}')

    def tile(self, key, w, h, painter):
        if key in self.cache:
            return self.cache[key]
        x0, y0 = self.alloc(w, h)
        for (i, j), color in painter(w, h).items():
            self.img.putpixel((x0 + i, y0 + j), color)
        self.cache[key] = (x0, y0)
        return x0, y0


def grid(w, h, f):
    return {(i, j): f(i, j) for i in range(w) for j in range(h)}


def ci(v):
    return max(1, int(math.ceil(v - 1e-6)))


def obsidian_painter(seed):
    def paint(w, h):
        r = random.Random(seed)

        def f(i, j):
            v = 2 if r.random() < 0.65 else r.choice([1, 3])
            if i == 0 or j == 0:
                v = 4 if r.random() < 0.75 else 3
            if i == w - 1 or j == h - 1:
                v = 0 if i == w - 1 and j == h - 1 else 1
            if (i + 2 * j + seed) % 11 == 0 and 0 < i < w - 1 and 0 < j < h - 1:
                v = 5
            return OBSIDIAN[v]
        return grid(w, h, f)
    return paint


def gold_painter(seed):
    def paint(w, h):
        r = random.Random(seed)

        def f(i, j):
            if w == 1 or h == 1:
                return GOLD[4] if (i + j) % 3 == 0 else GOLD[3]
            v = 3 if r.random() < 0.7 else 2
            if i == 0 or j == 0:
                v = 5 if (i + j) % 3 == 0 else 4
            if i == w - 1 or j == h - 1:
                v = 1
            if w > 2 and h > 2 and (i + j + seed) % 7 == 0:
                v = 4
            return GOLD[v]
        return grid(w, h, f)
    return paint


RUNE_GLYPHS = [
    ["X.X", ".X.", "XXX"],
    ["XXX", "X..", "XX."],
    [".X.", "XXX", ".X."],
    ["X..", "XXX", "..X"],
    ["XX.", ".XX", "X.."],
]


def rune_strip_painter(w, h):
    out = obsidian_painter(91)(w, h)
    for i in range(w):
        out[(i, 0)] = GOLD[4]
        out[(i, h - 1)] = GOLD[1]
    y = 2
    k = 0
    while y + 3 < h - 1:
        glyph = RUNE_GLYPHS[k % len(RUNE_GLYPHS)]
        for gy, row in enumerate(glyph):
            for gx, ch in enumerate(row):
                x = (w - 3) // 2 + gx
                if 0 <= x < w and ch == 'X':
                    out[(x, y + gy)] = RUNE[2 if gy != 1 else 3]
        y += 4
        k += 1
    return out


def lintel_rune_painter(w, h):
    out = obsidian_painter(77)(w, h)
    for i in range(w):
        out[(i, 0)] = GOLD[4]
        out[(i, h - 1)] = GOLD[1]
    for i in range(2, w - 2, 4):
        out[(i, 1)] = RUNE[2]
        if i + 1 < w - 1:
            out[(i + 1, 1)] = RUNE[3]
    return out


def crystal_painter(seed):
    def paint(w, h):
        r = random.Random(seed)

        def f(i, j):
            v = 3 if r.random() < 0.6 else 2
            if i == 0:
                v = 5 if j < h // 2 else 4
            elif i == w - 1:
                v = 1
            if j == 0 and i < w - 1:
                v = 5
            if j == h - 1:
                v = 0
            return CRYSTAL[v]
        return grid(w, h, f)
    return paint


def gem_painter(w, h):
    cx, cy = (w - 1) / 2, (h - 1) / 2

    def f(i, j):
        d = abs(i - cx) + abs(j - cy)
        if d < 0.6:
            return CRYSTAL[5]
        if d < 1.6:
            return CRYSTAL[4]
        return CRYSTAL[2]
    return grid(w, h, f)


def portal_painter(w, h):
    r = random.Random(42)
    out = {}
    for j in range(h):
        for i in range(w):
            nx = (i + 0.5 - w / 2) / (w / 2)
            ny = (j + 0.5 - h / 2) / (h / 2)
            edge = (abs(nx) ** 3 + abs(ny) ** 3) ** (1 / 3) + (r.random() - 0.5) * 0.1
            radius = math.hypot(nx * w / 2, ny * h / 2) / (min(w, h) / 2)
            spiral = math.sin(3 * math.atan2(ny, nx) - 10 * radius)
            if edge > 0.93:
                c = PORTAL[4] if r.random() < 0.65 else PORTAL[5]
            elif edge > 0.82:
                c = PORTAL[6] if r.random() < 0.5 else PORTAL[5]
            elif radius < 0.12:
                c = PORTAL[7]
            elif radius < 0.24:
                c = PORTAL[0]
            elif spiral > 0.55:
                c = PORTAL[6] if radius < 0.45 else PORTAL[5]
            elif spiral > 0.0:
                c = PORTAL[4]
            elif spiral > -0.55:
                c = PORTAL[2]
            else:
                c = PORTAL[1]
            if r.random() < 0.025 and edge < 0.8:
                c = PORTAL[7]
            out[(i, j)] = c
    return out


_id = [2]


def nid():
    _id[0] += 1
    return str(_id[0])


def quat_euler(x=0.0, y=0.0, z=0.0):
    def q(axis, angle):
        angle = math.radians(angle) / 2
        v = [0, 0, 0]
        v[axis] = math.sin(angle)
        return v[0], v[1], v[2], math.cos(angle)

    def mul(a, b):
        ax, ay, az, aw = a
        bx, by, bz, bw = b
        return (aw * bx + ax * bw + ay * bz - az * by,
                aw * by - ax * bz + ay * bw + az * bx,
                aw * bz + ax * by - ay * bx + az * bw,
                aw * bw - ax * bx - ay * by - az * bz)

    result = mul(q(2, z), mul(q(1, y), q(0, x)))
    return {k: round(v, 5) for k, v in zip('xyzw', result)}


def xyz(x, y, z):
    return {'x': round(x, 5), 'y': round(y, 5), 'z': round(z, 5)}


def face(offset):
    return {'offset': {'x': offset[0], 'y': offset[1]}, 'mirror': {'x': False, 'y': False}, 'angle': 0}


def empty_shape():
    return {'type': 'none', 'offset': xyz(0, 0, 0), 'stretch': xyz(0, 0, 0),
            'settings': {'isPiece': False}, 'textureLayout': {}, 'unwrapMode': 'custom',
            'visible': True, 'doubleSided': False, 'shadingMode': 'flat'}


def group(name, pos=(0, 0, 0), rot=None, children=None):
    return {'id': nid(), 'name': name, 'position': xyz(*pos),
            'orientation': rot or {'x': 0, 'y': 0, 'z': 0, 'w': 1},
            'shape': empty_shape(), 'children': children or []}


def box(name, pos, size, faces, rot=None, shading='flat'):
    return {'id': nid(), 'name': name, 'position': xyz(*pos),
            'orientation': rot or {'x': 0, 'y': 0, 'z': 0, 'w': 1},
            'shape': {'type': 'box', 'offset': xyz(0, 0, 0), 'stretch': xyz(1, 1, 1),
                      'settings': {'isPiece': False, 'size': xyz(*size)},
                      'textureLayout': {k: face(v) for k, v in faces.items()},
                      'unwrapMode': 'custom', 'visible': True, 'doubleSided': False,
                      'shadingMode': shading}}


A = Atlas(TEX_W, TEX_H)
PORTAL_W, PORTAL_H = 13, 18
portal_uv = A.tile('portal', PORTAL_W, PORTAL_H, portal_painter)
rune_uv = A.tile('rune_strip', 2, 18, rune_strip_painter)
lintel_uv = A.tile('lintel_rune', 15, 3, lintel_rune_painter)
gem_uv = A.tile('gem', 3, 3, gem_painter)

PAINTERS = {'obsidian': obsidian_painter, 'gold': gold_painter, 'crystal': crystal_painter}


def mat_tile(material, w, h, var):
    w, h = ci(w), ci(h)
    var %= 2
    key = (material, w, h, var)
    return A.tile(key, w, h, PAINTERS[material](w * 37 + h * 11 + var * 5))


def mat_box(name, pos, size, material, var=0, rot=None, shading='flat', front=None):
    w, h, d = size
    faces = {
        'front': front or mat_tile(material, w, h, var), 'back': mat_tile(material, w, h, var + 1),
        'left': mat_tile(material, d, h, var), 'right': mat_tile(material, d, h, var + 1),
        'top': mat_tile(material, w, d, var), 'bottom': mat_tile(material, w, d, var + 1),
    }
    return box(name, pos, size, faces, rot, shading)


def crystal(name, pos, rotz, scale=1.0):
    mid_h = 3 * scale
    tip_h = 1.5 * scale
    return group(name, pos, quat_euler(y=45, z=rotz), [
        mat_box(f'{name}_Core', (0, 0, 0), (2 * scale, mid_h, 2 * scale), 'crystal', 0, shading='fullbright'),
        mat_box(f'{name}_TipTop', (0, mid_h / 2 + tip_h / 2 - 0.05, 0), (1, tip_h, 1), 'crystal', 1, shading='fullbright'),
        mat_box(f'{name}_TipBottom', (0, -mid_h / 2 - tip_h / 2 + 0.05, 0), (1, tip_h, 1), 'crystal', 1, shading='fullbright'),
    ])


frame = group('Frame', children=[
    mat_box('Pillar_Left', (-8.5, -0.5, -0.5), (3, 23, 3), 'obsidian', 0),
    mat_box('Pillar_Right', (8.5, -0.5, -0.5), (3, 23, 3), 'obsidian', 1),
    mat_box('Lintel', (0, 10.5, -0.5), (15, 3, 3), 'obsidian', 0, front=lintel_uv),
    mat_box('Arch_Step', (0, 12.75, -0.5), (11, 1.5, 2.6), 'obsidian', 1),
    mat_box('Arch_Crown', (0, 14.25, -0.5), (6, 1.5, 2.4), 'obsidian', 0),
    mat_box('Base', (0, -11.25, -0.5), (21, 2.5, 3.6), 'obsidian', 1),
    mat_box('Plinth', (0, -13.25, -0.5), (15, 1.5, 4.4), 'obsidian', 0),
])

gold = group('Gold_Trim', children=[
    mat_box('Cap_Left', (-8.5, 11.75, -0.5), (4, 2.5, 4), 'gold', 0),
    mat_box('Cap_Right', (8.5, 11.75, -0.5), (4, 2.5, 4), 'gold', 1),
    mat_box('Spire_Left', (-8.5, 13.75, -0.5), (2, 1.5, 2), 'gold', 0),
    mat_box('Spire_Right', (8.5, 13.75, -0.5), (2, 1.5, 2), 'gold', 1),
    mat_box('Spire_Left_Tip', (-8.5, 15.0, -0.5), (1, 1, 1), 'gold', 0),
    mat_box('Spire_Right_Tip', (8.5, 15.0, -0.5), (1, 1, 1), 'gold', 1),
    mat_box('Band_Left_Mid', (-8.5, 0, -0.5), (3.4, 1, 3.4), 'gold', 0),
    mat_box('Band_Right_Mid', (8.5, 0, -0.5), (3.4, 1, 3.4), 'gold', 1),
    mat_box('Foot_Left', (-8.5, -9.75, -0.5), (4, 1.5, 4), 'gold', 0),
    mat_box('Foot_Right', (8.5, -9.75, -0.5), (4, 1.5, 4), 'gold', 1),
    mat_box('Rim_Top', (0, 8.85, 1.1), (13, 0.5, 0.5), 'gold', 0),
    mat_box('Rim_Bottom', (0, -9.85, 1.1), (13, 0.5, 0.5), 'gold', 1),
    mat_box('Rim_Left', (-6.75, -0.5, 1.1), (0.5, 19.2, 0.5), 'gold', 0),
    mat_box('Rim_Right', (6.75, -0.5, 1.1), (0.5, 19.2, 0.5), 'gold', 1),
    mat_box('Keystone', (0, 15.8, -0.5), (3, 2.2, 3.2), 'gold', 0),
    box('Keystone_Gem', (0, 15.8, 1.25), (1.6, 1.6, 0.4),
        {k: gem_uv for k in ('front', 'back', 'left', 'right', 'top', 'bottom')}, shading='fullbright'),
    box('Base_Gem', (0, -11.25, 1.45), (1.6, 1.6, 0.4),
        {k: gem_uv for k in ('front', 'back', 'left', 'right', 'top', 'bottom')}, shading='fullbright'),
])

runes = group('Runes', children=[
    box('Rune_Left', (-8.5, -0.5, 1.08), (2, 18, 0.15), {'front': rune_uv}, shading='fullbright'),
    box('Rune_Right', (8.5, -0.5, 1.08), (2, 18, 0.15), {'front': rune_uv}, shading='fullbright'),
    box('Rune_Left_Back', (-8.5, -0.5, -2.08), (2, 18, 0.15), {'back': rune_uv}, shading='fullbright'),
    box('Rune_Right_Back', (8.5, -0.5, -2.08), (2, 18, 0.15), {'back': rune_uv}, shading='fullbright'),
])

portal = group('Portal', children=[
    box('Portal_Energy', (0, -0.5, -0.5), (PORTAL_W, PORTAL_H, 0.4),
        {'front': portal_uv, 'back': portal_uv}, shading='fullbright'),
])

crystals = group('Floating_Crystals', children=[
    crystal('Crystal_Left_Upper', (-12.4, 6.5, 0.4), 12, 1.0),
    crystal('Crystal_Left_Lower', (-12.0, -5.5, -0.8), -18, 0.75),
    crystal('Crystal_Right_Upper', (12.5, 3.5, -0.6), -14, 1.0),
    crystal('Crystal_Right_Lower', (12.1, -7.0, 0.6), 20, 0.7),
    crystal('Crystal_Top', (0, 19.4, -0.5), 0, 0.65),
])

body = {
    'id': '2', 'name': 'Body', 'position': xyz(0, 15, 0),
    'orientation': {'x': 0.04324, 'y': 0.9905, 'z': 0.0057, 'w': 0.13041},
    'shape': empty_shape(),
    'children': [frame, gold, runes, portal, crystals],
}
root = {
    'id': '1', 'name': 'R-Attachment', 'position': xyz(0, 0, 0),
    'orientation': {'x': 0, 'y': 0, 'z': 0, 'w': 1},
    'shape': empty_shape(), 'children': [body],
}
model = {'nodes': [root], 'format': 'character', 'lod': 'auto'}

if __name__ == '__main__':
    with open(OUT_DIR / 'PocketPortal.blockymodel', 'w', newline='\n') as f:
        json.dump(model, f, indent=2)
    A.img.save(OUT_DIR / 'PocketPortal_Texture.png')
    print('atlas used up to y =', A.max_y, 'nodes', _id[0])
