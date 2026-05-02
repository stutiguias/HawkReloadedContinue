package uk.co.oliwali.HawkEye.entry;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import uk.co.oliwali.HawkEye.DataType;
import uk.co.oliwali.HawkEye.util.BlockUtil;
import uk.co.oliwali.HawkEye.util.PlayerIdentity;

import java.sql.Timestamp;

/**
 * Represents a block change entry - one block changing to another
 *
 * @author oliverw92
 */
public class BlockChangeEntry extends DataEntry {


    private String from = null;
    private String to = null;


    public BlockChangeEntry(String playerUuid, String player, Timestamp timestamp, int dataId, DataType type, String data, String world, int x, int y, int z) {
        super(playerUuid, player, timestamp, dataId, type, world, x, y, z);

        String[] info = data.split("-");

        from = info[0];
        to = info[1];
    }

    public BlockChangeEntry(Player player, DataType type, Location loc, BlockState from, BlockState to) {
        this(player, type, loc, BlockUtil.getBlockString(from), BlockUtil.getBlockString(to));
    }

    public BlockChangeEntry(PlayerIdentity player, DataType type, Location loc, BlockState from, BlockState to) {
        this(player, type, loc, BlockUtil.getBlockString(from), BlockUtil.getBlockString(to));
    }

    public BlockChangeEntry(String player, DataType type, Location loc, BlockState from, BlockState to) {
        this(player, type, loc, BlockUtil.getBlockString(from), BlockUtil.getBlockString(to));
    }

    public BlockChangeEntry(Player player, DataType type, Location loc, String from, String to) {
        this(PlayerIdentity.from(player), type, loc, from, to);
    }

    public BlockChangeEntry(PlayerIdentity player, DataType type, Location loc, String from, String to) {
        super(player, type, loc);
        this.from = from;
        this.to = to;
    }

    public BlockChangeEntry(String player, DataType type, Location loc, BlockState from, String to) {
        this(player, type, loc, BlockUtil.getBlockString(from), to);
    }

    public BlockChangeEntry(Player player, DataType type, Location loc, BlockState from, String to) {
        this(PlayerIdentity.from(player), type, loc, BlockUtil.getBlockString(from), to);
    }

    public BlockChangeEntry(PlayerIdentity player, DataType type, Location loc, BlockState from, String to) {
        this(player, type, loc, BlockUtil.getBlockString(from), to);
    }

    public BlockChangeEntry(String player, DataType type, Location loc, String from, String to) {
        super(player, type, loc);
        this.from = from;
        this.to = to;
    }

    @Override
    public String getStringData() {
        if (BlockUtil.isAir(from)) return BlockUtil.getBlockStringName(to);
        return BlockUtil.getBlockStringName(from) + " changed to " + BlockUtil.getBlockStringName(to);
    }

    @Override
    public String getSqlData() {
        return from + "-" + to;
    }

    @Override
    public boolean rollback(Block block) {
        BlockUtil.setBlockString(block, from);
        return true;
    }

    @Override
    public boolean rollbackPlayer(Block block, Player player) {
        player.sendBlockChange(block.getLocation(), BlockUtil.getBlockDataFromString(from));
        return true;
    }

    @Override
    public boolean rebuild(Block block) {
        if (to == null) return false;
        else BlockUtil.setBlockString(block, to);
        return true;
    }

}
