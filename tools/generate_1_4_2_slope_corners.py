#!/usr/bin/env python3
"""Generate stair-like inner/outer corner models and blockstates for stepped slopes."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/faithfulblocks'
SHAPES = ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right')


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def texture(textures, face):
    if 'all' in textures:
        return '#all'
    return '#' + {'up': 'top', 'down': 'bottom'}.get(face, 'side')


def face_def(face, textures, lo, hi):
    definition = {'texture': texture(textures, face)}
    axis = {'west': (0, 0), 'east': (0, 16), 'down': (1, 0),
            'up': (1, 16), 'north': (2, 0), 'south': (2, 16)}
    index, boundary = axis[face]
    coord = lo[index] if face in ('west', 'down', 'north') else hi[index]
    if coord == boundary:
        definition['cullface'] = face
    return definition


def cell_element(ix, iz, height_steps, height_map, textures, top=False):
    x0, x1 = ix * 2, ix * 2 + 2
    z0, z1 = iz * 2, iz * 2 + 2
    h = height_steps * 2
    if top:
        y0, y1 = 16 - h, 16
    else:
        y0, y1 = 0, h

    lo = [x0, y0, z0]
    hi = [x1, y1, z1]
    faces = []

    # The exposed cap is always required. The opposite cap is only on the
    # block boundary and is eligible for normal neighbour culling.
    if top:
        faces.extend(('down', 'up'))
    else:
        faces.extend(('down', 'up'))

    neighbours = {
        'west': (ix - 1, iz),
        'east': (ix + 1, iz),
        'north': (ix, iz - 1),
        'south': (ix, iz + 1),
    }
    for face, (nx, nz) in neighbours.items():
        if nx < 0 or nx > 7 or nz < 0 or nz > 7:
            faces.append(face)
        else:
            nh = height_map[nz][nx]
            # Only the taller column owns the shared face. This avoids pairs
            # of coplanar internal faces while still exposing the riser above
            # a shorter neighbour.
            if nh < height_steps:
                faces.append(face)

    return {
        'from': lo,
        'to': hi,
        'faces': {face: face_def(face, textures, lo, hi) for face in faces},
    }


def height_map(shape):
    """Base orientation: low edge NORTH, high edge SOUTH."""
    rows = []
    for iz in range(8):
        row = []
        for ix in range(8):
            if shape == 'straight':
                h = iz + 1
            elif shape == 'outer_left':
                # Uphill direction SOUTH; left is EAST.
                h = min(ix, iz) + 1
            elif shape == 'outer_right':
                # Uphill direction SOUTH; right is WEST.
                h = min(7 - ix, iz) + 1
            elif shape == 'inner_left':
                h = max(ix, iz) + 1
            elif shape == 'inner_right':
                h = max(7 - ix, iz) + 1
            else:
                raise ValueError(shape)
            row.append(h)
        rows.append(row)
    return rows


def slope_elements(textures, shape, top=False):
    hm = height_map(shape)
    result = []
    for iz in range(8):
        for ix in range(8):
            result.append(cell_element(ix, iz, hm[iz][ix], hm, textures, top=top))
    return result


def model(textures, elements):
    return {
        'parent': 'minecraft:block/block',
        'ambientocclusion': True,
        'textures': textures,
        'elements': elements,
    }


def generate():
    materials = sorted(path.name.removesuffix('_rounded_corner.json')
                       for path in (ASSETS / 'blockstates').glob('*_rounded_corner.json'))
    assert len(materials) == 46, len(materials)

    for mat in materials:
        textures = read(ASSETS / f'models/block/{mat}_pedestal.json')['textures']
        base = f'{mat}_stepped_slope'

        for shape in SHAPES:
            suffix = '' if shape == 'straight' else f'_{shape}'
            write(ASSETS / f'models/block/{base}{suffix}.json',
                  model(textures, slope_elements(textures, shape, top=False)))
            write(ASSETS / f'models/block/{base}{suffix}_top.json',
                  model(textures, slope_elements(textures, shape, top=True)))

        variants = {}
        for turns, facing in enumerate(('north', 'east', 'south', 'west')):
            for half in ('bottom', 'top'):
                for shape in SHAPES:
                    suffix = '' if shape == 'straight' else f'_{shape}'
                    model_name = f'faithfulblocks:block/{base}{suffix}' + ('_top' if half == 'top' else '')
                    variant = {'model': model_name}
                    if turns:
                        variant['y'] = turns * 90
                        variant['uvlock'] = True
                    variants[f'facing={facing},half={half},shape={shape}'] = variant
        write(ASSETS / f'blockstates/{base}.json', {'variants': variants})

    print(f'Generated stair-like slope corner assets for {len(materials)} materials.')


if __name__ == '__main__':
    generate()
