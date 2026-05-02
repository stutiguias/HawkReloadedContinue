package uk.co.oliwali.HawkEye.entry;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import uk.co.oliwali.HawkEye.DataType;
import uk.co.oliwali.HawkEye.util.BlockUtil;
import uk.co.oliwali.HawkEye.util.PlayerIdentity;

import java.sql.Timestamp;

/**
 * Represents a block-type entry in the database
 * Rollbacks will set the block to the data value
 * @author oliverw92
 */
public class BlockEntry extends DataEntry {

	public BlockEntry() { }

	public BlockEntry(String playerUuid, String player, Timestamp timestamp, int dataId, DataType type, String data, String world, int x, int y, int z) {
		super(playerUuid, player, timestamp, dataId, type, data, world, x, y, z);
	}
	
	public BlockEntry(String player, DataType type, Block block) {
		this(player, type, block.getLocation(), BlockUtil.getBlockString(block));
	}

	public BlockEntry(PlayerIdentity player, DataType type, Block block) {
		this(player, type, block, block.getLocation());
	}

	public BlockEntry(Player player, DataType type, Block block) {
		this(player, type, block, block.getLocation());
	}

	public BlockEntry(PlayerIdentity player, DataType type, Block block, Location l) {
		this(player, type, l, BlockUtil.getBlockString(block));
	}

	public BlockEntry(Player player, DataType type, Block block, Location l) {
		this(PlayerIdentity.from(player), type, l, BlockUtil.getBlockString(block));
	}

	public BlockEntry(String player, DataType type, Location l,  String data) {
		super(player, type, l, data);
	}

	public BlockEntry(PlayerIdentity player, DataType type, Location l, String data) {
		super(player, type, l, data);
	}

	@Override
	public String getStringData() {
		return BlockUtil.getBlockStringName(data);
	}

	@Override
	public boolean rollback(Block block) {
		BlockUtil.setBlockString(block, data);
		return true;
	}

	@Override
	public boolean rollbackPlayer(Block block, Player player) {
		player.sendBlockChange(block.getLocation(), BlockUtil.getBlockDataFromString(data));
		return true;
	}

	@Override
	public boolean rebuild(Block block) {
		BlockUtil.setBlockString(block, BlockUtil.getBlockString(Material.AIR));
		return true;
	}

}
