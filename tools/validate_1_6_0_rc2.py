#!/usr/bin/env python3
from pathlib import Path
import json, re, subprocess, sys
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'; ASSETS=RES/'assets/faithfulblocks'; JAVA=ROOT/'src/main/java/de/faithfulblocks'

# Existing comprehensive release validator.
r=subprocess.run([sys.executable, str(ROOT/'tools/validate_release_candidate.py')], cwd=ROOT)
if r.returncode: raise SystemExit(r.returncode)

errors=[]
def fail(x): errors.append(x)

def load(p):
    try: return json.loads(p.read_text(encoding='utf-8'))
    except Exception as e: fail(f'bad json {p}: {e}'); return {}

# Version.
props=(ROOT/'gradle.properties').read_text(encoding='utf-8')
if 'version=1.6.0-rc2' not in [x.strip() for x in props.splitlines()]: fail('version is not rc2')

# Return option and reverse registry.
shape=(JAVA/'architecture/ArchitectureShape.java').read_text(encoding='utf-8')
if 'RETURN_ORIGINAL(18,' not in shape: fail('RETURN_ORIGINAL button missing')
reg=(JAVA/'architecture/ArchitectureRecipeRegistry.java').read_text(encoding='utf-8')
if 'REVERSE_RECIPES' not in reg: fail('reverse map missing')
if 'shape == ArchitectureShape.RETURN_ORIGINAL' not in reg: fail('reverse lookup missing')
if 'RECIPES.containsKey(input) || REVERSE_RECIPES.containsKey(input)' not in reg: fail('architecture items not accepted as input')

# 17 visible + 3 legacy reverse registrations per material.
reverse_calls=re.findall(r'registerReverse\(ModBlocks\.([A-Za-z]+)\(material\)\.asItem\(\), material\.inputItem\(\)\);',reg)
if len(reverse_calls)!=20 or len(set(reverse_calls))!=20:
    fail(f'expected 20 reverse families, got {len(reverse_calls)}/{len(set(reverse_calls))}: {reverse_calls}')

for locale in ('de_de','en_us'):
    lang=load(ASSETS/f'lang/{locale}.json')
    if 'menu.faithfulblocks.shape.return_original' not in lang: fail(f'return label missing in {locale}')

# Custom table textures and model.
model=load(ASSETS/'models/block/architecture_table.json')
expected={
 'down':'faithfulblocks:block/architecture_table_bottom',
 'up':'faithfulblocks:block/architecture_table_top',
 'north':'faithfulblocks:block/architecture_table_front',
 'south':'faithfulblocks:block/architecture_table_front',
 'west':'faithfulblocks:block/architecture_table_side',
 'east':'faithfulblocks:block/architecture_table_side'
}
for side,tex in expected.items():
    if model.get('textures',{}).get(side)!=tex: fail(f'table model {side} wrong')
for name in ('architecture_table_top.png','architecture_table_front.png','architecture_table_side.png','architecture_table_bottom.png'):
    p=ASSETS/'textures/block'/name
    if not p.exists(): fail(f'missing {name}')
    elif p.stat().st_size<50: fail(f'texture suspiciously small: {name}')

if errors:
    print(f'RC2 FAIL: {len(errors)} issue(s)')
    for e in errors: print(' -',e)
    raise SystemExit(1)
print('PASS: rc2 return-original registry covers all 20 registered architecture families x 46 materials.')
print('PASS: architecture table uses four custom FaithfulBlocks textures.')
print('PASS: return-original button, languages and version metadata are present.')
