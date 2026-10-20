#!/usr/bin/env python3
"""Generate window frame surfaces without overlapping or internal faces."""
from itertools import product
from generate_1_3_assets import ASSETS, RES, read, write, model, element

DIRECTIONS = {'west': (0, -1), 'east': (0, 1), 'down': (1, -1),
              'up': (1, 1), 'north': (2, -1), 'south': (2, 1)}

def oriented(cells, facing):
    transforms = {
        'up': lambda x, y, z: (x, y, z),
        'down': lambda x, y, z: (x, 15-y, 15-z),
        'north': lambda x, y, z: (x, z, 15-y),
        'south': lambda x, y, z: (x, 15-z, y),
        'east': lambda x, y, z: (y, 15-x, z),
        'west': lambda x, y, z: (15-y, x, z),
    }
    return {transforms[facing](*p) for p in cells}

def mesh(cells, textures):
    # Collect exposed unit squares by face plane, then merge into rectangles.
    planes = {}
    for cell in cells:
        for face, (axis, sign) in DIRECTIONS.items():
            neighbor = list(cell)
            neighbor[axis] += sign
            if tuple(neighbor) in cells:
                continue
            others = [i for i in range(3) if i != axis]
            plane = cell[axis] + (1 if sign > 0 else 0)
            planes.setdefault((face, plane), set()).add(tuple(cell[i] for i in others))
    result = []
    for (face, plane), squares in sorted(planes.items()):
        axis, sign = DIRECTIONS[face]
        a, b = [i for i in range(3) if i != axis]
        while squares:
            u, v = min(squares)
            end_u = u + 1
            while (end_u, v) in squares:
                end_u += 1
            end_v = v + 1
            while all((i, end_v) in squares for i in range(u, end_u)):
                end_v += 1
            squares.difference_update(product(range(u, end_u), range(v, end_v)))
            lo, hi = [0, 0, 0], [0, 0, 0]
            lo[a], hi[a], lo[b], hi[b] = u, end_u, v, end_v
            # Non-degenerate box, emitting just its one exposed face.
            lo[axis] = plane - 1 if sign > 0 else plane
            hi[axis] = lo[axis] + 1
            result.append(element(lo, hi, [face], textures))
    return result

def generate():
    materials = sorted(p.name.removesuffix('_rounded_corner.json')
                       for p in (ASSETS/'blockstates').glob('*_rounded_corner.json'))
    assert len(materials) == 46
    cells = {(x, y, z) for x, y, z in product(range(16), range(4), range(16))
             if x < 2 or x >= 14 or z < 2 or z >= 14}
    for mat in materials:
        textures = read(ASSETS/f'models/block/{mat}_pedestal.json')['textures']
        name = f'{mat}_window_frame'
        variants = {}
        for facing in ('up', 'down', 'north', 'south', 'east', 'west'):
            suffix = '' if facing == 'up' else '_' + facing
            model_name = name + suffix
            write(ASSETS/f'models/block/{model_name}.json', model(textures, mesh(oriented(cells, facing), textures)))
            variants[f'facing={facing}'] = {'model': f'faithfulblocks:block/{model_name}'}
        write(ASSETS/f'blockstates/{name}.json', {'variants': variants})
        # Display the frame upright in the inventory so the opening is easy to see.
        write(ASSETS/f'models/item/{name}.json', {'parent': f'faithfulblocks:block/{name}_north'})
        write(ASSETS/f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'faithfulblocks:item/{name}'}})
        loot = read(RES/f'data/faithfulblocks/loot_table/blocks/{mat}_pedestal.json')
        loot['pools'][0]['entries'][0]['name'] = f'faithfulblocks:{name}'
        write(RES/f'data/faithfulblocks/loot_table/blocks/{name}.json', loot)
    for tool in ('axe', 'pickaxe'):
        path = RES/f'data/minecraft/tags/block/mineable/{tool}.json'
        tag = read(path)
        for mat in materials:
            name = f'faithfulblocks:{mat}_window_frame'
            if f'faithfulblocks:{mat}_pedestal' in tag['values'] and name not in tag['values']:
                tag['values'].append(name)
        write(path, tag)
    for locale in ('de_de', 'en_us'):
        path = ASSETS/f'lang/{locale}.json'
        lang = read(path)
        for mat in materials:
            outer = lang[f'block.faithfulblocks.{mat}_rounded_corner']
            material_name = outer.removeprefix('Abgerundete ').removesuffix('-Ecke') if locale == 'de_de' else outer.removeprefix('Rounded ').removesuffix(' Corner')
            lang[f'block.faithfulblocks.{mat}_window_frame'] = material_name + ('-Fensterrahmen' if locale == 'de_de' else ' Window Frame')
        lang['menu.faithfulblocks.shape.window_frame'] = 'Fensterrahmen' if locale == 'de_de' else 'Window Frame'
        write(path, lang)
    print('Generated 46 window frames, each with six orientations.')

if __name__ == '__main__':
    generate()
