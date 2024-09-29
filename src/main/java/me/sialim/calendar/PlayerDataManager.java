package me.sialim.calendar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private final Calendar plugin;
    private final File dataFile;

    public PlayerDataManager (Calendar plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerData.txt");
        loadPlayerData();
    }

    private Map<UUID, PlayerPreferences> playerPreferences = new HashMap<>();

    private void loadPlayerData() {
        try {
            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }
            List<String> lines = Files.readAllLines(Paths.get(dataFile.getPath()));
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    UUID uuid = UUID.fromString(parts[0]);
                    String dateFormat = parts[1];
                    String temperatureUnit = parts[2];
                    String timeFormat = parts[3];
                    playerPreferences.put(uuid, new PlayerPreferences(dateFormat, temperatureUnit, timeFormat));
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load player data: " + e.getMessage());
        }
    }

    public void savePlayerData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile))) {
            for (Map.Entry<UUID, PlayerPreferences> entry : playerPreferences.entrySet()) {
                UUID uuid = entry.getKey();
                PlayerPreferences preferences = entry.getValue();
                writer.write(uuid + "," + preferences.getDateFormat() + "," + preferences.getTemperatureUnit() + "," + preferences.getTimeFormat());
                writer.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data: " + e.getMessage());
        }
    }

    public PlayerPreferences getPlayerPreferences(UUID uuid) {
        return playerPreferences.getOrDefault(uuid, new PlayerPreferences("YYYY-MM-DD", "Celsius", "24"));
    }

    public void setPlayerPreferences(UUID uuid, String dateFormat, String temperatureUnit, String timeFormat) {
        playerPreferences.put(uuid, new PlayerPreferences(dateFormat, temperatureUnit, timeFormat));
    }

    public void setPlayerPreferences(UUID uuid, PlayerPreferences preferences) {
        playerPreferences.put(uuid, preferences);
    }

    public class PlayerPreferences {
        private String dateFormat = "YYYY/MM/DD";
        private String temperatureUnit = "Celsius";
        private String timeFormat = "24";

        public PlayerPreferences(String dateFormat, String temperatureUnit, String timeFormat) {
            this.dateFormat = dateFormat;
            this.temperatureUnit = temperatureUnit;
            this.timeFormat = timeFormat;
        }

        public String getDateFormat() {
            return dateFormat;
        }

        public void setDateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
        }

        public String getTemperatureUnit() {
            return temperatureUnit;
        }

        public void setTemperatureUnit(String unit) {
            this.temperatureUnit = unit;
        }

        public String getTimeFormat() {
            return timeFormat;
        }

        public void setTimeFormat(String timeFormat) {
            this.timeFormat = timeFormat;
        }
    }
}
