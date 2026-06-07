package fr.foxelia.proceduraldungeon.gui;

import fr.foxelia.proceduraldungeon.gameplay.MythicMobsHook;
import fr.foxelia.proceduraldungeon.utilities.DungeonManager;
import fr.foxelia.proceduraldungeon.utilities.rooms.Room;
import fr.foxelia.tools.minecraft.ui.gui.NavigableGUI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for selecting MythicMobs to add to a room.
 * Left click = increase count, Right click = decrease count.
 */
public class MobSelectGUI extends NavigableGUI<String> implements DungeonInterface, RoomInterface {

    private final DungeonManager dungeon;
    private final Room room;
    private static final int inventorySize = 6;

    public MobSelectGUI(DungeonManager dungeon, Room room) {
        super("mobselect");
        this.dungeon = dungeon;
        this.room = room;
        setupDisplayedList();
    }

    @Override
    public void constructGUI() {
        addPlaceholder("%room%", room.getFile().getName().replace(".dungeon", ""));
        addPlaceholder("%page%", String.valueOf(getCurrentPage() + 1));
        int maxPage = Math.max(1, (int) Math.ceil((double) getDisplayedList().size() / ((inventorySize - 1) * 9)));
        addPlaceholder("%maxpage%", String.valueOf(maxPage));

        createInventory(inventorySize, "&8Mobs - &e%room% &8(%page%/%maxpage%)", getPlaceholderReplacement());

        addNavigationButtons();
        fillInventory();
        fillEmpty(createPane());
    }

    @Override
    public void addNextButton(int bottomRow) {
        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta meta = next.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aNext page"));
        next.setItemMeta(meta);
        getInventory().setItem(bottomRow + 8, next);
    }

    @Override
    public void addPreviousButton(int bottomRow) {
        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta meta = prev.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aPrevious page"));
        prev.setItemMeta(meta);
        getInventory().setItem(bottomRow, prev);
    }

    @Override
    public void addCustomButtons(int bottomRow) {
        // Back button
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cBack to Room Edit"));
        List<String> backLore = new ArrayList<>();
        backLore.add(ChatColor.translateAlternateColorCodes('&', "&7Total mobs: &e" + room.getTotalMobCount()));
        backMeta.setLore(backLore);
        back.setItemMeta(backMeta);
        getInventory().setItem(bottomRow + 4, back);
    }

    @Override
    public void setupDisplayedList() {
        // Load MythicMobs list
        List<String> mobs = MythicMobsHook.getAvailableMobs();
        for (String mob : mobs) {
            getDisplayedList().add(mob);
        }

        // If MythicMobs not available, add vanilla mob types
        if (mobs.isEmpty()) {
            for (org.bukkit.entity.EntityType type : org.bukkit.entity.EntityType.values()) {
                if (type.isAlive() && type.isSpawnable() && type != org.bukkit.entity.EntityType.PLAYER) {
                    getDisplayedList().add(type.name());
                }
            }
        }
    }

    @Override
    public ItemStack processItem(String mobName) {
        int count = room.getMobs().getOrDefault(mobName, 0);
        boolean isAdded = count > 0;

        Material material = isAdded ? Material.ZOMBIE_SPAWN_EGG : Material.ZOMBIE_HEAD;
        ItemStack item = new ItemStack(material);
        if (isAdded) {
            item.setAmount(Math.min(count, 64));
        }

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                (isAdded ? "&a" : "&e") + mobName + (isAdded ? " &8x&e" + count : "")));

        List<String> lore = new ArrayList<>();
        if (isAdded) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Count: &e" + count));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&aLeft click &7to add +1"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&cRight click &7to remove -1"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&cShift+Right click &7to remove all"));
        } else {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Not added to this room"));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&aLeft click &7to add"));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        HumanEntity human = event.getWhoClicked();

        if (!GUIManager.checkPermissionToEdit(human)) return;

        int bottomRow = inventorySize * 9 - 9;

        if (event.getSlot() == bottomRow + 8) {
            goToNextPage();
        } else if (event.getSlot() == bottomRow) {
            goToPreviousPage();
        } else if (event.getSlot() == bottomRow + 4) {
            // Back to room GUI
            GUIManager.openRoomGUI(dungeon, room, human);
        } else if (event.getSlot() < bottomRow && event.getSlot() >= 0) {
            // Select/modify mob count
            int index = event.getSlot() + getCurrentPage() * ((inventorySize - 1) * 9);
            if (index >= getDisplayedList().size()) return;

            String mobName = getDisplayedList().get(index);
            int currentCount = room.getMobs().getOrDefault(mobName, 0);

            if (event.isLeftClick()) {
                // Add +1
                room.getMobs().put(mobName, currentCount + 1);
                human.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&aSet &e" + mobName + " &ato &e" + (currentCount + 1) + "&a."));
            } else if (event.isRightClick()) {
                if (event.isShiftClick()) {
                    // Remove all
                    room.getMobs().remove(mobName);
                    human.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&cRemoved all &e" + mobName + " &cfrom room."));
                } else {
                    // Remove -1
                    if (currentCount <= 1) {
                        room.getMobs().remove(mobName);
                        human.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&cRemoved &e" + mobName + " &cfrom room."));
                    } else {
                        room.getMobs().put(mobName, currentCount - 1);
                        human.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&cSet &e" + mobName + " &cto &e" + (currentCount - 1) + "&c."));
                    }
                }
            }

            // Refresh the GUI - reopen to player
            constructGUI();
            fillInventory();
            human.openInventory(getInventory());
        }
    }

    private ItemStack createPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    @Override
    public DungeonManager getDungeon() {
        return dungeon;
    }

    @Override
    public MobSelectGUI getGUI() {
        return this;
    }

    @Override
    public Room getRoom() {
        return room;
    }
}
