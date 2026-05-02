package uk.co.oliwali.HawkEye.blocks.blockhandlers;


import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.entity.Player;
import uk.co.oliwali.HawkEye.DataType;
import uk.co.oliwali.HawkEye.database.Consumer;
import uk.co.oliwali.HawkEye.entry.BlockEntry;

public class VineBlockHandler implements BlockHandler {

    @Override
    public void logAttachedBlocks(Consumer consumer, Block b, Player p, DataType type) {
        b = b.getRelative(BlockFace.DOWN);

        while (b.getType() == Material.VINE) {
            Material b2 = b.getRelative(getVineFace(b.getBlockData())).getType();

            if (!b2.isSolid())
                consumer.addEntry(new BlockEntry(p, type, b));
            else
                break;

            b = b.getRelative(BlockFace.DOWN);
        }
    }

    @Override
    public boolean isAttached() {
        return true;
    }

    private BlockFace getVineFace(BlockData blockData) {
        if (blockData instanceof MultipleFacing multifaced) {
            for (BlockFace face : new BlockFace[]{BlockFace.SOUTH, BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST}) {
                if (multifaced.hasFace(face)) {
                    return face;
                }
            }
        }

        return BlockFace.NORTH;
    }

    @Override
    public void restore(Block b, BlockData blockData) {
        b.setBlockData(blockData.clone(), false);
    }

    @Override
    public Block getCorrectBlock(Block b) {
        return b;
    }

    @Override
    public boolean isTopBlock() {
        return false;
    }
}
