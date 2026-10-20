#!/usr/bin/env python3
"""FaithfulBlocks 1.6 placement-polish window frame assets.

New frames are vertical, centered like panes and use 1px shared mullions when
connected to another frame. Legacy UP/DOWN models remain referenced for old worlds.
"""
from itertools import product
from generate_1_3_assets import ASSETS, read, write, model
from generate_1_3_2_assets import mesh

COLORS = ('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink',
          'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black')
GLAZINGS = ('clear',) + COLORS

BOOL = ('false', 'true')


def frame_cells(normal_axis: str, neg_connected: bool, pos_connected: bool,
                down_connected: bool, up_connected: bool):
    neg_w = 1 if neg_connected else 2
    pos_w = 1 if pos_connected else 2
    down_w = 1 if down_connected else 2
    up_w = 1 if up_connected else 2
    cells = set()

    if normal_axis == 'z':
        # Centered depth z=6..9, horizontal in-plane coordinate is X.
        for x, y, z in product(range(16), range(16), range(6, 10)):
            if y < down_w or y >= 16-up_w or x < neg_w or x >= 16-pos_w:
                cells.add((x, y, z))
    else:
        # Centered depth x=6..9, horizontal in-plane coordinate is Z.
        for x, y, z in product(range(6, 10), range(16), range(16)):
            if y < down_w or y >= 16-up_w or z < neg_w or z >= 16-pos_w:
                cells.add((x, y, z))
    return cells


def glass_model(glazing: str, axis: str):
    texture = 'minecraft:block/' + ('glass' if glazing == 'clear' else glazing + '_stained_glass')
    if axis == 'z':
        lo, hi, faces = [2, 2, 7.5], [14, 14, 8.5], ('north', 'south')
    else:
        lo, hi, faces = [7.5, 2, 2], [8.5, 14, 14], ('west', 'east')
    return {
        'parent': 'minecraft:block/block',
        'ambientocclusion': False,
        'textures': {'glass': {'sprite': texture, 'force_translucent': True}, 'particle': texture},
        'elements': [{
            'from': lo, 'to': hi, 'shade': False,
            'faces': {face: {'texture': '#glass', 'uv': [2, 2, 14, 14]} for face in faces}
        }]
    }


def generate():
    materials = sorted(p.name.removesuffix('_rounded_corner.json')
                       for p in (ASSETS/'blockstates').glob('*_rounded_corner.json'))
    assert len(materials) == 46

    # Shared glass models: 17 panes x two vertical planes.
    for glazing in GLAZINGS:
        for axis in ('x', 'z'):
            write(ASSETS/f'models/block/frame_glass_{glazing}_centered_{axis}.json', glass_model(glazing, axis))

    for mat in materials:
        textures = read(ASSETS/f'models/block/{mat}_pedestal.json')['textures']
        name = f'{mat}_window_frame'
        parts = []

        # 16 connection combinations for each of the two vertical planes.
        for neg, pos, down, up in product((False, True), repeat=4):
            flags = (1 if neg else 0) | (2 if pos else 0) | (4 if down else 0) | (8 if up else 0)
            code = f'{flags:02x}'
            for axis in ('x', 'z'):
                model_name = f'{name}_connected_{axis}_{code}'
                write(ASSETS/f'models/block/{model_name}.json',
                      model(textures, mesh(frame_cells(axis, neg, pos, down, up), textures)))

            conditions = {
                'negative_connected': str(neg).lower(),
                'positive_connected': str(pos).lower(),
                'down_connected': str(down).lower(),
                'up_connected': str(up).lower(),
            }
            for facing, axis in (('north','z'),('south','z'),('east','x'),('west','x')):
                when = {'facing': facing, **conditions}
                parts.append({'when': when, 'apply': {'model': f'faithfulblocks:block/{name}_connected_{axis}_{code}'}})

        # Centered glazing for all new horizontal states.
        for facing, axis in (('north','z'),('south','z'),('east','x'),('west','x')):
            for glazing in GLAZINGS:
                parts.append({
                    'when': {'facing': facing, 'glazing': glazing},
                    'apply': {'model': f'faithfulblocks:block/frame_glass_{glazing}_centered_{axis}'}
                })

        # Legacy horizontal placement is gone, but UP/DOWN states remain renderable
        # in old worlds. Their connection properties are intentionally ignored.
        for facing in ('up', 'down'):
            suffix = '' if facing == 'up' else '_down'
            parts.append({'when': {'facing': facing},
                          'apply': {'model': f'faithfulblocks:block/{name}{suffix}'}})
            for glazing in GLAZINGS:
                parts.append({'when': {'facing': facing, 'glazing': glazing},
                              'apply': {'model': f'faithfulblocks:block/frame_glass_{glazing}_{facing}'}})

        write(ASSETS/f'blockstates/{name}.json', {'multipart': parts})
        write(ASSETS/f'models/item/{name}.json',
              {'parent': f'faithfulblocks:block/{name}_connected_z_00'})

    print(f'Generated pane-style connected frames for {len(materials)} materials.')


if __name__ == '__main__':
    generate()
