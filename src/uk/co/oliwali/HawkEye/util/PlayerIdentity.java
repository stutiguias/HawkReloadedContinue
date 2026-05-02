package uk.co.oliwali.HawkEye.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class PlayerIdentity {

    private final String uuid;

    private final String name;

    public PlayerIdentity(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public static PlayerIdentity from(Player player) {
        return new PlayerIdentity(player.getUniqueId().toString(), player.getName());
    }

    public static PlayerIdentity named(String name) {
        return new PlayerIdentity(null, name);
    }

    public static PlayerIdentity resolve(String name) {
        if (name == null || name.isEmpty()) {
            return named(name);
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);

        if (offlinePlayer != null && (offlinePlayer.isOnline() || offlinePlayer.hasPlayedBefore())) {
            String resolvedName = offlinePlayer.getName() == null ? name : offlinePlayer.getName();
            return new PlayerIdentity(offlinePlayer.getUniqueId().toString(), resolvedName);
        }

        return named(name);
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public boolean hasUuid() {
        return uuid != null && !uuid.isEmpty();
    }
}
