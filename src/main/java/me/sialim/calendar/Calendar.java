package me.sialim.calendar;

import net.advancedplugins.seasons.api.AdvancedSeasonsAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.*;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class Calendar extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    public AdvancedSeasonsAPI api = new AdvancedSeasonsAPI();
    public PlayerDataManager playerDataManager;
    public PlayerAgeManager playerAgeManager;
    private static final String DATE_FOLDER = "world_dates/";
    private static final String TICK_FILE = "world_ticks/";
    private static final String PLAYER_BIRTHDAY_FILE = "player_birthdays.txt";
    private Map<String, LocalDate> worldDates = new HashMap<>();
    private Map<String, Long> lastDayTicks = new HashMap<>();
    private boolean isPaused = false;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadDates();
        loadLastDayTicks();

        playerDataManager = new PlayerDataManager(this);
        playerAgeManager = new PlayerAgeManager(this);

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
                        //updateSeasons(world, getWorldDate(world));
                    }
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (calculateAge(p.getUniqueId(), true) < 15) {
                            playerAgeManager.setSize(p.getUniqueId(), calculateSize(p.getUniqueId()));
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isPaused) {
                    for (World world : getServer().getWorlds()) {
                        updateSeasons(world, getWorldDate(world));
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1200L);

        getCommand("setdate").setExecutor(this);
        getCommand("pause").setExecutor(this);
        getCommand("resume").setExecutor(this);

        getCommand("temperature").setExecutor(this);
        getCommand("temperature").setTabCompleter(this);

        getCommand("date").setExecutor(this);
        getCommand("date").setTabCompleter(this);

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        saveDates();
        saveLastDayTicks();
        playerDataManager.savePlayerData();
        playerAgeManager.savePlayerAges();
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

    public String getFormattedDate(World world, Player p) {
        LocalDate date = worldDates.getOrDefault(world.getName(), LocalDate.of(476, 1, 1));
        PlayerDataManager.PlayerPreferences preferences = playerDataManager.getPlayerPreferences(p.getUniqueId());

        DateTimeFormatter formatter;
        try {
            formatter = DateTimeFormatter.ofPattern(preferences.getDateFormat());
        } catch (IllegalArgumentException e) {
            formatter = DateTimeFormatter.ofPattern("MMMM/d/yyyy");
        }
        return date.format(formatter);
    }

    public String getFormattedTime(World world, Player p) {
        long currentTick = world.getTime();
        long ticksInDay = currentTick % 24000;

        long adjustedTicks = (ticksInDay + 6000) % 24000;

        long totalMinutes = (adjustedTicks * 1440) / 24000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        PlayerDataManager.PlayerPreferences preferences = playerDataManager.getPlayerPreferences(p.getUniqueId());
        String timeFormat = preferences.getTimeFormat().equals("24") ? "HH:mm" : "h:mm a";

        LocalTime time = LocalTime.of((int) hours, (int) minutes);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);

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
        if (sender instanceof Player p) {
            UUID uuid = p.getUniqueId();
            PlayerDataManager.PlayerPreferences preferences = playerDataManager.getPlayerPreferences(uuid);

            if (command.getName().equals("date") && args.length == 2 && args[0].equals("format")) {
                preferences.setDateFormat(args[1]);
                playerDataManager.setPlayerPreferences(uuid, preferences);
                p.sendMessage(ChatColor.GREEN + "Date format set to " + args[1]);
                return true;
            }
            if (command.getName().equals("temperature") && args.length == 2 && args[0].equals("set")) {
                String unit = args[1].toLowerCase();
                if (unit.equals("celsius") || unit.equals("fahrenheit")) {
                    preferences.setTemperatureUnit(unit);
                    playerDataManager.setPlayerPreferences(uuid, preferences);
                    p.sendMessage(ChatColor.GREEN + "Temperature unit set to " + unit);
                    return true;
                } else {
                    p.sendMessage(ChatColor.RED + "Invalid temperature unit. Use 'celsius' or 'fahrenheit'.");
                    return false;
                }
            }
            if (command.getName().equals("date") && args.length == 2 && args[0].equals("time")) {
                if (args[1].equals("12") || args[1].equals("24")) {
                    preferences.setTimeFormat(args[1]);
                    playerDataManager.setPlayerPreferences(uuid, preferences);
                    p.sendMessage(ChatColor.GREEN + "Time format set to " + args[1] + "-hour");
                    return true;
                } else {
                    p.sendMessage(ChatColor.RED + "Invalid time format. Use '12' or '24'.");
                    return false;
                }
            }
        }
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

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("temperature")) {
            if (args.length == 1) {
                return Arrays.asList("set");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
                return Arrays.asList("celsius", "fahrenheit");
            }
        } else if (command.getName().equalsIgnoreCase("date")) {
            if (args.length == 1) {
                return Arrays.asList("format", "time");
            } else if (args.length == 2 && args[0].equals("format")) {
                return Arrays.asList("dd/MM/yyy", "MM/dd/yyy", "yyy-MM-dd");
            } else if (args.length == 2 && args[0].equals("time")) {
                return Arrays.asList("12", "24");
            }
        }

        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        String command = e.getMessage().toLowerCase();

        if (command.startsWith("/time")) {
            e.getPlayer().sendMessage("The /time command is disabled on this server.");
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onTimeSkip(TimeSkipEvent e) {
        if (e.getSkipReason() == TimeSkipEvent.SkipReason.NIGHT_SKIP) {
            World w = e.getWorld();
            long currentTime = w.getTime();

            if (currentTime < 18000) {
                LocalDate date = worldDates.getOrDefault(w.getName(), LocalDate.of(476, 1, 1));
                date = date.plusDays(1);
                worldDates.put(w.getName(), date);
            }
        }
    }

    public LocalDate getWorldDate(World world) {
        return worldDates.getOrDefault(world.getName(), LocalDate.of(476, 1, 1));
    }

    private void updateSeasons(World world, LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        String command = "advancedseasons setseason ";

        if ((month == 12 && day < 23)) {
            command += "FALL_TRANSITION_3";
        } else if ((month == 12 && day >= 23) || (month == 1 && day < 14)) {
            command += "WINTER";
        } else if ((month == 1 && day >= 14) || (month == 2 && day < 5)) {
            command += "WINTER_TRANSITION_1";
        } else if (month == 2 && day >= 5) {
            command += "WINTER_TRANSITION_2";
        }

        else if ((month == 3 && day < 24)) {
            command += "WINTER_TRANSITION_3";
        } else if ((month == 3 && day >= 24) || (month == 4 && day < 16)) {
            command += "SPRING";
        } else if ((month == 4 && day >= 16) && (month == 5 && day < 9)) {
            command += "SPRING_TRANSITION_1";
        } else if (month == 5 && day >= 9) {
            command += "SPRING_TRANSITION_2";
        }

        else if ((month == 6 && day < 24)) {
            command += "SPRING_TRANSITION_3";
        } else if ((month == 6 && day >= 24) && (month == 7 && day < 17)) {
            command += "SUMMER";
        } else if ((month == 7 && day >= 17) && (month == 8 && day < 9)) {
            command += "SUMMER_TRANSITION_1";
        } else if (month == 8 && day >= 9) {
            command += "SUMMER_TRANSITION_2";
        }

        else if ((month == 9 && day < 24)) {
            command += "SUMMER_TRANSITION_3";
        } else if ((month == 9 && day >= 24) && (month == 10 && day < 17)) {
            command += "FALL";
        } else if ((month == 10 && day >= 17) && (month == 11 && day < 9)) {
            command += "FALL_TRANSITION_1";
        } else if (month == 11 && day >= 9) {
            command += "FALL_TRANSITION_2";
        } else {
            return;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command + " " + world.getName());
    }

    public String calculateAge(UUID uuid) {
        LocalDate birthDate = playerAgeManager.getPlayerAge(uuid);
        LocalDate currentDate = getWorldDate(Bukkit.getPlayer(uuid).getWorld());


        if (birthDate.equals(currentDate)) {
            return "0 days";
        }

        int years = currentDate.getYear() - birthDate.getYear();
        int days = currentDate.getDayOfYear() - birthDate.getDayOfYear();

        if (birthDate.getDayOfYear() > currentDate.getDayOfYear()) {
            years--;
            days += birthDate.lengthOfYear();
        }

        String yearText = years == 1 ? "1 year" : years + " years";
        String dayText = days == 1 ? "1 day" : days + " days";

        if (years > 0) {
            return yearText + " " + ((days > 0) ? dayText : "");
        } else {
            return days > 0 ? dayText : "Born today";
        }
    }

    public int calculateAge(UUID uuid, boolean bool) {
        LocalDate birthDate = playerAgeManager.getPlayerAge(uuid);
        LocalDate currentDate = getWorldDate(Bukkit.getPlayer(uuid).getWorld());

        return currentDate.getYear() - birthDate.getYear();
    }

    public float calculateSize(UUID uuid) {
        LocalDate birthDate = playerAgeManager.getPlayerAge(uuid);
        LocalDate currentDate = getWorldDate(Bukkit.getPlayer(uuid).getWorld());

        int currentAge = currentDate.getYear() - birthDate.getYear();
        int maxAge = 15;
        float minSize = 0.7f;

        currentAge = Math.min(currentAge, maxAge);

        double size = (playerAgeManager.getPlayerMaxSize(uuid) - minSize) * Math.sqrt((1.0 / maxAge) * currentAge) + minSize;
        return (float) size;
    }

    @EventHandler public void onPlayerJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        LocalDate birthdate = playerAgeManager.getPlayerAge(uuid);

        if (birthdate == null || playerAgeManager.getPlayerMaxSize(uuid) == 0.0f) {
            birthdate = getWorldDate(e.getPlayer().getWorld());
            playerAgeManager.setPlayerAge(uuid, birthdate);
            playerAgeManager.rerollMaxSize(uuid);
            playerAgeManager.savePlayerAges();
        }
    }

    @EventHandler public void onPlayerRespawn(PlayerRespawnEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        LocalDate birthdate = getWorldDate(e.getPlayer().getWorld());
        playerAgeManager.setPlayerAge(uuid, birthdate);
        playerAgeManager.rerollMaxSize(uuid);
        playerAgeManager.savePlayerAges();
    }
}
