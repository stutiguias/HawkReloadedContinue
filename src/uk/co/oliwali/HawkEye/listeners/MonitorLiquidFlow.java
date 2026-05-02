package uk.co.oliwali.HawkEye.listeners;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import uk.co.oliwali.HawkEye.DataType;
import uk.co.oliwali.HawkEye.HawkEvent;
import uk.co.oliwali.HawkEye.HawkEye;
import uk.co.oliwali.HawkEye.database.Consumer;
import uk.co.oliwali.HawkEye.entry.BlockChangeEntry;
import uk.co.oliwali.HawkEye.util.BlockUtil;
import uk.co.oliwali.HawkEye.util.PlayerIdentity;

import java.util.HashMap;

public class MonitorLiquidFlow extends HawkEyeListener {

    private HashMap<Location, PlayerIdentity> playerCache = new HashMap<Location, PlayerIdentity>(10);
    private int cacheRunTime = 10;
    private int timerId = -1;

    public MonitorLiquidFlow(Consumer consumer) {
        super(consumer);
    }

    public void registerEvents() {
        super.registerEvents();

        startCacheCleaner();
    }

    /**
     * Clears the Player cache when it's been 10 seconds after a waterflow event
     * Every time the event fires, the timer resets to allow the water to be tracked
     */
    private void startCacheCleaner() {
        if (DataType.PLAYER_LAVA_FLOW.isLogged() || DataType.PLAYER_WATER_FLOW.isLogged()) {
            Bukkit.getScheduler().cancelTask(timerId);
            timerId = Bukkit.getScheduler().scheduleSyncRepeatingTask(HawkEye.getInstance(), new Runnable() {
                @Override
                public void run() {
                    cacheRunTime--;
                    if (cacheRunTime == 0) {
                        playerCache.clear();
                    }
                }
            }, 20L, 20L);
        }
    }

    /**
     * Resets cache timer and
     * adds the new location
     */
    private void addToCache(Location l, PlayerIdentity p) {
        cacheRunTime = 10; //Reset cache timer
        playerCache.put(l, p); //Add location to cache
    }

    @HawkEvent(dataType = {DataType.PLAYER_LAVA_FLOW, DataType.PLAYER_WATER_FLOW})
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Material bucket = event.getBucket();
        Location loc = event.getBlockClicked().getRelative(event.getBlockFace()).getLocation();

        if ((bucket == Material.WATER_BUCKET && DataType.PLAYER_WATER_FLOW.isLogged()) || (bucket == Material.LAVA_BUCKET && DataType.PLAYER_LAVA_FLOW.isLogged())) {
            playerCache.put(loc, PlayerIdentity.from(event.getPlayer()));
        }
    }

    @HawkEvent(dataType = {DataType.PLAYER_LAVA_FLOW, DataType.PLAYER_WATER_FLOW})
    public void onPlayerBlockFromTo(BlockFromToEvent event) {

        //Only interested in liquids flowing
        if (!event.getBlock().isLiquid()) return;

        Location loc = event.getToBlock().getLocation();
        BlockState from = event.getBlock().getState();
        BlockState to = event.getToBlock().getState();

        if (from.getType() == to.getType()) return;

        Location fromloc = from.getLocation();

        PlayerIdentity player = playerCache.get(fromloc);

        if (player == null) return; //This is basically what containsKey does, but is 10x faster :)

        //Lava
        if (BlockUtil.isLava(from)) {

            //Flowing into a normal block
            if (BlockUtil.isFluidReplaceable(to)) {
                increaseLiquidLevel(from);
            }

            //Flowing into water
            else if (BlockUtil.isWater(to)) {
                from.setBlockData((event.getFace() == BlockFace.DOWN ? Material.LAVA : Material.COBBLESTONE).createBlockData());
            }
            consumer.addEntry(new BlockChangeEntry(player, DataType.PLAYER_LAVA_FLOW, loc, to, from));
            addToCache(loc, player);
        }

        //Water
        else if (BlockUtil.isWater(from)) {

            //Normal block
            if (BlockUtil.isFluidReplaceable(to)) {
                increaseLiquidLevel(from);
                consumer.addEntry(new BlockChangeEntry(player, DataType.PLAYER_WATER_FLOW, loc, to, from));
                addToCache(loc, player);
            }
            //If we are flowing over lava, cobble or obsidian will form
            BlockState lower = event.getToBlock().getRelative(BlockFace.DOWN).getState();
            if (BlockUtil.isLava(lower)) {
                if (BlockUtil.isSourceLiquid(lower)) {
                    from.setBlockData(Material.OBSIDIAN.createBlockData());
                } else {
                    from.setBlockData(Material.COBBLESTONE.createBlockData());
                }
                loc.setY(loc.getY() - 1);
                consumer.addEntry(new BlockChangeEntry(player, DataType.PLAYER_WATER_FLOW, loc, lower, from));
                addToCache(loc, player);
            }
        }
    }

    @HawkEvent(dataType = {DataType.LAVA_FLOW, DataType.WATER_FLOW})
    public void onBlockFromTo(BlockFromToEvent event) {

        //Only interested in liquids flowing
        if (!event.getBlock().isLiquid()) return;

        Location loc = event.getToBlock().getLocation();
        BlockState from = event.getBlock().getState();
        BlockState to = event.getToBlock().getState();

        if (from.getType() == to.getType()) return;

        //Lava
        if (BlockUtil.isLava(from)) {

            //Flowing into a normal block
            if (BlockUtil.isFluidReplaceable(to)) {
                increaseLiquidLevel(from);
            }

            //Flowing into water
            else if (BlockUtil.isWater(to)) {
                from.setBlockData((event.getFace() == BlockFace.DOWN ? Material.LAVA : Material.COBBLESTONE).createBlockData());
            }
            consumer.addEntry(new BlockChangeEntry(ENVIRONMENT, DataType.LAVA_FLOW, loc, to, from));

        }

        //Water
        else if (BlockUtil.isWater(from)) {

            //Normal block
            if (BlockUtil.isFluidReplaceable(to)) {
                increaseLiquidLevel(from);
                consumer.addEntry(new BlockChangeEntry(ENVIRONMENT, DataType.WATER_FLOW, loc, to, from));
            }

            //If we are flowing over lava, cobble or obsidian will form
            BlockState lower = event.getToBlock().getRelative(BlockFace.DOWN).getState();
            if (BlockUtil.isLava(lower)) {
                if (BlockUtil.isSourceLiquid(lower)) {
                    from.setBlockData(Material.OBSIDIAN.createBlockData());
                } else {
                    from.setBlockData(Material.COBBLESTONE.createBlockData());
                }
                loc.setY(loc.getY() - 1);
                consumer.addEntry(new BlockChangeEntry(ENVIRONMENT, DataType.WATER_FLOW, loc, lower, from));
            }

        }

    }

    private void increaseLiquidLevel(BlockState state) {
        if (state.getBlockData() instanceof Levelled levelled) {
            Levelled updated = (Levelled) levelled.clone();
            updated.setLevel(Math.min(levelled.getLevel() + 1, levelled.getMaximumLevel()));
            state.setBlockData(updated);
        }
    }
}
