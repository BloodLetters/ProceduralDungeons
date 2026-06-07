package fr.foxelia.proceduraldungeon.gui;

import fr.foxelia.proceduraldungeon.Main;
import fr.foxelia.proceduraldungeon.utilities.DungeonManager;
import fr.foxelia.proceduraldungeon.utilities.rooms.Room;
import fr.foxelia.tools.minecraft.ui.gui.GUI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;

public class RoomGUI extends GUI implements DungeonInterface, RoomInterface {

    /*
     * Constants
     */
    private final DungeonManager dungeon;
    private final Room room;
    private static final int inventorySize = 3;

    /*
     * Constructor
     */

    public RoomGUI(DungeonManager dungeon, Room room) {
        super("room");
        this.dungeon = dungeon;
        this.room = room;
    }

    /*
     * Foxelia Methods
     */

    @Override
    public void constructGUI() {
        // Update placeholders
        addPlaceholder("%room%", room.getFile().getName().replace(".dungeon", ""));
        addPlaceholder("%number%", String.valueOf(dungeon.getDungeonRooms().getRooms().indexOf(room)));
        addPlaceholder("%mobcount%", String.valueOf(room.getTotalMobCount()));

        createInventory(inventorySize, getPlaceholderReplacement()); // Create Inventory


        setItem(0, "roomitem"); // Icon
        setItem(8, "delete"); // Delete Icon
        updateSpawnRate(); // SpawnRate Icon

        // Add SpawnRate +1% and +10%
        addPlaceholder("%ratemodifier%", "1");
        setItem(12, "addspawnrate");
        addPlaceholder("%ratemodifier%", "10");
        setItem(21, "addspawnrate");

        // Lower SpawnRate -1% and -10%
        addPlaceholder("%ratemodifier%", "1");
        setItem(14, "lowerspawnrate");
        addPlaceholder("%ratemodifier%", "10");
        setItem(23, "lowerspawnrate");

        // Mob configuration button
        org.bukkit.inventory.ItemStack mobsButton = new org.bukkit.inventory.ItemStack(org.bukkit.Material.SPAWNER);
        org.bukkit.inventory.meta.ItemMeta mobsMeta = mobsButton.getItemMeta();
        mobsMeta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&aConfigure Mobs &8(&e" + room.getTotalMobCount() + "&8)"));
        java.util.List<String> mobsLore = new java.util.ArrayList<>();
        mobsLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Configure which mobs spawn"));
        mobsLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7in this room during gameplay."));
        mobsLore.add("");
        mobsLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Supports &dMythicMobs &7& vanilla"));
        mobsLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eLeft click to configure"));
        mobsMeta.setLore(mobsLore);
        mobsButton.setItemMeta(mobsMeta);
        setItem(18, mobsButton);

        // Fill empty slots
        fillEmpty(generateItem("empty", getPlaceholderReplacement()));
    }

    /*
     * Events
     */
    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        HumanEntity human = event.getWhoClicked();

        // Check if player can interact with the GUI
        // Check if the dungeons is not removed
        // Check if the room still exists
        if(!GUIManager.checkPermissionToEdit(human) || !GUIManager.checkDungeonExists(this, dungeon) && !GUIManager.checkRoomExists(this, room)) return;

        if(!event.isLeftClick()) return;
        switch(event.getSlot()) {
            // Go backward
            case 0 -> GUIManager.openDungeonGUI(dungeon, human);
            // Delete Room
            case 8 -> {
                if(!human.hasPermission("proceduraldungeon.admin.edit.deleteroom")) {
                    human.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("lackingpermission").replace("%permission%", "proceduraldungeon.admin.edit.deleteroom")));
                    break;
                }
                GUIManager.openRoomDeleteGUI(dungeon, room, human);
                break;
            }
            // Change Room Spawn Percentage
            case 12 -> updateSpawnRate(1);
            case 14 -> updateSpawnRate(-1);
            case 21 -> updateSpawnRate(10);
            case 23 -> updateSpawnRate(-10);
            // Mob configuration
            case 18 -> {
                MobSelectGUI mobGui = new MobSelectGUI(dungeon, room);
                mobGui.openInventory(human);
            }
        }


    }

    /*
     * Other Methods
     */
    public void updateSpawnRate() {
        addPlaceholder("%rate%", String.valueOf(room.getSpawnrate()));
        setItem(4, "spawnrate");
    }

    public void updateSpawnRate(int rateModifier) {
        room.addSpawnrate(rateModifier);
        updateSpawnRate();
    }

    /*
     * Interface
     */
    @Override
    public DungeonManager getDungeon() {
        return dungeon;
    }

    @Override
    public RoomGUI getGUI() {
        return this;
    }

    @Override
    public Room getRoom() {
        return room;
    }
}
