package com.greatmancode.hungergame;

import me.ampayne2.ultimategames.UltimateGames;
import me.ampayne2.ultimategames.api.GamePlugin;
import me.ampayne2.ultimategames.arenas.Arena;
import me.ampayne2.ultimategames.arenas.ArenaStatus;
import me.ampayne2.ultimategames.arenas.scoreboards.ArenaScoreboard;
import me.ampayne2.ultimategames.arenas.spawnpoints.PlayerSpawnPoint;
import me.ampayne2.ultimategames.games.Game;
import me.ampayne2.ultimategames.utils.UGUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class HungerGames extends GamePlugin {
    private UltimateGames ultimateGames;
    private Game game;
    private final Map<Arena, List<Location>> chestOpened = new HashMap<Arena, List<Location>>();
    private final Map<Arena, Boolean> gracePeriods = new HashMap<Arena, Boolean>();
    private static final String ALIVE = ChatColor.GREEN + "Alive     ";
    private static final String DEAD = ChatColor.RED + "Dead          ";
    private static final Random RANDOM = new Random();

    @Override
    public boolean loadGame(UltimateGames ultimateGames, Game game) {
        this.ultimateGames = ultimateGames;
        this.game = game;
        return true;
    }

    @Override
    public void unloadGame() {
        this.ultimateGames = null;
        this.game = null;
    }

    @Override
    public boolean reloadGame() {
        return true;
    }

    @Override
    public boolean stopGame() {
        return true;
    }

    @Override
    public boolean loadArena(Arena arena) {
        chestOpened.put(arena, new ArrayList<Location>());
        return true;
    }

    @Override
    public boolean unloadArena(Arena arena) {
        chestOpened.remove(arena);
        gracePeriods.remove(arena);
        return true;
    }

    @Override
    public boolean isStartPossible(Arena arena) {
        return arena.getStatus() == ArenaStatus.OPEN;
    }

    @Override
    public boolean startArena(Arena arena) {
        chestOpened.get(arena).clear();
        return true;
    }

    @Override
    public boolean beginArena(final Arena arena) {
        chestOpened.get(arena).clear();
        gracePeriods.put(arena, true);
        ultimateGames.getServer().getScheduler().scheduleSyncDelayedTask(ultimateGames, new Runnable() {
            @Override
            public void run() {
                gracePeriods.put(arena, false);
            }
        }, ultimateGames.getConfigManager().getGameConfig(game).getLong("CustomValues.GracePeriodLength"));
        for (PlayerSpawnPoint spawnPoint : ultimateGames.getSpawnpointManager().getSpawnPointsOfArena(arena)) {
            spawnPoint.lock(false);
        }
        ArenaScoreboard scoreboard = ultimateGames.getScoreboardManager().createScoreboard(arena, "Status");
        scoreboard.setScore(ALIVE, arena.getPlayers().size());
        scoreboard.setScore(DEAD, 0);
        scoreboard.setVisible(true);
        for (String player : arena.getPlayers()) {
            scoreboard.addPlayer(Bukkit.getPlayer(player));
        }
        return true;
    }

    @Override
    public void endArena(Arena arena) {
        ultimateGames.getMessenger().sendGameMessage(arena, game, "End", arena.getPlayers().get(0));
    }

    @Override
    public boolean resetArena(Arena arena) {
        chestOpened.get(arena).clear();
        return true;
    }

    @Override
    public boolean openArena(Arena arena) {
        return true;
    }

    @Override
    public boolean stopArena(Arena arena) {
        chestOpened.get(arena).clear();
        return true;
    }

    @Override
    public boolean addPlayer(Player player, Arena arena) {
        if (arena.getStatus() == ArenaStatus.OPEN && arena.getPlayers().size() >= arena.getMinPlayers() && !ultimateGames.getCountdownManager().hasStartingCountdown(arena)) {
            ultimateGames.getCountdownManager().createStartingCountdown(arena, ultimateGames.getConfigManager().getGameConfig(game).getInt("CustomValues.StartWaitTime"));
        }

        for (PlayerSpawnPoint spawnPoint : ultimateGames.getSpawnpointManager().getSpawnPointsOfArena(arena)) {
            spawnPoint.lock(false);
        }

        List<PlayerSpawnPoint> spawnPoints = ultimateGames.getSpawnpointManager().getDistributedSpawnPoints(arena, arena.getPlayers().size());
        for (int i = 0; i < arena.getPlayers().size(); i++) {
            PlayerSpawnPoint spawnPoint = spawnPoints.get(i);
            spawnPoint.lock(true);
            spawnPoint.teleportPlayer(Bukkit.getPlayerExact(arena.getPlayers().get(i)));
        }

        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(potionEffect.getType());
        }

        resetInventory(player);
        return true;
    }

    @Override
    public void removePlayer(Player player, Arena arena) {
        ultimateGames.getMessenger().sendGameMessage(arena, game, "leave", player.getDisplayName());
        if (arena.getPlayers().size() <= 1) {
            ultimateGames.getArenaManager().endArena(arena);
        }
    }

    @Override
    public boolean addSpectator(Player player, Arena arena) {
        ultimateGames.getSpawnpointManager().getSpectatorSpawnPoint(arena).teleportPlayer(player);
        resetInventory(player);
        return true;
    }

    @Override
    public void removeSpectator(Player player, Arena arena) {
    }

    public void resetInventory(Player player) {
        player.getInventory().clear();
        player.updateInventory();
    }

    @Override
    public void onPlayerDeath(Arena arena, PlayerDeathEvent event) {
        Player player = event.getEntity();
        UGUtils.autoRespawn(player);
        if (arena.getStatus() == ArenaStatus.RUNNING) {
            ultimateGames.getPlayerManager().makePlayerSpectator(player);
            String playerName = player.getName();
            Player killer = player.getKiller();
            if (killer != null) {
                String killerName = killer.getName();
                ultimateGames.getMessenger().sendGameMessage(arena, game, "Kill", killerName, playerName);
                ultimateGames.getPointManager().addPoint(game, killerName, "kill", 1);
                ultimateGames.getPointManager().addPoint(game, killerName, "store", 2);
            } else {
                ultimateGames.getMessenger().sendGameMessage(arena, game, "Death", playerName);
            }
            ArenaScoreboard scoreboard = ultimateGames.getScoreboardManager().getScoreboard(arena);
            scoreboard.setScore(ALIVE, scoreboard.getScore(ALIVE) - 1);
            scoreboard.setScore(DEAD, scoreboard.getScore(DEAD) + 1);
            ultimateGames.getPointManager().addPoint(game, playerName, "death", 1);
            if (arena.getPlayers().size() <= 1) {
                ultimateGames.getArenaManager().endArena(arena);
            }
        }
    }

    @Override
    public void onInventoryOpen(Arena arena, InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof Chest && !chestOpened.get(arena).contains(((Chest) event.getInventory().getHolder()).getLocation())) {
            chestOpened.get(arena).add(((Chest) event.getInventory().getHolder()).getLocation());
            event.getInventory().clear();
            List<String> drops = ultimateGames.getConfigManager().getGameConfig(game).getStringList("CustomValues.items");
            int amount = 2 + RANDOM.nextInt(4);
            boolean[] slotsList = new boolean[event.getInventory().getSize()];
            for (int i = 0; i <= amount; i++) {
                boolean ok = false;
                int slot = 0;
                while (!ok) {
                    slot = RANDOM.nextInt(event.getInventory().getSize());
                    if (!slotsList[slot]) {
                        slotsList[slot] = true;
                        ok = true;
                    }
                }
                event.getInventory().setItem(slot, new ItemStack(Material.matchMaterial(drops.get(RANDOM.nextInt(drops.size())))));
            }
        }
    }

    @Override
    public void onEntityDamageByEntity(Arena arena, EntityDamageByEntityEvent event) {
        if (!gracePeriods.containsKey(arena) || gracePeriods.get(arena)) {
            event.setCancelled(true);
        }
    }
}