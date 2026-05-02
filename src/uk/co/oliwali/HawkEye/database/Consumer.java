package uk.co.oliwali.HawkEye.database;

import uk.co.oliwali.HawkEye.entry.DataEntry;
import uk.co.oliwali.HawkEye.util.Config;
import uk.co.oliwali.HawkEye.util.Util;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author bob7l
 */
public class Consumer implements Runnable, Closeable {

    private final String WORLD_NAME_COLUMN = "world";

    private final String WORLD_ID_COLUMN = "world_id";

    private final LinkedBlockingQueue<DataEntry> queue = new LinkedBlockingQueue<>();

    private final IdMapCache<Integer> worldDb;

    private DataManager dataManager;

    private ConnectionManager connectionManager;

    private final ConcurrentHashMap<String, String> playerNameCache = new ConcurrentHashMap<>();

    private AtomicBoolean busy = new AtomicBoolean(false);

    public Consumer(DataManager dataManager) {
        this.dataManager = dataManager;

        this.connectionManager = dataManager.getConnectionManager();
        this.worldDb = dataManager.getWorldCache();
    }

    /**
     * Adds a {@link DataEntry} to the database queue.
     * {Rule}s are checked at this point
     *
     * @param entry {@link DataEntry} to be added
     */
    public void addEntry(DataEntry entry) {

        if (!entry.getType().isLogged()) return;

        if (Config.IgnoreWorlds.contains(entry.getWorld())) return;

        queue.add(entry);
    }

    public LinkedBlockingQueue<DataEntry> getQueue() {
        return queue;
    }

    public boolean isBusy() {
        return busy.get();
    }

    @Override
    public void run() {
        if (queue.isEmpty() || !busy.compareAndSet(false, true)) return;

        if (queue.size() > 70000)
            Util.info("HawkEye consumer can't keep up! Current Queue: " + queue.size());

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmnt = conn.prepareStatement("INSERT IGNORE into `" + Config.DbHawkEyeTable + "` (timestamp, player_uuid, action, world_id, x, y, z, data, data_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

            Map<String, String> playersToUpsert = new LinkedHashMap<>();

            for (int i = 0; i < queue.size(); i++) {

                DataEntry entry = queue.poll();

                if (entry == null || entry.getPlayerUuid() == null && entry.getPlayer() == null) {
                    Util.debug("Entry without actor, skipping");
                    continue;
                }

                int worldId = dataManager.getKeyId(worldDb, Config.DbWorldTable, WORLD_ID_COLUMN, WORLD_NAME_COLUMN, entry.getWorld());

                if (worldId < 0) {
                    Util.debug("World '" + entry.getWorld() + "' not found, skipping entry");
                    continue;
                }

                // Track player name changes for hawk_players upsert
                if (entry.getPlayerUuid() != null && entry.getPlayer() != null) {
                    String cachedName = playerNameCache.get(entry.getPlayerUuid());
                    if (!entry.getPlayer().equals(cachedName)) {
                        playersToUpsert.put(entry.getPlayerUuid(), entry.getPlayer());
                    }
                }

                stmnt.setTimestamp(1, entry.getTimestamp());
                stmnt.setString(2, entry.getPlayerUuid());
                stmnt.setInt(3, entry.getType().getId());
                stmnt.setInt(4, worldId);
                stmnt.setDouble(5, entry.getX());
                stmnt.setDouble(6, entry.getY());
                stmnt.setDouble(7, entry.getZ());
                stmnt.setString(8, entry.getSqlData());
                stmnt.setInt(9, entry.getDataId());

                stmnt.addBatch();

                if (i % 1000 == 0) stmnt.executeBatch();
            }

            stmnt.executeBatch();
            conn.commit();

            // Upsert new/renamed players into hawk_players
            if (!playersToUpsert.isEmpty()) {
                try (PreparedStatement playerStmnt = conn.prepareStatement(
                        "INSERT INTO `" + Config.DbPlayerTable + "` (player_uuid, player_name) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name)")) {
                    for (Map.Entry<String, String> e : playersToUpsert.entrySet()) {
                        playerStmnt.setString(1, e.getKey());
                        playerStmnt.setString(2, e.getValue());
                        playerStmnt.addBatch();
                    }
                    playerStmnt.executeBatch();
                    conn.commit();
                }
                playerNameCache.putAll(playersToUpsert);
            }

        } catch (Exception ex) {
            Util.warning(ex.getMessage());
            ex.printStackTrace();
        } finally {
            busy.set(false);
        }
    }

    @Override
    public void close() {
        while (!queue.isEmpty()) {
            run();
        }
    }
}
