package uk.co.oliwali.HawkEye;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import uk.co.oliwali.HawkEye.util.BlockUtil;
import uk.co.oliwali.HawkEye.util.Config;
import uk.co.oliwali.HawkEye.util.Util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Class for parsing HawkEye arguments ready to be used by an instance of {@SearchQuery}
 * @author oliverw92
 */
public class SearchParser {

	public CommandSender player = null;
	public List<String> players = new ArrayList<String>();
	public Vector loc = null;
	public Vector minLoc = null;
	public Vector maxLoc = null;
	public Integer radius = null;
	public List<DataType> actions = new ArrayList<DataType>();
	public String[] worlds = null;
	public String dateFrom = null;
	public String dateTo = null;
	public String[] filters = null;

	public SearchParser() { }

	public SearchParser(CommandSender player) {
		this.player = player;
	}

	public SearchParser(CommandSender player, int radius) {
		this.player = player;
		this.radius = radius;
		parseLocations();
	}

	public SearchParser(CommandSender player, String[] args) throws IllegalArgumentException {
		this.player = player;

		String lastParam = "";
		boolean paramSet = false;
		boolean worldedit = false;

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.isEmpty()) continue;

			if (!paramSet) {
				if (arg.length() < 2)
					throw new IllegalArgumentException("Invalid argument format: &7" + arg);
				if (!arg.substring(1,2).equals(":")) {
					if (arg.contains(":"))
						throw new IllegalArgumentException("Invalid argument format: &7" + arg);

					// No arg specified, treat as player
					players.add(arg);
					continue;
				}


				lastParam = arg.substring(0,1).toLowerCase();
				paramSet = true;

				if (arg.length() == 2) {
					if (i == (args.length - 1)) // No values specified
						throw new IllegalArgumentException("Invalid argument format: &7" + arg);
					else // User put a space between the colon and value
						continue;
				}

				// Get values out of argument
				arg = arg.substring(2);
			}

			if (paramSet) {
				if (arg.isEmpty()) {
					throw new IllegalArgumentException("Invalid argument format: &7" + lastParam + ":");
				}

				String[] values = arg.split(",");

				// Players
				if (lastParam.equals("p")) for (String p : values) players.add(p);
				// Worlds
				else if (lastParam.equals("w")) worlds = values;
				// Filters
				else if (lastParam.equals("f")) {
					if (filters != null) filters = Util.concat(filters, values);
					else filters = values;
				}
				// Blocks
				else if (lastParam.equals("b")) {
					for (int j = 0; j < values.length; j++) {
						boolean excluded = values[j].startsWith("!");
						String rawValue = excluded ? values[j].substring(1) : values[j];
						String normalized = BlockUtil.normalizeBlockString(rawValue);

						if (normalized != null) {
							values[j] = (excluded ? "!" : "") + "%" + normalized + "%";
						}
					}

					if (filters != null) filters = Util.concat(filters, values);
					else filters = values;
				}
				// Actions
				else if (lastParam.equals("a")) {
					for (String value : values) {
						DataType type = DataType.fromName(value);
						if (type == null) throw new IllegalArgumentException("Invalid action supplied: &7" + value);
						if (!Util.hasPerm(player, "search." + type.getConfigName().toLowerCase())) throw new IllegalArgumentException("You do not have permission to search for: &7" + type.getConfigName());
						actions.add(type);
					}
				}
				// EditSpeed
				else if (lastParam.equals("s")) {
					if (!Util.isInteger(values[0])) throw new IllegalArgumentException("Invalid edit-speed supplied: &7" + values[0]);
					
					int speed = Integer.parseInt(values[0]);
					
					if (speed > Config.MaxEditSpeed) throw new IllegalArgumentException("Max edit-speed: &7" + Config.MaxEditSpeed);
					SessionManager.getSession(player).setEditSpeed(speed);
				}
				// Location
				else if (lastParam.equals("l")) {
					if (values[0].equalsIgnoreCase("here"))
						if (player instanceof Entity) {
							loc = ((Entity) player).getLocation().toVector();
						} else {
							throw new IllegalArgumentException("Invalid location: &7here");
						}
					else {
						loc = new Vector();
						loc.setX(Integer.parseInt(values[0]));
						loc.setY(Integer.parseInt(values[1]));
						loc.setZ(Integer.parseInt(values[2]));
					}
				}
				// Radius
				else if (lastParam.equals("r")) {
					if (!Util.isInteger(values[0])) {
						if (player instanceof Player && (values[0].equalsIgnoreCase("we") || values[0].equalsIgnoreCase("worldedit")) && hasWorldEdit()) {
							Object sel = getWorldEditSelection((Player) player);
							int lRadius = (int) Math.ceil(invokeNumber(sel, "getLength") / 2.0);
							int wRadius = (int) Math.ceil(invokeNumber(sel, "getWidth") / 2.0);
							int hRadius = (int) Math.ceil(invokeNumber(sel, "getHeight") / 2.0);

							if (Config.MaxRadius != 0 && (lRadius > Config.MaxRadius || wRadius > Config.MaxRadius || hRadius > Config.MaxRadius))
								throw new IllegalArgumentException("Selection too large, max radius: &7" + Config.MaxRadius);

							worldedit = true;
							minLoc = getVectorPoint(sel, "getMinimumPoint");
							maxLoc = getVectorPoint(sel, "getMaximumPoint");
						} else if (values[0].equals("*")) {
							if (!player.hasPermission("hawkeye.override"))
								throw new IllegalArgumentException("You do not have permission to override the MaxRadius!");
							radius = -1;
						} else {
							throw new IllegalArgumentException("Invalid radius supplied: &7" + values[0]);
						}

					} else {
						radius = Integer.parseInt(values[0]);
						if (Config.MaxRadius != 0 && radius > Config.MaxRadius)
							throw new IllegalArgumentException("Radius too large, max allowed: &7" + Config.MaxRadius);
						if (radius < 0)
							throw new IllegalArgumentException("Radius too small");
					}
				}
				//Time
				else if (lastParam.equals("t")) {

					int type = 2;
					boolean isTo = false;
					for (int j = 0; j < arg.length(); j++) {
						String c = arg.substring(j, j+1);
						if (!Util.isInteger(c)) {
							if (c.equals("m") || c .equals("s") || c.equals("h") || c.equals("d") || c.equals("w")) {
								type = 0;
							}
							if (c.equals("-") || c.equals(":"))
								type = 1;
						}
					}

					//If the time is in the format '0w0d0h0m0s'
					if (type == 0) {

						int weeks = 0;
						int days = 0;
						int hours = 0;
						int mins = 0;
						int secs = 0;

						String nums = "";
						for (int j = 0; j < values[0].length(); j++) {
							String c = values[0].substring(j, j+1);
							if (c.equals("!")) { //If the number has a ! infront of it the time inverts
								c = values[0].substring(j, j+2).replace("!", "");
								isTo = true;
							} else {
								if (Util.isInteger(c)) {
									nums += c;
									continue;
								} 

								int num = Integer.parseInt(nums);
								if (c.equals("w")) weeks = num;
								else if (c.equals("d")) days = num;
								else if (c.equals("h")) hours = num;
								else if (c.equals("m")) mins = num;
								else if (c.equals("s")) secs = num;
								else throw new IllegalArgumentException("Invalid time measurement: &7" + c);
								nums = "";
							}
						}

						Calendar cal = Calendar.getInstance();
						cal.add(Calendar.WEEK_OF_YEAR, -1 * weeks);
						cal.add(Calendar.DAY_OF_MONTH, -1 * days);
						cal.add(Calendar.HOUR, -1 * hours);
						cal.add(Calendar.MINUTE, -1 * mins);
						cal.add(Calendar.SECOND, -1 * secs);
						SimpleDateFormat form = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						if (isTo)
							dateTo = form.format(cal.getTime());
						else
							dateFrom = form.format(cal.getTime());
					}

					//If the time is in the format 'yyyy-MM-dd HH:mm:ss'
					else if (type == 1) {
						if (values.length == 1) {
							SimpleDateFormat form = new SimpleDateFormat("yyyy-MM-dd");
							dateFrom = form.format(Calendar.getInstance().getTime()) + " " + values[0];
						}
						if (values.length >= 2)
							dateFrom = values[0] + " " + values[1];
						if (values.length == 4)
							dateTo = values[2] + " " + values[3];
					}
					//Invalid time format
					else if (type == 2)
						throw new IllegalArgumentException("Invalid time format!");

				}
				else throw new IllegalArgumentException("Invalid parameter supplied: &7" + lastParam);

				paramSet = false;
			}
		}

		//Sort out locations
		if (!worldedit) parseLocations();
	}

	/**
	 * Formats min and max locations if the radius is set
	 */
	private void parseLocations() {

		if (!(player instanceof Player)) return;

		// Check if there is a max radius
		if (radius == null && Config.MaxRadius != 0) radius = Config.MaxRadius;

		//If the radius is set we need to format the min and max locations
		if (radius != null && radius > 0) {

			//Check if location and world are supplied
			if (loc == null) loc = ((Player) player).getLocation().toVector();
			if (worlds == null) worlds = new String[]{ ((Player) player).getWorld().getName() };

			//Format min and max
			minLoc = new Vector(loc.getX() - radius, loc.getY() - radius, loc.getZ() - radius);
			maxLoc = new Vector(loc.getX() + radius, loc.getY() + radius, loc.getZ() + radius);

		}

	}

	private boolean hasWorldEdit() {
		return Bukkit.getPluginManager().getPlugin("WorldEdit") != null;
	}

	private Object getWorldEditSelection(Player player) {
		Object worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");
		if (worldEdit == null) {
			throw new IllegalArgumentException("WorldEdit is not installed");
		}

		try {
			Object session = invokeObject(worldEdit, "getSession", player);
			Object adaptedWorld = adaptWorldEditWorld(player);

			if (adaptedWorld != null) {
				try {
					return invokeObject(session, "getSelection", adaptedWorld);
				} catch (ReflectiveOperationException ignored) {
				}
			}

			return invokeObject(session, "getSelection");
		} catch (InvocationTargetException ex) {
			Throwable cause = ex.getCause();
			if (cause != null && "IncompleteRegionException".equals(cause.getClass().getSimpleName())) {
				throw new IllegalArgumentException("You do not have a complete WorldEdit selection");
			}
			throw new IllegalArgumentException("Unable to read the WorldEdit selection", ex);
		} catch (ReflectiveOperationException ex) {
			throw new IllegalArgumentException("Unable to read the WorldEdit selection", ex);
		}
	}

	private Object adaptWorldEditWorld(Player player) {
		try {
			Class<?> adapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
			Method adaptMethod = adapterClass.getMethod("adapt", org.bukkit.World.class);
			return adaptMethod.invoke(null, player.getWorld());
		} catch (ReflectiveOperationException ex) {
			return null;
		}
	}

	private Object invokeObject(Object target, String methodName, Object argument) throws ReflectiveOperationException {
		for (Method method : target.getClass().getMethods()) {
			if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
				continue;
			}

			Class<?> parameterType = method.getParameterTypes()[0];
			if (parameterType.isInstance(argument) || parameterType.isAssignableFrom(argument.getClass())) {
				return method.invoke(target, argument);
			}
		}

		throw new NoSuchMethodException(target.getClass().getName() + "." + methodName + "(...)");
	}

	private Object invokeObject(Object target, String methodName) throws ReflectiveOperationException {
		Method method = target.getClass().getMethod(methodName);
		return method.invoke(target);
	}

	private double invokeNumber(Object target, String methodName) {
		try {
			Method method = target.getClass().getMethod(methodName);
			Object value = method.invoke(target);
			return ((Number) value).doubleValue();
		} catch (ReflectiveOperationException ex) {
			throw new IllegalArgumentException("Unable to read WorldEdit selection data", ex);
		}
	}

	private Vector getVectorPoint(Object target, String methodName) {
		try {
			Object point = invokeObject(target, methodName);
			return new Vector(invokeNumber(point, "x"), invokeNumber(point, "y"), invokeNumber(point, "z"));
		} catch (ReflectiveOperationException ex) {
			throw new IllegalArgumentException("Unable to read WorldEdit selection data", ex);
		}
	}

}
