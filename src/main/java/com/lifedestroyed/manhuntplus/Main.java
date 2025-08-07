package com.lifedestroyed.manhuntplus;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.java.*;
import org.bukkit.scheduler.*;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.stream.*;

public class Main extends JavaPlugin implements Listener {

    // Game state
    private final Set<UUID> hunters = new HashSet<>();
    private final Set<UUID> speedrunners = new HashSet<>();
    private boolean gameRunning = false;
    private final Map<UUID, InventoryData> savedInventories = new HashMap<>();
    private final Map<UUID, Integer> playerScores = new HashMap<>();
    private int gameTime = 0;
    private BukkitTask gameTask;
    private BukkitTask compassTask;

    // Configuration
    private String compassName;
    private int freezeDuration;
    private int hunterSpawnRadius;
    private boolean endOnDragonKill;

    @Override
    public void onEnable() {
        // Load configuration
        saveDefaultConfig();
        loadConfig();

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Setup commands
        Objects.requireNonNull(getCommand("manhunt")).setExecutor(this);
    }

    @Override
    public void onDisable() {
        if (gameRunning) {
            endGame(false, "plugin disabled");
        }
    }

    private void loadConfig() {
        compassName = ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("compass.name", "&a&lHunter Compass"));
        freezeDuration = getConfig().getInt("game.freeze-duration", 5);
        hunterSpawnRadius = getConfig().getInt("game.hunter-spawn-radius", 20);
        endOnDragonKill = getConfig().getBoolean("game.end-on-dragon-kill", true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "hunter":
                return handleHunterCommand(player, args);
            case "speedrunner":
                return handleSpeedrunnerCommand(player, args);
            case "start":
                return handleStartCommand(player);
            case "stop":
                return handleStopCommand(player);
            case "status":
                return handleStatusCommand(player);
            default:
                showHelp(player);
                return true;
        }
    }

    private boolean handleHunterCommand(Player player, String[] args) {
        if (gameRunning) {
            player.sendMessage(ChatColor.RED + "Cannot modify hunters while game is running!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /manhunt hunter <player>");
            return true;
        }

        Player target = getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        if (speedrunners.contains(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + target.getName() + " is already a speedrunner!");
            return true;
        }

        hunters.add(target.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Added " + target.getName() + " as a hunter!");
        return true;
    }

    private boolean handleSpeedrunnerCommand(Player player, String[] args) {
        if (gameRunning) {
            player.sendMessage(ChatColor.RED + "Cannot modify speedrunners while game is running!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /manhunt speedrunner <player>");
            return true;
        }

        Player target = getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        if (hunters.contains(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + target.getName() + " is already a hunter!");
            return true;
        }

        speedrunners.add(target.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Added " + target.getName() + " as a speedrunner!");
        return true;
    }

    private boolean handleStartCommand(Player player) {
        if (gameRunning) {
            player.sendMessage(ChatColor.RED + "Game is already running!");
            return true;
        }

        if (hunters.isEmpty() || speedrunners.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Need at least one hunter and one speedrunner to start!");
            return true;
        }

        startGame();
        return true;
    }

    private boolean handleStopCommand(Player player) {
        if (!gameRunning) {
            player.sendMessage(ChatColor.RED + "No game is currently running!");
            return true;
        }

        endGame(false, "manual stop");
        player.sendMessage(ChatColor.GREEN + "Game stopped!");
        return true;
    }

    private boolean handleStatusCommand(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Manhunt Status ===");
        player.sendMessage(ChatColor.YELLOW + "Game running: " + (gameRunning ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));

        if (gameRunning) {
            player.sendMessage(ChatColor.YELLOW + "Time elapsed: " + ChatColor.WHITE + formatTime(gameTime));
        }

        player.sendMessage(ChatColor.YELLOW + "Hunters: " + ChatColor.RED +
                hunters.stream()
                        .map(uuid -> Bukkit.getPlayer(uuid))
                        .filter(Objects::nonNull)
                        .map(Player::getName)
                        .collect(Collectors.joining(", ")));

        player.sendMessage(ChatColor.YELLOW + "Speedrunners: " + ChatColor.GREEN +
                speedrunners.stream()
                        .map(uuid -> Bukkit.getPlayer(uuid))
                        .filter(Objects::nonNull)
                        .map(Player::getName)
                        .collect(Collectors.joining(", ")));

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Manhunt Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/manhunt hunter <player> - Add a hunter");
        player.sendMessage(ChatColor.YELLOW + "/manhunt speedrunner <player> - Add a speedrunner");
        player.sendMessage(ChatColor.YELLOW + "/manhunt start - Start the game");
        player.sendMessage(ChatColor.YELLOW + "/manhunt stop - Stop the game");
        player.sendMessage(ChatColor.YELLOW + "/manhunt status - Show game status");
    }

    private void startGame() {
        gameRunning = true;
        gameTime = 0;

        // Save inventories and set game modes
        for (Player player : getServer().getOnlinePlayers()) {
            savedInventories.put(player.getUniqueId(), new InventoryData(player));

            if (!hunters.contains(player.getUniqueId()) && !speedrunners.contains(player.getUniqueId())) {
                player.setGameMode(GameMode.SPECTATOR);
            } else {
                clearPlayer(player);
            }
        }

        // Teleport hunters around first speedrunner
        Player firstRunner = getServer().getPlayer(speedrunners.iterator().next());
        if (firstRunner != null) {
            Location runnerLoc = firstRunner.getLocation();

            for (UUID hunterId : hunters) {
                Player hunter = getServer().getPlayer(hunterId);
                if (hunter != null) {
                    Location hunterLoc = generateHunterSpawn(runnerLoc);
                    hunter.teleport(hunterLoc);
                }
            }
        }

        // Freeze all players
        freezePlayers(true);

        // Start countdown
        new BukkitRunnable() {
            int count = freezeDuration;

            @Override
            public void run() {
                if (count > 0) {
                    Bukkit.broadcastMessage(ChatColor.GOLD + "Game starting in " + count + "...");
                    count--;
                } else {
                    // Unfreeze players
                    freezePlayers(false);

                    // Give hunters compass
                    giveHunterCompasses();

                    // Start game tasks
                    startGameTasks();

                    Bukkit.broadcastMessage(ChatColor.GREEN + "Manhunt has begun!");
                    this.cancel();
                }
            }
        }.runTaskTimer(this, 0, 20);
    }

    private Location generateHunterSpawn(Location center) {
        double angle = Math.random() * Math.PI * 2;
        double distance = 10 + (Math.random() * hunterSpawnRadius);
        double x = center.getX() + Math.cos(angle) * distance;
        double z = center.getZ() + Math.sin(angle) * distance;

        World world = center.getWorld();
        int y = world.getHighestBlockYAt((int)x, (int)z) + 1;

        return new Location(world, x, y, z);
    }

    private void freezePlayers(boolean freeze) {
        for (Player player : getServer().getOnlinePlayers()) {
            if (hunters.contains(player.getUniqueId()) || speedrunners.contains(player.getUniqueId())) {
                if (freeze) {
                    player.setWalkSpeed(0);
                    player.setFlySpeed(0);
                    player.setInvulnerable(true);
                } else {
                    player.setWalkSpeed(0.2f);
                    player.setFlySpeed(0.1f);
                    player.setInvulnerable(false);
                }
            }
        }
    }

    private void giveHunterCompasses() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        meta.setDisplayName(compassName);
        compass.setItemMeta(meta);

        for (UUID hunterId : hunters) {
            Player hunter = getServer().getPlayer(hunterId);
            if (hunter != null) {
                hunter.getInventory().addItem(compass.clone());
            }
        }
    }

    private void startGameTasks() {
        // Game timer
        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                gameTime++;
                updateScoreboards();
            }
        }.runTaskTimer(this, 0, 20);

        // Compass updater
        compassTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateCompasses();
            }
        }.runTaskTimer(this, 0, 20);
    }

    private void updateCompasses() {
        for (UUID hunterId : hunters) {
            Player hunter = getServer().getPlayer(hunterId);
            if (hunter == null) continue;

            Player nearestRunner = getNearestSpeedrunner(hunter);
            if (nearestRunner != null) {
                hunter.setCompassTarget(nearestRunner.getLocation());
            }
        }
    }

    private Player getNearestSpeedrunner(Player hunter) {
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (UUID runnerId : speedrunners) {
            Player runner = getServer().getPlayer(runnerId);
            if (runner == null || !runner.getWorld().equals(hunter.getWorld())) continue;

            double dist = hunter.getLocation().distance(runner.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = runner;
            }
        }

        return nearest;
    }

    private void updateScoreboards() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("manhunt", "dummy", ChatColor.GOLD + "Manhunt");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        objective.getScore(ChatColor.YELLOW + "Time: " + ChatColor.WHITE + formatTime(gameTime)).setScore(3);
        objective.getScore(ChatColor.YELLOW + "Hunters: " + ChatColor.RED + hunters.size()).setScore(2);
        objective.getScore(ChatColor.YELLOW + "Speedrunners: " + ChatColor.GREEN + speedrunners.size()).setScore(1);

        for (Player player : getServer().getOnlinePlayers()) {
            player.setScoreboard(board);
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private void endGame(boolean speedrunnersWon, String reason) {
        gameRunning = false;

        // Cancel tasks
        if (gameTask != null) gameTask.cancel();
        if (compassTask != null) compassTask.cancel();

        // Restore players
        for (Player player : getServer().getOnlinePlayers()) {
            InventoryData data = savedInventories.get(player.getUniqueId());
            if (data != null) {
                data.restore(player);
            }
            player.setGameMode(GameMode.SURVIVAL);
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }

        // Clear collections
        savedInventories.clear();
        playerScores.clear();

        // Broadcast result
        if (speedrunnersWon) {
            Bukkit.broadcastMessage(ChatColor.GREEN + "Speedrunners have won the Manhunt!");
        } else {
            Bukkit.broadcastMessage(ChatColor.RED + "Hunters have won the Manhunt!");
        }

        if (!reason.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.GRAY + "Reason: " + reason);
        }

        // Clear teams
        hunters.clear();
        speedrunners.clear();
    }

    private void clearPlayer(Player player) {
        player.getInventory().clear();
        player.getEnderChest().clear();
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setFireTicks(0);
        player.setFallDistance(0);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (gameRunning) {
            if (!hunters.contains(player.getUniqueId()) && !speedrunners.contains(player.getUniqueId())) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (gameRunning) {
            if (speedrunners.contains(player.getUniqueId())) {
                endGame(false, "speedrunner left");
            } else if (hunters.contains(player.getUniqueId())) {
                // Check if all hunters left
                boolean allHuntersLeft = true;
                for (UUID hunterId : hunters) {
                    if (getServer().getPlayer(hunterId) != null) {
                        allHuntersLeft = false;
                        break;
                    }
                }
                if (allHuntersLeft) {
                    endGame(true, "all hunters left");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (gameRunning && speedrunners.contains(player.getUniqueId())) {
            endGame(false, "speedrunner died");
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (gameRunning && endOnDragonKill && event.getEntityType() == EntityType.ENDER_DRAGON) {
            endGame(true, "ender dragon killed");
        }
    }

    private static class InventoryData {
        private final ItemStack[] inventory;
        private final ItemStack[] armor;
        private final ItemStack[] enderChest;
        private final float xp;
        private final int level;

        public InventoryData(Player player) {
            this.inventory = player.getInventory().getContents();
            this.armor = player.getInventory().getArmorContents();
            this.enderChest = player.getEnderChest().getContents();
            this.xp = player.getExp();
            this.level = player.getLevel();
        }

        public void restore(Player player) {
            player.getInventory().setContents(inventory);
            player.getInventory().setArmorContents(armor);
            player.getEnderChest().setContents(enderChest);
            player.setExp(xp);
            player.setLevel(level);
        }
    }
}