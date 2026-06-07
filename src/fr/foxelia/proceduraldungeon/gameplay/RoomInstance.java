package fr.foxelia.proceduraldungeon.gameplay;

import fr.foxelia.proceduraldungeon.Main;
import fr.foxelia.proceduraldungeon.utilities.rooms.Room;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Represents a single room within an active dungeon instance.
 * Tracks spawned mobs, clear state, and exit barrier.
 */
public class RoomInstance {

    private final Room room;
    private final Location location;
    private final Location center;
    private final int index;
    private final List<UUID> spawnedMobs;
    private final List<Location> barrierBlocks;
    private boolean cleared;
    private Location exitPoint;

    public RoomInstance(Room room, Location location, int index, Location center) {
        this.room = room;
        this.location = location;
        this.center = center;
        this.index = index;
        this.spawnedMobs = new ArrayList<>();
        this.barrierBlocks = new ArrayList<>();
        this.cleared = false;
    }

    public Room getRoom() {
        return room;
    }

    public Location getLocation() {
        return location;
    }

    public Location getCenter() {
        return center;
    }

    public int getIndex() {
        return index;
    }

    public boolean isCleared() {
        return cleared;
    }

    public List<UUID> getSpawnedMobs() {
        return spawnedMobs;
    }

    public Location getExitPoint() {
        return exitPoint;
    }

    public void setExitPoint(Location exitPoint) {
        this.exitPoint = exitPoint;
    }

    /**
     * Place barrier blocks at the exit point to prevent player from advancing.
     * Creates a 3x3 wall of barrier blocks at the exit.
     */
    public void placeBarrier(World world) {
        if (exitPoint == null) return;

        // Place a 5 wide x 4 tall barrier wall at exit point
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                Location barrierLoc = exitPoint.clone().add(dx, dy, 0);
                // Also try perpendicular direction
                Location barrierLocX = exitPoint.clone().add(0, dy, dx);

                // Determine barrier orientation based on which blocks are air
                Block blockZ = world.getBlockAt(barrierLoc);
                Block blockX = world.getBlockAt(barrierLocX);

                if (blockZ.getType() == Material.AIR || blockZ.getType() == Material.CAVE_AIR) {
                    blockZ.setType(Material.BARRIER);
                    barrierBlocks.add(barrierLoc);
                }
                if (blockX.getType() == Material.AIR || blockX.getType() == Material.CAVE_AIR) {
                    blockX.setType(Material.BARRIER);
                    barrierBlocks.add(barrierLocX);
                }
            }
        }
    }

    /**
     * Remove all barrier blocks (called when room is cleared).
     */
    public void removeBarrier(World world) {
        for (Location loc : barrierBlocks) {
            Block block = world.getBlockAt(loc);
            if (block.getType() == Material.BARRIER) {
                block.setType(Material.AIR);
            }
        }
        barrierBlocks.clear();
    }

    /**
     * Spawn all configured mobs for this room at the center with slight random spread.
     */
    public void spawnMobs(World world) {
        List<String> mobs = room.getMobsAsList();
        if (mobs == null || mobs.isEmpty()) {
            cleared = true;
            for (org.bukkit.entity.Player player : world.getPlayers()) {
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&eTidak ada mobs yang ditemukan pada room " + (index + 1) + ". Melanjutkan..."));
            }
            return;
        }

        Random rand = new Random();

        for (String mobId : mobs) {
            double offsetX = (rand.nextDouble() - 0.5) * 4.0;
            double offsetZ = (rand.nextDouble() - 0.5) * 4.0;
            Location spawnLoc = center.clone().add(offsetX, 1, offsetZ);
            spawnLoc.setWorld(world);

            Entity entity = MythicMobsHook.spawnMob(mobId, spawnLoc);
            if (entity != null) {
                spawnedMobs.add(entity.getUniqueId());
            } else {
                Main.getMain().getLogger().log(Level.WARNING,
                        "Failed to spawn mob '" + mobId + "' in room " + index);
            }
        }

        if (spawnedMobs.isEmpty()) {
            cleared = true;
        }
    }

    /**
     * Check if all spawned mobs are dead.
     */
    public boolean areAllMobsDead() {
        if (cleared) return true;
        if (spawnedMobs.isEmpty()) {
            cleared = true;
            return true;
        }

        for (UUID mobId : new ArrayList<>(spawnedMobs)) {
            Entity entity = Bukkit.getEntity(mobId);
            if (entity != null && !entity.isDead() && entity instanceof LivingEntity living) {
                if (living.getHealth() > 0) {
                    return false;
                }
            }
        }

        cleared = true;
        return true;
    }

    /**
     * Remove a mob from tracking (called on mob death).
     */
    public void onMobDeath(UUID entityId) {
        spawnedMobs.remove(entityId);
        if (spawnedMobs.isEmpty()) {
            cleared = true;
        }
    }
}
