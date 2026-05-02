package uk.co.oliwali.HawkEye.database.userqueries;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import uk.co.oliwali.HawkEye.DataType;
import uk.co.oliwali.HawkEye.SearchParser;
import uk.co.oliwali.HawkEye.callbacks.QueryCallback;
import uk.co.oliwali.HawkEye.database.DataManager;
import uk.co.oliwali.HawkEye.entry.DataEntry;
import uk.co.oliwali.HawkEye.querybuilder.QueryBuilder;
import uk.co.oliwali.HawkEye.util.Config;
import uk.co.oliwali.HawkEye.util.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Threadable class for performing a search query
 * Used for in-game searches and rollbacks
 *
 * @author oliverw92
 */
public class SearchQuery extends Query<QueryCallback, List<DataEntry>> {

    public SearchQuery(DataManager dataManager, QueryCallback callBack, SearchParser parser, Query.SearchDir dir) {
        super(dataManager, callBack, parser, dir);
    }

    public SearchQuery(QueryCallback callBack, SearchParser parser, Query.SearchDir dir) {
        super(callBack, parser, dir);
    }

    @Override
    protected QueryBuilder initializeQueryBuilder() {
        return new QueryBuilder("SELECT D.*, W.world, P.player_name " +
                "FROM " + Config.DbHawkEyeTable + " D " +
                "INNER JOIN " + Config.DbWorldTable + " W ON W.world_id=D.world_id " +
                "LEFT JOIN " + Config.DbPlayerTable + " P ON P.player_uuid=D.player_uuid");
    }


    @Override
    protected List<DataEntry> executeQuery(Connection conn, PreparedStatement stmnt) throws Exception {
        List<DataEntry> results = new ArrayList<>();

        try (ResultSet res = stmnt.executeQuery()) {

            Util.debug("Getting results");

            DataType type;

            //Retrieve results
            while (res.next()) {

                type = DataType.fromId(res.getInt("action"));

                String playerUuid = res.getString("player_uuid");
                String playerName = res.getString("player_name");
                if (playerName == null) playerName = resolvePlayerName(playerUuid);

                results.add(
                        type.getEntryConstructor().newInstance(
                                playerUuid,
                                playerName,
                                res.getTimestamp("timestamp"),
                                res.getInt("data_id"),
                                type,
                                res.getString("data"),
                                res.getString("world"),
                                res.getInt("x"),
                                res.getInt("y"),
                                res.getInt("z")
                        ));
            }
        }

        Util.debug(results.size() + " results found");

        return results;
    }

    private String resolvePlayerName(String uuid) {
        if (uuid == null) return null;
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            return offlinePlayer.getName();
        } catch (IllegalArgumentException e) {
            return uuid;
        }
    }
}
