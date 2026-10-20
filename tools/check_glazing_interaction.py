#!/usr/bin/env python3
"""Run the exact useItemOn method and enum against a tiny isolated test API.

Requires javac/java 17+. Tests consumption and state transitions; does NOT check
Minecraft/Fabric binary compatibility or replace a Gradle build/playtest.
"""
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT/'src/main/java/de/faithfulblocks/block'

def main():
    block = (JAVA/'WindowFrameBlock.java').read_text()
    begin = block.index('    protected InteractionResult useItemOn(')
    end = block.index('\n    private static VoxelShape stateShape', begin)
    method = block[begin:end]
    enum = (JAVA/'FrameGlazing.java').read_text()
    enum = re.sub(r'^(?:package|import) .*;\n', '', enum, flags=re.M).replace('public enum FrameGlazing', 'enum FrameGlazing')
    items = sorted({name.upper() for name in re.findall(r'\w+\("[a-z_]+", "([a-z_]+)"\)', enum)} | {'STONE', 'GLASS'})
    fields = '\n'.join(f'    static final Item {name} = new Item("minecraft:{name.lower()}");' for name in items)
    source = '''
class InteractionCheck {
    static final Object GLAZING = new Object();
    static int checks;
    static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("check " + checks); }
METHOD
    static InteractionResult click(InteractionCheck block, ItemStack stack, BlockState state, Level level, Player player, InteractionHand hand) {
        return block.useItemOn(stack,state,level,new BlockPos(),player,hand,new BlockHitResult());
    }
    public static void main(String[] args) {
        InteractionCheck block = new InteractionCheck();
        for (FrameGlazing pane : FrameGlazing.values()) {
            if (pane == FrameGlazing.NONE) continue;
            Item item = Items.find(pane);
            for (String facing : new String[]{"up","down","north","south","east","west"}) {
                for (InteractionHand hand : InteractionHand.values()) {
                    BlockState empty = new BlockState(FrameGlazing.NONE,facing);
                    Player player = new Player();
                    Level server = new Level(false);
                    ItemStack stack = new ItemStack(item,3);
                    check(click(block,stack,empty,server,player,hand) == InteractionResult.SUCCESS_SERVER);
                    check(stack.count == 2 && server.writes == 1 && server.sounds == 1);
                    check(server.state.glazing == pane && server.state.facing.equals(facing));
                    // The next use must neither replace glass nor spend another item.
                    check(click(block,stack,server.state,server,player,hand) == InteractionResult.FAIL);
                    check(stack.count == 2 && server.writes == 1 && server.sounds == 1);
                    Level client = new Level(true);
                    check(click(block,stack,empty,client,player,hand) == InteractionResult.SUCCESS);
                    check(stack.count == 2 && client.writes == 0 && client.sounds == 0);
                    player.abilities.instabuild = true;
                    Level creative = new Level(false);
                    check(click(block,stack,empty,creative,player,hand) == InteractionResult.SUCCESS_SERVER);
                    check(stack.count == 2 && creative.state.glazing == pane);
                    player.abilities.instabuild = false;
                    player.abilities.mayBuild = false;
                    Level restricted = new Level(false);
                    check(click(block,stack,empty,restricted,player,hand) == InteractionResult.FAIL);
                    check(stack.count == 2 && restricted.writes == 0);
                    player.abilities.mayBuild = true;
                    Level rejected = new Level(false); rejected.accept = false;
                    check(click(block,stack,empty,rejected,player,hand) == InteractionResult.FAIL);
                    check(stack.count == 2 && rejected.state == null && rejected.sounds == 0);
                }
            }
        }
        for (Item item : new Item[]{Items.STONE,Items.GLASS,Items.AIR}) {
            Level server = new Level(false); ItemStack stack = new ItemStack(item,5);
            check(click(block,stack,new BlockState(FrameGlazing.NONE,"up"),server,new Player(),InteractionHand.MAIN_HAND) == InteractionResult.TRY_WITH_EMPTY_HAND);
            check(stack.count == 5 && server.writes == 0);
        }
        check(FrameGlazing.fromStack(new ItemStack(Items.GLASS_PANE,0)) == FrameGlazing.NONE);
        check(FrameGlazing.fromStack(new ItemStack(new Item("othermod:red_stained_glass_pane"),1)) == FrameGlazing.NONE);
        check(FrameGlazing.fromStack(new ItemStack(new Item("minecraft:red_stained_glass"),1)) == FrameGlazing.NONE);
        System.out.println("PASS: " + checks + " isolated interaction checks using actual useItemOn and FrameGlazing sources.");
        System.out.println("Not a Minecraft/Fabric API compilation or a game integration test.");
    }
}
interface StringRepresentable { String getSerializedName(); }
record Identifier(String value) {
    static Identifier withDefaultNamespace(String path) { return new Identifier("minecraft:" + path); }
}
class Item { final Identifier id; Item(String id) { this.id=new Identifier(id); } }
class BuiltInRegistries {
    static final ItemRegistry ITEM = new ItemRegistry();
    static class ItemRegistry { Identifier getKey(Item item) { return item.id; } }
}
class Items {
FIELDS
    static Item find(FrameGlazing pane) {
        for (java.lang.reflect.Field f : Items.class.getDeclaredFields()) {
            try { Item item = (Item) f.get(null); if (FrameGlazing.fromStack(new ItemStack(item,1)) == pane) return item; }
            catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        }
        throw new AssertionError(pane);
    }
}
class ItemStack {
    final Item item; int count;
    ItemStack(Item item,int count) { this.item=item; this.count=count; }
    boolean is(Item other) { return count > 0 && item==other; }
    boolean isEmpty() { return count <= 0 || item == Items.AIR; }
    Item getItem() { return item; }
    void shrink(int n) { count-=n; }
}
class Abilities { boolean mayBuild=true, instabuild; }
class Player { final Abilities abilities=new Abilities(); Abilities getAbilities() { return abilities; } }
class BlockState {
    final FrameGlazing glazing; final String facing;
    BlockState(FrameGlazing glazing,String facing) { this.glazing=glazing; this.facing=facing; }
    FrameGlazing getValue(Object property) { return glazing; }
    BlockState setValue(Object property,FrameGlazing glazing) { return new BlockState(glazing,facing); }
}
class Level {
    final boolean client; boolean accept=true; int writes,sounds; BlockState state;
    Level(boolean client) { this.client=client; }
    boolean isClientSide() { return client; }
    boolean setBlock(BlockPos pos,BlockState state,int flags) { writes++; if (accept) this.state=state; return accept; }
    void playSound(Object player,BlockPos pos,Object sound,Object source,float volume,float pitch) { sounds++; }
}
class Block { static final int UPDATE_ALL=3; }
class BlockPos {}
class BlockHitResult {}
class SoundEvents { static final Object GLASS_PLACE=new Object(); }
class SoundSource { static final Object BLOCKS=new Object(); }
enum InteractionHand { MAIN_HAND, OFF_HAND }
enum InteractionResult { TRY_WITH_EMPTY_HAND, FAIL, SUCCESS, SUCCESS_SERVER }
ENUM
'''.replace('METHOD', method).replace('FIELDS', fields).replace('ENUM', enum)
    with tempfile.TemporaryDirectory(prefix='faithfulblocks-glazing-') as directory:
        path = Path(directory)/'InteractionCheck.java'
        path.write_text(source)
        compiler = ['javac'] if shutil.which('javac') else ['java', '-m', 'jdk.compiler/com.sun.tools.javac.Main']
        subprocess.run(compiler + [str(path)], check=True)
        subprocess.run(['java','-cp',directory,'InteractionCheck'], check=True)

if __name__ == '__main__':
    main()
