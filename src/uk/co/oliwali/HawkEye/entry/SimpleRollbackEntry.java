package uk.co.oliwali.HawkEye.entry;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import uk.co.oliwali.HawkEye.DataType;
import uk.co.oliwali.HawkEye.util.PlayerIdentity;

import java.sql.Timestamp;

/**
 * Used for simple rollbacks - sets the block to air regardless of the data
 * @author oliverw92
 */
public class SimpleRollbackEntry extends DataEntry {

	public SimpleRollbackEntry(String playerUuid, String player, Timestamp timestamp, int dataId, DataType type, String data, String world, int x, int y, int z) {
		super(playerUuid, player, timestamp, dataId, type, data, world, x, y, z);
	}
	
	public SimpleRollbackEntry() { }

	public SimpleRollbackEntry(Player player, DataType type, Location loc, String data) {
		this(PlayerIdentity.from(player), type, loc, data);
	}

	public SimpleRollbackEntry(PlayerIdentity player, DataType type, Location loc, String data) {
		super(player, type, loc);
		this.data = data;
	}
	public SimpleRollbackEntry(String player, DataType type, Location loc, String data) {
		super(player, type, loc);
		this.data = data;
	}

	@Override
	public boolean rollback(Block block) {
		block.setBlockData(Material.AIR.createBlockData(), false);
		return true;
	}

	@Override
	public boolean rollbackPlayer(Block block, Player player) {
		player.sendBlockChange(block.getLocation(), Material.AIR.createBlockData());
		return true;
	}

}
