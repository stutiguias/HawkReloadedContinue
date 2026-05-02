package uk.co.oliwali.HawkEye.blocks.blockhandlers;


import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import uk.co.oliwali.HawkEye.DataType;
import uk.co.oliwali.HawkEye.database.Consumer;
import uk.co.oliwali.HawkEye.util.InventoryUtil;

public class ContainerBlockHandler extends BasicBlockHandler {

    @Override
    public void restore(Block b, BlockData blockData) {
        b.setBlockData(blockData.clone(), false);
    }

    @Override
    public void logAttachedBlocks(Consumer consumer, Block b, Player p, DataType type) {
        InventoryUtil.handleHolderRemoval(consumer, p, b.getState());

        super.logAttachedBlocks(consumer, b, p, type);
    }

    @Override
    public Block getCorrectBlock(Block b) {
        return b;
    }

    @Override
    public boolean isTopBlock() {
        return false;
    }

    @Override
    public boolean isAttached() {
        return false;
    }
}
