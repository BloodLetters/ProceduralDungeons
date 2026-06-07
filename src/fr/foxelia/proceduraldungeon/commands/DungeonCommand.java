package fr.foxelia.proceduraldungeon.commands;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import fr.foxelia.proceduraldungeon.Main;
import fr.foxelia.proceduraldungeon.gui.GUIManager;
import fr.foxelia.proceduraldungeon.utilities.ActionType;
import fr.foxelia.proceduraldungeon.utilities.DungeonManager;
import fr.foxelia.proceduraldungeon.utilities.Pair;
import fr.foxelia.proceduraldungeon.utilities.WorldEditSchematic;
import fr.foxelia.proceduraldungeon.utilities.rooms.Coordinate;
import fr.foxelia.proceduraldungeon.utilities.rooms.Direction;
import fr.foxelia.proceduraldungeon.utilities.rooms.Room;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import javax.management.InstanceAlreadyExistsException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DungeonCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String str, String[] args) {
		
		if(args.length != 0) {
/*
 * Version Command
 */
			if(args[0].equalsIgnoreCase("version")) {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
						"&b" + Main.getProceduralDungeon().getDescription().getName() +
						" &7v" + Main.getProceduralDungeon().getDescription().getVersion() +
						" &8by &7" + String.join(", ", Main.getProceduralDungeon().getDescription().getAuthors())));
				return true;
/*
 * Reload Command
 */
			} else if(args[0].equalsIgnoreCase("reload") && sender.hasPermission("proceduraldungeon.admin.reload")) {
				Main.getMain().reloadConfig();
				List<DungeonManager> dms = List.copyOf(Main.getDungeons().values());
				Main.getDungeons().clear();
				for(DungeonManager dm : dms) {
					if(dm.getDungeonFolder().exists()) {
						dm.getDungeonConfig().reloadConfig();
						dm.restoreDungeon();
						if(Main.getDungeons().containsKey(dm.getName().toLowerCase())) {
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("duplicatename").replace("%folder%", dm.getDungeonFolder().getName())));
						} else Main.getDungeons().put(dm.getName().toLowerCase(), dm);						
					}
				}
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getOthersMessage("reload")));
				return true;
/*
 * Help Command
 */
			} else if(args[0].equalsIgnoreCase("help") && sender.hasPermission("proceduraldungeon.dungeon")) { 
				boolean isPlayer = (sender instanceof Player);
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getHelpMessage("helptop")));
				if(sender.hasPermission("proceduraldungeon.admin.reload")) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getHelpMessage("helpcmd")
							.replace("%cmd%", "dungeon reload")
							.replace("%info%", Main.getHelpMessage("info.reload"))));
				}
				if(sender.hasPermission("proceduraldungeon.admin.create")) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getHelpMessage("helpcmd")
							.replace("%cmd%", "dungeon create {dungeonname}")
							.replace("%info%", Main.getHelpMessage("info.create"))));
				}
				if(sender.hasPermission("proceduraldungeon.admin.remove")) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getHelpMessage("helpcmd")
							.replace("%cmd%", "dungeon remove {dungeonname}")
							.replace("%info%", Main.getHelpMessage("info.remove"))));
				}
				if(sender.hasPermission("proceduraldungeon.admin.delete")) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getHelpMessage("helpcmd")
							.replace("%cmd%", "dungeon delete {dungeonname}")
							.replace("%info%", Main.getHelpMessage("info.delete"))));
				}
				if(sender.hasPermission("proceduraldungeon.admin.edit") && isPlayer) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getHelpMessage("helpcmd")
							.replace("%cmd%", "dungeon edit {dungeonname} (roomnumber)")
							.replace("%info%", Main.getHelpMessage("info.edit"))));
				}
				if(sender.hasPermission("proceduraldungeon.admin.addroom") && isPlayer) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getHelpMessage("helpcmd")
							.replace("%cmd%", "dungeon addroom {dungeonname} (customname)")
							.replace("%info%", Main.getHelpMessage("info.addroom"))));
				}
				if(sender.hasPermission("proceduraldungeon.admin.addroom") && isPlayer) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getHelpMessage("helpcmd")
							.replace("%cmd%", "dungeon setexit")
							.replace("%info%", Main.getHelpMessage("info.setexit"))));
				}
				if(sender.hasPermission("proceduraldungeon.admin.generate")) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getHelpMessage("helpcmd")
							.replace("%cmd%", "dungeon generate {dungeonname} {x} {y} {z} (World)")
							.replace("%info%", Main.getHelpMessage("info.generate"))));
				}
				if(sender.hasPermission("proceduraldungeon.admin.list")) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getHelpMessage("helpcmd")
							.replace("%cmd%", "dungeon list")
							.replace("%info%", Main.getHelpMessage("info.list"))));
				}
			
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getHelpMessage("helpcmd")
						.replace("%cmd%", "dungeon help")
						.replace("%info%", Main.getHelpMessage("info.help"))));
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getHelpMessage("helpbottom")));
				return true;
/*
 * List Command
 */
			} else if(args[0].equalsIgnoreCase("list") && sender.hasPermission("proceduraldungeon.admin.list")) {
				File dungeonFolder = new File(Main.getProceduralDungeon().getDataFolder(), "dungeons");
				if(!dungeonFolder.exists() || dungeonFolder.listFiles().length == 0) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("nodungeon")));
					return false;
				}
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getOthersMessage("listtop")));
				List<String> loadeds= new ArrayList<>();
				for(DungeonManager dm : Main.getDungeons().values()) {
					loadeds.add(dm.getDungeonFolder().getName());
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getOthersMessage("listcontent")
							.replace("%dungeon%", dm.getName())
							.replace("%status%", Main.getOthersMessage("liststatus.loaded"))));
				}
				
				for(File removed : dungeonFolder.listFiles()) {
					if(loadeds.contains(removed.getName()) || removed.isFile()) continue;
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getOthersMessage("listcontent")
							.replace("%dungeon%", removed.getName())
							.replace("%status%", Main.getOthersMessage("liststatus.unloaded"))));
				}
				
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getOthersMessage("listbottom")));
				return true;
				
/*
 * Create Command
 */	
			} else if(args[0].equalsIgnoreCase("create") && sender.hasPermission("proceduraldungeon.admin.create")) {
				if(args.length >= 2) {
					Long delay = System.currentTimeMillis();
					DungeonManager manager = new DungeonManager(args[1].toLowerCase());
					DungeonManager checker = manager.checkExists();
					if(checker == null && Main.getDungeons().containsKey(args[1].toLowerCase())) checker = Main.getDungeons().get(args[1].toLowerCase());
// Instance Already Exist
					if(checker != null) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("alreadyexist")
								.replace("%dungeon%", checker.getName())));
						return false;
// Folder Already Exist
					} else if(manager.getDungeonFolder().exists()) {
						if(Main.getConfirmation().containsKey(sender)) {
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("importantaction")));
							return false;
						} else {
							Pair<ActionType, String> action = new Pair<ActionType, String>(ActionType.IMPORT, args[1]);
							Main.getConfirmation().put(sender, action);
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getOthersMessage("importdungeon")));
							Bukkit.getScheduler().runTaskLater(Main.getProceduralDungeon(), () -> {
								if(Main.getConfirmation().containsKey(sender)) {
									Main.getConfirmation().remove(sender);
									sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getTaskMessage("actioncanceled")));
								}
							}, 200);
							return true;
						}
// Creation
					} else {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getTaskMessage("dungeoncreation")));
						try {
							manager.createDungeon(args[1]);
						} catch (InstanceAlreadyExistsException | NullPointerException e) {
							e.printStackTrace();
							Main.sendInternalError(sender);
							return false;
						}
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getTaskMessage("timetook")
								.replace("%time%", String.valueOf(System.currentTimeMillis() - delay))));
						return true;
					}
				}
/*
 * Remove Command
 */
			} else if(args[0].equalsIgnoreCase("remove") && sender.hasPermission("proceduraldungeon.admin.remove")) {
				if(args.length >= 2) {
					if(!Main.getDungeons().containsKey(args[1].toLowerCase())) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("doesnotexist").replace("%dungeon%", args[1])));
						return false;
					}
					DungeonManager dm = Main.getDungeons().get(args[1].toLowerCase());
					String dungeonName = dm.getName();
					// Close all GUIs and remove from memory
					GUIManager.closeAllGUIOf(dm);
					Main.getDungeons().remove(args[1].toLowerCase());
					Main.saveDungeons();
					// Delete the dungeon folder
					try {
						delete(dm.getDungeonFolder());
					} catch (Exception e) {
						e.printStackTrace();
					}
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getSuccessMessage("removedfromconfig").replace("%dungeon%", dungeonName)));
					return true;
				}
/*
 * Delete Command
 */
			} else if(args[0].equalsIgnoreCase("delete") && sender.hasPermission("proceduraldungeon.admin.delete")) {
				if(args.length >= 2) {
					if(!Main.getDungeons().containsKey(args[1].toLowerCase())) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("doesnotexist").replace("%dungeon%", args[1])));
						return false;
					}
					if(Main.getConfirmation().containsKey(sender)) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("importantaction")));
						return false;
					} else {
						Pair<ActionType, String> action = new Pair<ActionType, String>(ActionType.DELETE, args[1]);
						Main.getConfirmation().put(sender, action);
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getOthersMessage("deletedungeon").replace("%dungeon%", Main.getDungeons().get(args[1].toLowerCase()).getName())));
						Bukkit.getScheduler().runTaskLater(Main.getProceduralDungeon(), () -> {
							if(Main.getConfirmation().containsKey(sender)) {
								Main.getConfirmation().remove(sender);
								sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getTaskMessage("actioncanceled")));
							}
						}, 200);
						return true;
					}
				}
/*
 * Confirm Command
 */
			} else if(args[0].equalsIgnoreCase("confirm") && Main.getConfirmation().containsKey(sender)){
				Long delay = System.currentTimeMillis();
				
				Pair<ActionType, String> confirm = Main.getConfirmation().get(sender);
				Main.getConfirmation().remove(sender);
				
// Import Action
				if(confirm.getFirst().equals(ActionType.IMPORT)) {
					if(!sender.hasPermission("proceduraldungeon.admin.create")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("lackingpermission")
								.replace("%permission%", "proceduraldungeon.admin.create")));
						return false;
					}
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getTaskMessage("dungeonimport")));
					
					DungeonManager dm = new DungeonManager(confirm.getSecond().toLowerCase());
					try {
						dm.restoreDungeon();
					} catch (Exception e) {
						e.printStackTrace();
						Main.sendInternalError(sender);
						return false;
					}
					if(Main.getDungeons().containsKey(dm.getName().toLowerCase())) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("alreadyexist")
								.replace("%dungeon%", Main.getDungeons().get(dm.getName().toLowerCase()).getName())));
						return false;
					}
					Main.getDungeons().put(dm.getName().toLowerCase(), dm);
					Main.saveDungeons();
					
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getTaskMessage("timetook")
							.replace("%time%", String.valueOf(System.currentTimeMillis() - delay))));
					return true;
// Delete Action
				} else if(confirm.getFirst().equals(ActionType.DELETE)) {
					if(!sender.hasPermission("proceduraldungeon.admin.delete")) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("lackingpermission")
								.replace("%permission%", "proceduraldungeon.admin.delete")));
						return false;
					}
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getTaskMessage("dungeondelete")));
					
					if(!Main.getDungeons().containsKey(confirm.getSecond().toLowerCase())) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("doesnotexist").replace("%dungeon%", confirm.getSecond())));
						return false;
					}

					try {
						DungeonManager dm = Main.getDungeons().get(confirm.getSecond().toLowerCase());
						List<HumanEntity> closed = GUIManager.closeAllGUIOf(dm);
						Main.getDungeons().remove(confirm.getSecond().toLowerCase());
						Main.saveDungeons();
						delete(dm.getDungeonFolder());
						closed.forEach(human -> human.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getOthersMessage("dungeondeleted"))));
					} catch (Exception e) {
						e.printStackTrace();
						Main.sendInternalError(sender);
						return false;
					}
					
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getTaskMessage("timetook")
							.replace("%time%", String.valueOf(System.currentTimeMillis() - delay))));
					return true;
				}
/*
 * AddRoom
 */
			} else if(args[0].equalsIgnoreCase("addroom") && sender.hasPermission("proceduraldungeon.admin.addroom")) {
				if(!(sender instanceof Player)) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("noconsole")));
					return false;					
				}
				
				if(args.length >= 2) {
					Player p = (Player) sender;
					
					if(!Main.getDungeons().containsKey(args[1].toLowerCase())) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("doesnotexist").replace("%dungeon%", args[1])));
						return false;
					}
					
					Region pr;
					try {
						pr = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(p)).getSelection();
					} catch (IncompleteRegionException e) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("invalidregion")));
						return false;
					}
					
					if(!Main.getExitLocation().containsKey(p)) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("exitnotset")));
						return false;
					}

					Location exitLoc = Main.getExitLocation().get(p);
					Location entranceLoc = p.getLocation().getBlock().getLocation();

					// Detect exit direction (which face of the selection the exit is on)
					Direction exitDir = detectFaceDirection(pr, exitLoc);
					if(exitDir == null || !isOnFace(pr, exitLoc)) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("exitincorectregion")));
						return false;
					}

					// Detect entrance direction (which face of the selection the player/entrance is on)
					Direction entranceDir = detectFaceDirection(pr, entranceLoc);
					if(entranceDir == null || !isOnFace(pr, entranceLoc)) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("entranceincorrectregion")));
						return false;
					}

					// Entrance and exit cannot be on the same face
					if(entranceDir == exitDir) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("sameface")));
						return false;
					}

					DungeonManager dungeon = Main.getDungeons().get(args[1].toLowerCase());
					if(dungeon.getDungeonRooms() == null) {
						Main.sendInternalError(sender);
						throw new NullPointerException("Room manager cannot be null");
					}
					WorldEditSchematic schematic = new WorldEditSchematic();
					File roomFile;
					if(args.length > 2) {
						roomFile = new File(dungeon.getDungeonRooms().getFolder(), args[2].replace(".dungeon", "") + ".dungeon");
						if(roomFile.getName().equals(".dungeon")) {
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("namenull")));
							return false;
						}
						if(roomFile.exists()) {
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("namealreadytaken")));
							return false;
						}
					} else {
						for(int i = 0; true; i++) {
							roomFile = new File(dungeon.getDungeonRooms().getFolder(), "room_" + i + ".dungeon");
							if(!roomFile.exists()) break;
						}
					}
					
					schematic.saveSchematic(entranceLoc, WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(p)), roomFile);
					if(schematic.getSaveException() != null) {
						Main.sendInternalError(sender);
						schematic.getSaveException().printStackTrace();
						return false;
					}
					
					Coordinate coord = generateCoordinate(entranceLoc, exitLoc);
					Room dungeonRoom = new Room(roomFile, coord, entranceDir, exitDir);
					dungeon.getDungeonRooms().addRoom(dungeonRoom);
					
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getSuccessMessage("roomadded")
							.replace("%dungeon%", dungeon.getName())
							.replace("%file%", roomFile.getName())
							.replace("%entrance%", entranceDir.name())
							.replace("%exit%", exitDir.name())));

					GUIManager.reopenDungeonGUI(dungeon);
					return true;
				}
// Set Exit
			} else if(args[0].equalsIgnoreCase("setexit") && sender.hasPermission("proceduraldungeon.admin.addroom")) {
				if(!(sender instanceof Player)) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("noconsole")));
					return false;					
				}
				
				Player p = (Player) sender;
				
				Region pr;
				try {
					pr = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(p)).getSelection();
				} catch (IncompleteRegionException e) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("invalidregion")));
					return false;
				}

				Location exitLoc = p.getLocation().getBlock().getLocation();
				if(!isOnFace(pr, exitLoc)) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("exitincorectlocation")
							.replace("%z%", "")
							.replace("%info%", "The exit must be placed on a face of the selection.")));
					return false;
				}

				Direction exitDir = detectFaceDirection(pr, exitLoc);
				Main.getExitLocation().put(p, exitLoc);
				p.getWorld().spawnEntity(exitLoc.clone().add(0.5, 0, 0.5), EntityType.FIREWORK);
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getSuccessMessage("exitset")
						.replace("%x%", String.valueOf(p.getLocation().getBlockX()))
						.replace("%y%", String.valueOf(p.getLocation().getBlockY()))
						.replace("%z%", String.valueOf(p.getLocation().getBlockZ()))
						.replace("%direction%", exitDir != null ? exitDir.name() : "UNKNOWN")));
				return true;
// Spawnroom — mark a room as SPAWN type
			} else if(args[0].equalsIgnoreCase("spawnroom") && sender.hasPermission("proceduraldungeon.admin.addroom")) {
				if(args.length >= 3) {
					if(!Main.getDungeons().containsKey(args[1].toLowerCase())) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("doesnotexist").replace("%dungeon%", args[1])));
						return false;
					}
					DungeonManager dm = Main.getDungeons().get(args[1].toLowerCase());
					Room target = findRoomInDungeon(dm, args[2]);
					if(target == null) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("roomnotexist").replace("%room%", args[2])));
						return false;
					}
					// Clear previous spawn room
					for(Room r : dm.getDungeonRooms().getRooms()) {
						if(r.getRoomType() == Room.RoomType.SPAWN) r.setRoomType(Room.RoomType.NORMAL);
					}
					target.setRoomType(Room.RoomType.SPAWN);
					dm.getConfig().set("startroom", target.getFile().getName().replace(".dungeon", ""));
					dm.getDungeonConfig().saveConfig();
					dm.getDungeonRooms().saveRooms();
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aRoom &e" + args[2] + " &aset as &bSpawn Room&a."));
					return true;
				}
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /dungeon spawnroom <dungeon> <roomname>"));
				return false;
// Finalroom — mark a room as FINAL/BOSS type
			} else if(args[0].equalsIgnoreCase("finalroom") && sender.hasPermission("proceduraldungeon.admin.addroom")) {
				if(args.length >= 3) {
					if(!Main.getDungeons().containsKey(args[1].toLowerCase())) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("doesnotexist").replace("%dungeon%", args[1])));
						return false;
					}
					DungeonManager dm = Main.getDungeons().get(args[1].toLowerCase());
					Room target = findRoomInDungeon(dm, args[2]);
					if(target == null) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("roomnotexist").replace("%room%", args[2])));
						return false;
					}
					// Clear previous final room
					for(Room r : dm.getDungeonRooms().getRooms()) {
						if(r.getRoomType() == Room.RoomType.FINAL) r.setRoomType(Room.RoomType.NORMAL);
					}
					target.setRoomType(Room.RoomType.FINAL);
					dm.getConfig().set("bossroom", target.getFile().getName().replace(".dungeon", ""));
					dm.getDungeonConfig().saveConfig();
					dm.getDungeonRooms().saveRooms();
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aRoom &e" + args[2] + " &aset as &cBoss/Final Room&a."));
					return true;
				}
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /dungeon finalroom <dungeon> <roomname>"));
				return false;
// Edit
			} else if(args[0].equalsIgnoreCase("edit") && sender.hasPermission("proceduraldungeon.admin.edit")) {
				if(!(sender instanceof Player)) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("noconsole")));
					return false;					
				}
				
				if(args.length == 2) {
					Player p = (Player) sender;
					
					if(!Main.getDungeons().containsKey(args[1].toLowerCase())) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("doesnotexist").replace("%dungeon%", args[1])));
						return false;
					}

					GUIManager.openDungeonGUI(Main.getDungeons().get(args[1].toLowerCase()), p);
					return true;
				} 
				if (args.length >= 3) {
					Player p = (Player) sender;
					
					DungeonManager dungeon = Main.getDungeons().get(args[1].toLowerCase());
					try {
						Room room = dungeon.getDungeonRooms().getRooms().get(Integer.valueOf(args[2]));
						GUIManager.openRoomGUI(dungeon, room, p);
					} catch(IndexOutOfBoundsException | NumberFormatException e) {
						p.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("roomnotexist").replace("%room%", args[2])));
					}
					return true;
				}
/*
 * Play
 * dungeon play dungeon
 * cmd     0    1
 */
			} else if(args[0].equalsIgnoreCase("play")) {
				if(!(sender instanceof Player)) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("noconsole")));
					return false;
				}
				
				if(args.length >= 2) {
					Player p = (Player) sender;

					if(!Main.getDungeons().containsKey(args[1].toLowerCase())) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("doesnotexist").replace("%dungeon%", args[1])));
						return false;
					}

					DungeonManager dungeon = Main.getDungeons().get(args[1].toLowerCase());

					if(dungeon.getDungeonRooms().getRooms().size() == 0) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("norooms")));
						return false;
					}

					if(fr.foxelia.proceduraldungeon.gameplay.DungeonInstanceManager.getInstance().isPlayerInDungeon(p)) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("alreadyindungeon")));
						return false;
					}

					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getTaskMessage("dungeongeneration")));
					fr.foxelia.proceduraldungeon.gameplay.DungeonInstance instance =
							fr.foxelia.proceduraldungeon.gameplay.DungeonInstanceManager.getInstance().createInstance(dungeon, p);

					if(instance == null) {
						Main.sendInternalError(sender);
						return false;
					}

					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getSuccessMessage("dungeonstarted")
							.replace("%dungeon%", dungeon.getName())));
					return true;
				}
				
/*
 * Leave
 */
			} else if(args[0].equalsIgnoreCase("leave")) {
				if(!(sender instanceof Player)) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("noconsole")));
					return false;
				}

				Player p = (Player) sender;
				fr.foxelia.proceduraldungeon.gameplay.DungeonInstance instance =
						fr.foxelia.proceduraldungeon.gameplay.DungeonInstanceManager.getInstance().getPlayerInstance(p);

				if(instance == null) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("notindungeon")));
					return false;
				}

				instance.endSession();
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getSuccessMessage("dungeonleft")));
				return true;

			}
/*
 * End
 */
		}
		
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getErrorMessage("invalidcmd")));
		return false;
	}
	
	private void delete(File file) throws IOException {
		if(file.isDirectory()) {
			for(File subfiles : file.listFiles()) delete(subfiles);
		}
		if(!file.delete()) throw new FileNotFoundException("Failed to delete file: " + file);
	}

	/**
	 * Detect which face of the selection a location is on.
	 * Returns the direction the face is facing outward.
	 * For example, if the point is on the north face (min Z), it returns NORTH.
	 */
	private Direction detectFaceDirection(Region selection, Location loc) {
		int x = loc.getBlockX();
		int z = loc.getBlockZ();

		int minX = selection.getMinimumPoint().getBlockX();
		int maxX = selection.getMaximumPoint().getBlockX();
		int minZ = selection.getMinimumPoint().getBlockZ();
		int maxZ = selection.getMaximumPoint().getBlockZ();

		// Check which face the point is closest to (must be on the face)
		if (z == minZ && x >= minX && x <= maxX) return Direction.NORTH;
		if (z == maxZ && x >= minX && x <= maxX) return Direction.SOUTH;
		if (x == maxX && z >= minZ && z <= maxZ) return Direction.EAST;
		if (x == minX && z >= minZ && z <= maxZ) return Direction.WEST;

		return null;
	}

	/**
	 * Check if a location is on any face of the selection (within Y bounds).
	 */
	private boolean isOnFace(Region selection, Location loc) {
		BlockVector3 pos = BlockVector3.at(loc.getX(), loc.getY(), loc.getZ());
		int y = pos.getBlockY();
		int minY = selection.getMinimumPoint().getBlockY();
		int maxY = selection.getMaximumPoint().getBlockY();
		if (y < minY || y > maxY) return false;
		return detectFaceDirection(selection, loc) != null;
	}

	private Coordinate generateCoordinate(Location origin, Location exit) {
		return new Coordinate(exit.getBlockX() - origin.getBlockX(), exit.getBlockY() - origin.getBlockY(), exit.getBlockZ() - origin.getBlockZ());
	}

	private Room findRoomInDungeon(DungeonManager dm, String roomName) {
		for (Room room : dm.getDungeonRooms().getRooms()) {
			String name = room.getFile().getName().replace(".dungeon", "");
			if (name.equalsIgnoreCase(roomName)) return room;
		}
		return null;
	}
	
	private boolean isNumeric(String strNum) {
	    if(strNum == null) {
	        return false;
	    }
	    try {
	        Double.parseDouble(strNum);
	    } catch (NumberFormatException e) {
	        return false;
	    }
	    return true;
	}
}
