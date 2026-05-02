package uk.co.oliwali.HawkEye.undoData;


import org.bukkit.block.BlockState;
import uk.co.oliwali.HawkEye.HawkEye;

public class UndoBlock {

	protected BlockState state;

	public UndoBlock(BlockState state) {
		this.state = state;
	}

	public void undo() {
		if (state != null) {
			HawkEye.getBlockHandlerContainer().getBlockHandler(state.getType()).restore(state.getBlock(), state.getBlockData());
		}
	}

	public BlockState getState() {
		return state;
	}
}
