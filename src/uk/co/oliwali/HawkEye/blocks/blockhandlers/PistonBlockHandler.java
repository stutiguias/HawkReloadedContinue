package uk.co.oliwali.HawkEye.blocks.blockhandlers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import uk.co.oliwali.HawkEye.DataType;
import uk.co.oliwali.HawkEye.database.Consumer;

public class PistonBlockHandler implements BlockHandler {

    @Override
    public void restore(Block b, BlockData blockData) {
        b.setBlockData(blockData.clone(), true);
    }

    @Override
    public void logAttachedBlocks(Consumer consumer, Block b, Player p, DataType type) {}

    @Override
    public Block getCorrectBlock(Block b) {
        Material type = b.getType();
        if ((type.name().equals("MOVING_PISTON") || type == Material.PISTON_HEAD) && b.getBlockData() instanceof Directional directional) {
            return b.getRelative(directional.getFacing().getOppositeFace());
        }
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
