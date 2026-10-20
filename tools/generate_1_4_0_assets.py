#!/usr/bin/env python3
"""Generate the FaithfulBlocks 1.4.0 stepped-slope assets for all materials."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/faithfulblocks'


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


def strip(lo, hi, faces, textures):
    return {
        'from': lo,
        'to': hi,
        'faces': {face: face_def(face, textures, lo, hi) for face in faces},
    }


def slope_elements(textures, top=False):
    elements = []
    for step in range(8):
        z0 = step * 2
        z1 = z0 + 2
        height = (step + 1) * 2
        if top:
            y0, y1 = 16 - height, 16
        else:
            y0, y1 = 0, height

        # North face of each strip provides the visible 2/16 riser. South faces
        # are omitted except at the back edge, so neighbouring strips never
        # render coplanar faces against each other.
        faces = ['down', 'up', 'north', 'west', 'east']
        if step == 7:
            faces.append('south')
        elements.append(strip([0, y0, z0], [16, y1, z1], faces, textures))
    return elements


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
        name = f'{mat}_stepped_slope'
        write(ASSETS / f'models/block/{name}.json', model(textures, slope_elements(textures, top=False)))
        write(ASSETS / f'models/block/{name}_top.json', model(textures, slope_elements(textures, top=True)))

        variants = {}
        for turns, facing in enumerate(('north', 'east', 'south', 'west')):
            for half in ('bottom', 'top'):
                variant = {'model': f'faithfulblocks:block/{name}' + ('_top' if half == 'top' else '')}
                if turns:
                    variant['y'] = turns * 90
                    variant['uvlock'] = True
                variants[f'facing={facing},half={half}'] = variant
        write(ASSETS / f'blockstates/{name}.json', {'variants': variants})
        write(ASSETS / f'models/item/{name}.json', {'parent': f'faithfulblocks:block/{name}'})
        write(ASSETS / f'items/{name}.json', {
            'model': {'type': 'minecraft:model', 'model': f'faithfulblocks:item/{name}'}
        })

        loot = read(RES / f'data/faithfulblocks/loot_table/blocks/{mat}_pedestal.json')
        loot['pools'][0]['entries'][0]['name'] = f'faithfulblocks:{name}'
        write(RES / f'data/faithfulblocks/loot_table/blocks/{name}.json', loot)

    # Preserve material-specific tool choice by following the pedestal tags.
    for tool in ('axe', 'pickaxe'):
        path = RES / f'data/minecraft/tags/block/mineable/{tool}.json'
        tag = read(path)
        for mat in materials:
            if f'faithfulblocks:{mat}_pedestal' in tag['values']:
                entry = f'faithfulblocks:{mat}_stepped_slope'
                if entry not in tag['values']:
                    tag['values'].append(entry)
        write(path, tag)

    # Derive the material display name from the existing pedestal names.
    for locale in ('de_de', 'en_us'):
        path = ASSETS / f'lang/{locale}.json'
        lang = read(path)
        for mat in materials:
            existing = lang[f'block.faithfulblocks.{mat}_pedestal']
            if locale == 'de_de':
                material_name = existing.removesuffix('-Sockel')
                display = material_name + '-Schrägblock'
            else:
                material_name = existing.removesuffix(' Pedestal')
                display = material_name + ' Stepped Slope'
            lang[f'block.faithfulblocks.{mat}_stepped_slope'] = display
        lang['menu.faithfulblocks.shape.stepped_slope'] = (
            'Schräg-/Stufenblock' if locale == 'de_de' else 'Stepped Slope'
        )
        write(path, lang)

    print(f'Generated stepped slopes for {len(materials)} materials.')


if __name__ == '__main__':
    generate()
