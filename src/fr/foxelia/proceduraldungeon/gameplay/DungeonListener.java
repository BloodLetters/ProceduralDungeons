package fr.foxelia.proceduraldungeon.gameplay;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Listens for game events related to active dungeon instances.
 */
public class DungeonListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        UUID entityId = entity.getUniqueId();

        // Check if this entity belongs to a dungeon instance
        for (DungeonInstance instance : DungeonInstanceManager.getInstance().getActiveInstances()) {
            if (!instance.isActive()) continue;

            RoomInstance currentRoom = instance.getCurrentRoom();
            if (currentRoom == null) continue;

            if (currentRoom.getSpawnedMobs().contains(entityId)) {
                currentRoom.onMobDeath(entityId);

                // Check if room is cleared
                if (currentRoom.isCleared()) {
                    instance.onRoomCleared();
                }
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        DungeonInstance instance = DungeonInstanceManager.getInstance().getPlayerInstance(player);

        if (instance != null && instance.isActive()) {
            // Cancel death and handle dungeon failure
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

            // Schedule respawn and dungeon end
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                    fr.foxelia.proceduraldungeon.Main.getProceduralDungeon(),
                    () -> {
                        player.spigot().respawn();
                        instance.onPlayerDeath(player);
                    }, 1L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DungeonInstance instance = DungeonInstanceManager.getInstance().getPlayerInstance(player);

        if (instance != null && instance.isActive()) {
            instance.endSession();
        }
    }
}
