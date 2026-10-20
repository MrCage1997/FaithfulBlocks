#!/usr/bin/env python3
"""Offline checks for FaithfulBlocks 1.6 placement-polish preview."""
from pathlib import Path
from itertools import product
import json, re, sys

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
ASSETS=RES/'assets/faithfulblocks'
JAVA=ROOT/'src/main/java/de/faithfulblocks/block'
errors=[]

def fail(msg): errors.append(msg)
def load(p):
    try: return json.loads(p.read_text(encoding='utf-8'))
    except Exception as e: fail(f'Invalid JSON {p}: {e}'); return {}

materials=sorted(p.name.removesuffix('_rounded_corner.json') for p in (ASSETS/'blockstates').glob('*_rounded_corner.json'))
if len(materials)!=46: fail(f'Expected 46 materials, got {len(materials)}')

glazings=('none','clear','white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black')

# Every horizontal state selects one connected frame model and optional centered glass.
checked=0
for mat in materials:
    parts=load(ASSETS/f'blockstates/{mat}_window_frame.json').get('multipart',[])
    if len(parts)!=168: fail(f'{mat}: expected 168 multipart entries, got {len(parts)}')
    for facing in ('north','south','east','west'):
        for neg,pos,down,up in product(('false','true'), repeat=4):
            for glazing in glazings:
                state={'facing':facing,'negative_connected':neg,'positive_connected':pos,
                       'down_connected':down,'up_connected':up,'glazing':glazing}
                selected=[]
                for part in parts:
                    when=part.get('when',{})
                    if all(state.get(k)==v for k,v in when.items()):
                        selected.append(part['apply']['model'])
                frames=[m for m in selected if '_connected_' in m]
                glasses=[m for m in selected if 'frame_glass_' in m]
                if len(frames)!=1: fail(f'{mat} {state}: selected {len(frames)} frame models')
                if len(glasses)!=(0 if glazing=='none' else 1): fail(f'{mat} {state}: bad glass selection {glasses}')
                checked+=1

# Generated connected-frame models stay within the normal model cube.
models=0
for p in (ASSETS/'models/block').glob('*_window_frame_connected_*.json'):
    d=load(p)
    for e in d.get('elements',[]):
        lo=e.get('from',[]); hi=e.get('to',[])
        if len(lo)!=3 or len(hi)!=3 or any(v<0 or v>16 for v in lo+hi):
            fail(f'{p.name}: geometry outside 0..16')
    models+=1
if models != 46*32: fail(f'Expected {46*32} connected frame models, got {models}')

# Java contract: no new UP/DOWN placement, connection properties, and neighbor updates.
wf=(JAVA/'WindowFrameBlock.java').read_text(encoding='utf-8')
for needle in ('NEGATIVE_CONNECTED','POSITIVE_CONNECTED','DOWN_CONNECTED','UP_CONNECTED',
               'choosePlacementFacing','findNeighbourFacing','withConnections','context.getHorizontalDirection()'):
    if needle not in wf: fail(f'WindowFrameBlock missing {needle}')
if 'preferred = clicked.getAxis() == Direction.Axis.Y' not in wf:
    fail('WindowFrameBlock does not convert floor/ceiling clicks to a horizontal facing')

# Other conservative placement assists.
for file, needle in [
    ('FacadeOffsetBlock.java','instanceof FacadeOffsetBlock'),
    ('PilasterBlock.java','context.getClickedPos().below()'),
    ('WallEndBlock.java','context.getClickedPos().below()')]:
    if needle not in (JAVA/file).read_text(encoding='utf-8'):
        fail(f'{file} missing placement assist')

if errors:
    print(f'FAIL: {len(errors)} issue(s)')
    for e in errors[:100]: print(' -',e)
    sys.exit(1)
print(f'PASS: {checked} horizontal window-frame states select exactly one frame and optional glazing.')
print(f'PASS: {models} connected frame models are bounded to 0..16.')
print('PASS: window frames are vertical-only for new placements; facade/pilaster/wall-end placement assists are present.')
