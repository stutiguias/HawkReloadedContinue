package uk.co.oliwali.HawkEye.blocks.blockhandlers;


import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import uk.co.oliwali.HawkEye.DataType;
import uk.co.oliwali.HawkEye.database.Consumer;

public class BedBlockHandler implements BlockHandler {

	@Override
	public void restore(Block b, BlockData blockData) {
		if (!(blockData instanceof Bed)) {
			b.setBlockData(blockData.clone(), false);
			return;
		}

		Bed foot = (Bed) blockData.clone();
		if (foot.getPart() == Bed.Part.HEAD) {
			return;
		}

		foot.setPart(Bed.Part.FOOT);
		b.setBlockData(foot, false);

		Bed head = (Bed) foot.clone();
		head.setPart(Bed.Part.HEAD);
		b.getRelative(foot.getFacing()).setBlockData(head, false);
	}

	@Override
	public void logAttachedBlocks(Consumer consumer, Block b, Player p, DataType type) { }

	@Override
	public Block getCorrectBlock(Block b) {
		if (b.getBlockData() instanceof Bed bed && bed.getPart() == Bed.Part.HEAD) {
			return b.getRelative(bed.getFacing().getOppositeFace());
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
