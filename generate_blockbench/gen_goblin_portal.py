import json, math, random, sys
from PIL import Image

OUT_DIR = r"G:\Github\HytaleModding\_Plugins\PortalHome\src\main\resources\Common\Resources\Items\GoblinPortal"
TEX_W, TEX_H = 64, 64

def hx(s):
    s = s.lstrip('#')
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)

STONE = [hx('2c2a34'), hx('3c3a46'), hx('504e5c'), hx('63616f'), hx('7a7888'), hx('908ea0')]
MOSS = [hx('3e6a22'), hx('55882c')]
WOOD = [hx('3a2012'), hx('4e2c1a'), hx('663a22'), hx('7c4a2a'), hx('925a32')]
IRON = [hx('2a2a30'), hx('44444c'), hx('62626c')]
BONE = [hx('8e7c54'), hx('b4a274'), hx('d4c69c'), hx('ece2c4'), hx('fbf4de')]
GREEN = [hx('244a14'), hx('2e5e1a'), hx('3e7a22'), hx('559a2c'), hx('6eb83a')]
RED = [hx('5a120e'), hx('7a1912'), hx('9a241c'), hx('b8322a'), hx('d04a38')]
SIGN = [hx('3a2212'), hx('7a4622'), hx('945a2c'), hx('aa6c36'), hx('c08046')]
ROPE = [hx('6a4a28'), hx('8e6c40'), hx('b08c58')]
P_GREEN = [hx('2f8a14'), hx('4fc81c'), hx('7ef02c'), hx('b4ff5a'), hx('e2ffa0')]
P_PURPLE = [hx('16043c'), hx('2c0a6e'), hx('4614a6'), hx('6a28dc'), hx('9450ff'), hx('c08cff')]


# ---------------------------------------------------------------- atlas
class Atlas:
    def __init__(self, w, h):
        self.img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        self.w, self.h = w, h
        self.cache = {}
        self.x = self.y = self.row_h = 0

    def alloc(self, w, h):
        if self.x + w > self.w:
            self.x, self.y, self.row_h = 0, self.y + self.row_h, 0
        if self.y + h > self.h:
            raise RuntimeError(f'atlas full allocating {w}x{h}')
        pos = (self.x, self.y)
        self.x += w
        self.row_h = max(self.row_h, h)
        return pos

    def tile(self, key, w, h, painter):
        if key in self.cache:
            return self.cache[key]
        x0, y0 = self.alloc(w, h)
        for (i, j), c in painter(w, h).items():
            self.img.putpixel((x0 + i, y0 + j), c)
        self.cache[key] = (x0, y0)
        return x0, y0

    def place(self, key, x0, y0, w, h, painter):
        for (i, j), c in painter(w, h).items():
            self.img.putpixel((x0 + i, y0 + j), c)
        self.cache[key] = (x0, y0)
        return x0, y0


def grid(w, h, f):
    return {(i, j): f(i, j) for i in range(w) for j in range(h)}


# ---------------------------------------------------------------- painters
def stone_painter(seed, mossy=False):
    def p(w, h):
        r = random.Random(seed)
        out = {}
        for j in range(h):
            for i in range(w):
                v = 3 if r.random() < 0.65 else r.choice([2, 2, 4])
                if j == 0 or i == 0:
                    v = 4 if r.random() < 0.8 else 5
                if j == h - 1 or i == w - 1:
                    v = 1 if (j == h - 1 and i == w - 1) else 2
                c = STONE[v]
                if mossy and j == 0 and r.random() < 0.7:
                    c = r.choice(MOSS)
                out[(i, j)] = c
        return out
    return p


def wood_painter(seed, vertical, bands=()):
    def p(w, h):
        r = random.Random(seed)
        out = {}
        along, across = (h, w) if vertical else (w, h)
        grain = [r.choice([2, 3, 3, 4]) for _ in range(across)]
        for a in range(along):
            for b in range(across):
                v = grain[b]
                if r.random() < 0.18:
                    v = max(1, v - 1)
                if b == across - 1:
                    v = 1
                if b == 0:
                    v = min(4, v + 1)
                c = WOOD[v]
                if a in bands:
                    c = IRON[1] if b not in (0, across - 1) else IRON[2]
                    if b == across // 2:
                        c = IRON[2]
                i, j = (b, a) if vertical else (a, b)
                out[(i, j)] = c
        return out
    return p


def endgrain_painter(w, h):
    def f(i, j):
        cx, cy = (w - 1) / 2, (h - 1) / 2
        d = max(abs(i - cx), abs(j - cy))
        return WOOD[4] if d < 0.6 else (WOOD[3] if int(d) % 2 == 0 else WOOD[2])
    return grid(w, h, f)


def bone_painter(seed):
    def p(w, h):
        r = random.Random(seed)
        def f(i, j):
            if i == 0:
                return BONE[4] if r.random() < 0.6 else BONE[3]
            if i == w - 1:
                return BONE[1]
            return BONE[3] if r.random() < 0.75 else BONE[2]
        return grid(w, h, f)
    return p


def bone_tip_painter(w, h):
    def f(i, j):
        return BONE[4] if i == 0 else BONE[3]
    return grid(w, h, f)


def cloth_painter(pal, seed, w_fold=2):
    def p(w, h):
        r = random.Random(seed)
        def f(i, j):
            fold = (i // w_fold) % 2
            v = 3 if fold == 0 else 2
            if r.random() < 0.15:
                v -= 1
            if j == h - 1:
                v = 1
            if j == 0:
                v = 4
            return pal[v]
        return grid(w, h, f)
    return p


# X = bone, O = dark socket, b = bone shadow
BANNER_SKULL = [
    ".XXX.",
    "XXXXX",
    "XOXOX",
    "XXbXX",
    ".XbX.",
    ".....",
    "X...X",
    ".b.b.",
    "..X..",
    ".b.b.",
    "X...X",
]


def banner_painter(w, h):
    out = cloth_painter(GREEN, 3)(w, h)
    for i in range(w):
        out[(i, 0)] = GREEN[4]
        if i % 2 == 0:
            out[(i, h - 1)] = GREEN[0]
            out[(i, h - 2)] = GREEN[1]
    sx, sy = (w - 5) // 2 + (w - 5) % 2, 1
    for j, row in enumerate(BANNER_SKULL):
        for i, ch in enumerate(row):
            x, y = sx + i, sy + j
            if not (0 <= x < w and 0 <= y < h - 1):
                continue
            if ch == 'X':
                out[(x, y)] = BONE[3]
            elif ch == 'b':
                out[(x, y)] = BONE[1]
            elif ch == 'O':
                out[(x, y)] = hx('16200e')
    return out


SIGN_SKULL = [
    ".BBB.",
    "BEBEB",
    "BBBBB",
    ".B.B.",
    "B...B",
]


def sign_painter(w, h):
    r = random.Random(11)
    out = {}
    for j in range(h):
        for i in range(w):
            v = 3 if (j % 3) != 2 else 2
            if r.random() < 0.15:
                v = 2
            if i in (0, w - 1) or j in (0, h - 1):
                v = 1
            out[(i, j)] = SIGN[v]
    sx, sy = (w - 5) // 2, (h - 5) // 2
    for j, row in enumerate(SIGN_SKULL):
        for i, ch in enumerate(row):
            if ch == 'B':
                out[(sx + i, sy + j)] = SIGN[0]
            elif ch == 'E':
                out[(sx + i, sy + j)] = hx('e07a2a')
    return out

def skull_charm_painter(w, h):
    def f(i, j):
        if j == 1 and i in (0, w - 1):
            return BONE[0]
        if j == 1 and 0 < i < w - 1 and i != w // 2:
            return hx('1a1a1a')
        if j == h - 1 and i % 2 == 1:
            return BONE[1]
        return BONE[3] if j == 0 else BONE[2]
    return grid(w, h, f)


def rope_painter(w, h):
    return grid(w, h, lambda i, j: ROPE[(i + j) % 3])


def iron_painter(w, h):
    return grid(w, h, lambda i, j: IRON[2] if (i + j) % 3 == 0 else IRON[1])


def portal_painter(w, h):
    r = random.Random(42)
    out = {}
    for j in range(h):
        for i in range(w):
            nx = (i + 0.5 - w / 2) / (w / 2)
            ny = (j + 0.5 - h / 2) / (h / 2)
            p = 3.0
            d = (abs(nx) ** p + abs(ny) ** p) ** (1 / p)
            d += (r.random() - 0.5) * 0.12
            if d > 0.93:
                c = P_GREEN[1] if r.random() < 0.7 else P_GREEN[0]
            elif d > 0.8:
                c = P_GREEN[2] if r.random() < 0.6 else P_GREEN[3]
            elif d > 0.69:
                c = P_GREEN[3] if r.random() < 0.55 else P_GREEN[4]
            elif d > 0.6:
                c = P_GREEN[2] if r.random() < 0.45 else P_PURPLE[4]
            else:
                rr = math.hypot(nx * w / 2, ny * h / 2) / (min(w, h) / 2)
                th = math.atan2(ny, nx)
                s = math.sin(2 * th - 11.0 * rr)
                if rr < 0.12:
                    c = P_PURPLE[5]
                elif rr < 0.22:
                    c = P_PURPLE[1]
                elif s > 0.55:
                    c = P_PURPLE[5] if rr < 0.4 else P_PURPLE[4]
                elif s > -0.1:
                    c = P_PURPLE[3]
                elif s > -0.6:
                    c = P_PURPLE[2]
                else:
                    c = P_PURPLE[1]
            out[(i, j)] = c
    return out


# ---------------------------------------------------------------- model helpers
_id = [2]


def nid():
    _id[0] += 1
    return str(_id[0])


def quat_euler(x=0.0, y=0.0, z=0.0):
    """Degrees, applied in X then Y then Z order (q = qz*qy*qx)."""
    def q(axis, a):
        a = math.radians(a) / 2
        s = math.sin(a)
        v = [0, 0, 0]
        v[axis] = s
        return (v[0], v[1], v[2], math.cos(a))

    def mul(a, b):
        ax, ay, az, aw = a
        bx, by, bz, bw = b
        return (aw * bx + ax * bw + ay * bz - az * by,
                aw * by - ax * bz + ay * bw + az * bx,
                aw * bz + ax * by - ay * bx + az * bw,
                aw * bw - ax * bx - ay * by - az * bz)
    res = mul(q(2, z), mul(q(1, y), q(0, x)))
    return {k: round(v, 5) for k, v in zip('xyzw', res)}


def xyz(x, y, z):
    return {'x': round(x, 5), 'y': round(y, 5), 'z': round(z, 5)}


def face(off):
    return {'offset': {'x': off[0], 'y': off[1]}, 'mirror': {'x': False, 'y': False}, 'angle': 0}


def empty_shape():
    return {'type': 'none', 'offset': xyz(0, 0, 0), 'stretch': xyz(0, 0, 0),
            'settings': {'isPiece': False}, 'textureLayout': {}, 'unwrapMode': 'custom',
            'visible': True, 'doubleSided': False, 'shadingMode': 'flat'}


def group(name, pos=(0, 0, 0), rot=None, children=None):
    return {'id': nid(), 'name': name, 'position': xyz(*pos),
            'orientation': rot or {'x': 0, 'y': 0, 'z': 0, 'w': 1},
            'shape': empty_shape(), 'children': children or []}


def box(name, pos, size, faces, rot=None, shading='flat', children=None):
    sx, sy, sz = size
    n = {'id': nid(), 'name': name, 'position': xyz(*pos),
         'orientation': rot or {'x': 0, 'y': 0, 'z': 0, 'w': 1},
         'shape': {'type': 'box', 'offset': xyz(0, 0, 0), 'stretch': xyz(1, 1, 1),
                   'settings': {'isPiece': False, 'size': xyz(sx, sy, sz)},
                   'textureLayout': {k: face(v) for k, v in faces.items()},
                   'unwrapMode': 'custom', 'visible': True, 'doubleSided': False,
                   'shadingMode': shading}}
    if children:
        n['children'] = children
    return n


def ci(v):
    return max(1, int(math.ceil(v - 1e-6)))


# ---------------------------------------------------------------- build
A = Atlas(TEX_W, TEX_H)

# Fixed regions first (larger tiles)
PORTAL_W, PORTAL_H = 14, 18
portal_uv = A.tile('portal', PORTAL_W, PORTAL_H, portal_painter)
post_uv = A.tile('post_v', 3, 25, wood_painter(1, True, bands=(3, 4, 20, 21)))
post_uv2 = A.tile('post_v2', 3, 25, wood_painter(2, True, bands=(5, 6, 18, 19)))
banner_uv = A.tile('banner', 6, 13, banner_painter)
beam_uv = A.tile('beam_h', 21, 3, wood_painter(3, False, bands=(2, 3, 17, 18)))
beam_uv2 = A.tile('beam_h2', 21, 3, wood_painter(4, False, bands=(4, 5, 15, 16)))
banner_top_uv = A.tile('banner_top', 6, 7, cloth_painter(GREEN, 5))
banner_back_uv = A.tile('banner_back', 6, 8, cloth_painter(GREEN, 6))
red_uv = A.tile('red_side', 3, 9, cloth_painter(RED, 9, 1))
red2_uv = A.tile('red_front', 3, 7, cloth_painter(RED, 10, 1))
sign_uv = A.tile('sign', 7, 7, sign_painter)
endgrain_uv = A.tile('endgrain', 3, 3, endgrain_painter)
rope_uv = A.tile('rope', 1, 4, rope_painter)
iron_uv = A.tile('iron', 4, 4, iron_painter)
charm_uv = A.tile('charm', 3, 3, skull_charm_painter)


def stone_tile(w, h, var, mossy=False):
    w, h = ci(w), ci(h)
    var %= 3
    return A.tile(('stone', w, h, var, mossy), w, h, stone_painter(w * 97 + h * 13 + var * 5 + mossy, mossy))


def bone_tile(w, h, var=0):
    w, h = ci(w), ci(h)
    return A.tile(('bone', w, h, var), w, h, bone_painter(w * 31 + h * 7 + var))


def stone(name, pos, size, var=0, rotz=0.0, mossy=False):
    w, h, d = size
    faces = {
        'front': stone_tile(w, h, var, mossy), 'back': stone_tile(w, h, var + 1, mossy),
        'left': stone_tile(d, h, var + 2), 'right': stone_tile(d, h, var + 3),
        'top': stone_tile(w, d, var + 4, mossy), 'bottom': stone_tile(w, d, var + 5),
    }
    return box(name, pos, size, faces, quat_euler(z=rotz) if rotz else None)


def bone_spike(name, pos, rotz, scale=1.0, depth_scale=1.0, bend=0.0):
    """Tapered bone spike built from stacked boxes along local +Y; each segment is bent by `bend` degrees."""
    segs = [(4, 3), (3, 3), (2, 3), (1, 2)] if scale >= 1 else [(2, 2), (1, 2)]
    root = group(name, pos, quat_euler(z=rotz))
    parent, prev_h = root, 0.0
    for k, (wd, ht) in enumerate(segs):
        dd = max(1, wd * depth_scale)
        uv = bone_tile(wd, ht, k)
        uvt = bone_tile(wd, dd, k + 3)
        uvs = bone_tile(dd, ht, k + 6)
        faces = {'front': uv, 'back': uv, 'left': uvs, 'right': uvs, 'top': uvt, 'bottom': uvt}
        joint = group(f'{name}_Joint{k + 1}', (0, prev_h - 0.2 if k else 0, 0), quat_euler(z=bend) if k and bend else None)
        joint['children'].append(box(f'{name}_Seg{k + 1}', (0, ht / 2, 0), (wd, ht, dd), faces))
        parent['children'].append(joint)
        parent, prev_h = joint, ht
    return root


def wood_box(name, pos, size, uv_long, vertical, rot=None):
    w, h, d = size
    end = endgrain_uv
    if vertical:
        faces = {'front': uv_long, 'back': uv_long, 'left': uv_long, 'right': uv_long,
                 'top': end, 'bottom': end}
    else:
        faces = {'front': uv_long, 'back': uv_long, 'top': uv_long, 'bottom': uv_long,
                 'left': end, 'right': end}
    return box(name, pos, size, faces, rot)


# --- Frame (wood)
frame = group('Frame', children=[
    wood_box('Post_Left', (-8.5, 0, -0.5), (3, 25, 3), post_uv, True),
    wood_box('Post_Right', (8.5, 0, -0.5), (3, 25, 3), post_uv2, True),
    wood_box('Beam_Top', (0, 10.5, -0.6), (21, 3, 2.6), beam_uv, False),
    wood_box('Beam_Bottom', (0, -10.5, -0.6), (21, 3, 2.6), beam_uv2, False),
    box('Strap_Top_Left', (-8.5, 10.5, -0.5), (3.4, 1, 3.4),
        {k: iron_uv for k in ('front', 'back', 'left', 'right', 'top', 'bottom')}),
    box('Strap_Top_Right', (8.5, 10.5, -0.5), (3.4, 1, 3.4),
        {k: iron_uv for k in ('front', 'back', 'left', 'right', 'top', 'bottom')}),
    box('Strap_Bottom_Left', (-8.5, -10.5, -0.5), (3.4, 1, 3.4),
        {k: iron_uv for k in ('front', 'back', 'left', 'right', 'top', 'bottom')}),
    box('Strap_Bottom_Right', (8.5, -10.5, -0.5), (3.4, 1, 3.4),
        {k: iron_uv for k in ('front', 'back', 'left', 'right', 'top', 'bottom')}),
])

# --- Portal swirl (visible from both sides, glowing)
portal = group('Portal', children=[
    box('Portal_Energy', (0, 0, -0.5), (PORTAL_W, PORTAL_H, 0.4),
        {'front': portal_uv, 'back': portal_uv}, shading='fullbright'),
])

# --- Slate stones stacked around the opening (front side)
stones = group('Stones', children=[
    # left column
    stone('Stone_L1', (-7.6, 7.0, 1.10), (5, 4, 4), 0, mossy=True),
    stone('Stone_L2', (-7.2, 2.6, 1.35), (4, 4, 4), 1, rotz=-4),
    stone('Stone_L3', (-8.6, -1.8, 0.95), (5, 4, 4), 2),
    stone('Stone_L4', (-7.3, -6.2, 1.20), (4, 4, 4), 3, rotz=3),
    stone('Stone_L5', (-8.2, -10.6, 1.05), (5, 3, 4), 4),
    # right column
    stone('Stone_R1', (7.6, 7.6, 1.15), (4, 4, 4), 5, mossy=True),
    stone('Stone_R2', (7.2, 3.6, 1.40), (4, 3, 4), 6, rotz=5),
    stone('Stone_R3', (8.4, -0.6, 0.90), (4, 4, 4), 7),
    stone('Stone_R4', (7.4, -5.6, 1.25), (4, 4, 4), 8, rotz=-3),
    stone('Stone_R5', (8.1, -10.4, 1.00), (5, 3, 4), 9),
    # top row
    stone('Stone_T1', (-2.8, 10.9, 1.30), (4, 3, 4), 10, mossy=True),
    stone('Stone_T2', (1.8, 11.3, 1.00), (5, 3, 4), 11, rotz=3),
    stone('Stone_T3', (5.4, 10.4, 1.55), (3, 3, 4), 12),
    # bottom row
    stone('Stone_B1', (-3.2, -10.9, 1.25), (4, 3, 4), 13),
    stone('Stone_B2', (1.6, -11.3, 0.95), (5, 3, 4), 14, rotz=-3),
    stone('Stone_B3', (5.0, -10.1, 1.45), (3, 3, 4), 15),
    # back side stones
    stone('Stone_Back1', (-8.0, 5.0, -2.25), (4, 4, 2), 16),
    stone('Stone_Back2', (8.2, -3.5, -2.20), (4, 5, 2), 17),
    stone('Stone_Back3', (-1.5, 10.6, -2.30), (5, 3, 2), 18),
    stone('Stone_Back4', (2.5, -10.6, -2.35), (5, 3, 2), 19),
    stone('Stone_Back5', (-8.4, -7.0, -2.15), (3, 4, 2), 20),
])

# --- Bone horns and teeth
bones = group('Bones', children=[
    bone_spike('Horn_Big', (8.0, 12.4, -0.4), -22, 1.0, bend=12),
    bone_spike('Horn_Top_A', (-0.2, 12.6, 0.9), 6, 0.5),
    bone_spike('Horn_Top_B', (3.8, 11.9, 1.4), -8, 0.5),
    bone_spike('Horn_Left', (-10.2, 11.0, -0.5), 50, 0.5),
    # teeth biting into the portal
    bone_spike('Tooth_Top_A', (-2.6, 9.4, 1.6), 180, 0.5, 0.6),
    bone_spike('Tooth_Top_B', (2.2, 9.6, 1.4), 172, 0.5, 0.6),
    bone_spike('Tooth_Bottom_A', (-3.0, -9.3, 1.6), 4, 0.5, 0.6),
    bone_spike('Tooth_Bottom_B', (0.8, -9.6, 1.3), -6, 0.5, 0.6),
    bone_spike('Tooth_Bottom_C', (4.4, -8.8, 1.7), 0, 0.5, 0.6),
])

# --- Cloth: green skull banner draped over the top-left, red rags
banner_side = bone_tile(1, 1, 99)  # tiny pale edge, overwritten below
cloth = group('Cloth', children=[
    box('Banner_Front', (-6.3, 6.9, 3.85), (6, 13, 0.5),
        {'front': banner_uv, 'back': banner_uv, 'left': banner_back_uv, 'right': banner_back_uv,
         'bottom': banner_top_uv, 'top': banner_top_uv}, quat_euler(z=-4)),
    box('Banner_Top', (-6.6, 13.35, 0.55), (6, 0.5, 7.1),
        {'front': banner_top_uv, 'back': banner_top_uv, 'left': banner_top_uv, 'right': banner_top_uv,
         'top': banner_top_uv, 'bottom': banner_top_uv}),
    box('Banner_Back', (-6.6, 9.3, -3.25), (6, 8, 0.5),
        {'front': banner_back_uv, 'back': banner_back_uv, 'left': banner_back_uv, 'right': banner_back_uv,
         'top': banner_back_uv, 'bottom': banner_back_uv}, quat_euler(z=3)),
    box('Rag_Red_Right', (10.85, 7.0, -0.4), (0.5, 9, 3),
        {'left': red_uv, 'right': red_uv, 'front': red_uv, 'back': red_uv, 'top': red_uv, 'bottom': red_uv},
        quat_euler(z=4)),
    box('Rag_Red_Left', (-9.3, -10.8, 3.45), (3, 7, 0.5),
        {'front': red2_uv, 'back': red2_uv, 'left': red2_uv, 'right': red2_uv, 'top': red2_uv, 'bottom': red2_uv},
        quat_euler(z=6)),
])

# --- Trinkets: skull sign + hanging charms
sign_side = A.tile(('sign_edge',), 1, 7, lambda w, h: grid(w, h, lambda i, j: SIGN[1]))
trinkets = group('Trinkets', children=[
    box('Sign_Skull', (9.6, 0.4, 3.75), (7, 7, 0.8),
        {'front': sign_uv, 'back': sign_uv, 'left': sign_side, 'right': sign_side,
         'top': sign_side, 'bottom': sign_side}, quat_euler(z=7)),
    group('Charm_Left', (-10.6, 3.5, 0.6), quat_euler(z=-5), [
        box('Charm_Left_Rope', (0, -1.5, 0), (0.5, 3, 0.5),
            {k: rope_uv for k in ('front', 'back', 'left', 'right', 'top', 'bottom')}),
        box('Charm_Left_Skull', (0, -4.4, 0), (3, 3, 2.5),
            {'front': charm_uv, 'back': charm_uv, 'left': bone_tile(3, 3, 50), 'right': bone_tile(3, 3, 50),
             'top': bone_tile(3, 3, 51), 'bottom': bone_tile(3, 3, 51)}),
        box('Charm_Left_Jaw', (0, -6.3, 0.2), (2, 1, 2),
            {k: bone_tile(2, 2, 52) for k in ('front', 'back', 'left', 'right', 'top', 'bottom')}),
    ]),
    group('Charm_Right', (10.6, -3.0, 0.8), quat_euler(z=6), [
        box('Charm_Right_Rope', (0, -1.5, 0), (0.5, 3, 0.5),
            {k: rope_uv for k in ('front', 'back', 'left', 'right', 'top', 'bottom')}),
        box('Charm_Right_Bone', (0, -4.5, 0), (1, 3, 1),
            {k: bone_tile(1, 3, 60) for k in ('front', 'back', 'left', 'right', 'top', 'bottom')}),
        box('Charm_Right_Knob_Top', (0, -3.1, 0), (2, 1, 1.2),
            {k: bone_tile(2, 1, 61) for k in ('front', 'back', 'left', 'right', 'top', 'bottom')}),
        box('Charm_Right_Knob_Bottom', (0, -5.9, 0), (2, 1, 1.2),
            {k: bone_tile(2, 1, 61) for k in ('front', 'back', 'left', 'right', 'top', 'bottom')}),
    ]),
])

# Keep the original hierarchy: R-Attachment -> Body (Body is animated by the interaction anim)
body = {
    'id': '2', 'name': 'Body', 'position': xyz(0, 15, 0),
    'orientation': {'x': 0.04324, 'y': 0.9905, 'z': 0.0057, 'w': 0.13041},
    'shape': empty_shape(),
    'children': [frame, portal, stones, bones, cloth, trinkets],
}
root = {
    'id': '1', 'name': 'R-Attachment', 'position': xyz(0, 0, 0),
    'orientation': {'x': 0, 'y': 0, 'z': 0, 'w': 1},
    'shape': empty_shape(), 'children': [body],
}
model = {'nodes': [root], 'format': 'character', 'lod': 'auto'}

if __name__ == '__main__':
    with open(OUT_DIR + r'\GoblinPortal.blockymodel', 'w', newline='\n') as f:
        json.dump(model, f, indent=2)
    A.img.save(OUT_DIR + r'\GoblinPortal_Texture.png')
    print('atlas used up to y =', A.y, 'nodes', _id[0])
