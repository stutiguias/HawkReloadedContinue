package uk.co.oliwali.HawkEye;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Class for parsing managing player's {@PlayerSession}s
 * @author oliverw92
 */
public class SessionManager {

	private static final HashMap<String, PlayerSession> playerSessions = new HashMap<String, PlayerSession>();

	public SessionManager() {

		//Add console session
		addSession(Bukkit.getServer().getConsoleSender());

        //Create player sessions
        for (Player player : Bukkit.getServer().getOnlinePlayers()) addSession(player);

	}

	/**
	 * Get a PlayerSession from the list
	 */
	public static PlayerSession getSession(CommandSender player) {
		PlayerSession session = playerSessions.get(getSessionKey(player));
		if (session == null)
			session = addSession(player);
		session.setSender(player);
		return session;
	}

	/**
	 * Adds a PlayerSession to the list
	 */
	public static PlayerSession addSession(CommandSender player) {
		String key = getSessionKey(player);
		PlayerSession session;
		if (playerSessions.containsKey(key)) {
			session = playerSessions.get(key);
			session.setSender(player);
		}
		else {
			session = new PlayerSession(player);
			playerSessions.put(key, session);
		}
		return session;
	}

	/**
	 * Removes a PlayerSession to avoid memory leaks
	 */
	public static void removeSession(CommandSender player) {
		playerSessions.remove(getSessionKey(player));
	}

	private static String getSessionKey(CommandSender sender) {
		if (sender instanceof Player) {
			return ((Player) sender).getUniqueId().toString();
		}

		return sender.getName();
	}
}
