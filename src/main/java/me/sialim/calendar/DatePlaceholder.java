package me.sialim.calendar;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.UUID;
import java.util.List;

public class DatePlaceholder extends PlaceholderExpansion {
    private final Calendar plugin;

    public DatePlaceholder(Calendar plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "calendar";
    }

    @Override
    public @NotNull String getAuthor() {
        return "sialim";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        PlayerDataManager.PlayerPreferences preferences = plugin.playerDataManager.getPlayerPreferences(player.getUniqueId());
        if (identifier.startsWith("formatted_date_world_")) {
            String worldName = identifier.substring("formatted_date_world_".length());
            World world = Bukkit.getServer().getWorld(worldName);
            if (world != null) {
                LocalDate date = plugin.getWorldDate(world);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(preferences.getDateFormat());
                return plugin.getFormattedDate(world);
            }
        } else if (identifier.startsWith("formatted_time_world")) {
            String worldName = identifier.substring("formatted_date_world_".length());
            World world = Bukkit.getServer().getWorld(worldName);
            if (world != null) {
                return plugin.getFormattedTime(world, player);
            }
        } else if (identifier.equals("temperature")) {
            int temperature = plugin.api.getTemperature(player);
            String temperatureUnit = preferences.getTemperatureUnit();
            if ("fahrenheit".equalsIgnoreCase(temperatureUnit)) {
                temperature = (temperature * 9 / 5) + 32;
            }
            return temperature + "*" + (temperatureUnit.equalsIgnoreCase("fahrenheit") ? " Fahrenheit" : " Celsius");
        } else if (identifier.equals("localized_time")) {
            String format = preferences.getDateFormat().replace("DD","dd");
            format = format.replace("mm", "MM");
            if (format.isEmpty()) return "Invalid date format";

            try {
                World world = player.getWorld();
                LocalDate date = plugin.getWorldDate(world);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

                return date.format(formatter);
            } catch (IllegalArgumentException | UnsupportedTemporalTypeException e) {
                return "Invalid date format pattern";
            }
        } else if (identifier.equals("age")) {
            UUID uuid = player.getUniqueId();
            return plugin.calculateAge(uuid);
        } else if (identifier.equals("birthday")) {
            UUID uuid = player.getUniqueId();
            LocalDate birthdate = plugin.playerAgeManager.getPlayerAge(uuid);
            String format = preferences.getDateFormat().replace("DD","dd");
            format = format.replace("mm", "MM");
            if (format.isEmpty()) return "Invalid date format";
            try {

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

                return birthdate.format(formatter);
            } catch (IllegalArgumentException | UnsupportedTemporalTypeException e) {
                return "Invalid date format pattern";
            }
        } else if (identifier.equals("season")) {
            String season = plugin.api.getSeason(player.getWorld());
            String[] message = season.split(" ");
            return Character.toUpperCase(message[0].charAt(0)) + message[0].substring(1).toLowerCase();
        }
        return null;
    }
}
