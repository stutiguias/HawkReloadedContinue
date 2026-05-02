package uk.co.oliwali.HawkEye.blocks;

import org.bukkit.Material;
import uk.co.oliwali.HawkEye.blocks.blockhandlers.*;
import uk.co.oliwali.HawkEye.util.BlockUtil;

public class BlockHandlerContainer {

    private final BlockHandler air;
    private final BlockHandler leaf;
    private final BlockHandler container;
    private final BlockHandler sign;
    private final BlockHandler bed;
    private final BlockHandler door;
    private final BlockHandler plant;
    private final BlockHandler tallPlant;
    private final BlockHandler attached;
    private final BlockHandler top;
    private final BlockHandler basic;
    private final BlockHandler vine;
    private final BlockHandler piston;
    private final BlockHandler doublePlant;
    private final BlockHandler defaultHandler;

    public BlockHandlerContainer() {

        tallPlant = new TallPlantHandler(this);
        defaultHandler = new DefaultBlockHandler(this);
        top = new TopBlockHandler();
        attached = new AttachedBlockHandler();
        basic = new BasicBlockHandler();
        vine = new VineBlockHandler();
        bed = new BedBlockHandler();
        door = new DoorBlockHandler();
        plant = new PlantHandler();
        container = new ContainerBlockHandler();
        sign = new SignBlockHandler(this);
        piston = new PistonBlockHandler();
        leaf = new LeafBlockHandler(this);
        doublePlant = new DoublePlantHandler();
        air = new AirHandler();
    }

    public BlockHandler getBlockHandler(Material material) {
        if (material == null) {
            return defaultHandler;
        }

        if (material.isAir()) {
            return air;
        }

        if (isLeaf(material)) {
            return leaf;
        }

        if (isContainer(material)) {
            return container;
        }

        if (BlockUtil.isSign(material)) {
            return sign;
        }

        if (isBed(material)) {
            return bed;
        }

        if (isDoor(material)) {
            return door;
        }

        if (isCrop(material)) {
            return plant;
        }

        if (isTallPlant(material)) {
            return tallPlant;
        }

        if (isDoublePlant(material)) {
            return doublePlant;
        }

        if (material == Material.VINE) {
            return vine;
        }

        if (isPiston(material)) {
            return piston;
        }

        if (isAttached(material)) {
            return attached;
        }

        if (isTopBlock(material)) {
            return top;
        }

        if (isBasicBlock(material)) {
            return basic;
        }

        return defaultHandler;
    }

    private boolean isLeaf(Material material) {
        return material.name().endsWith("_LEAVES");
    }

    private boolean isContainer(Material material) {
        String name = material.name();
        return name.equals("CHEST")
                || name.equals("TRAPPED_CHEST")
                || name.equals("FURNACE")
                || name.equals("BLAST_FURNACE")
                || name.equals("SMOKER")
                || name.equals("DISPENSER")
                || name.equals("DROPPER")
                || name.equals("HOPPER")
                || name.equals("BARREL")
                || name.endsWith("_SHULKER_BOX");
    }

    private boolean isBed(Material material) {
        return material.name().endsWith("_BED");
    }

    private boolean isDoor(Material material) {
        return material.name().endsWith("_DOOR");
    }

    private boolean isCrop(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, PUMPKIN_STEM, MELON_STEM, ATTACHED_MELON_STEM, ATTACHED_PUMPKIN_STEM -> true;
            default -> false;
        };
    }

    private boolean isTallPlant(Material material) {
        return switch (material) {
            case CACTUS, SUGAR_CANE, BAMBOO, KELP_PLANT, KELP -> true;
            default -> false;
        };
    }

    private boolean isDoublePlant(Material material) {
        return switch (material) {
            case SUNFLOWER, LILAC, ROSE_BUSH, PEONY, TALL_GRASS, LARGE_FERN -> true;
            default -> false;
        };
    }

    private boolean isPiston(Material material) {
        String name = material.name();
        return name.equals("PISTON") || name.equals("STICKY_PISTON") || name.equals("MOVING_PISTON") || name.equals("PISTON_HEAD");
    }

    private boolean isAttached(Material material) {
        String name = material.name();
        return name.contains("TORCH")
                || name.endsWith("_BUTTON")
                || name.equals("LEVER")
                || name.equals("LADDER")
                || name.contains("RAIL")
                || name.endsWith("_TRAPDOOR")
                || name.equals("COCOA")
                || name.equals("TRIPWIRE_HOOK");
    }

    private boolean isTopBlock(Material material) {
        String name = material.name();
        return name.endsWith("_SAPLING")
                || name.contains("RAIL")
                || name.endsWith("_PRESSURE_PLATE")
                || name.endsWith("_CARPET")
                || name.equals("FIRE")
                || name.equals("REDSTONE_WIRE")
                || name.equals("REPEATER")
                || name.equals("COMPARATOR")
                || name.equals("NETHER_WART")
                || name.equals("LILY_PAD")
                || name.endsWith("_MUSHROOM")
                || name.endsWith("_FLOWER")
                || name.equals("DANDELION")
                || name.equals("POPPY")
                || name.equals("DEAD_BUSH")
                || name.equals("SHORT_GRASS")
                || name.equals("FERN")
                || name.equals("SNOW")
                || name.equals("CAKE");
    }

    private boolean isBasicBlock(Material material) {
        return material.isBlock();
    }

}
