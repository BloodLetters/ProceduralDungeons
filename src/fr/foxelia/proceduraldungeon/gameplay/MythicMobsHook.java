package fr.foxelia.proceduraldungeon.gameplay;

import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Direct integration with MythicMobs 5.x API.
 * MythicMobs is a soft dependency — check isAvailable() before calling.
 */
public class MythicMobsHook {

    private static Boolean available = null;

    /**
     * Check if MythicMobs is available on the server.
     */
    public static boolean isAvailable() {
        if (available == null) {
            available = Bukkit.getPluginManager().getPlugin("MythicMobs") != null;
        }
        return available;
    }

    /**
     * Spawn a MythicMob at the given location.
     *
     * @param mobId    The MythicMobs internal name
     * @param location The spawn location
     * @return The spawned entity, or null if failed
     */
    public static Entity spawnMob(String mobId, Location location) {
        if (!isAvailable()) {
            // Fallback to vanilla
            try {
                org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(mobId.toUpperCase());
                return location.getWorld().spawnEntity(location, type);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().log(Level.WARNING,
                        "[ProceduralDungeon] Unknown mob type '" + mobId + "' and MythicMobs is not installed.");
                return null;
            }
        }

        try {
            Optional<MythicMob> mythicMob = MythicProvider.get().getMobManager().getMythicMob(mobId);
            if (mythicMob.isEmpty()) {
                Bukkit.getLogger().log(Level.WARNING,
                        "[ProceduralDungeon] MythicMob '" + mobId + "' not found in registry.");
                return null;
            }

            ActiveMob activeMob = mythicMob.get().spawn(BukkitAdapter.adapt(location), 1);
            if (activeMob != null && activeMob.getEntity() != null) {
                return activeMob.getEntity().getBukkitEntity();
            }
            return null;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING,
                    "[ProceduralDungeon] Failed to spawn MythicMob '" + mobId + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Get a list of all available MythicMob type names.
     *
     * @return Sorted list of mob internal names, or empty list if MythicMobs is not available
     */
    public static List<String> getAvailableMobs() {
        if (!isAvailable()) return Collections.emptyList();

        try {
            List<String> names = new ArrayList<>(
                    MythicProvider.get().getMobManager().getMobNames()
            );
            Collections.sort(names);
            return names;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING,
                    "[ProceduralDungeon] Failed to get MythicMobs list: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
