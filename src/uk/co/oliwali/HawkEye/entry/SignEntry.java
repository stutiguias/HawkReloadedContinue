package uk.co.oliwali.HawkEye.entry;


import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import uk.co.oliwali.HawkEye.Base64;
import uk.co.oliwali.HawkEye.DataType;
import uk.co.oliwali.HawkEye.util.BlockUtil;
import uk.co.oliwali.HawkEye.util.PlayerIdentity;
import uk.co.oliwali.HawkEye.util.Util;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a sign entry in the database
 * Contains system for encoding sign text and storing sign orientation etc
 * @author oliverw92
 */
public class SignEntry extends DataEntry {

	private String blockDataString = BlockUtil.getBlockString(Material.OAK_SIGN);
	private String[] lines = new String[4];

	public SignEntry(String playerUuid, String player, Timestamp timestamp, int dataId, DataType type, String data, String world, int x, int y, int z) {
        super(playerUuid, player, timestamp, dataId, type, world, x, y, z);

		if (!data.contains("@")) return;

		String[] modern = data.split("@", 2);
		blockDataString = modern[0];
		if (modern.length == 2) {
			parseLines(modern[1]);
		}

	}
	
	public SignEntry() { }

	public SignEntry(Player player, DataType type, Block block) {
		this(PlayerIdentity.from(player), type, block);
	}

	public SignEntry(PlayerIdentity player, DataType type, Block block) {
		this(player, type, block.getState());
	}

	public SignEntry(String player, DataType type, Block block) {
		this(player, type, block.getState());
	}

    public SignEntry(Player player, DataType type, Block block, String[] lines) {
        this(PlayerIdentity.from(player), type, block, lines);
    }

    public SignEntry(PlayerIdentity player, DataType type, Block block, String[] lines) {
        this(player, type, block.getState());
        this.lines = lines;
    }

    public SignEntry(String player, DataType type, Block block, String[] lines) {
        this(player, type, block.getState());
        this.lines = lines;
    }

	public SignEntry(PlayerIdentity player, DataType type, BlockState state) {
		super(player, type, state.getLocation());
		interpretSignBlock(state);
	}

	public SignEntry(String player, DataType type, BlockState state) {
		super(player, type, state.getLocation());
		interpretSignBlock(state);
	}

	/**
	 * Extracts the sign data from a block
	 * @param state
	 */
	private void interpretSignBlock(BlockState state) {
		if (!(state instanceof Sign)) return;
		Sign sign = (Sign) state;
		this.blockDataString = BlockUtil.getBlockString(sign.getBlockData());
		this.lines = sign.getLines();
	}

	@Override
	public String getStringData() {
		String joined = Util.join(Arrays.asList(lines), " | ");
		return joined.isEmpty() ? data : joined;
	}

	@Override
	public String getSqlData() {
		List<String> encoded = new ArrayList<String>(4);
		for (int i = 0; i < 4; i++) encoded.add((lines[i] == null) ? "" : Base64.encode(lines[i].getBytes()));
		return blockDataString + "@" + Util.join(encoded, ",");
	}

	@Override
	public boolean rollback(Block block) {

		//If it is a sign place
		if (type == DataType.SIGN_PLACE) block.setBlockData(Material.AIR.createBlockData(), false);

		//if it is a sign break
		else {
			block.setBlockData(BlockUtil.getBlockDataFromString(blockDataString), false);
			Sign sign = (Sign)(block.getState());
			for (int i = 0; i < lines.length; i++) if (lines[i] != null) sign.setLine(i, lines[i]);
			sign.update();
		}

		return true;

	}

	@Override
	public boolean rollbackPlayer(Block block, Player player) {
		//If it is a sign place
		if (type == DataType.SIGN_PLACE) player.sendBlockChange(block.getLocation(), Material.AIR.createBlockData());
		else player.sendBlockChange(block.getLocation(), BlockUtil.getBlockDataFromString(blockDataString));
		return true;
	}

	@Override
	public boolean rebuild(Block block) {

		if (type == DataType.SIGN_BREAK) block.setBlockData(Material.AIR.createBlockData(), false);
		else {
			block.setBlockData(BlockUtil.getBlockDataFromString(blockDataString), false);
			Sign sign = (Sign)(block.getState());
			for (int i = 0; i < lines.length; i++) if (lines[i] != null) sign.setLine(i, lines[i]);
			sign.update();
		}
		return true;

	}

	private void parseLines(String encodedLines) {
		String[] encLines = encodedLines.split(",");
		for (int i = 0; i < encLines.length && i < lines.length; i++) {
			if (encLines[i] != null && !encLines[i].isEmpty()) {
				lines[i] = new String(Base64.decode(encLines[i]));
			}
		}
	}

}
