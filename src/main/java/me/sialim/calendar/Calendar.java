package me.sialim.calendar;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public final class Calendar extends JavaPlugin implements Listener, CommandExecutor {
    private static final String DATE_FOLDER = "world_dates/";
    private static final String TICK_FILE = "world_ticks/";
    private Map<String, LocalDate> worldDates = new HashMap<>();
    private Map<String, Long> lastDayTicks = new HashMap<>();
    private boolean isPaused = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadDates();
        loadLastDayTicks();

        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DatePlaceholder(this).register();
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isPaused) {
                    for (World world : getServer().getWorlds()) {
                        updateDate(world);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);

        getCommand("setdate").setExecutor(this);
        getCommand("pause").setExecutor(this);
        getCommand("resume").setExecutor(this);

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        saveDates();
        saveLastDayTicks();
    }

    private void loadDates() {
        File folder = new File(getDataFolder(), DATE_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        for (World world : getServer().getWorlds()) {
            File file = new File(folder, world.getName() + ".txt");
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String date = reader.readLine();
                    if (date != null) {
                        worldDates.put(world.getName(), LocalDate.parse(date));
                    } else {
                        worldDates.put(world.getName(), LocalDate.of(476, 1, 1));
                    }
                } catch (IOException e) {
                    getLogger().warning("Could not load date for world: " + world.getName());
                    worldDates.put(world.getName(), LocalDate.of(476, 1, 1));
                }
            } else {
                worldDates.put(world.getName(), LocalDate.of(476, 1, 1));
            }
        }
    }

    private void saveDates() {
        File folder = new File(getDataFolder(), DATE_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        for (Map.Entry<String, LocalDate> entry : worldDates.entrySet()) {
            File file = new File(folder, entry.getKey() + ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(entry.getValue().toString());
            } catch (IOException e) {
                getLogger().warning("Could not save date for world: " + entry.getKey());
            }
        }
    }

    private void loadLastDayTicks() {
        File folder = new File(getDataFolder(), TICK_FILE);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        for (World world : getServer().getWorlds()) {
            File file = new File(folder, world.getName() + ".txt");
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String tick = reader.readLine();
                    if (tick != null) {
                        lastDayTicks.put(world.getName(), Long.parseLong(tick));
                    } else {
                        lastDayTicks.put(world.getName(), world.getFullTime());
                    }
                } catch (IOException e) {
                    getLogger().warning("Could not load last day tick for world: " + world.getName());
                    lastDayTicks.put(world.getName(), world.getFullTime());
                }
            } else {
                lastDayTicks.put(world.getName(), world.getFullTime());
            }
        }
    }

    private void saveLastDayTicks() {
        File folder = new File(getDataFolder(), TICK_FILE);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        for (Map.Entry<String, Long> entry : lastDayTicks.entrySet()) {
            File file = new File(folder, entry.getKey() + ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(entry.getValue().toString());
            } catch (IOException e) {
                getLogger().warning("Could not save last day tick for world: " + entry.getKey());
            }
        }
    }

    public String getFormattedDate(World world) {
        LocalDate date = worldDates.getOrDefault(world.getName(), LocalDate.of(476, 1, 1));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM");
        String month = date.format(formatter);
        String dayWithSuffix = getDayWithSuffix(date.getDayOfMonth());
        int year = date.getYear();
        return month + " " + dayWithSuffix + ", " + year;
    }

    public String getFormattedTime(World world) {
        long currentTick = world.getTime();
        long ticksInDay = currentTick % 24000;

        long adjustedTicks = (ticksInDay + 6000) % 24000;

        long totalMinutes = (adjustedTicks * 1440) / 24000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        LocalTime time = LocalTime.of((int) hours, (int) minutes);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");

        return time.format(formatter);
    }

    private String getDayWithSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return day + "th";
        }
        switch (day % 10) {
            case 1: return day + "st";
            case 2: return day + "nd";
            case 3: return day + "rd";
            default: return day + "th";
        }
    }

    private void updateDate(World world) {
        long currentTime = world.getTime();
        long lastUpdateTick = lastDayTicks.getOrDefault(world.getName(), -1L);

        if (currentTime >= 18000) {
            if (lastUpdateTick < 18000) {
                LocalDate date = worldDates.getOrDefault(world.getName(), LocalDate.of(476, 1, 1));

                date = date.plusDays(1);
                worldDates.put(world.getName(), date);

                lastDayTicks.put(world.getName(), currentTime);

                getLogger().info("New day in world " + world.getName() + ": " + date.toString());

                if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    new DatePlaceholder(this).register();
                }
            }
        } else {
            lastDayTicks.put(world.getName(), currentTime);
        }
    }

    public void setLastDayTick(World world, long tick) {
        lastDayTicks.put(world.getName(), tick);
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        saveDates();
        saveLastDayTicks();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        if (!worldDates.containsKey(world.getName())) {
            worldDates.put(world.getName(), LocalDate.of(476, 1, 1));
            lastDayTicks.put(world.getName(), world.getFullTime());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("pause") && sender.hasPermission("calendar.pause")) {
            isPaused = true;
            sender.sendMessage(ChatColor.GREEN + "Calendar paused.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("resume") && sender.hasPermission("calendar.resume")) {
            isPaused = false;
            sender.sendMessage(ChatColor.GREEN + "Calendar resumed.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("setdate") && sender.hasPermission("calendar.setdate")) {
            if (args.length != 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /setdate <year> <month> <day>");
                return false;
            }

            try {
                int year = Integer.parseInt(args[0]);
                int month = Integer.parseInt(args[1]);
                int day = Integer.parseInt(args[2]);

                LocalDate newDate = LocalDate.of(year, month, day);

                for (World world : getServer().getWorlds()) {
                    worldDates.put(world.getName(), newDate);
                }

                sender.sendMessage(ChatColor.GREEN + "Date set to " + newDate.toString() + " for all worlds.");
                return true;

            } catch (NumberFormatException | DateTimeException e) {
                sender.sendMessage(ChatColor.RED + "Invalid date. Make sure to input valid numbers.");
                return false;
            }
        }

        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        String command = e.getMessage().toLowerCase();

        if (command.startsWith("/time")) {
            e.getPlayer().sendMessage("The /time command is disabled on this server.");
            e.setCancelled(true);
        }
    }
}
