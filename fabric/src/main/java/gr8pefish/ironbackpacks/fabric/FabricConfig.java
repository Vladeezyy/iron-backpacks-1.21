package gr8pefish.ironbackpacks.fabric;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import gr8pefish.ironbackpacks.IronBackpacks;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The upgrade switches on Fabric: config/ironbackpacks-startup.json (NeoForge: ironbackpacks-startup.toml), read once
 * at start because they decide which upgrades get registered. Written with every upgrade on if it's missing.
 */
public final class FabricConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String[] UPGRADES = {"damage_bar", "lock", "extra_upgrade", "everlasting"};
    private static Map<String, Boolean> upgrades;

    public static boolean upgradeEnabled(String upgrade) {
        if (upgrades == null)
            load();
        return upgrades.getOrDefault(upgrade, true);
    }

    private static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(IronBackpacks.MODID + "-startup.json");
        Map<String, Boolean> read = new LinkedHashMap<>();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Map<String, Map<String, Boolean>> json = GSON.fromJson(reader, new TypeToken<Map<String, Map<String, Boolean>>>() {}.getType());
                if (json != null && json.get("upgrades") != null)
                    read.putAll(json.get("upgrades"));
            } catch (Exception e) {
                IronBackpacks.LOGGER.error("Could not read {}, using the defaults", file, e);
            }
        }
        for (String upgrade : UPGRADES)
            read.putIfAbsent(upgrade, true);
        upgrades = read;
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(Map.of("upgrades", upgrades), writer);
        } catch (Exception e) {
            IronBackpacks.LOGGER.error("Could not write {}", file, e);
        }
    }

    private FabricConfig() {}
}
