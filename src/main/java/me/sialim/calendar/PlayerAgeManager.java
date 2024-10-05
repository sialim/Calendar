package me.sialim.calendar;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import javax.swing.text.html.HTML;
import java.io.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerAgeManager {
    private final Calendar plugin;
    private File file;
    private Map<UUID, LocalDate> playerAges = new HashMap<>();
    private Map<UUID, Float> playerMaxSizes = new HashMap<>();

    public PlayerAgeManager(Calendar plugin) {
        this.plugin = plugin;
        loadPlayerAges();
    }

    private void loadPlayerAges() {
        file = new File(plugin.getDataFolder(), "player_ages.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create player ages file: " + e.getMessage());
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    UUID uuid = UUID.fromString(parts[0]);
                    LocalDate birthDate = LocalDate.parse(parts[1]);
                    float maxSize = Float.parseFloat(parts[2]);
                    playerAges.put(uuid, birthDate);
                    playerMaxSizes.put(uuid, maxSize);

                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load player ages: " + e.getMessage());
        }
    }

    public void savePlayerAges() {
        file = new File(plugin.getDataFolder(), "player_ages.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create player ages file: " + e.getMessage());
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Map.Entry<UUID, LocalDate> entry : playerAges.entrySet()) {
                UUID uuid = entry.getKey();
                LocalDate birthdate = entry.getValue();
                float maxSize = playerMaxSizes.getOrDefault(uuid, 0.0f);

                writer.write(entry.getKey().toString() + "," + birthdate.toString() + "," + maxSize);
                writer.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save player ages: " + e.getMessage());
        }
    }

    public LocalDate getPlayerAge(UUID uuid) {
        return playerAges.get(uuid);
    }

    public void setPlayerAge(UUID uuid, LocalDate birthDate) {
        playerAges.put(uuid, birthDate);
    }

    public float getPlayerMaxSize(UUID uuid) {
        return playerMaxSizes.getOrDefault(uuid, 0.0f);
    }

    public void setPlayerMaxSize(UUID uuid, float maxSize) {
        playerMaxSizes.put(uuid, maxSize);
    }

    public void rerollMaxSize(UUID uuid) {
        float newMaxSize = 0.9f + (float) (Math.random() * (1.1f - 0.9f));
        playerMaxSizes.put(uuid, newMaxSize);
    }

    public void setSize(UUID uuid, float size) {
        Player p = Bukkit.getPlayer(uuid);
        p.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(size);
    }
}
