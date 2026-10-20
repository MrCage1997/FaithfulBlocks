#!/usr/bin/env python3
"""Test the actual placement method in isolation; not a Minecraft API build."""
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
code = (ROOT/'src/main/java/de/faithfulblocks/block/WindowFrameBlock.java').read_text()
begin = code.index('    public BlockState getStateForPlacement(')
end = code.index('\n    @Override', begin)
method = code[begin:end]
source = '''
class PlacementCheck {
    public static void main(String[] args) {
        int count = 0;
        WindowFrameBlock placed = new WindowFrameBlock("oak");
        for (Direction existing : Direction.values())
        for (Direction clicked : Direction.values())
        for (boolean sneak : new boolean[]{false,true})
        for (boolean replace : new boolean[]{false,true})
        for (boolean frame : new boolean[]{false,true})
        for (boolean glazed : new boolean[]{false,true})
        for (String material : new String[]{"oak","stone"}) {
            BlockPos target = new BlockPos(-13,55,21);
            Block supportBlock = frame ? new WindowFrameBlock(material) : new Block();
            BlockState support = new BlockState(supportBlock,existing,glazed);
            Level level = new Level(target.relative(clicked.getOpposite()),support);
            BlockPlaceContext context = new BlockPlaceContext(target,clicked,sneak,replace,level);
            BlockState actual = placed.getStateForPlacement(context);
            boolean inherit = frame && !sneak && !replace && existing.getAxis()!=clicked.getAxis();
            Direction expected = inherit ? existing : clicked;
            if (actual.direction != expected || actual.block != placed || actual.glazed)
                throw new AssertionError(existing+" "+clicked+" sneak="+sneak+" replace="+replace+" frame="+frame);
            if (level.reads != ((!sneak && !replace) ? 1 : 0)) throw new AssertionError("Unexpected neighbor lookup");
            count++;
        }
        System.out.println("PASS: " + count + " placement scenarios: six orientations, edge/front/back, sneak, replaceable blocks, mixed materials and glazing.");
        System.out.println("Isolated method test only; Minecraft/Fabric build and playtest remain necessary.");
    }
}
enum Direction {
    DOWN(0,-1,0,1),UP(0,1,0,1),NORTH(0,0,-1,2),SOUTH(0,0,1,2),WEST(-1,0,0,0),EAST(1,0,0,0);
    final int x,y,z,axis;
    Direction(int x,int y,int z,int axis) { this.x=x;this.y=y;this.z=z;this.axis=axis; }
    int getAxis() { return axis; }
    Direction getOpposite() { return values()[ordinal() ^ 1]; }
}
record BlockPos(int x,int y,int z) {
    BlockPos relative(Direction d) { return new BlockPos(x+d.x,y+d.y,z+d.z); }
}
class Block {}
class BlockState {
    final Block block; final Direction direction; final boolean glazed;
    BlockState(Block b,Direction d,boolean g) { block=b;direction=d;glazed=g; }
    Block getBlock() { return block; }
    Direction getValue(Object property) {
        if (!(block instanceof WindowFrameBlock)) throw new AssertionError("Non-frame property read");
        return direction;
    }
    BlockState setValue(Object property,Direction d) { return new BlockState(block,d,glazed); }
}
class Level {
    final BlockPos expected; final BlockState support; int reads;
    Level(BlockPos p,BlockState s) { expected=p;support=s; }
    BlockState getBlockState(BlockPos p) {
        if (!p.equals(expected)) throw new AssertionError("Wrong support position");
        reads++; return support;
    }
}
record BlockPlaceContext(BlockPos pos,Direction face,boolean sneak,boolean replace,Level level) {
    Direction getClickedFace() { return face; }
    BlockPos getClickedPos() { return pos; }
    Level getLevel() { return level; }
    boolean isSecondaryUseActive() { return sneak; }
    boolean replacingClickedOnBlock() { return replace; }
}
class WindowFrameBlock extends Block {
    static final Object FACING = new Object();
    final String material;
    WindowFrameBlock(String material) { this.material=material; }
    BlockState defaultBlockState() { return new BlockState(this,Direction.UP,false); }
METHOD
}
'''.replace('METHOD',method)
with tempfile.TemporaryDirectory(prefix='faithfulblocks-placement-') as folder:
    path = Path(folder)/'PlacementCheck.java'
    path.write_text(source)
    compiler = ['javac'] if shutil.which('javac') else ['java','-m','jdk.compiler/com.sun.tools.javac.Main']
    subprocess.run(compiler+[str(path)],check=True)
    subprocess.run(['java','-cp',folder,'PlacementCheck'],check=True)
