package me.sialim.calendar;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
        if (identifier.startsWith("formatted_date_world_")) {
            String worldName = identifier.substring("formatted_date_world_".length());
            World world = Bukkit.getServer().getWorld(worldName);
            if (world != null) {
                return plugin.getFormattedDate(world);
            }
        } else if (identifier.startsWith("formatted_time_world")) {
            String worldName = identifier.substring("formatted_date_world_".length());
            World world = Bukkit.getServer().getWorld(worldName);
            if (world != null) {
                return plugin.getFormattedTime(world);
            }
        }
        return null;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (identifier.startsWith("formatted_date_world_")) {
            String worldName = identifier.substring("formatted_date_world_".length());
            World world = Bukkit.getServer().getWorld(worldName);
            if (world != null) {
                return plugin.getFormattedDate(world);
            }
        } else if (identifier.startsWith("formatted_time_world")) {
            String worldName = identifier.substring("formatted_date_world_".length());
            World world = Bukkit.getServer().getWorld(worldName);
            if (world != null) {
                return plugin.getFormattedTime(world);
            }
        }
        return null;
    }
}
