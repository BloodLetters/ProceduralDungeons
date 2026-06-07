package fr.foxelia.proceduraldungeon.gameplay;

import fr.foxelia.proceduraldungeon.Main;
import fr.foxelia.proceduraldungeon.utilities.DungeonManager;
import fr.foxelia.proceduraldungeon.utilities.WorldEditSchematic;
import fr.foxelia.proceduraldungeon.utilities.rooms.Direction;
import fr.foxelia.proceduraldungeon.utilities.rooms.Room;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Represents an active dungeon play session.
 * Each instance runs in a unique void world and can have multiple players.
 */
public class DungeonInstance {

    private final String instanceId;
    private final DungeonManager dungeon;
    private final List<Player> players;
    private final List<RoomInstance> rooms;
    private World instanceWorld;
    private int currentRoomIndex;
    private volatile boolean active;
    private volatile boolean cleaning;

    public DungeonInstance(DungeonManager dungeon, Player player) {
        this.instanceId = UUID.randomUUID().toString().substring(0, 8);
        this.dungeon = dungeon;
        this.players = new ArrayList<>();
        this.players.add(player);
        this.rooms = new ArrayList<>();
        this.currentRoomIndex = 0;
        this.active = false;
        this.cleaning = false;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public DungeonManager getDungeon() {
        return dungeon;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public World getInstanceWorld() {
        return instanceWorld;
    }

    public int getCurrentRoomIndex() {
        return currentRoomIndex;
    }

    public boolean isActive() {
        return active;
    }

    public List<RoomInstance> getRooms() {
        return rooms;
    }

    public RoomInstance getCurrentRoom() {
        if (currentRoomIndex >= 0 && currentRoomIndex < rooms.size()) {
            return rooms.get(currentRoomIndex);
        }
        return null;
    }

    /**
     * Start the dungeon instance — create world, generate rooms, teleport player.
     */
    public boolean start() {
        String worldName = "dungeon_" + instanceId;
        WorldCreator creator = new WorldCreator(worldName);
        creator.type(WorldType.FLAT);
        creator.generatorSettings("{\"layers\": [], \"biome\": \"the_void\"}");
        creator.generateStructures(false);

        instanceWorld = Bukkit.createWorld(creator);
        if (instanceWorld == null) {
            Main.getMain().getLogger().log(Level.SEVERE, "Failed to create dungeon instance world: " + worldName);
            return false;
        }

        // Optimize world settings for dungeon — reduce unnecessary ticking
        instanceWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        instanceWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        instanceWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        instanceWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
        instanceWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        instanceWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
        instanceWorld.setGameRule(GameRule.MOB_GRIEFING, false);
        instanceWorld.setGameRule(GameRule.DO_TILE_DROPS, false);
        instanceWorld.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        instanceWorld.setTime(6000);
        instanceWorld.setAutoSave(false); // No need to save dungeon worlds

        // Generate dungeon rooms
        if (!generateRooms()) {
            unloadAndDeleteWorld();
            return false;
        }

        // Teleport player to the start
        Location spawnLoc = new Location(instanceWorld, 0, 65, 0);
        for (Player player : players) {
            player.teleport(spawnLoc);
            player.setGameMode(GameMode.ADVENTURE);
        }

        active = true;

        // Spawn mobs in first room after a short delay to let chunks load
        Bukkit.getScheduler().runTaskLater(Main.getProceduralDungeon(), () -> {
            RoomInstance firstRoom = getCurrentRoom();
            if (firstRoom != null && active) {
                firstRoom.spawnMobs(instanceWorld);
                // Place barrier at first room's exit if it has mobs
                if (!firstRoom.isCleared()) {
                    firstRoom.placeBarrier(instanceWorld);
                }
                // Auto-advance if room has no mobs
                if (firstRoom.isCleared()) {
                    onRoomCleared();
                }
            }
        }, 20L);

        return true;
    }

    /**
     * Generate all rooms in sequence: Start Room -> Procedural Rooms -> Boss Room.
     * 
     * Pattern: L-shaped rooms only appear every 2-5 straight rooms.
     * L-shaped rooms are randomly flipped (mirror) to go left OR right.
     * This ensures the dungeon never loops back on itself.
     */
    private boolean generateRooms() {
        Location spawnLoc = new Location(instanceWorld, 0, 64, 0);
        Direction currentDirection = Direction.NORTH;
        Random rand = new Random();

        // --- Start Room ---
        String startRoomName = dungeon.getConfig().getString("startroom", "");
        if (startRoomName != null && !startRoomName.isEmpty()) {
            Room startRoom = findRoomByName(startRoomName);
            if (startRoom != null) {
                currentDirection = pasteRoom(startRoom, spawnLoc, currentDirection, 0);
                if (currentDirection == null) return false;
            }
        }

        // --- Procedural Rooms ---
        List<Room> used = new ArrayList<>();
        List<Room> masked = new ArrayList<>();
        int roomCount = dungeon.getConfig().getInt("roomcount");

        // Exclude start and boss rooms from procedural pool
        String bossRoomName = dungeon.getConfig().getString("bossroom", "");
        List<Room> allRooms = new ArrayList<>(dungeon.getDungeonRooms().getRooms());
        allRooms.removeIf(r -> {
            String name = r.getFile().getName().replace(".dungeon", "");
            return name.equals(startRoomName) || name.equals(bossRoomName);
        });

        // Separate into linear and L-shaped rooms
        List<Room> linearRooms = new ArrayList<>();
        List<Room> lShapedRooms = new ArrayList<>();
        for (Room r : allRooms) {
            if (r.getEntranceDirection().opposite() == r.getExitDirection()) {
                linearRooms.add(r); // entrance and exit are opposite = straight
            } else {
                lShapedRooms.add(r); // entrance and exit are perpendicular = L-shape
            }
        }

        if(Main.getMain().getConfig().getBoolean("debug")) Main.getMain().getLogger().info("[Dungeon] Linear rooms: " + linearRooms.size() + ", L-shaped rooms: " + lShapedRooms.size());

        // Counter: how many straight rooms since last L-shape
        int straightCount = 0;
        int nextLAt = 2 + rand.nextInt(4); // L-shape after 2-5 straight rooms
        // Track last turn direction to alternate (prevent spiraling)
        boolean lastTurnedRight = rand.nextBoolean();

        for (int i = 0; i < roomCount; i++) {
            boolean useLShape = false;

            // Decide if this should be an L-shaped room
            if (!lShapedRooms.isEmpty() && straightCount >= nextLAt) {
                useLShape = true;
                straightCount = 0;
                nextLAt = 2 + rand.nextInt(4); // next L after 2-5 more straight
            }

            Room roomchoosen = null;

            if (useLShape) {
                // Pick an L-shaped room and flip it if needed
                roomchoosen = pickRoom(lShapedRooms, used, masked, rand, dungeon.getConfig().getBoolean("roomrecyling"));
                if (roomchoosen == null) {
                    // No L-shape available, use linear instead
                    roomchoosen = pickRoom(linearRooms, used, masked, rand, dungeon.getConfig().getBoolean("roomrecyling"));
                    straightCount++;
                }
            } else {
                // Pick a linear room
                roomchoosen = pickRoom(linearRooms, used, masked, rand, dungeon.getConfig().getBoolean("roomrecyling"));
                if (roomchoosen == null) {
                    // No linear available, try L-shape
                    roomchoosen = pickRoom(lShapedRooms, used, masked, rand, dungeon.getConfig().getBoolean("roomrecyling"));
                }
                straightCount++;
            }

            if (roomchoosen == null) {
                Main.getMain().getLogger().log(Level.SEVERE, "Unable to generate " + dungeon.getName() + ". No rooms available.");
                return false;
            }

            int roomIndex = rooms.size();

            // For L-shaped rooms: decide turn direction and flip if needed
            if (useLShape && roomchoosen.getEntranceDirection().opposite() != roomchoosen.getExitDirection()) {
                // Alternate turn direction to prevent spiraling back
                boolean turnRight = !lastTurnedRight;
                lastTurnedRight = turnRight;

                currentDirection = pasteRoomWithFlip(roomchoosen, spawnLoc, currentDirection, roomIndex, turnRight);
            } else {
                currentDirection = pasteRoom(roomchoosen, spawnLoc, currentDirection, roomIndex);
            }

            if (currentDirection == null) return false;
        }

        // --- Boss Room ---
        if (bossRoomName != null && !bossRoomName.isEmpty()) {
            Room bossRoom = findRoomByName(bossRoomName);
            if (bossRoom != null) {
                currentDirection = pasteRoom(bossRoom, spawnLoc, currentDirection, rooms.size());
                if (currentDirection == null) return false;
            }
        }

        return !rooms.isEmpty();
    }

    /**
     * Pick a room from the pool respecting spawnrate and recycling rules.
     */
    private Room pickRoom(List<Room> pool, List<Room> used, List<Room> masked, Random rand, boolean recycling) {
        if (pool.isEmpty()) return null;

        int attempts = 0;
        int maxAttempts = pool.size() * 10;

        while (attempts < maxAttempts) {
            attempts++;
            if (used.size() >= pool.size()) used.clear();

            int choose = rand.nextInt(pool.size());
            Room room = pool.get(choose);

            if (!recycling && used.contains(room)) continue;
            if (!room.getFile().exists()) {
                pool.remove(room);
                continue;
            }
            if (room.getSpawnrate() <= 0) {
                if (!masked.contains(room)) masked.add(room);
                continue;
            }
            if (rand.nextInt(1, 101) <= room.getSpawnrate()) {
                used.add(room);
                return room;
            }
        }
        // Fallback: just return the first available
        for (Room room : pool) {
            if (room.getFile().exists() && room.getSpawnrate() > 0) return room;
        }
        return null;
    }

    /**
     * Paste an L-shaped room with a specific turn direction.
     * 
     * @param turnRight true = room exits to the RIGHT of travel, false = LEFT
     */
    private Direction pasteRoomWithFlip(Room room, Location spawnLoc, Direction currentDirection, int index, boolean turnRight) {
        Direction entranceMustFace = currentDirection.opposite();

        // Determine which way the room natively turns
        // Native: entrance → exit. If entrance=SOUTH, exit=EAST → turns LEFT (from player perspective going north)
        // We need to figure out if the native room turns left or right relative to travel
        int nativeRotation = room.getEntranceDirection().rotationTo(entranceMustFace);
        Direction nativeExitAfterAlign = room.getExitDirection().rotate(nativeRotation);

        // Is the native exit to the right or left of currentDirection?
        boolean nativeGoesRight = isRightTurn(currentDirection, nativeExitAfterAlign);

        int rotationDegrees;
        if (nativeGoesRight == turnRight) {
            // Native direction matches desired → normal rotation
            rotationDegrees = nativeRotation;
        } else {
            // Need to flip: mirror the room by using a different rotation
            // If we want right but native goes left (or vice versa), rotate 180° more 
            // This effectively mirrors the L-shape
            // Actually: we rotate so entrance faces correctly, then the exit naturally goes the OTHER way
            // The trick: rotate entrance to face the SAME side but flip exit
            // We achieve this by adding 180° to make exit go opposite, but that breaks entrance.
            // 
            // Better approach: use the formula that directly targets the desired exit direction
            Direction desiredExit = turnRight ? turnRightFrom(currentDirection) : turnLeftFrom(currentDirection);
            // We need: after rotation, entrance = entranceMustFace AND exit = desiredExit
            // Since the room is L-shaped, entrance.opposite != exit, so we can't always satisfy both.
            // Instead: prioritize entrance alignment, accept whichever exit comes out.
            // If native doesn't give us the right turn, just use the room the other way.
            // Simplest fix: just rotate differently - try all 4 rotations and pick the one where
            // entrance matches AND exit is on the desired side
            rotationDegrees = -1;
            for (int tryRot : new int[]{0, 90, 180, 270}) {
                Direction entranceAfter = room.getEntranceDirection().rotate(tryRot);
                Direction exitAfter = room.getExitDirection().rotate(tryRot);
                if (entranceAfter == entranceMustFace && isRightTurn(currentDirection, exitAfter) == turnRight) {
                    rotationDegrees = tryRot;
                    break;
                }
            }
            if (rotationDegrees == -1) {
                // Couldn't find a perfect match — fall back to normal alignment
                rotationDegrees = nativeRotation;
            }
        }

        if(Main.getMain().getConfig().getBoolean("debug")) Main.getMain().getLogger().info("[Dungeon Debug] Room " + index + " (L-SHAPE " + (turnRight ? "RIGHT" : "LEFT") + ") (" + room.getFile().getName() + ")");
        Main.getMain().getLogger().info("  Travel: " + currentDirection + ", Rotation: " + rotationDegrees + "°");

        Location roomLocation = spawnLoc.clone();

        WorldEditSchematic schematic = new WorldEditSchematic();
        schematic.loadSchematic(room.getFile());
        schematic.pasteSchematic(spawnLoc, rotationDegrees);

        double exitX = room.getExit().getX();
        double exitY = room.getExit().getY();
        double exitZ = room.getExit().getZ();
        double[] rotatedExit = rotatePoint(exitX, exitZ, rotationDegrees);

        Location roomCenter = roomLocation.clone().add(rotatedExit[0] / 2.0, exitY / 2.0, rotatedExit[1] / 2.0);
        RoomInstance roomInstance = new RoomInstance(room, roomLocation, index, roomCenter);
        rooms.add(roomInstance);

        spawnLoc.add(rotatedExit[0], exitY, rotatedExit[1]);

        // Store exit point for barrier
        roomInstance.setExitPoint(spawnLoc.clone());

        Direction exitDir = room.getExitDirection().rotate(rotationDegrees);

        Main.getMain().getLogger().info("  Exit direction: " + exitDir);
        Main.getMain().getLogger().info("  Next paste at: " + spawnLoc.getBlockX() + ", " + spawnLoc.getBlockY() + ", " + spawnLoc.getBlockZ());

        return exitDir;
    }

    /**
     * Check if going from 'from' to 'to' is a right turn.
     */
    private boolean isRightTurn(Direction from, Direction to) {
        // Right turns: NORTH→EAST, EAST→SOUTH, SOUTH→WEST, WEST→NORTH
        return switch (from) {
            case NORTH -> to == Direction.EAST;
            case EAST -> to == Direction.SOUTH;
            case SOUTH -> to == Direction.WEST;
            case WEST -> to == Direction.NORTH;
        };
    }

    private Direction turnRightFrom(Direction dir) {
        return switch (dir) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
        };
    }

    private Direction turnLeftFrom(Direction dir) {
        return switch (dir) {
            case NORTH -> Direction.WEST;
            case EAST -> Direction.NORTH;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
        };
    }

    /**
     * Paste a single room and add it to the rooms list.
     * 
     * LINEAR CONNECTION LOGIC:
     * - currentDirection = the direction the corridor is heading
     * - Room entrance must face the direction we're COMING FROM
     * - After paste, we continue in the direction of the room's exit
     *
     * Example: corridor heading NORTH
     *   → We arrive from SOUTH side
     *   → Room entrance must face SOUTH (player enters from south)
     *   → If room's native entrance is SOUTH and exit is NORTH: no rotation
     *   → If room's native entrance is SOUTH and exit is EAST (L-shape): rotate so entrance faces SOUTH
     */
    private Direction pasteRoom(Room room, Location spawnLoc, Direction currentDirection, int index) {
        // The entrance of this room must face the direction we're coming FROM.
        Direction entranceMustFace = currentDirection.opposite();
        
        // How much to rotate: from room's native entrance direction to where it needs to be
        int rotationDegrees = room.getEntranceDirection().rotationTo(entranceMustFace);

        // Debug logging
        if(Main.getMain().getConfig().getBoolean("debug")) Main.getMain().getLogger().info("[Dungeon Debug] Room " + index + " (LINEAR) (" + room.getFile().getName() + ")");
        Main.getMain().getLogger().info("  Travel: " + currentDirection + ", Rotation: " + rotationDegrees + "°");

        Location roomLocation = spawnLoc.clone();

        WorldEditSchematic schematic = new WorldEditSchematic();
        schematic.loadSchematic(room.getFile());
        schematic.pasteSchematic(spawnLoc, rotationDegrees);

        // Calculate rotated exit offset
        double exitX = room.getExit().getX();
        double exitY = room.getExit().getY();
        double exitZ = room.getExit().getZ();
        double[] rotatedExit = rotatePoint(exitX, exitZ, rotationDegrees);

        // Room center for mob spawning
        Location roomCenter = roomLocation.clone().add(rotatedExit[0] / 2.0, exitY / 2.0, rotatedExit[1] / 2.0);

        RoomInstance roomInstance = new RoomInstance(room, roomLocation, index, roomCenter);
        rooms.add(roomInstance);

        // Move spawnLoc to exit point
        spawnLoc.add(rotatedExit[0], exitY, rotatedExit[1]);

        // Store exit point on room instance for barrier placement
        roomInstance.setExitPoint(spawnLoc.clone());

        // The new travel direction is the room's exit direction after rotation
        Direction exitDir = room.getExitDirection().rotate(rotationDegrees);
        
        Main.getMain().getLogger().info("  Exit direction: " + exitDir);
        Main.getMain().getLogger().info("  Next paste at: " + spawnLoc.getBlockX() + ", " + spawnLoc.getBlockY() + ", " + spawnLoc.getBlockZ());

        return exitDir;
    }

    /**
     * Find a room by its file name (without .dungeon extension).
     */
    private Room findRoomByName(String name) {
        if (name == null || name.isEmpty()) return null;
        for (Room room : dungeon.getDungeonRooms().getRooms()) {
            if (room.getFile().getName().replace(".dungeon", "").equals(name)) {
                return room;
            }
        }
        return null;
    }

    /**
     * Called when all mobs in the current room are killed.
     */
    public void onRoomCleared() {
        if (!active) return;

        // Remove barrier from the room that was just cleared
        RoomInstance clearedRoom = getCurrentRoom();
        if (clearedRoom != null && instanceWorld != null) {
            clearedRoom.removeBarrier(instanceWorld);
        }

        currentRoomIndex++;

        if (currentRoomIndex >= rooms.size()) {
            onDungeonComplete();
            return;
        }

        for (Player player : players) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&aRoom cleared! Proceeding to room &e" + (currentRoomIndex + 1) + "&a/&e" + rooms.size()));
        }

        // Spawn mobs in next room and place barrier at its exit
        RoomInstance nextRoom = getCurrentRoom();
        if (nextRoom != null) {
            nextRoom.spawnMobs(instanceWorld);
            // Place barrier at next room's exit (so player can't skip ahead)
            if (!nextRoom.isCleared()) {
                nextRoom.placeBarrier(instanceWorld);
            }
            // Auto-advance if room has no mobs
            if (nextRoom.isCleared()) {
                Bukkit.getScheduler().runTask(Main.getProceduralDungeon(), this::onRoomCleared);
            }
        }
    }

    /**
     * Called when the dungeon is successfully completed.
     */
    private void onDungeonComplete() {
        for (Player player : players) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a&lDungeon Complete! &aYou cleared all " + rooms.size() + " rooms!"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&7Teleporting to spawn in &e10 seconds&7..."));
        }

        // Execute reward commands from console
        List<String> rewards = dungeon.getConfig().getStringList("rewards");
        if (rewards != null && !rewards.isEmpty()) {
            for (Player player : players) {
                for (String command : rewards) {
                    String parsed = command.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                }
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&6&lRewards received!"));
            }
        }

        // 10 second delay before teleporting back
        Bukkit.getScheduler().runTaskLater(Main.getProceduralDungeon(), () -> {
            endSession();
        }, 200L); // 200 ticks = 10 seconds
    }

    /**
     * Called when a player dies (dungeon failed).
     */
    public void onPlayerDeath(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&c&lDungeon Failed! &cYou have been defeated."));
        endSession();
    }

    /**
     * End the session — teleport players back to spawn and cleanup.
     * Thread-safe: only executes once even if called multiple times.
     */
    public void endSession() {
        if (cleaning) return; // Prevent double cleanup
        cleaning = true;
        active = false;

        // Remove from instance manager first to prevent further event processing
        DungeonInstanceManager.getInstance().removeInstance(this);

        // Teleport players out immediately
        for (Player player : new ArrayList<>(players)) {
            player.setGameMode(GameMode.SURVIVAL);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + player.getName());
        }

        // Cleanup in stages to avoid lag spikes
        // Stage 1: Remove entities in batches (2 ticks later, after teleport)
        Bukkit.getScheduler().runTaskLater(Main.getProceduralDungeon(), () -> {
            if (instanceWorld == null) return;
            removeEntitiesInBatches();
        }, 2L);
    }

    /**
     * Remove entities in batches to avoid lag, then unload world.
     */
    private void removeEntitiesInBatches() {
        if (instanceWorld == null) return;

        List<Entity> entities = new ArrayList<>(instanceWorld.getEntities());
        // Remove players from list (they should already be teleported out)
        entities.removeIf(e -> e instanceof Player);

        if (entities.isEmpty()) {
            unloadAndDeleteWorld();
            return;
        }

        // Process in batches of 50 per tick to prevent lag
        final int batchSize = 50;
        int[] index = {0};

        Bukkit.getScheduler().runTaskTimer(Main.getProceduralDungeon(), task -> {
            if (instanceWorld == null) {
                task.cancel();
                return;
            }

            int end = Math.min(index[0] + batchSize, entities.size());
            for (int i = index[0]; i < end; i++) {
                Entity entity = entities.get(i);
                if (entity.isValid() && !(entity instanceof Player)) {
                    entity.remove();
                }
            }
            index[0] = end;

            if (index[0] >= entities.size()) {
                task.cancel();
                // Stage 2: Unload world after all entities removed
                Bukkit.getScheduler().runTaskLater(Main.getProceduralDungeon(),
                        this::unloadAndDeleteWorld, 1L);
            }
        }, 0L, 1L);
    }

    /**
     * Unload the world and delete its folder asynchronously.
     */
    private void unloadAndDeleteWorld() {
        if (instanceWorld == null) return;

        String worldName = instanceWorld.getName();

        // Unload world (no save needed)
        boolean unloaded = Bukkit.unloadWorld(instanceWorld, false);
        instanceWorld = null;

        if (!unloaded) {
            Main.getMain().getLogger().log(Level.WARNING,
                    "Failed to unload dungeon world: " + worldName);
            return;
        }

        // Delete world folder asynchronously to avoid blocking main thread
        Bukkit.getScheduler().runTaskAsynchronously(Main.getProceduralDungeon(), () -> {
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            deleteFolder(worldFolder);
        });
    }

    private void deleteFolder(File folder) {
        if (!folder.exists()) return;
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFolder(file);
                }
            }
        }
        folder.delete();
    }

    /**
     * Check if all mobs in the current room are dead.
     */
    public boolean isCurrentRoomCleared() {
        RoomInstance room = getCurrentRoom();
        if (room == null) return true;
        return room.areAllMobsDead();
    }

    private static double[] rotatePoint(double x, double z, int degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double newX = x * cos - z * sin;
        double newZ = x * sin + z * cos;
        newX = Math.round(newX * 1000.0) / 1000.0;
        newZ = Math.round(newZ * 1000.0) / 1000.0;
        return new double[]{newX, newZ};
    }
}
