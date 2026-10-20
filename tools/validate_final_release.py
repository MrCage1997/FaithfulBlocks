#!/usr/bin/env python3
"""Final offline release validation for FaithfulBlocks 1.5.0."""
from pathlib import Path
import json, subprocess, sys

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'

# First run the comprehensive RC consistency validator unchanged.
r=subprocess.run([sys.executable, str(ROOT/'tools/validate_release_candidate.py')], cwd=ROOT)
if r.returncode:
    sys.exit(r.returncode)

errors=[]
def fail(msg): errors.append(msg)
def load(p):
    try: return json.loads(p.read_text(encoding='utf-8'))
    except Exception as e: fail(f'Invalid JSON {p.relative_to(ROOT)}: {e}'); return {}

props=(ROOT/'gradle.properties').read_text(encoding='utf-8')
if 'version=1.5.0' not in [line.strip() for line in props.splitlines()]:
    fail('gradle.properties is not set to final version 1.5.0')
mod=load(RES/'fabric.mod.json')
if mod.get('icon')!='assets/faithfulblocks/icon.png':
    fail('fabric.mod.json missing final mod icon')
icon=RES/'assets/faithfulblocks/icon.png'
if not icon.exists(): fail('Final mod icon is missing')
for required in ('README.txt','CHANGELOG.txt','LICENSE.txt','FINAL-QA.txt'):
    if not (ROOT/required).exists(): fail(f'Missing final release file: {required}')
old_notes=[p.name for p in ROOT.iterdir() if p.is_file() and (p.name.startswith('TEST-') or p.name.startswith('CHANGELOG-'))]
if old_notes: fail('Historical root test/changelog files remain: '+', '.join(sorted(old_notes)))
# BUILD helper should point at final jar.
bat=(ROOT/'BUILD-WINDOWS.bat').read_text(encoding='utf-8', errors='replace')
if 'faithfulblocks-1.5.0.jar' not in bat: fail('BUILD-WINDOWS.bat does not point to final JAR')

if errors:
    print(f'FINAL FAIL: {len(errors)} issue(s)')
    for e in errors: print(' -',e)
    sys.exit(1)
print('PASS: final 1.5.0 metadata, icon, release notes and cleanup are consistent.')
