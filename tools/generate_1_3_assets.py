#!/usr/bin/env python3
"""Generate the two 1.3 shapes using the existing material texture definitions."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/faithfulblocks'
FACES = ('down', 'up', 'north', 'south', 'west', 'east')

def read(path):
    return json.loads(path.read_text())

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')

def texture(textures, face):
    if 'all' in textures:
        return '#all'
    return '#' + {'up': 'top', 'down': 'bottom'}.get(face, 'side')

def element(lo, hi, faces, textures):
    result = {'from': lo, 'to': hi, 'faces': {}}
    axis = {'west': (0, 0), 'east': (0, 16), 'down': (1, 0),
            'up': (1, 16), 'north': (2, 0), 'south': (2, 16)}
    for face in faces:
        definition = {'texture': texture(textures, face)}
        index, boundary = axis[face]
        coord = lo[index] if face in ('west', 'down', 'north') else hi[index]
        if coord == boundary:
            definition['cullface'] = face
        result['faces'][face] = definition
    return result

def model(textures, elements):
    return {'parent': 'minecraft:block/block', 'ambientocclusion': True,
            'textures': textures, 'elements': elements}

def generate():
    materials = sorted(path.name.removesuffix('_rounded_corner.json')
                       for path in (ASSETS/'blockstates').glob('*_rounded_corner.json'))
    assert len(materials) == 46
    for mat in materials:
        textures = read(ASSETS/f'models/block/{mat}_pedestal.json')['textures']
        # A triangular prism in 1/16-block increments. Emit only exposed faces:
        # no internal coplanar quads, no overlapping cuboids, no stretched UVs.
        elements = []
        for z in range(16):
            width = 16 - z
            faces = ['down', 'up', 'west', 'east']
            if z == 0:
                faces.append('north')
            elements.append(element([0, 0, z], [width, 16, z+1], faces, textures))
            elements.append(element([width-1, 0, z], [width, 16, z+1], ['south'], textures))
        name = f'{mat}_beveled_corner'
        write(ASSETS/f'models/block/{name}.json', model(textures, elements))
        variants = {}
        for turns, facing in enumerate(('north', 'east', 'south', 'west')):
            variant = {'model': f'faithfulblocks:block/{name}'}
            if turns:
                variant.update(y=turns*90, uvlock=True)
            variants[f'facing={facing}'] = variant
        write(ASSETS/f'blockstates/{name}.json', {'variants': variants})

        # Same transforms as DirectionalShapeUtils, with world-aligned textures.
        panels = {
            'up': ([0, 0, 0], [16, 4, 16]),
            'down': ([0, 12, 0], [16, 16, 16]),
            'north': ([0, 0, 12], [16, 16, 16]),
            'south': ([0, 0, 0], [16, 16, 4]),
            'east': ([0, 0, 0], [4, 16, 16]),
            'west': ([12, 0, 0], [16, 16, 16]),
        }
        name = f'{mat}_facade_offset'
        variants = {}
        for facing, (lo, hi) in panels.items():
            suffix = '' if facing == 'up' else '_' + facing
            model_name = name + suffix
            write(ASSETS/f'models/block/{model_name}.json',
                  model(textures, [element(lo, hi, FACES, textures)]))
            variants[f'facing={facing}'] = {'model': f'faithfulblocks:block/{model_name}'}
        write(ASSETS/f'blockstates/{name}.json', {'variants': variants})

        for shape in ('beveled_corner', 'facade_offset'):
            name = f'{mat}_{shape}'
            write(ASSETS/f'models/item/{name}.json', {'parent': f'faithfulblocks:block/{name}'})
            write(ASSETS/f'items/{name}.json',
                  {'model': {'type': 'minecraft:model', 'model': f'faithfulblocks:item/{name}'}})
            loot = read(RES/f'data/faithfulblocks/loot_table/blocks/{mat}_pedestal.json')
            loot['pools'][0]['entries'][0]['name'] = f'faithfulblocks:{name}'
            write(RES/f'data/faithfulblocks/loot_table/blocks/{name}.json', loot)

    for tool in ('axe', 'pickaxe'):
        path = RES/f'data/minecraft/tags/block/mineable/{tool}.json'
        tag = read(path)
        for mat in materials:
            if f'faithfulblocks:{mat}_pedestal' in tag['values']:
                for shape in ('beveled_corner', 'facade_offset'):
                    entry = f'faithfulblocks:{mat}_{shape}'
                    if entry not in tag['values']:
                        tag['values'].append(entry)
        write(path, tag)

    for locale in ('de_de', 'en_us'):
        path = ASSETS/f'lang/{locale}.json'
        lang = read(path)
        for mat in materials:
            existing = lang[f'block.faithfulblocks.{mat}_rounded_corner']
            if locale == 'de_de':
                material_name = existing.removeprefix('Abgerundete ').removesuffix('-Ecke')
                corner = 'Abgeschrägte ' + material_name + '-Ecke'
                facade = material_name + '-Fassadenplatte'
            else:
                material_name = existing.removeprefix('Rounded ').removesuffix(' Corner')
                corner = 'Beveled ' + material_name + ' Corner'
                facade = material_name + ' Facade Panel'
            lang[f'block.faithfulblocks.{mat}_beveled_corner'] = corner
            lang[f'block.faithfulblocks.{mat}_facade_offset'] = facade
        lang['menu.faithfulblocks.shape.beveled_corner'] = 'Abgeschrägte Ecke' if locale == 'de_de' else 'Beveled Corner'
        lang['menu.faithfulblocks.shape.facade_offset'] = 'Fassaden-/Versatzblock' if locale == 'de_de' else 'Facade / Offset Panel'
        write(path, lang)
    print('Generated both shapes for all 46 materials.')

if __name__ == '__main__':
    generate()
