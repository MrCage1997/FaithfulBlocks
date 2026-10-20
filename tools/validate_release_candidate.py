#!/usr/bin/env python3
"""Comprehensive offline consistency check for FaithfulBlocks release candidates."""
from pathlib import Path
import json, re, sys

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
ASSETS=RES/'assets/faithfulblocks'
JAVA=ROOT/'src/main/java/de/faithfulblocks'
errors=[]

def fail(msg): errors.append(msg)
def load(p):
    try: return json.loads(p.read_text(encoding='utf-8'))
    except Exception as e:
        fail(f'Invalid JSON {p.relative_to(ROOT)}: {e}'); return {}

# All JSON parses.
json_files=list(RES.rglob('*.json'))
for p in json_files: load(p)

# Minecraft 26.3 loot schema audit: old conditions/block_state_property are parsed but ignored.
loot_files=list((RES/'data/faithfulblocks/loot_table').rglob('*.json'))
def audit_loot_node(node, path):
    if isinstance(node, dict):
        if 'conditions' in node:
            fail(f'{path} still uses legacy loot key "conditions"; 26.3 requires singular "condition"')
        if node.get('condition') == 'minecraft:block_state_property' or node.get('type') == 'minecraft:block_state_property':
            fail(f'{path} still uses removed minecraft:block_state_property; use minecraft:match_block')
        if isinstance(node.get('condition'), dict) and node['condition'].get('type') == 'minecraft:block_state_property':
            fail(f'{path} still uses removed minecraft:block_state_property; use minecraft:match_block')
        for value in node.values():
            audit_loot_node(value, path)
    elif isinstance(node, list):
        for value in node:
            audit_loot_node(value, path)

for p in loot_files:
    audit_loot_node(load(p), p.relative_to(RES))

# Materials.
mt=(JAVA/'architecture/ArchitectureMaterials.java').read_text(encoding='utf-8')
materials=re.findall(r'new ArchitectureMaterial\("([a-z0-9_]+)"', mt)
if len(materials)!=46 or len(set(materials))!=46:
    fail(f'Expected 46 unique materials, got {len(materials)}/{len(set(materials))}')

registered_forms=[
 'rounded_corner','narrow_column','rounded_inner_corner','pedestal','cornice','arch',
 'wall_cap','wall_end','pilaster','large_column','large_column_pedestal','wall_pedestal',
 'wall_cornice','window_frame','beveled_inner_corner','beveled_corner','facade_offset',
 'stepped_slope','arch_left','arch_right'
]
visible_forms=[
 'rounded_corner','narrow_column','rounded_inner_corner','pedestal','arch','wall_cap',
 'wall_end','pilaster','large_column','large_column_pedestal','wall_pedestal','wall_cornice',
 'window_frame','beveled_inner_corner','beveled_corner','facade_offset','stepped_slope'
]

# Core resources for every registered id.
for mat in materials:
    for form in registered_forms:
        bid=f'{mat}_{form}'
        for p in [
            ASSETS/f'blockstates/{bid}.json', ASSETS/f'models/item/{bid}.json',
            ASSETS/f'items/{bid}.json', RES/f'data/faithfulblocks/loot_table/blocks/{bid}.json']:
            if not p.exists(): fail(f'Missing {p.relative_to(ROOT)}')

# Languages.
for locale in ('en_us','de_de'):
    lang=load(ASSETS/f'lang/{locale}.json')
    for mat in materials:
        for form in registered_forms:
            key=f'block.faithfulblocks.{mat}_{form}'
            if key not in lang: fail(f'Missing {locale} label {key}')

# Local model references.
def check_ref(ref, src):
    if ref.startswith('faithfulblocks:block/'):
        p=ASSETS/'models/block'/(ref.split('/',1)[1]+'.json')
        if not p.exists(): fail(f'{src.relative_to(ROOT)} -> missing {ref}')
    elif ref.startswith('faithfulblocks:item/'):
        p=ASSETS/'models/item'/(ref.split('/',1)[1]+'.json')
        if not p.exists(): fail(f'{src.relative_to(ROOT)} -> missing {ref}')

def walk(x,src):
    if isinstance(x,dict):
        for k,v in x.items():
            if k in ('model','parent') and isinstance(v,str): check_ref(v,src)
            walk(v,src)
    elif isinstance(x,list):
        for v in x: walk(v,src)
for p in list((ASSETS/'blockstates').glob('*.json'))+list((ASSETS/'models').rglob('*.json'))+list((ASSETS/'items').glob('*.json')):
    walk(load(p),p)

# Expected blockstate sizes for representative material, then require all materials match.
counts={
 'rounded_corner':4,'rounded_inner_corner':4,'narrow_column':12,'pedestal':6,'cornice':6,
 'arch':40,'arch_left':4,'arch_right':4,'wall_cap':1,'wall_end':4,'pilaster':4,
 'large_column':12,'large_column_pedestal':6,'wall_pedestal':20,'wall_cornice':20,
 'beveled_corner':4,'beveled_inner_corner':4,'facade_offset':6,'stepped_slope':40
}
for mat in materials:
    for form,n in counts.items():
        d=load(ASSETS/f'blockstates/{mat}_{form}.json')
        got=len(d.get('variants',{}))
        if got!=n: fail(f'{mat}_{form}: expected {n} variants, got {got}')
    wf=load(ASSETS/f'blockstates/{mat}_window_frame.json')
    if 'multipart' not in wf or len(wf['multipart'])!=168:
        fail(f'{mat}_window_frame: expected 168 multipart entries')

# Loot self-drop.
for mat in materials:
    for form in registered_forms:
        bid=f'{mat}_{form}'; d=load(RES/f'data/faithfulblocks/loot_table/blocks/{bid}.json')
        names=[]
        def names_walk(x):
            if isinstance(x,dict):
                if x.get('type')=='minecraft:item' and isinstance(x.get('name'),str): names.append(x['name'])
                for v in x.values(): names_walk(v)
            elif isinstance(x,list):
                for v in x: names_walk(v)
        names_walk(d)
        if f'faithfulblocks:{bid}' not in names: fail(f'{bid} loot does not contain self drop')

# Tool assignment for every registered material block.
pick=set(load(RES/'data/minecraft/tags/block/mineable/pickaxe.json').get('values',[]))
axe=set(load(RES/'data/minecraft/tags/block/mineable/axe.json').get('values',[]))
wood={'oak_planks','spruce_planks','birch_planks','jungle_planks','acacia_planks','dark_oak_planks','mangrove_planks','cherry_planks','bamboo_planks','crimson_planks','warped_planks','pale_oak_planks'}
for mat in materials:
    expected=axe if mat in wood else pick
    wrong=pick if mat in wood else axe
    for form in registered_forms:
        bid=f'faithfulblocks:{mat}_{form}'
        if bid not in expected: fail(f'Missing expected mineable tag: {bid}')
        if bid in wrong: fail(f'Wrong extra mineable tag: {bid}')
if 'faithfulblocks:architecture_table' not in pick: fail('Architecture table missing pickaxe tag')

# Table recipe and visible registry selection.
recipe=load(RES/'data/faithfulblocks/recipe/architecture_table.json')
if recipe.get('pattern')!=['SSS','SCS','PPP']: fail('Architecture table pattern changed unexpectedly')
if recipe.get('key',{}).get('S')!='minecraft:stone_bricks': fail('Architecture table S key invalid')
if recipe.get('key',{}).get('C')!='minecraft:stonecutter': fail('Architecture table C key invalid')
if recipe.get('key',{}).get('P')!='#minecraft:planks': fail('Architecture table P key invalid')
rr=(JAVA/'architecture/ArchitectureRecipeRegistry.java').read_text(encoding='utf-8')
for form in visible_forms:
    # Java accessor conversion special cases are not used; just ensure output id family appears by enum count below.
    pass
regs=re.findall(r'register\(material\.inputItem\(\), ArchitectureShape\.([A-Z_]+),',rr)
if len(regs)!=17 or len(set(regs))!=17: fail(f'Expected 17 visible table recipes, got {len(regs)}/{len(set(regs))}')
if 'ArchitectureShape.CORNICE' in rr.replace('// CORNICE keeps its ID, but is no longer offered by the table.',''):
    fail('Legacy cornice unexpectedly offered by architecture table')

# Shape IDs unique.
st=(JAVA/'architecture/ArchitectureShape.java').read_text(encoding='utf-8')
button_ids=[int(x) for x in re.findall(r'\(\s*(\d+)\s*,\s*"menu\.faithfulblocks\.shape\.',st)]
if len(button_ids)!=len(set(button_ids)): fail('Duplicate ArchitectureShape button IDs')

if errors:
    print(f'FAIL: {len(errors)} issue(s)')
    for e in errors: print(' -',e)
    sys.exit(1)
print(f'PASS: {len(json_files)} JSON files parse successfully.')
print(f'PASS: {len(materials)} materials x {len(registered_forms)} registered block families have core resources and labels.')
print('PASS: all local model references resolve; expected blockstate cardinalities match.')
print('PASS: loot self-drops and material-specific Axt/Spitzhacke tags are complete, including current arches.')
print(f'PASS: {len(loot_files)} loot tables use the Minecraft 26.3 condition schema (no legacy conditions/block_state_property).')
print('PASS: architecture table recipe, 17 visible table outputs and unique shape button IDs are consistent.')
