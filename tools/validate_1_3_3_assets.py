#!/usr/bin/env python3
"""Check every material, direction and glazing state, including conditional drops."""
import re
from itertools import product
from validate_1_3_2_assets import validate_frame
from validate_1_3_assets import ROOT, RES, ASSETS, read

COLORS = ('white','orange','magenta','light_blue','yellow','lime','pink','gray',
          'light_gray','cyan','purple','blue','brown','green','red','black')
TYPES = ('none', 'clear') + COLORS

def validate_glazing():
    validate_frame()
    java = ROOT/'src/main/java/de/faithfulblocks'
    source = (java/'block/FrameGlazing.java').read_text()
    mappings = dict(re.findall(r'\w+\("([a-z_]+)", "([a-z_]+)"\)', source))
    assert set(mappings) == set(TYPES)
    assert 'Items.' not in source
    assert 'BuiltInRegistries.ITEM.getKey(stack.getItem())' in source
    assert mappings['none'] == 'air'
    for color in TYPES[1:]:
        assert mappings[color].lower() == ('glass_pane' if color == 'clear' else color + '_stained_glass_pane')
    frame_java = (java/'block/WindowFrameBlock.java').read_text()
    assert '.setValue(GLAZING, FrameGlazing.NONE)' in frame_java
    glass_source = frame_java.split('GLASS_BOXES =',1)[1].split(';',1)[0]
    coordinates = [float(x) for x in re.findall(r'\d+(?:\.\d+)?',glass_source)]
    assert coordinates == [2,1.5,2,14,2.5,14]
    transforms = {
        'up': lambda x,y,z: (x,y,z), 'down': lambda x,y,z: (x,16-y,16-z),
        'north': lambda x,y,z: (x,z,16-y), 'south': lambda x,y,z: (x,16-z,y),
        'east': lambda x,y,z: (y,16-x,z), 'west': lambda x,y,z: (16-y,x,z),
    }
    materials = sorted(p.name.removesuffix('_rounded_corner.json')
                       for p in (ASSETS/'blockstates').glob('*_rounded_corner.json'))
    count = 0
    for mat in materials:
        name = f'{mat}_window_frame'
        parts = read(ASSETS/f'blockstates/{name}.json')['multipart']
        loot = read(RES/f'data/faithfulblocks/loot_table/blocks/{name}.json')
        assert len(loot['pools']) == 2
        pane_pool = loot['pools'][1]
        assert pane_pool['rolls'] == 1
        assert pane_pool['condition'] == {'type':'minecraft:survives_explosion'}
        alternatives = pane_pool['entries'][0]['children']
        assert len(alternatives) == 17
        for facing, transform in transforms.items():
            points = [transform(*p) for p in product(*zip(coordinates[:3], coordinates[3:]))]
            expected_lo = [min(p[i] for p in points) for i in range(3)]
            expected_hi = [max(p[i] for p in points) for i in range(3)]
            for glazing in TYPES:
                state = {'facing':facing,'glazing':glazing}
                selected = [p['apply']['model'] for p in parts
                            if all(state[k] == v for k,v in p['when'].items())]
                assert len(selected) == (1 if glazing == 'none' else 2)
                assert sum('frame_glass_' in m for m in selected) == int(glazing != 'none')
                drops = [a['name'] for a in alternatives
                         if a.get('condition', {}).get('type') == 'minecraft:match_block'
                         and a['condition'].get('blocks') == 'faithfulblocks:' + name
                         and a['condition'].get('state', {}).get('glazing') == glazing]
                assert drops == ([] if glazing == 'none' else ['minecraft:' + mappings[glazing].lower()])
                if glazing != 'none':
                    model_name = next(m for m in selected if 'frame_glass_' in m)
                    model = read(ASSETS/'models'/f'{model_name.split(":",1)[1]}.json')
                    assert len(model['elements']) == 1
                    e = model['elements'][0]
                    assert e['from'] == expected_lo and e['to'] == expected_hi
                    expected_faces = {'up','down'} if facing in ('up','down') else {'north','south'} if facing in ('north','south') else {'east','west'}
                    assert set(e['faces']) == expected_faces
                    assert not any('cullface' in f for f in e['faces'].values())
                    texture = 'minecraft:block/' + ('glass' if glazing == 'clear' else glazing + '_stained_glass')
                    assert model['textures']['glass'] == {'sprite': texture, 'force_translucent': True}
                count += 1
    client = (ROOT/'src/client/java/de/faithfulblocks/client/FaithfulBlocksClient.java').read_text()
    assert 'ChunkSectionLayerMap' not in client
    assert 'BlockRenderLayerMap' not in client
    assert 'ChunkSectionLayer' not in client
    print(f'PASS: {count} frame states; exactly one frame, optional glass, matching pane drop and Java glass bounds.')
    print('PASS: old frame default remains empty; all 17 pane item mappings and glass texture references.')

if __name__ == '__main__':
    validate_glazing()
