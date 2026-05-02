package uk.co.oliwali.HawkEye.database;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import uk.co.oliwali.HawkEye.HawkEye;
import uk.co.oliwali.HawkEye.util.Config;
import uk.co.oliwali.HawkEye.util.Util;

import java.sql.*;
import java.util.UUID;

/**
 * Handler for everything to do with the database.
 * All queries except searching goes through this class.
 *
 * @author oliverw92
 */

public class DataManager implements AutoCloseable {

    private final IdMapCache<Integer> worldCache = new IdMapCache<>();

    private DeleteManager deleteManager;

    private ConnectionManager connectionManager;

    private Consumer consumer;

    /**
     * Initiates database connection pool, checks tables, starts cleansing utility
     * Throws an exception if it is unable to complete setup
     *
     * @throws Exception
     */
    public DataManager() throws Exception {

        connectionManager = new ConnectionManager();

        //Check tables and update world list
        createTables();

        if (Config.populateCachesOnBoot)
            populateCaches();

        consumer = new Consumer(this);

        deleteManager = new DeleteManager(connectionManager);

        Bukkit.getScheduler().runTaskTimerAsynchronously(HawkEye.getInstance(), consumer, Config.LogDelay * 20L, Config.LogDelay * 20L);

        Bukkit.getScheduler().runTaskTimerAsynchronously(HawkEye.getInstance(), deleteManager, 20 * 15, 20 * 5);

        //Start cleansing utility
        try {
            new CleanseUtil(connectionManager);
        } catch (Exception e) {
            Util.severe(e.getMessage());
            Util.severe("Unable to start cleansing utility - check your cleanse age");
        }
    }

    public DeleteManager getDeleteManager() {
        return deleteManager;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    /**
     * Get the world cache
     */
    public IdMapCache<Integer> getWorldCache() {
        return worldCache;
    }


    /**
     * Populates world local cache
     */
    private void populateCaches() throws Exception {
        try (Connection conn = connectionManager.getConnection();
             Statement stmnt = conn.createStatement()) {

            try (ResultSet res = stmnt.executeQuery("SELECT * FROM `" + Config.DbWorldTable + "`;")) {
                while (res.next())
                    worldCache.put(res.getInt("world_id"), res.getString("world"));
            }
        }
    }

    /**
     * Gets or creates an ID for the provided value
     *
     * @return The ID for the value, or -1 if the method failed
     */
    public int getKeyId(IdMapCache<Integer> cache, String table, String idColumn, String column, String value) {
        Integer id = cache.get(value);

        if (id == null) {
            try (Connection conn = connectionManager.getConnection();
                 PreparedStatement stmnt = conn.prepareStatement("SELECT " + idColumn + " FROM " + table + " WHERE " + column + "=?")) {

                stmnt.setString(1, value);

                try (ResultSet rs = stmnt.executeQuery()) {

                    if (rs.next()) {
                        id = rs.getInt(1);

                        cache.put(id, value);

                    } else {

                        try (PreparedStatement stmnt2 = conn.prepareStatement("INSERT INTO " + table + " (" + column + ") " +
                                "VALUES (?) ON DUPLICATE KEY UPDATE " + column + "=VALUES(" + column + ");", Statement.RETURN_GENERATED_KEYS)) {

                            stmnt2.setString(1, value);

                            stmnt2.executeUpdate();

                            conn.commit();

                            try (ResultSet rs2 = stmnt2.getGeneratedKeys()) {

                                if (rs2.next()) {
                                    id = rs2.getInt(1);

                                    cache.put(id, value);
                                }
                            }
                        }
                    }
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        return (id == null ? -1 : id);
    }

    /**
     * Checks that all tables are up to date and exist
     *
     * @return true on success, false on failure
     */
    private void createTables() throws Exception {
        try (Connection conn = connectionManager.getConnection();
             Statement stmnt = conn.createStatement()) {

            String worldTable = "CREATE TABLE IF NOT EXISTS `" + Config.DbWorldTable + "` (" +
                    "`world_id` TINYINT(3) UNSIGNED NOT NULL AUTO_INCREMENT, " +
                    "`world` varchar(40) CHARACTER SET latin1 COLLATE latin1_general_ci NOT NULL, " +
                    "PRIMARY KEY (`world_id`), " +
                    "UNIQUE KEY `world` (`world`)" +
                    ") COLLATE latin1_general_ci, ENGINE = INNODB;";

            String playerTable = "CREATE TABLE `" + Config.DbPlayerTable + "` (" +
                    "`player_uuid` char(36) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL," +
                    "`player_name` varchar(40) CHARACTER SET latin1 COLLATE latin1_general_ci DEFAULT NULL," +
                    "PRIMARY KEY (`player_uuid`)" +
                    ") ENGINE = INNODB;";

            String dataTable = "CREATE TABLE `" + Config.DbHawkEyeTable + "` (" +
                    "`data_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT," +
                    "`timestamp` datetime NOT NULL," +
                    "`player_uuid` char(36) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL," +
                    "`action` TINYINT(3) UNSIGNED NOT NULL," +
                    "`world_id` TINYINT(3) UNSIGNED NOT NULL," +
                    "`x` int(11) NOT NULL," +
                    "`y` SMALLINT(6) NOT NULL," +
                    "`z` int(11) NOT NULL," +
                    "`data` varchar(500) CHARACTER SET latin1 COLLATE latin1_general_ci DEFAULT NULL," +
                    "PRIMARY KEY (`data_id`)," +
                    "KEY `timestamp` (`timestamp`)," +
                    "KEY `player_uuid` (`player_uuid`)," +
                    "KEY `action` (`action`)," +
                    "KEY `world_id` (`world_id`)," +
                    "KEY `x_y_z` (`x`,`y`,`z`)" +
                    ") COLLATE latin1_general_ci, ENGINE = INNODB;";

            DatabaseMetaData dbm = conn.getMetaData();

            //Check if tables exist
            if (!JDBCUtil.tableExists(dbm, Config.DbWorldTable)) {
                Util.info("Table `" + Config.DbWorldTable + "` not found, creating...");
                stmnt.execute(worldTable);
            }

            if (!JDBCUtil.tableExists(dbm, Config.DbHawkEyeTable)) {
                Util.info("Table `" + Config.DbHawkEyeTable + "` not found, creating...");
                stmnt.execute(dataTable);
            }

            //This will print an error if the user does not have SUPER privilege
            try {
                stmnt.execute("SET GLOBAL innodb_flush_log_at_trx_commit = 2");
                stmnt.execute("SET GLOBAL sync_binlog = 0");
            } catch (Exception e) {
                Util.debug("HawkEye does not have enough privileges for setting global settings");
            }

            boolean needsDataMigration;

            try (ResultSet rs = stmnt.executeQuery("SHOW FIELDS FROM `" + Config.DbHawkEyeTable + "` where Field ='action'")) {
                needsDataMigration = rs.next() && (
                        !rs.getString(2).contains("tinyint") ||
                                JDBCUtil.columnExists(dbm, Config.DbHawkEyeTable, "plugin") ||
                                JDBCUtil.columnExists(dbm, Config.DbHawkEyeTable, "player_id") ||
                                !JDBCUtil.columnExists(dbm, Config.DbHawkEyeTable, "player_uuid")
                );
            }

            boolean playerTableIsLegacy = JDBCUtil.tableExists(dbm, Config.DbPlayerTable) &&
                    JDBCUtil.columnExists(dbm, Config.DbPlayerTable, "player_id");
            boolean playerTableIsNew = JDBCUtil.tableExists(dbm, Config.DbPlayerTable) &&
                    JDBCUtil.columnExists(dbm, Config.DbPlayerTable, "player_uuid");

            if (needsDataMigration) {
                Util.info("Migrating " + Config.DbHawkEyeTable + " to UUID-only player identity...");
                migrateDataTable(conn, stmnt, dbm, dataTable);
                Util.info("Finished!");
            } else if (JDBCUtil.columnExists(dbm, Config.DbHawkEyeTable, "player")) {
                Util.info("Dropping legacy `player` name column from `" + Config.DbHawkEyeTable + "`...");
                stmnt.execute("ALTER TABLE `" + Config.DbHawkEyeTable + "` DROP COLUMN `player`;");
                Util.info("Finished!");
            }

            // Rebuild or create the player lookup table (UUID-keyed)
            if (playerTableIsLegacy) {
                Util.info("Replacing legacy `" + Config.DbPlayerTable + "` with UUID-keyed schema...");
                stmnt.execute("DROP TABLE `" + Config.DbPlayerTable + "`;");
                playerTableIsNew = false;
            }

            if (!playerTableIsNew) {
                Util.info("Creating player lookup table `" + Config.DbPlayerTable + "`...");
                stmnt.execute(playerTable);
                populatePlayerTable(conn);
            }

            conn.commit();
        }
    }

    private void migrateDataTable(Connection conn, Statement stmnt, DatabaseMetaData dbm, String dataTableSql) throws Exception {
        String newTable = "new" + Config.DbHawkEyeTable;
        String oldTable = "old" + Config.DbHawkEyeTable;

        if (JDBCUtil.tableExists(dbm, newTable)) {
            stmnt.execute("DROP TABLE `" + newTable + "`;");
        }

        if (JDBCUtil.tableExists(dbm, oldTable)) {
            stmnt.execute("DROP TABLE `" + oldTable + "`;");
        }

        stmnt.execute(dataTableSql.replace(Config.DbHawkEyeTable, newTable));

        boolean hasLegacyPlayerId = JDBCUtil.columnExists(dbm, Config.DbHawkEyeTable, "player_id");
        boolean hasPlayerUuid = JDBCUtil.columnExists(dbm, Config.DbHawkEyeTable, "player_uuid");
        boolean hasPlayerName = JDBCUtil.columnExists(dbm, Config.DbHawkEyeTable, "player");

        String sourceQuery;

        if (hasLegacyPlayerId) {
            if (!JDBCUtil.tableExists(dbm, Config.DbPlayerTable)) {
                throw new SQLException("Legacy player table `" + Config.DbPlayerTable + "` is required for migration.");
            }

            sourceQuery = "SELECT D.data_id, D.timestamp, NULL AS player_uuid, P.player AS player_name, D.action, D.world_id, D.x, D.y, D.z, D.data " +
                    "FROM `" + Config.DbHawkEyeTable + "` D " +
                    "LEFT JOIN `" + Config.DbPlayerTable + "` P ON P.player_id = D.player_id " +
                    "ORDER BY D.data_id ASC";
        } else if (hasPlayerUuid && hasPlayerName) {
            sourceQuery = "SELECT data_id, timestamp, player_uuid, player AS player_name, action, world_id, x, y, z, data " +
                    "FROM `" + Config.DbHawkEyeTable + "` ORDER BY data_id ASC";
        } else if (hasPlayerUuid) {
            sourceQuery = "SELECT data_id, timestamp, player_uuid, NULL AS player_name, action, world_id, x, y, z, data " +
                    "FROM `" + Config.DbHawkEyeTable + "` ORDER BY data_id ASC";
        } else if (hasPlayerName) {
            sourceQuery = "SELECT data_id, timestamp, NULL AS player_uuid, player AS player_name, action, world_id, x, y, z, data " +
                    "FROM `" + Config.DbHawkEyeTable + "` ORDER BY data_id ASC";
        } else {
            throw new SQLException("Cannot migrate `" + Config.DbHawkEyeTable + "` because no player column is available.");
        }

        try (PreparedStatement select = conn.prepareStatement(sourceQuery);
             ResultSet res = select.executeQuery();
             PreparedStatement insert = conn.prepareStatement(
                     "INSERT INTO `" + newTable + "` (`data_id`,`timestamp`,`player_uuid`,`action`,`world_id`,`x`,`y`,`z`,`data`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

            int batchCount = 0;

            while (res.next()) {
                String playerName = res.getString("player_name");
                String playerUuid = res.getString("player_uuid");

                if (playerUuid == null && playerName != null) {
                    playerUuid = resolveLegacyPlayerUuid(playerName);
                }

                insert.setInt(1, res.getInt("data_id"));
                insert.setTimestamp(2, res.getTimestamp("timestamp"));
                insert.setString(3, playerUuid);
                insert.setInt(4, res.getInt("action"));
                insert.setInt(5, res.getInt("world_id"));
                insert.setInt(6, res.getInt("x"));
                insert.setInt(7, res.getInt("y"));
                insert.setInt(8, res.getInt("z"));
                insert.setString(9, res.getString("data"));
                insert.addBatch();

                batchCount++;

                if (batchCount % 1000 == 0) {
                    insert.executeBatch();
                }
            }

            insert.executeBatch();
        }

        stmnt.execute("RENAME TABLE `" + Config.DbHawkEyeTable + "` TO `" + oldTable + "`, `" + newTable + "` TO `" + Config.DbHawkEyeTable + "`;");
        stmnt.execute("DROP TABLE `" + oldTable + "`;");
    }

    private void populatePlayerTable(Connection conn) throws SQLException {
        try (Statement sel = conn.createStatement();
             ResultSet res = sel.executeQuery(
                     "SELECT DISTINCT player_uuid FROM `" + Config.DbHawkEyeTable + "` WHERE player_uuid IS NOT NULL");
             PreparedStatement upsert = conn.prepareStatement(
                     "INSERT INTO `" + Config.DbPlayerTable + "` (player_uuid, player_name) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE player_name = IF(VALUES(player_name) IS NOT NULL, VALUES(player_name), player_name)")) {

            int count = 0;
            while (res.next()) {
                String uuid = res.getString("player_uuid");
                OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                upsert.setString(1, uuid);
                upsert.setString(2, op != null ? op.getName() : null);
                upsert.addBatch();
                if (++count % 100 == 0) upsert.executeBatch();
            }
            upsert.executeBatch();
            conn.commit();
            Util.info("Populated `" + Config.DbPlayerTable + "` with " + count + " players.");
        }
    }

    public void upsertPlayer(String uuid, String name) {
        if (uuid == null || name == null) return;
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmnt = conn.prepareStatement(
                     "INSERT INTO `" + Config.DbPlayerTable + "` (player_uuid, player_name) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name)")) {
            stmnt.setString(1, uuid);
            stmnt.setString(2, name);
            stmnt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            Util.warning("Failed to upsert player " + uuid + ": " + e.getMessage());
        }
    }

    private String resolveLegacyPlayerUuid(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return null;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

        if (offlinePlayer == null || (!offlinePlayer.isOnline() && !offlinePlayer.hasPlayedBefore())) {
            return null;
        }

        return offlinePlayer.getUniqueId().toString();
    }

    /**
     * Closes down all connections
     */
    public void close() throws Exception {
        if (connectionManager != null) {
            consumer.close();
            connectionManager.close();
        }
    }
}
