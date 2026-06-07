package fr.foxelia.proceduraldungeon.gui;

import fr.foxelia.proceduraldungeon.Main;
import fr.foxelia.proceduraldungeon.utilities.DungeonManager;
import fr.foxelia.proceduraldungeon.utilities.rooms.Room;
import fr.foxelia.tools.minecraft.ui.gui.NavigableGUI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DungeonGUI extends NavigableGUI<Room> implements DungeonInterface {

    /*
     * Constants
     */
    private final DungeonManager dungeon;
    private static final int inventorySize = 5;

    private enum SelectionMode { NONE, START_ROOM, BOSS_ROOM }
    private SelectionMode selectionMode = SelectionMode.NONE;

    /*
     * Constructor
     */

    public DungeonGUI(DungeonManager dungeon) {
        super("dungeon");
        this.dungeon = dungeon;
        setupDisplayedList();
    }

    /*
     * Foxelia Methods
     */

    @Override
    public void constructGUI() {
        addPlaceholder("%dungeon%", dungeon.getName());
        refreshPagesPlaceholders();
        createInventory(inventorySize, getPlaceholderReplacement());

        addNavigationButtons();
        fillInventory();
        fillEmpty(generateItem("empty", getPlaceholderReplacement()));

    }

    @Override
    public void addNextButton(int bottomRow) {
        setItem(bottomRow + 8, "navbar.next");
    }

    @Override
    public void addPreviousButton(int bottomRow) {
        setItem(bottomRow, "navbar.previous");
    }

    @Override
    public void addCustomButtons(int bottomRow) {
        // Rename
        setItem(bottomRow + 2, "navbar.rename");
        // Room Count
        updateRoomCount();
        // Room Recycling
        updateRoomRecycling();
        // Start Room button
        updateStartRoomButton(bottomRow);
        // Boss Room button
        updateBossRoomButton(bottomRow);
    }

    @Override
    public void setupDisplayedList() {
        for(Room room : dungeon.getDungeonRooms().getRooms()) {
            getDisplayedList().add(room);
        }
    }

    @Override
    public ItemStack processItem(Room room) {
        addPlaceholder("%number%", String.valueOf(getIndexOf(room)));
        addPlaceholder("%room%", room.getFile().getName().replace(".dungeon", ""));
        return generateItem("roomitem", getPlaceholderReplacement());
    }

    public void refreshPagesPlaceholders() {
        addPlaceholder("%page%", String.valueOf(getCurrentPage() + 1));
        addPlaceholder("%maxpage%", String.valueOf((int) Math.ceil((double) getDisplayedList().size() / (inventorySize * 9 - 9))));
    }

    @Nullable
    public Room getSelected(int slot) {
        int index = slot + getCurrentPage() * ((inventorySize - 1) * 9);
        if(index >= getDisplayedList().size()) return null;
        return getDisplayedList().get(index);
    }

    /*
     * Events
     */

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        HumanEntity human = event.getWhoClicked();

        if(!GUIManager.checkPermissionToEdit(human) || !GUIManager.checkDungeonExists(this, dungeon)) return;

        int bottomRow = inventorySize * 9 - 9;

        switch (event.getSlot()) {
            // Previous button
            case inventorySize * 9 - 9 -> goToPreviousPage();
            // Next button
            case inventorySize * 9 - 1 -> goToNextPage();
            // Start Room button (bottomRow + 1)
            case inventorySize * 9 - 8 -> {
                if (event.isLeftClick()) {
                    selectionMode = SelectionMode.START_ROOM;
                    human.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aClick a room to set it as the &eStart Room&a."));
                } else if (event.isRightClick()) {
                    dungeon.getConfig().set("startroom", "");
                    selectionMode = SelectionMode.NONE;
                    updateStartRoomButton(bottomRow);
                    human.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cStart room cleared."));
                }
            }
            // Rename
            case inventorySize * 9 - 7 -> {
                if(event.isLeftClick()) {
                    if(Main.getRenaming().containsKey(human)) {
                        human.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("alreadyrenaming")));
                        return;
                    }
                    Main.getRenaming().put(human, dungeon);
                    human.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getOthersMessage("renaming")));
                    human.closeInventory();
                    Bukkit.getScheduler().runTaskLater(Main.getProceduralDungeon(), () -> {
                        if(Main.getRenaming().containsKey(human)) {
                            Main.getRenaming().remove(human);
                            new DungeonGUI(dungeon).openInventory(human);
                        }
                    }, 200);
                }
            }
            // Room Count
            case inventorySize * 9 - 5 -> {
                int roomCount = event.getCurrentItem().getAmount();
                if(event.isLeftClick()) {
                    if(roomCount >= 64) break;
                    dungeon.getConfig().set("roomcount", ++roomCount);
                } else if(event.isRightClick()) {
                    if(roomCount <= 1) break;
                    dungeon.getConfig().set("roomcount", --roomCount);
                }
                GUIManager.updateDungeonRoomCountSettings(dungeon);
            }
            // Room Recycling
            case inventorySize * 9 - 3 -> {
                if(event.isLeftClick()) {
                    if(dungeon.getConfig().getBoolean("roomrecyling")) {
                        dungeon.getConfig().set("roomrecyling", false);
                    } else {
                        dungeon.getConfig().set("roomrecyling", true);
                    }
                    GUIManager.updateDungeonRoomRecyclingSettings(dungeon);
                }
            }
            // Boss Room button (bottomRow + 7)
            case inventorySize * 9 - 2 -> {
                if (event.isLeftClick()) {
                    selectionMode = SelectionMode.BOSS_ROOM;
                    human.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aClick a room to set it as the &cBoss Room&a."));
                } else if (event.isRightClick()) {
                    dungeon.getConfig().set("bossroom", "");
                    selectionMode = SelectionMode.NONE;
                    updateBossRoomButton(bottomRow);
                    human.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cBoss room cleared."));
                }
            }
            // Other — room selection
            default -> {
                if(event.isLeftClick() && event.getSlot() < bottomRow && event.getSlot() >= 0) {
                    Room room = getSelected(event.getSlot());
                    if (room == null) return;

                    if (selectionMode == SelectionMode.START_ROOM) {
                        if (room.getRoomType() != Room.RoomType.SPAWN) {
                            human.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&cThis room is not marked as a Spawn Room! Use &4/dungeon spawnroom <dungeon> <room> &cfirst."));
                            selectionMode = SelectionMode.NONE;
                            return;
                        }
                        String roomName = room.getFile().getName().replace(".dungeon", "");
                        dungeon.getConfig().set("startroom", roomName);
                        selectionMode = SelectionMode.NONE;
                        updateStartRoomButton(bottomRow);
                        human.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aStart room set to: &e" + roomName));
                    } else if (selectionMode == SelectionMode.BOSS_ROOM) {
                        if (room.getRoomType() != Room.RoomType.FINAL) {
                            human.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&cThis room is not marked as a Final Room! Use &4/dungeon finalroom <dungeon> <room> &cfirst."));
                            selectionMode = SelectionMode.NONE;
                            return;
                        }
                        String roomName = room.getFile().getName().replace(".dungeon", "");
                        dungeon.getConfig().set("bossroom", roomName);
                        selectionMode = SelectionMode.NONE;
                        updateBossRoomButton(bottomRow);
                        human.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cBoss room set to: &e" + roomName));
                    } else {
                        interactWith(event.getSlot(), human);
                    }
                }
            }
        }
    }

    /*
     * Other Methods
     */
    public void interactWith(int slot, HumanEntity human) {
        Room room = getSelected(slot);
        if(room == null) return;
        GUIManager.openRoomGUI(dungeon, room, human);
    }

    public int getIndexOf(Room room) {
        return dungeon.getDungeonRooms().getRooms().indexOf(room);
    }

    public void updateRoomCount() {
        ItemStack roomCountIcon = generateItem("navbar.roomcount", getPlaceholderReplacement());
        int roomCountValue = dungeon.getConfig().getInt("roomcount");
        if(roomCountValue > 64) {
            roomCountValue = 64;
        } else if(roomCountValue < 1) roomCountValue = 1;
        roomCountIcon.setAmount(roomCountValue);
        setItem(inventorySize * 9 - 5, roomCountIcon);
    }

    public void updateRoomRecycling() {
        setItem(inventorySize * 9 - 3, "navbar.roomrecyling.r" + dungeon.getConfig().getBoolean("roomrecyling"));
    }

    public void updateStartRoomButton(int bottomRow) {
        String startRoom = dungeon.getConfig().getString("startroom", "");
        boolean hasStart = startRoom != null && !startRoom.isEmpty();

        ItemStack item = new ItemStack(hasStart ? Material.LIME_BED : Material.RED_BED);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&aStart Room: " + (hasStart ? "&e" + startRoom : "&cNot Set")));
        List<String> lore = new ArrayList<>();
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7The first room players spawn in."));
        lore.add("");
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&aLeft click &7a room above to set"));
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&cRight click &7to clear"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        setItem(bottomRow + 1, item);
    }

    public void updateBossRoomButton(int bottomRow) {
        String bossRoom = dungeon.getConfig().getString("bossroom", "");
        boolean hasBoss = bossRoom != null && !bossRoom.isEmpty();

        ItemStack item = new ItemStack(hasBoss ? Material.DRAGON_HEAD : Material.WITHER_SKELETON_SKULL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&cBoss Room: " + (hasBoss ? "&e" + bossRoom : "&cNot Set")));
        List<String> lore = new ArrayList<>();
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7The final room (boss fight)."));
        lore.add("");
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&aLeft click &7a room above to set"));
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&cRight click &7to clear"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        setItem(bottomRow + 7, item);
    }

    /*
     * Interface
     */
    @Override
    public DungeonManager getDungeon() {
        return dungeon;
    }

    @Override
    public DungeonGUI getGUI() {
        return this;
    }
}
