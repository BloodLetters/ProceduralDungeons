package fr.foxelia.proceduraldungeon.utilities.rooms;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Room {

    public enum RoomType {
        NORMAL, SPAWN, FINAL
    }

    private File file;
    private Coordinate exit;
    private Direction entranceDirection;
    private Direction exitDirection;
    private int spawnrate;
    private Map<String, Integer> mobs;
    private RoomType roomType;

    public Room(File file, Coordinate exit) {
        this(file, exit, Direction.SOUTH, Direction.NORTH);
    }

    public Room(File file, Coordinate exit, Direction entranceDirection, Direction exitDirection) {
        this.file = file;
        this.exit = exit;
        this.entranceDirection = entranceDirection;
        this.exitDirection = exitDirection;
        this.setSpawnrate(100);
        this.mobs = new HashMap<>();
        this.roomType = RoomType.NORMAL;
    }

    public File getFile() {
        return file;
    }

    public Coordinate getExit() {
        return exit;
    }

    public Direction getEntranceDirection() {
        return entranceDirection;
    }

    public void setEntranceDirection(Direction entranceDirection) {
        this.entranceDirection = entranceDirection;
    }

    public Direction getExitDirection() {
        return exitDirection;
    }

    public void setExitDirection(Direction exitDirection) {
        this.exitDirection = exitDirection;
    }

    public int getSpawnrate() {
        return spawnrate;
    }

    public void addSpawnrate(int spawnrate) {
        if ((this.spawnrate + spawnrate) > 100) {
            this.spawnrate = 100;
        } else if ((this.spawnrate + spawnrate) < 0) {
            this.spawnrate = 0;
        } else this.spawnrate += spawnrate;
    }

    public void setSpawnrate(int spawnrate) {
        if (spawnrate > 100) {
            this.spawnrate = 100;
        } else if (spawnrate < 0) {
            this.spawnrate = 0;
        } else this.spawnrate = spawnrate;
    }

    /**
     * Get the mob configuration map (mobId -> count).
     */
    public Map<String, Integer> getMobs() {
        return mobs;
    }

    public void setMobs(Map<String, Integer> mobs) {
        this.mobs = mobs;
    }

    /**
     * Get total mob count for this room.
     */
    public int getTotalMobCount() {
        return mobs.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Get mobs as a flat list (expanded by count) for spawning.
     */
    public List<String> getMobsAsList() {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : mobs.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                list.add(entry.getKey());
            }
        }
        return list;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public boolean isSpawnRoom() {
        return roomType == RoomType.SPAWN;
    }

    public boolean isFinalRoom() {
        return roomType == RoomType.FINAL;
    }
}
