package fr.foxelia.proceduraldungeon.utilities.rooms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class RoomsManager {

    private File folder;
    File propertiesFile;
    private Properties properties = new Properties();
    private List<Room> rooms = new ArrayList<>();

    public RoomsManager(File root) {
        folder = new File(root, "rooms");
        if (!folder.exists()) folder.mkdirs();
        propertiesFile = new File(folder, "rooms.properties");
        try {
            if (!propertiesFile.exists()) propertiesFile.createNewFile();
            FileInputStream fis = new FileInputStream(propertiesFile);
            properties.load(fis);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        restoreRooms();
    }

    private void restoreRooms() {
        for (String key : properties.stringPropertyNames()) {
            File keyFile = new File(folder, key + ".dungeon");
            if (!keyFile.exists()) continue;
            String[] values = properties.getProperty(key).split(",");
            if (values.length < 4) continue;

            Coordinate coord = new Coordinate(
                    Double.parseDouble(values[0]),
                    Double.parseDouble(values[1]),
                    Double.parseDouble(values[2])
            );

            // Format: x,y,z,spawnrate,entranceDir,exitDir,mob1;mob2;mob3
            Direction entranceDir = Direction.SOUTH;
            Direction exitDir = Direction.NORTH;
            java.util.Map<String, Integer> mobs = new java.util.HashMap<>();

            if (values.length >= 6) {
                entranceDir = Direction.fromString(values[4]);
                exitDir = Direction.fromString(values[5]);
            }
            if (values.length >= 7 && !values[6].isEmpty()) {
                String[] mobArray = values[6].split(";");
                for (String mob : mobArray) {
                    if (mob.isEmpty()) continue;
                    // Format: mobId:count or just mobId (count defaults to 1)
                    if (mob.contains(":")) {
                        String[] parts = mob.split(":");
                        mobs.put(parts[0], Integer.parseInt(parts[1]));
                    } else {
                        mobs.put(mob, 1);
                    }
                }
            }

            Room dungeonRoom = new Room(keyFile, coord, entranceDir, exitDir);
            dungeonRoom.setSpawnrate(Integer.parseInt(values[3]));
            dungeonRoom.setMobs(mobs);
            // Load room type (index 7 if present)
            if (values.length >= 8 && !values[7].isEmpty()) {
                try {
                    dungeonRoom.setRoomType(Room.RoomType.valueOf(values[7]));
                } catch (IllegalArgumentException ignored) {}
            }
            this.getRooms().add(dungeonRoom);
        }
    }

    public File getFolder() {
        return this.folder;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public void reloadRooms() {
        this.rooms.clear();
        this.restoreRooms();
    }

    public void addRoom(Room room) {
        this.getRooms().add(room);
        this.saveRooms();
    }

    public void saveRooms() {
        if (propertiesFile.exists()) propertiesFile.delete();
        try {
            propertiesFile.createNewFile();
            properties.clear();
            for (Room room : rooms) {
                // Build mob string: mobId:count;mobId:count
                StringBuilder mobsBuilder = new StringBuilder();
                for (java.util.Map.Entry<String, Integer> entry : room.getMobs().entrySet()) {
                    if (mobsBuilder.length() > 0) mobsBuilder.append(";");
                    mobsBuilder.append(entry.getKey()).append(":").append(entry.getValue());
                }
                String value = room.getExit().getX() + ","
                        + room.getExit().getY() + ","
                        + room.getExit().getZ() + ","
                        + room.getSpawnrate() + ","
                        + room.getEntranceDirection().name() + ","
                        + room.getExitDirection().name() + ","
                        + mobsBuilder + ","
                        + room.getRoomType().name();
                properties.setProperty(room.getFile().getName().replace(".dungeon", ""), value);
            }
            FileOutputStream fos = new FileOutputStream(propertiesFile);
            properties.store(fos, null);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isNumeric(String strNum) {
        if (strNum == null) return false;
        try {
            Double.parseDouble(strNum);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private boolean isDirection(String str) {
        if (str == null) return false;
        try {
            Direction.valueOf(str.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
