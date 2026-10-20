#!/usr/bin/env python3
"""Offline checks for the 1.4.0 stepped-slope feature."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/faithfulblocks'


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def volume(e):
    lo, hi = e['from'], e['to']
    return (hi[0]-lo[0]) * (hi[1]-lo[1]) * (hi[2]-lo[2])


def overlap(a, b):
    alo, ahi, blo, bhi = a['from'], a['to'], b['from'], b['to']
    return all(max(alo[i], blo[i]) < min(ahi[i], bhi[i]) for i in range(3))


def validate():
    materials = sorted(p.name.removesuffix('_rounded_corner.json')
                       for p in (ASSETS / 'blockstates').glob('*_rounded_corner.json'))
    assert len(materials) == 46

    axe = set(read(RES / 'data/minecraft/tags/block/mineable/axe.json')['values'])
    pickaxe = set(read(RES / 'data/minecraft/tags/block/mineable/pickaxe.json')['values'])

    for mat in materials:
        name = f'{mat}_stepped_slope'
        state = read(ASSETS / f'blockstates/{name}.json')['variants']
        assert len(state) == 8
        for turns, facing in enumerate(('north', 'east', 'south', 'west')):
            for half in ('bottom', 'top'):
                v = state[f'facing={facing},half={half}']
                expected = f'faithfulblocks:block/{name}' + ('_top' if half == 'top' else '')
                assert v['model'] == expected
                assert v.get('y', 0) == turns * 90
                if turns:
                    assert v.get('uvlock') is True

        for suffix, top in (('', False), ('_top', True)):
            model = read(ASSETS / f'models/block/{name}{suffix}.json')
            elements = model['elements']
            assert len(elements) == 8
            assert all(volume(e) > 0 for e in elements)
            for i, a in enumerate(elements):
                assert all(0 <= a['from'][n] < a['to'][n] <= 16 for n in range(3))
                for b in elements[i+1:]:
                    assert not overlap(a, b), (mat, suffix, a, b)
            heights = []
            for step, e in enumerate(elements):
                assert e['from'][2] == step * 2 and e['to'][2] == step * 2 + 2
                h = e['to'][1] - e['from'][1]
                heights.append(h)
                if top:
                    assert e['to'][1] == 16
                else:
                    assert e['from'][1] == 0
            assert heights == [2,4,6,8,10,12,14,16]

        item = read(ASSETS / f'items/{name}.json')
        assert item['model']['model'] == f'faithfulblocks:item/{name}'
        loot = read(RES / f'data/faithfulblocks/loot_table/blocks/{name}.json')
        assert loot['pools'][0]['entries'][0]['name'] == f'faithfulblocks:{name}'
        expected_tag = axe if mat.endswith('_planks') else pickaxe
        assert f'faithfulblocks:{name}' in expected_tag
        for locale in ('de_de', 'en_us'):
            assert f'block.faithfulblocks.{name}' in read(ASSETS / f'lang/{locale}.json')

    java = ROOT / 'src/main/java/de/faithfulblocks'
    shape_java = (java / 'architecture/ArchitectureShape.java').read_text(encoding='utf-8')
    recipes = (java / 'architecture/ArchitectureRecipeRegistry.java').read_text(encoding='utf-8')
    blocks = (java / 'block/ModBlocks.java').read_text(encoding='utf-8')
    slope = (java / 'block/SteppedSlopeBlock.java').read_text(encoding='utf-8')
    assert 'STEPPED_SLOPE(17,' in shape_java
    ids = re.findall(r'^    [A-Z_]+\((\d+),', shape_java, re.M)
    assert len(ids) == len(set(ids))
    assert 'ArchitectureShape.STEPPED_SLOPE' in recipes
    assert 'SteppedSlopeBlock::new' in blocks
    assert 'entries.accept(steppedSlope(material).asItem())' in blocks
    assert 'BlockStateProperties.HALF' in slope and 'BlockStateProperties.HORIZONTAL_FACING' in slope

    print('PASS: 46 stepped slopes, 8 placement states each, item/loot/lang/tool resources present.')
    print('PASS: bottom/top geometry stays inside 0..16 and contains no volumetric overlap.')
    print('PASS: shape id 17, recipe registration and creative registration are present.')


if __name__ == '__main__':
    validate()
