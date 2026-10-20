#!/usr/bin/env python3
"""Check six-direction frame geometry, including the empty opening."""
import re
from itertools import product
from validate_1_3_assets import ROOT, RES, ASSETS, read, surface, model_surface
from validate_1_3_1_assets import validate_inner

def validate_frame():
    validate_inner()
    java = ROOT/'src/main/java/de/faithfulblocks'
    source = (java/'block/WindowFrameBlock.java').read_text()
    box_source = source.split('BOXES = {', 1)[1].split('};', 1)[0]
    boxes = [[int(n) for n in re.findall(r'\d+', row)]
             for row in re.findall(r'\{([^{}]+)\}', box_source)]
    assert len(boxes) == 4
    base = set()
    for x0, y0, z0, x1, y1, z1 in boxes:
        box = set(product(range(x0, x1), range(y0, y1), range(z0, z1)))
        assert not base.intersection(box)
        base.update(box)
    assert len(base) == (16*16 - 12*12)*4
    assert not base.intersection(product(range(2, 14), range(16), range(2, 14)))
    # Independent world-coordinate predicates for every DirectionalShapeUtils orientation.
    conditions = {
        'up': lambda x,y,z: y < 4 and (x < 2 or x >= 14 or z < 2 or z >= 14),
        'down': lambda x,y,z: y >= 12 and (x < 2 or x >= 14 or z < 2 or z >= 14),
        'north': lambda x,y,z: z >= 12 and (x < 2 or x >= 14 or y < 2 or y >= 14),
        'south': lambda x,y,z: z < 4 and (x < 2 or x >= 14 or y < 2 or y >= 14),
        'east': lambda x,y,z: x < 4 and (y < 2 or y >= 14 or z < 2 or z >= 14),
        'west': lambda x,y,z: x >= 12 and (y < 2 or y >= 14 or z < 2 or z >= 14),
    }
    materials = sorted(p.name.removesuffix('_rounded_corner.json')
                       for p in (ASSETS/'blockstates').glob('*_rounded_corner.json'))
    for mat in materials:
        name = f'{mat}_window_frame'
        definition = read(ASSETS/f'blockstates/{name}.json')
        variants = definition.get('variants')
        if variants is None:
            variants = {f"facing={part['when']['facing']}": part['apply']
                        for part in definition['multipart'] if set(part['when']) == {'facing'}}
        assert set(variants) == {f'facing={f}' for f in conditions}
        for facing, condition in conditions.items():
            suffix = '' if facing == 'up' else '_' + facing
            occupied = {p for p in product(range(16), repeat=3) if condition(*p)}
            assert len(occupied) == 448
            assert model_surface(read(ASSETS/f'models/block/{name}{suffix}.json')) == surface(occupied), (mat, facing)
            assert variants[f'facing={facing}']['model'] == f'faithfulblocks:block/{name}{suffix}'
        for locale in ('de_de', 'en_us'):
            assert f'block.faithfulblocks.{name}' in read(ASSETS/f'lang/{locale}.json')
        assert read(RES/f'data/faithfulblocks/loot_table/blocks/{name}.json')['pools'][0]['entries'][0]['name'] == f'faithfulblocks:{name}'
        tool = 'axe' if mat.endswith('_planks') else 'pickaxe'
        assert f'faithfulblocks:{name}' in read(RES/f'data/minecraft/tags/block/mineable/{tool}.json')['values']
    assert 'WINDOW_FRAME(16,' in (java/'architecture/ArchitectureShape.java').read_text()
    assert 'ArchitectureShape.WINDOW_FRAME, ModBlocks.windowFrame(material)' in (java/'architecture/ArchitectureRecipeRegistry.java').read_text()
    print('PASS: 46 frames, six orientations, open 12x12 center and 4/16 depth.')
    print('PASS: all exposed frame surfaces, no internal/duplicate faces, loot, labels and tools.')

if __name__ == '__main__':
    validate_frame()
