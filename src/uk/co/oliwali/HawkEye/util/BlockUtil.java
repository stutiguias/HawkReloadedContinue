package uk.co.oliwali.HawkEye.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.Levelled;
import org.bukkit.inventory.ItemStack;
import uk.co.oliwali.HawkEye.HawkEye;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Contains utilities for manipulating blocks without losing data
 * @author oliverw92
 */
public class BlockUtil {

	public static final BlockFace[] faces = new BlockFace[]{BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH};

	/**
	 * Gets the block in string form.
	 * e.g. minecraft:stone or minecraft:oak_door[facing=east,half=lower]
	 */
	public static String getBlockString(Block block) {
		return getBlockString(block.getState());
	}

	public static String getBlockString(BlockState block) {
		return getBlockString(block.getBlockData());
	}

	public static String getBlockString(BlockData blockData) {
		return blockData.getAsString(false);
	}

	public static String getBlockString(Material material) {
		return getMaterialId(material);
	}

	public static String getBlockString(Material material, int data) {
		String materialId = getMaterialId(material);
		return data > 0 ? materialId + ":" + data : materialId;
	}

	public static String getMaterialId(Material material) {
		NamespacedKey key = material.getKey();
		return key != null ? key.toString() : material.name().toLowerCase(Locale.ROOT);
	}

	public static String getItemString(ItemStack stack) {
		short data = stack.getDurability();
		return data > 0 ? getMaterialId(stack.getType()) + ":" + data : getMaterialId(stack.getType());
	}

	public static ItemStack getItemFromString(String data) {
		return itemStringToStack(data, 1);
	}

	public static String formatItemStack(ItemStack itemStack) {
		StringBuilder sb = new StringBuilder();

		sb.append("x").append(itemStack.getAmount()).append(" ");
		sb.append(formatMaterialName(itemStack.getType()));

		if (itemStack.getDurability() > 0 && itemStack.getType().getMaxDurability() == 0) {
			sb.append(":").append(itemStack.getDurability());
		}

		if (!itemStack.getEnchantments().isEmpty()) {
			sb.append(" (x").append(itemStack.getEnchantments().size()).append(" Enchants)");
		}

		return sb.toString();
	}

	private static String formatMaterialName(Material material) {
		StringBuilder formatted = new StringBuilder();

		for (String word : material.name().toLowerCase(Locale.ROOT).split("_")) {
			if (!formatted.isEmpty()) {
				formatted.append(' ');
			}

			formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}

		return formatted.toString();
	}

	public static ItemStack itemStringToStack(String item, Integer amount) {
		ParsedMaterial parsed = parseMaterial(item);
		if (parsed == null) {
			return new ItemStack(Material.AIR, amount);
		}

		ItemStack stack = new ItemStack(parsed.material, amount);
		if (parsed.data > 0) {
			stack.setDurability((short) parsed.data);
		}

		return stack;
	}

	public static String getBlockStringName(String blockData) {
		BlockData parsedBlockData = tryParseBlockData(blockData);
		if (parsedBlockData != null) {
			return formatMaterialName(parsedBlockData.getMaterial());
		}

		Material material = getMaterialFromString(blockData);
		if (material == null) {
			return blockData;
		}

		ParsedMaterial parsed = parseMaterial(blockData);
		String name = formatMaterialName(material);
		return parsed != null && parsed.data > 0 ? name + ":" + parsed.data : name;
	}

	public static void setBlockString(Block block, String blockData) {
		BlockData parsed = getBlockDataFromString(blockData);
		HawkEye.getBlockHandlerContainer().getBlockHandler(parsed.getMaterial()).restore(block, parsed);
	}

	public static Material getMaterialFromString(String string) {
		BlockData blockData = tryParseBlockData(string);
		if (blockData != null) {
			return blockData.getMaterial();
		}

		ParsedMaterial parsed = parseMaterial(string);
		return parsed == null ? null : parsed.material;
	}

	public static String normalizeBlockString(String string) {
		BlockData blockData = tryParseBlockData(string);
		if (blockData != null) {
			return getBlockString(blockData);
		}

		ParsedMaterial parsed = parseMaterial(string);
		return parsed == null ? null : getBlockString(parsed.material);
	}

	public static BlockData getBlockDataFromString(String string) {
		BlockData blockData = tryParseBlockData(string);
		if (blockData != null) {
			return blockData;
		}

		ParsedMaterial parsed = parseMaterial(string);
		if (parsed == null || !parsed.material.isBlock()) {
			return Material.AIR.createBlockData();
		}

		return parsed.material.createBlockData();
	}

	public static String getItemDataString(ItemStack stack) {
		return stack.getAmount() + "x " + getItemString(stack);
	}

	public static boolean isSign(Material material) {
		String name = material.name();
		return name.endsWith("_SIGN") || name.endsWith("_WALL_SIGN");
	}

	public static boolean isAir(String string) {
		Material material = getMaterialFromString(string);
		return material != null && material.isAir();
	}

	public static boolean isTrackedFallingBlock(Material material) {
		return switch (material) {
			case SAND, RED_SAND, GRAVEL, ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL -> true;
			default -> false;
		};
	}

	public static boolean isWater(BlockState state) {
		return state.getType() == Material.WATER;
	}

	public static boolean isLava(BlockState state) {
		return state.getType() == Material.LAVA;
	}

	public static boolean isFluidReplaceable(BlockState state) {
		Material material = state.getType();
		return material.isAir() || !material.isSolid();
	}

	public static boolean isSourceLiquid(BlockState state) {
		return state.getBlockData() instanceof Levelled levelled && levelled.getLevel() == 0;
	}

	public static boolean isAttached(Block base, Block attached) {
		if (attached.getType() == Material.VINE) {
			return true;
		}

		BlockData blockData = attached.getBlockData();
		if (blockData instanceof FaceAttachable faceAttachable) {
			return switch (faceAttachable.getAttachedFace()) {
				case FLOOR -> attached.getRelative(BlockFace.DOWN).equals(base);
				case CEILING -> attached.getRelative(BlockFace.UP).equals(base);
				case WALL -> blockData instanceof Directional directional
						&& attached.getRelative(directional.getFacing().getOppositeFace()).equals(base);
			};
		}

		if (blockData instanceof Directional directional) {
			return attached.getRelative(directional.getFacing().getOppositeFace()).equals(base);
		}

		return true;
	}

	private static BlockData tryParseBlockData(String string) {
		if (string == null) {
			return null;
		}

		String value = string.trim();
		if (value.isEmpty()) {
			return null;
		}

		try {
			return Bukkit.createBlockData(value);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private static ParsedMaterial parseMaterial(String string) {
		if (string == null) {
			return null;
		}

		String value = string.trim();
		if (value.isEmpty()) {
			return null;
		}

		Material fullMatch = matchMaterial(value);
		if (fullMatch != null) {
			return new ParsedMaterial(fullMatch, 0);
		}

		int lastColon = value.lastIndexOf(':');
		String materialToken = value;
		int data = 0;

		if (lastColon >= 0) {
			String trailing = value.substring(lastColon + 1);
			if (Util.isInteger(trailing)) {
				materialToken = value.substring(0, lastColon);
				data = Integer.parseInt(trailing);
			}
		}

		Material material = matchMaterial(materialToken);
		return material == null ? null : new ParsedMaterial(material, data);
	}

	private static Material matchMaterial(String token) {
		for (String candidate : getMaterialCandidates(token)) {
			Material material = Material.matchMaterial(candidate, false);
			if (material == null) {
				material = Material.matchMaterial(candidate, true);
			}
			if (material != null) {
				return material;
			}
		}

		return null;
	}

	private static Set<String> getMaterialCandidates(String token) {
		Set<String> candidates = new LinkedHashSet<>();
		String trimmed = token.trim();
		String lowered = trimmed.toLowerCase(Locale.ROOT);

		candidates.add(trimmed);
		candidates.add(lowered);

		String stripped = stripNamespace(lowered);
		candidates.add(stripped);
		candidates.add(stripped.toUpperCase(Locale.ROOT));

		return candidates;
	}

	private static String stripNamespace(String token) {
		int separator = token.indexOf(':');
		if (separator > 0 && token.indexOf(':', separator + 1) == -1) {
			return token.substring(separator + 1);
		}

		return token;
	}

	private static final class ParsedMaterial {
		private final Material material;
		private final int data;

		private ParsedMaterial(Material material, int data) {
			this.material = material;
			this.data = data;
		}
	}
}
