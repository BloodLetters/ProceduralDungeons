package fr.foxelia.proceduraldungeon.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import fr.foxelia.proceduraldungeon.Main;
import fr.foxelia.proceduraldungeon.utilities.DungeonManager;

public class DungeonCommandCompleter implements TabCompleter {

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String str, String[] args) {
		ArrayList<String> suggest = new ArrayList<>();
		
		if(args.length == 1) {
			// /dungeon
			suggest.add("version");
			if(sender.hasPermission("proceduraldungeon.admin.reload")) suggest.add("reload");
			if(sender.hasPermission("proceduraldungeon.admin.create")) suggest.add("create");
			if(sender.hasPermission("proceduraldungeon.admin.remove")) suggest.add("remove");
			if(sender.hasPermission("proceduraldungeon.admin.delete")) suggest.add("delete");
			if(sender.hasPermission("proceduraldungeon.admin.edit") && sender instanceof Player) suggest.add("edit");
			if(sender.hasPermission("proceduraldungeon.admin.addroom") && sender instanceof Player) {
				suggest.add("addroom");
				suggest.add("setexit");
				suggest.add("spawnroom");
				suggest.add("finalroom");
			}
			if(sender.hasPermission("proceduraldungeon.admin.generate")) suggest.add("play");
			if(sender instanceof Player) suggest.add("leave");
			if(sender.hasPermission("proceduraldungeon.admin.list")) suggest.add("list");
			if(sender.hasPermission("proceduraldungeon.dungeon")) suggest.add("help");
			if(Main.getConfirmation().containsKey(sender)) suggest.add("confirm");
		} else if(args.length == 2) {
// /dungeon remove
			if(sender.hasPermission("proceduraldungeon.admin.remove") && args[0].equalsIgnoreCase("remove")) {
				for(DungeonManager dm : Main.getDungeons().values()) {
					suggest.add(dm.getName());
				}
// /dungeon delete
			} else if(sender.hasPermission("proceduraldungeon.admin.delete") && args[0].equalsIgnoreCase("delete")) {
				for(DungeonManager dm : Main.getDungeons().values()) {
					suggest.add(dm.getName());
				}
// /dungeon addroom
			} else if(sender.hasPermission("proceduraldungeon.admin.addroom") && args[0].equalsIgnoreCase("addroom") && sender instanceof Player) {
				for(DungeonManager dm : Main.getDungeons().values()) {
					suggest.add(dm.getName());
				}
// /dungeon edit
			} else if(sender.hasPermission("proceduraldungeon.admin.edit") && args[0].equalsIgnoreCase("edit") && sender instanceof Player) {
				for(DungeonManager dm : Main.getDungeons().values()) {
					suggest.add(dm.getName());
				}
// /dungeon play
			} else if(sender.hasPermission("proceduraldungeon.admin.generate") && args[0].equalsIgnoreCase("play")) {
				for(DungeonManager dm : Main.getDungeons().values()) {
					suggest.add(dm.getName());
				}
// /dungeon spawnroom / finalroom
			} else if(sender.hasPermission("proceduraldungeon.admin.addroom") &&
					(args[0].equalsIgnoreCase("spawnroom") || args[0].equalsIgnoreCase("finalroom"))) {
				for(DungeonManager dm : Main.getDungeons().values()) {
					suggest.add(dm.getName());
				}
			}
		} else if(args.length >= 3 && args.length <= 5) {
// /dungeon edit {dungeon}
			if(sender.hasPermission("proceduraldungeon.admin.edit") && args[0].equalsIgnoreCase("edit")) {
				// room number suggestions could go here
			}
// /dungeon spawnroom/finalroom {dungeon} {room}
			if(args.length == 3 && sender.hasPermission("proceduraldungeon.admin.addroom") &&
					(args[0].equalsIgnoreCase("spawnroom") || args[0].equalsIgnoreCase("finalroom"))) {
				if(Main.getDungeons().containsKey(args[1].toLowerCase())) {
					DungeonManager dm = Main.getDungeons().get(args[1].toLowerCase());
					for(fr.foxelia.proceduraldungeon.utilities.rooms.Room room : dm.getDungeonRooms().getRooms()) {
						suggest.add(room.getFile().getName().replace(".dungeon", ""));
					}
				}
			}
		}
		
		return suggest;
	}

}
