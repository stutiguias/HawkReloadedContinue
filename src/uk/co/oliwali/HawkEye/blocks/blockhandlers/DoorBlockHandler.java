package uk.co.oliwali.HawkEye.blocks.blockhandlers;


import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import uk.co.oliwali.HawkEye.DataType;
import uk.co.oliwali.HawkEye.database.Consumer;

public class DoorBlockHandler implements BlockHandler {

	@Override
	public void restore(Block b, BlockData blockData) {
		if (!(blockData instanceof Door)) {
			b.setBlockData(blockData.clone(), false);
			return;
		}

		Door lower = (Door) blockData.clone();
		if (lower.getHalf() == Bisected.Half.TOP) {
			return;
		}

		lower.setHalf(Bisected.Half.BOTTOM);
		b.setBlockData(lower, false);

		Door upper = (Door) lower.clone();
		upper.setHalf(Bisected.Half.TOP);
		b.getRelative(BlockFace.UP).setBlockData(upper, false);
	}

	@Override
	public void logAttachedBlocks(Consumer consumer, Block b, Player p, DataType type) {
	}

	@Override
	public Block getCorrectBlock(Block b) {
		if (b.getBlockData() instanceof Bisected bisected && bisected.getHalf() == Bisected.Half.TOP) {
			return b.getRelative(BlockFace.DOWN);
		}
		return b;
	}
	
	@Override
	public boolean isTopBlock() {
		return true;
	}
	
	@Override
	public boolean isAttached() {
		return false;
	}
}
