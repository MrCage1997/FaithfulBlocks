#!/usr/bin/env python3
"""Add saved glazing variants and pane drops to the existing frame IDs."""
from generate_1_3_assets import ASSETS, RES, read, write

COLORS = ('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink',
          'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black')
GLAZINGS = ('clear',) + COLORS
PANES = {color: ('glass_pane' if color == 'clear' else color + '_stained_glass_pane') for color in GLAZINGS}
GLASS = {
    'up': ([2, 1.5, 2], [14, 2.5, 14], ('down', 'up')),
    'down': ([2, 13.5, 2], [14, 14.5, 14], ('down', 'up')),
    'north': ([2, 2, 13.5], [14, 14, 14.5], ('north', 'south')),
    'south': ([2, 2, 1.5], [14, 14, 2.5], ('north', 'south')),
    'east': ([1.5, 2, 2], [2.5, 14, 14], ('west', 'east')),
    'west': ([13.5, 2, 2], [14.5, 14, 14], ('west', 'east')),
}

def generate():
    for glazing in GLAZINGS:
        texture = 'minecraft:block/' + ('glass' if glazing == 'clear' else glazing + '_stained_glass')
        for facing, (lo, hi, faces) in GLASS.items():
            write(ASSETS/f'models/block/frame_glass_{glazing}_{facing}.json', {
                'parent': 'minecraft:block/block', 'ambientocclusion': False,
                'textures': {'glass': {'sprite': texture, 'force_translucent': True}, 'particle': texture},
                'elements': [{'from': lo, 'to': hi, 'shade': False,
                    'faces': {face: {'texture': '#glass', 'uv': [2, 2, 14, 14]} for face in faces}}]
            })
    materials = sorted(p.name.removesuffix('_rounded_corner.json')
                       for p in (ASSETS/'blockstates').glob('*_rounded_corner.json'))
    for mat in materials:
        name = f'{mat}_window_frame'
        parts = []
        for facing in GLASS:
            suffix = '' if facing == 'up' else '_' + facing
            parts.append({'when': {'facing': facing}, 'apply': {'model': f'faithfulblocks:block/{name}{suffix}'}})
            for glazing in GLAZINGS:
                parts.append({'when': {'facing': facing, 'glazing': glazing},
                              'apply': {'model': f'faithfulblocks:block/frame_glass_{glazing}_{facing}'}})
        write(ASSETS/f'blockstates/{name}.json', {'multipart': parts})
        loot = read(RES/f'data/faithfulblocks/loot_table/blocks/{name}.json')
        loot['pools'] = loot['pools'][:1]
        loot['pools'].append({
            'rolls': 1, 'bonus_rolls': 0,
            'condition': {'type': 'minecraft:survives_explosion'},
            'entries': [{'type': 'minecraft:alternatives', 'children': [
                {'type': 'minecraft:item', 'name': 'minecraft:' + PANES[glazing],
                 'condition': {'type': 'minecraft:match_block',
                               'blocks': 'faithfulblocks:' + name, 'state': {'glazing': glazing}}}
                for glazing in GLAZINGS
            ]}]
        })
        write(RES/f'data/faithfulblocks/loot_table/blocks/{name}.json', loot)
    print('Generated 17 pane variants x 6 directions; glazing multipart and matching drops for 46 frames.')

if __name__ == '__main__':
    generate()
