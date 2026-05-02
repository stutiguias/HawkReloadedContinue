package uk.co.oliwali.HawkEye.entry;

import org.bukkit.Art;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import uk.co.oliwali.HawkEye.DataType;
import uk.co.oliwali.HawkEye.itemserializer.ItemSerializer;
import uk.co.oliwali.HawkEye.util.BlockUtil;
import uk.co.oliwali.HawkEye.util.EntityUtil;
import uk.co.oliwali.HawkEye.util.PlayerIdentity;

import java.sql.Timestamp;
/**
 * Represents a hanging-type entry in the database
 * Rollbacks will set the block to the data value
 * @author bob7l
 */
public class HangingEntry extends DataEntry {

    private static ItemSerializer serializer = new ItemSerializer();

	public HangingEntry(String playerUuid, String player, Timestamp timestamp, int dataId, DataType type, String data, String world, int x, int y, int z) {
		super(playerUuid, player, timestamp, dataId, type, data, world, x, y, z);
	}

	public HangingEntry() { }

	public HangingEntry(String player, DataType type, Location loc, String entityType, int faceId, ItemStack item) {
		this(player, type, loc, entityType, faceId, serializer.serializeItem(item));
	}

	public HangingEntry(Player player, DataType type, Location loc, String entityType, int faceId, ItemStack item) {
		this(PlayerIdentity.from(player), type, loc, entityType, faceId, serializer.serializeItem(item));
	}

	public HangingEntry(String player, DataType type, Location loc, String entityType, int faceId, String extra) {
		super(player, type, loc, (entityType + ":" + faceId + ":" + extra) );
	}

	public HangingEntry(Player player, DataType type, Location loc, String entityType, int faceId, String extra) {
		this(PlayerIdentity.from(player), type, loc, entityType, faceId, extra);
	}

	public HangingEntry(PlayerIdentity player, DataType type, Location loc, String entityType, int faceId, String extra) {
		super(player, type, loc, (entityType + ":" + faceId + ":" + extra));
	}


	@Override
	public String getStringData() {
        String[] args = data.split(":", 3);

        if (isItemFrame(args[0])) {
            ItemStack item = serializer.buildItemFromString(args[2]);
            return "ItemFrame" + (item.getType().equals(Material.AIR) ? "" : " with " + BlockUtil.formatItemStack(item));
        }

        return "Painting";
	}

	@Override
	public boolean rollback(Block block) {
        String[] args = data.split(":", 3);

        BlockFace face = EntityUtil.getFaceFromInt(Integer.parseInt(args[1]));

		//This can and WILL throw exceptions - I believe it's a bug with spigot's API
		try {
			if (isItemFrame(args[0])) {
				ItemFrame itemframe = block.getWorld().spawn(block.getLocation(), ItemFrame.class);
				itemframe.setFacingDirection(face.getOppositeFace(), true);
				itemframe.setItem(serializer.buildItemFromString(args[2]));
			} else {
				Painting painting = block.getWorld().spawn(block.getLocation(), Painting.class);
				painting.setFacingDirection(face.getOppositeFace(), true);
				painting.setArt(parseArt(args[2]));
			}
		} catch (Exception e) {
			return false; //The exception thrown is known, and shouldn't be printed
		}

		return true;
	}

	//Simply return true since we can't sendBlockChange (It's an entity)
	@Override
	public boolean rollbackPlayer(Block block, Player player) {
		return true;
	}

	//Simply return true since we can't rebuild (It's an entity)
	@Override
	public boolean rebuild(Block block) {
		return true;
	}

	private boolean isItemFrame(String entityType) {
		return "item_frame".equalsIgnoreCase(entityType);
	}

	private Art parseArt(String artName) {
		return Art.valueOf(artName.toUpperCase());
	}

}
