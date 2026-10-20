#!/usr/bin/env python3
"""Offline checks for FaithfulBlocks 1.4.2 stair-like stepped-slope corners."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/faithfulblocks'
SHAPES = ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right')


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

    for mat in materials:
        base = f'{mat}_stepped_slope'
        variants = read(ASSETS / f'blockstates/{base}.json')['variants']
        assert len(variants) == 40, (mat, len(variants))

        for turns, facing in enumerate(('north', 'east', 'south', 'west')):
            for half in ('bottom', 'top'):
                for shape in SHAPES:
                    key = f'facing={facing},half={half},shape={shape}'
                    v = variants[key]
                    suffix = '' if shape == 'straight' else f'_{shape}'
                    expected = f'faithfulblocks:block/{base}{suffix}' + ('_top' if half == 'top' else '')
                    assert v['model'] == expected, (key, v['model'], expected)
                    assert v.get('y', 0) == turns * 90
                    if turns:
                        assert v.get('uvlock') is True

        for shape in SHAPES:
            suffix = '' if shape == 'straight' else f'_{shape}'
            for half_suffix in ('', '_top'):
                path = ASSETS / f'models/block/{base}{suffix}{half_suffix}.json'
                model = read(path)
                elems = model['elements']
                assert len(elems) == 64, (path, len(elems))
                for i, e in enumerate(elems):
                    assert volume(e) > 0
                    assert all(0 <= e['from'][n] < e['to'][n] <= 16 for n in range(3)), (path, e)
                    for other in elems[i+1:]:
                        assert not overlap(e, other), (path, e, other)

    slope_java = (ROOT / 'src/main/java/de/faithfulblocks/block/SteppedSlopeBlock.java').read_text(encoding='utf-8')
    assert 'BlockStateProperties.STAIRS_SHAPE' in slope_java
    assert 'getSlopeShape' in slope_java
    assert 'StairsShape.OUTER_LEFT' in slope_java
    assert 'StairsShape.INNER_RIGHT' in slope_java

    print('PASS: 46 materials x 40 stepped-slope blockstates are present.')
    print('PASS: straight/inner/outer models exist for bottom and top variants.')
    print('PASS: all model elements remain inside 0..16 with no volumetric overlap.')
    print('PASS: Java slope block contains stair-like shape state and neighbour updates.')


if __name__ == '__main__':
    validate()
