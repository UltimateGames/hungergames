package com.greatmancode.hungergame;

import me.ampayne2.ultimategames.UltimateGames;
import me.ampayne2.ultimategames.api.GamePlugin;
import me.ampayne2.ultimategames.arenas.Arena;
import me.ampayne2.ultimategames.arenas.ArenaStatus;
import me.ampayne2.ultimategames.arenas.spawnpoints.PlayerSpawnPoint;
import me.ampayne2.ultimategames.games.Game;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class HungerGame extends GamePlugin {

    private UltimateGames plugin;
    private Game game;
    private Map<Arena, List<Location>> chestOpened = new HashMap<Arena, List<Location>>();
    private Random random = new Random();

    @Override
    public boolean loadGame(UltimateGames ultimateGames, Game game) {
        this.plugin = ultimateGames;
        this.game = game;
        return true;
    }

    @Override
    public void unloadGame() {
        this.plugin = null;
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
    public boolean beginArena(Arena arena) {
        chestOpened.get(arena).clear();
        for (PlayerSpawnPoint spawnPoint :plugin.getSpawnpointManager().getSpawnPointsOfArena(arena)) {
            spawnPoint.lock(false);
        }
        return true;
    }

    @Override
    public void endArena(Arena arena) {

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
        if (arena.getStatus() == ArenaStatus.OPEN && arena.getPlayers().size() >= arena.getMinPlayers() && !plugin.getCountdownManager().hasStartingCountdown(arena)) {
            plugin.getCountdownManager().createStartingCountdown(arena, plugin.getConfigManager().getGameConfig(game).getConfig().getInt("CustomValues.StartWaitTime"));
        }

        for (PlayerSpawnPoint spawnPoint : plugin.getSpawnpointManager().getSpawnPointsOfArena(arena)) {
            spawnPoint.lock(false);
        }

        List<PlayerSpawnPoint> spawnPoints = plugin.getSpawnpointManager().getDistributedSpawnPoints(arena, arena.getPlayers().size());
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
        plugin.getMessageManager().sendGameMessage(arena, game, "leave", player.getDisplayName());
        if (arena.getPlayers().size() <= 1) {
            plugin.getArenaManager().endArena(arena);
        }
    }

    @Override
    public boolean addSpectator(Player player, Arena arena) {
        plugin.getSpawnpointManager().getSpectatorSpawnPoint(arena).teleportPlayer(player);
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
        if (arena.getStatus() == ArenaStatus.RUNNING) {
            plugin.getPlayerManager().makePlayerSpectator(event.getEntity());
            String playerName = event.getEntity().getName();
            Player killer = event.getEntity().getKiller();
            String killerName = null;
            if (killer != null) {
                killerName = killer.getName();
                plugin.getMessageManager().sendGameMessage(arena, game, "Kill", killerName, event.getEntity().getName());
                plugin.getPointManager().addPoint(game, killerName, "kill", 1);
                plugin.getPointManager().addPoint(game, killerName, "store", 2);
            } else {
                plugin.getMessageManager().sendGameMessage(arena, game, "Death", event.getEntity().getName());
            }
            plugin.getPointManager().addPoint(game, event.getEntity().getName(), "death", 1);
            if (arena.getPlayers().size() <= 1) {
                plugin.getArenaManager().endArena(arena);
            }
        }
    }

    @Override
    public void onInventoryOpen(Arena arena, InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof Chest && !chestOpened.get(arena).contains(((Chest) event.getInventory().getHolder()).getLocation())) {
            chestOpened.get(arena).add(((Chest) event.getInventory().getHolder()).getLocation());
            List<String> drops = plugin.getConfigManager().getGameConfig(game).getConfig().getStringList("CustomValues.items");
            double chance = random.nextDouble();
            for (String drop: drops) {
                String[] values = drop.split(",");
                double dropChance = Double.parseDouble(values[2]);
                if (chance <= dropChance) {
                    event.getInventory().addItem(new ItemStack(Material.matchMaterial(values[0]), Integer.parseInt(values[1])));
                }
            }
        }
    }
}
