#!/usr/bin/env python3
"""Offline resource/geometry checks; not a replacement for a Minecraft playtest."""
import json
import re
from collections import Counter
from itertools import product
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT/'src/main/resources'
ASSETS = RES/'assets/faithfulblocks'

def read(path):
    return json.loads(path.read_text())

DIRECTIONS = {'west': (0, -1), 'east': (0, 1), 'down': (1, -1),
              'up': (1, 1), 'north': (2, -1), 'south': (2, 1)}

def surface(cells):
    result = Counter()
    for cell in cells:
        for face, (axis, sign) in DIRECTIONS.items():
            neighbor = list(cell)
            neighbor[axis] += sign
            if tuple(neighbor) not in cells:
                plane = list(cell)
                plane[axis] += int(sign > 0)
                result[(face, *plane)] += 1
    return result

def model_surface(model):
    result = Counter()
    for element in model['elements']:
        lo, hi = element['from'], element['to']
        assert all(0 <= a < b <= 16 for a, b in zip(lo, hi))
        for face, definition in element['faces'].items():
            axis, sign = DIRECTIONS[face]
            ranges = [range(lo[i], hi[i]) for i in range(3)]
            coord = hi[axis] if sign > 0 else lo[axis]
            ranges[axis] = [coord]
            if 'cullface' in definition:
                assert definition['cullface'] == face
                assert coord == (16 if sign > 0 else 0)
            assert definition['texture'][1:] in model['textures']
            for point in product(*ranges):
                result[(face, *point)] += 1
    return result

def validate():
    paths = list(RES.rglob('*.json'))
    for path in paths:
        value = read(path)
        # Resolve every local model/parent reference, including all old resources.
        def visit(value):
            if isinstance(value, dict):
                for key, item in value.items():
                    if key in ('model', 'parent') and isinstance(item, str) and item.startswith('faithfulblocks:'):
                        assert (ASSETS/'models'/f'{item.split(":", 1)[1]}.json').is_file(), (path, item)
                    visit(item)
            elif isinstance(value, list):
                for item in value:
                    visit(item)
        visit(value)
    materials = sorted(path.name.removesuffix('_rounded_corner.json')
                       for path in (ASSETS/'blockstates').glob('*_rounded_corner.json'))
    assert len(materials) == 46
    cells = {(x, y, z) for x, y, z in product(range(16), repeat=3) if x + z <= 15}
    panels = {
        'up': lambda x, y, z: y < 4,
        'down': lambda x, y, z: y >= 12,
        'north': lambda x, y, z: z >= 12,
        'south': lambda x, y, z: z < 4,
        'east': lambda x, y, z: x < 4,
        'west': lambda x, y, z: x >= 12,
    }
    for mat in materials:
        corner = read(ASSETS/f'models/block/{mat}_beveled_corner.json')
        assert model_surface(corner) == surface(cells), mat
        variants = read(ASSETS/f'blockstates/{mat}_beveled_corner.json')['variants']
        for i, face in enumerate(('north', 'east', 'south', 'west')):
            assert variants[f'facing={face}'].get('y', 0) == i * 90
        for facing, predicate in panels.items():
            suffix = '' if facing == 'up' else '_' + facing
            panel = read(ASSETS/f'models/block/{mat}_facade_offset{suffix}.json')
            occupied = {p for p in product(range(16), repeat=3) if predicate(*p)}
            assert model_surface(panel) == surface(occupied), (mat, facing)
        for shape in ('beveled_corner', 'facade_offset'):
            name = f'{mat}_{shape}'
            for locale in ('de_de', 'en_us'):
                assert f'block.faithfulblocks.{name}' in read(ASSETS/f'lang/{locale}.json')
            loot = read(RES/f'data/faithfulblocks/loot_table/blocks/{name}.json')
            assert loot['pools'][0]['entries'][0]['name'] == f'faithfulblocks:{name}'
            expected_tool = 'axe' if mat.endswith('_planks') else 'pickaxe'
            assert f'faithfulblocks:{name}' in read(RES/f'data/minecraft/tags/block/mineable/{expected_tool}.json')['values']
    java = ROOT/'src/main/java/de/faithfulblocks'
    recipes = (java/'architecture/ArchitectureRecipeRegistry.java').read_text()
    assert 'ArchitectureShape.CORNICE,' not in recipes
    assert 'ArchitectureShape.WALL_CORNICE,' in recipes
    for shape in ('PEDESTAL', 'LARGE_COLUMN_PEDESTAL', 'BEVELED_CORNER', 'FACADE_OFFSET'):
        assert f'ArchitectureShape.{shape},' in recipes
    ids = re.findall(r'^    [A-Z_]+\((\d+),', (java/'architecture/ArchitectureShape.java').read_text(), re.M)
    assert len(ids) == len(set(ids)) >= 15
    assert ids[:15] == [str(i) for i in range(15)]
    print(f'PASS: {len(paths)} JSON files and all local model references.')
    print('PASS: 46 materials; exact exposed corner/panel surfaces, no duplicate or internal faces.')
    print('PASS: items, loot, tool tags, DE/EN labels, recipe selection and unique button IDs.')

if __name__ == '__main__':
    validate()
