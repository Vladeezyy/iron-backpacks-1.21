package gr8pefish.ironbackpacks.neoforge;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 1.12 @Config "ironbackpacks/ironbackpacks.cfg". A startup config (config/ironbackpacks-startup.toml): the upgrade
 * switches decide which upgrades get registered, so they are read before registration and need a restart, like the
 * 1.12 registry events that read them once.
 * <p>
 * Not ported: "sizes" and "handholding.maxNests" — 1.12.2 declared them but never read them (backpack sizes come from
 * the backpack types, nesting is not implemented).
 */
public final class ConfigHandler {
    public static final ModConfigSpec SPEC;
    public static final Upgrades upgrades;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        upgrades = new Upgrades(builder);
        SPEC = builder.build();
    }

    public static final class Upgrades {
        public final ModConfigSpec.BooleanValue enableDamageBar;
        public final ModConfigSpec.BooleanValue enablePackLatch;
        public final ModConfigSpec.BooleanValue enableExtraUpgrade;
        public final ModConfigSpec.BooleanValue enableEverlasting;

        Upgrades(ModConfigSpec.Builder builder) {
            builder.push("upgrades");
            enableDamageBar = builder.comment("Enables the Damage Bar upgrade").define("enableDamageBar", true);
            enablePackLatch = builder.comment("Enables the Latch upgrade").define("enablePackLatch", true);
            enableExtraUpgrade = builder.comment("Enables the Extra Upgrade upgrade").define("enableExtraUpgrade", true);
            enableEverlasting = builder.comment("Enables the Everlasting upgrade").define("enableEverlasting", true);
            builder.pop();
        }
    }

    /** The switch of an upgrade by its id path ("damage_bar", "lock", "extra_upgrade", "everlasting"). */
    public static boolean upgradeEnabled(String upgrade) {
        return switch (upgrade) {
            case "damage_bar" -> upgrades.enableDamageBar.get();
            case "lock" -> upgrades.enablePackLatch.get();
            case "extra_upgrade" -> upgrades.enableExtraUpgrade.get();
            case "everlasting" -> upgrades.enableEverlasting.get();
            default -> true;
        };
    }

    private ConfigHandler() {}
}
