package fr.foxelia.proceduraldungeon.gameplay;

import org.bukkit.entity.Player;

import java.util.*;

/**
 * Singleton manager for all active dungeon instances.
 */
public class DungeonInstanceManager {

    private static DungeonInstanceManager instance;

    private final Map<String, DungeonInstance> activeInstances;
    private final Map<UUID, DungeonInstance> playerInstances;

    private DungeonInstanceManager() {
        this.activeInstances = new HashMap<>();
        this.playerInstances = new HashMap<>();
    }

    public static DungeonInstanceManager getInstance() {
        if (instance == null) {
            instance = new DungeonInstanceManager();
        }
        return instance;
    }

    /**
     * Create and start a new dungeon instance.
     */
    public DungeonInstance createInstance(fr.foxelia.proceduraldungeon.utilities.DungeonManager dungeon, Player player) {
        // Check if player is already in a dungeon
        if (playerInstances.containsKey(player.getUniqueId())) {
            return null;
        }

        DungeonInstance dungeonInstance = new DungeonInstance(dungeon, player);
        if (!dungeonInstance.start()) {
            return null;
        }

        activeInstances.put(dungeonInstance.getInstanceId(), dungeonInstance);
        playerInstances.put(player.getUniqueId(), dungeonInstance);
        return dungeonInstance;
    }

    /**
     * Get the dungeon instance a player is currently in.
     */
    public DungeonInstance getPlayerInstance(Player player) {
        return playerInstances.get(player.getUniqueId());
    }

    /**
     * Check if a player is currently in a dungeon.
     */
    public boolean isPlayerInDungeon(Player player) {
        return playerInstances.containsKey(player.getUniqueId());
    }

    /**
     * Remove an instance (called when session ends).
     */
    public void removeInstance(DungeonInstance dungeonInstance) {
        activeInstances.remove(dungeonInstance.getInstanceId());
        for (Player player : dungeonInstance.getPlayers()) {
            playerInstances.remove(player.getUniqueId());
        }
    }

    /**
     * Get all active instances.
     */
    public Collection<DungeonInstance> getActiveInstances() {
        return Collections.unmodifiableCollection(activeInstances.values());
    }

    /**
     * Cleanup all instances (called on plugin disable).
     */
    public void cleanupAll() {
        for (DungeonInstance di : new ArrayList<>(activeInstances.values())) {
            di.endSession();
        }
        activeInstances.clear();
        playerInstances.clear();
    }
}
