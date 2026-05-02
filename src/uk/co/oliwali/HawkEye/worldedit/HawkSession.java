package uk.co.oliwali.HawkEye.worldedit;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import uk.co.oliwali.HawkEye.DataType;
import uk.co.oliwali.HawkEye.database.Consumer;
import uk.co.oliwali.HawkEye.entry.BlockChangeEntry;
import uk.co.oliwali.HawkEye.entry.BlockEntry;
import uk.co.oliwali.HawkEye.entry.SignEntry;
import uk.co.oliwali.HawkEye.util.BlockUtil;
import uk.co.oliwali.HawkEye.util.PlayerIdentity;

public class HawkSession extends AbstractDelegateExtent {

	private final Consumer consumer;

	private final Actor player;

	private final PlayerIdentity playerIdentity;

	private final World world;


	public HawkSession(Consumer consumer, Actor player, com.sk89q.worldedit.world.World worldedit_world, Extent extent) {
		super(extent);
		this.consumer = consumer;
		this.player = player;
		this.world = BukkitAdapter.adapt(worldedit_world);
		this.playerIdentity = PlayerIdentity.resolve(player.getName());
	}


	@Override
	public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
		org.bukkit.block.Block bukkitBlock = world.getBlockAt(location.x(), location.y(), location.z());
		BlockState previousState = bukkitBlock.getState();
		String from = BlockUtil.getBlockString(previousState);
		String to = getCompatibleBlockString(block);
		boolean changed = super.setBlock(location, block);

		if (!changed || from.equals(to)) {
			return changed;
		}

		Location loc = new Location(world, location.x(), location.y(), location.z());

		if (!BlockUtil.isAir(to)) {
			consumer.addEntry(new BlockChangeEntry(playerIdentity, DataType.WORLDEDIT_PLACE, loc, from, to));
		} else if (BlockUtil.isSign(previousState.getType()) && DataType.SIGN_BREAK.isLogged()) {
			consumer.addEntry(new SignEntry(playerIdentity, DataType.SIGN_BREAK, previousState));
		} else if (!BlockUtil.isAir(from)) {
			consumer.addEntry(new BlockEntry(playerIdentity, DataType.WORLDEDIT_BREAK, loc, from));
		}

		return changed;
	}

	private String getCompatibleBlockString(BlockStateHolder<?> block) {
		return block.getAsString();
	}
}
