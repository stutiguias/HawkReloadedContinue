package uk.co.oliwali.HawkEye.itemserializer.entries;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * @author bob7l
 */
public class EnchantEntry implements SerializerEntry {

    @Override
    public char getKey() {
        return 'e';
    }

    @Override
    public String serialize(ItemStack item) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(entry.getKey().getKey().toString()).append(":").append(entry.getValue());
        }

        return sb.toString();
    }

    @Override
    public ItemStack applySerializedData(ItemStack item, String data) {
        String[] lines = data.split(",");

        for (String line : lines) {
            String[] e = line.split(":", 3);
            if (e.length != 3) {
                continue;
            }

            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.fromString(e[0] + ":" + e[1]));
            int level = Integer.parseInt(e[2]);
            if (enchantment != null) {
                item.addUnsafeEnchantment(enchantment, level);
            }
        }

        return item;
    }

    @Override
    public boolean isApplicable(ItemStack item) {
        return (!item.getEnchantments().isEmpty());
    }

}
