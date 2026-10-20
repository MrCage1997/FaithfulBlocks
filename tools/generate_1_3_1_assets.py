#!/usr/bin/env python3
"""Generate the L-shaped beveled inner corner for every existing material."""
from generate_1_3_assets import ASSETS, RES, read, write, model, element

STRIPS = [(0, 4, 16), (4, 5, 8), (5, 6, 7), (6, 7, 6), (7, 8, 5), (8, 16, 4)]

def generate():
    materials = sorted(p.name.removesuffix('_rounded_corner.json')
                       for p in (ASSETS/'blockstates').glob('*_rounded_corner.json'))
    assert len(materials) == 46
    for mat in materials:
        textures = read(ASSETS/f'models/block/{mat}_pedestal.json')['textures']
        elements = []
        for i, (z0, z1, width) in enumerate(STRIPS):
            faces = ['down', 'up', 'west', 'east']
            if i == 0:
                faces.append('north')
            elements.append(element([0, 0, z0], [width, 16, z1], faces, textures))
            next_width = STRIPS[i+1][2] if i+1 < len(STRIPS) else 0
            elements.append(element([next_width, 0, z0], [width, 16, z1], ['south'], textures))
        name = f'{mat}_beveled_inner_corner'
        write(ASSETS/f'models/block/{name}.json', model(textures, elements))
        variants = {}
        for turns, facing in enumerate(('north', 'east', 'south', 'west')):
            variant = {'model': f'faithfulblocks:block/{name}'}
            if turns:
                variant.update(y=turns*90, uvlock=True)
            variants[f'facing={facing}'] = variant
        write(ASSETS/f'blockstates/{name}.json', {'variants': variants})
        write(ASSETS/f'models/item/{name}.json', {'parent': f'faithfulblocks:block/{name}'})
        write(ASSETS/f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'faithfulblocks:item/{name}'}})
        loot = read(RES/f'data/faithfulblocks/loot_table/blocks/{mat}_pedestal.json')
        loot['pools'][0]['entries'][0]['name'] = f'faithfulblocks:{name}'
        write(RES/f'data/faithfulblocks/loot_table/blocks/{name}.json', loot)

    for tool in ('axe', 'pickaxe'):
        path = RES/f'data/minecraft/tags/block/mineable/{tool}.json'
        tag = read(path)
        for mat in materials:
            name = f'faithfulblocks:{mat}_beveled_inner_corner'
            if f'faithfulblocks:{mat}_pedestal' in tag['values'] and name not in tag['values']:
                tag['values'].append(name)
        write(path, tag)

    for locale in ('de_de', 'en_us'):
        path = ASSETS/f'lang/{locale}.json'
        lang = read(path)
        for mat in materials:
            outer = lang[f'block.faithfulblocks.{mat}_beveled_corner']
            label = outer.removesuffix('-Ecke') + '-Innenecke' if locale == 'de_de' else outer.removesuffix(' Corner') + ' Inner Corner'
            lang[f'block.faithfulblocks.{mat}_beveled_inner_corner'] = label
        lang['menu.faithfulblocks.shape.beveled_inner_corner'] = 'Abgeschrägte Innenecke' if locale == 'de_de' else 'Beveled Inner Corner'
        write(path, lang)
    print('Generated 46 beveled inner corners.')

if __name__ == '__main__':
    generate()
