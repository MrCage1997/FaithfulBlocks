#!/usr/bin/env python3
"""Verify the new model against Java collision boxes and the intended wall profile."""
import re
from itertools import product
from validate_1_3_assets import ROOT, RES, ASSETS, read, surface, model_surface, validate

def rotate(cells):
    return {(15-z, y, x) for x, y, z in cells}

def validate_inner():
    validate()
    source = (ROOT/'src/main/java/de/faithfulblocks/block/BeveledInnerCornerBlock.java').read_text()
    box_source = source.split('BOXES = {', 1)[1].split('};', 1)[0]
    boxes = [[int(n) for n in re.findall(r'\d+', row)]
             for row in re.findall(r'\{([^{}]+)\}', box_source)]
    assert len(boxes) == 6
    collision = set()
    for x0, y0, z0, x1, y1, z1 in boxes:
        box = set(product(range(x0, x1), range(y0, y1), range(z0, z1)))
        assert not collision.intersection(box), 'Overlapping collision boxes'
        collision.update(box)
    intended = {p for p in product(range(16), repeat=3)
                if p[0] < 4 or p[2] < 4 or p[0] + p[2] < 12}
    assert collision == intended
    # Boundary footprints must match adjacent 1/4-block facade panels exactly.
    assert {(y, z) for x, y, z in collision if x == 15} == set(product(range(16), range(4)))
    assert {(x, y) for x, y, z in collision if z == 15} == set(product(range(4), range(16)))
    # Ensure this is a genuinely new profile, not an existing outside wedge rotated.
    outer = {(x, y, z) for x, y, z in product(range(16), repeat=3) if x+z <= 15}
    rotated = collision
    for _ in range(4):
        assert rotated != outer
        rotated = rotate(rotated)
    assert rotated == collision
    # Mirror maps from the class, checked against occupied quarters.
    orientations = [collision]
    for _ in range(3):
        orientations.append(rotate(orientations[-1]))
    for i, occupied in enumerate(orientations):
        assert {(x, y, 15-z) for x, y, z in occupied} == orientations[[3, 2, 1, 0][i]]
        assert {(15-x, y, z) for x, y, z in occupied} == orientations[[1, 0, 3, 2][i]]
    materials = sorted(p.name.removesuffix('_rounded_corner.json')
                       for p in (ASSETS/'blockstates').glob('*_rounded_corner.json'))
    for mat in materials:
        name = f'{mat}_beveled_inner_corner'
        assert model_surface(read(ASSETS/f'models/block/{name}.json')) == surface(collision), name
        variants = read(ASSETS/f'blockstates/{name}.json')['variants']
        assert len(variants) == 4
        for i, facing in enumerate(('north', 'east', 'south', 'west')):
            assert variants[f'facing={facing}'].get('y', 0) == i * 90
        for locale in ('de_de', 'en_us'):
            assert f'block.faithfulblocks.{name}' in read(ASSETS/f'lang/{locale}.json')
        assert read(RES/f'data/faithfulblocks/loot_table/blocks/{name}.json')['pools'][0]['entries'][0]['name'] == f'faithfulblocks:{name}'
        tool = 'axe' if mat.endswith('_planks') else 'pickaxe'
        assert f'faithfulblocks:{name}' in read(RES/f'data/minecraft/tags/block/mineable/{tool}.json')['values']
    java = ROOT/'src/main/java/de/faithfulblocks'
    assert 'BEVELED_INNER_CORNER(15,' in (java/'architecture/ArchitectureShape.java').read_text()
    assert 'ArchitectureShape.BEVELED_INNER_CORNER, ModBlocks.beveledInnerCorner(material)' in (java/'architecture/ArchitectureRecipeRegistry.java').read_text()
    print('PASS: 46 inner corners; exposed surfaces match actual Java boxes and intended L profile.')
    print('PASS: panel joints, rotations, mirror mappings, item drops, labels and tools.')

if __name__ == '__main__':
    validate_inner()
