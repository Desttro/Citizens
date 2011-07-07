package com.citizens.Commands.Commands;

import java.util.ArrayDeque;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.citizens.Citizens;
import com.citizens.Constants;
import com.citizens.Permission;
import com.citizens.Economy.EconomyHandler;
import com.citizens.Economy.EconomyHandler.Operation;
import com.citizens.NPCs.NPCDataManager;
import com.citizens.NPCs.NPCManager;
import com.citizens.Properties.PropertyManager;
import com.citizens.Properties.Properties.UtilityProperties;
import com.citizens.Resources.NPClib.HumanNPC;
import com.citizens.Resources.sk89q.Command;
import com.citizens.Resources.sk89q.CommandContext;
import com.citizens.Resources.sk89q.CommandPermissions;
import com.citizens.Resources.sk89q.CommandRequirements;
import com.citizens.Resources.sk89q.ServerCommand;
import com.citizens.Utils.HelpUtils;
import com.citizens.Utils.MessageUtils;
import com.citizens.Utils.Messaging;
import com.citizens.Utils.ServerUtils;
import com.citizens.Utils.StringUtils;

@CommandRequirements(requireSelected = true, requireOwnership = true)
public class BasicCommands {

	@Command(
			aliases = "citizens",
			usage = "",
			desc = "zobrazi Citizens info",
			modifiers = "",
			min = 0,
			max = 0)
	@ServerCommand()
	@CommandPermissions("admin")
	@CommandRequirements()
	public static void viewInfo(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		sender.sendMessage(ChatColor.GREEN
				+ StringUtils.listify(StringUtils.wrap("Citizens")));
		sender.sendMessage(ChatColor.GREEN + "  Verze: "
				+ StringUtils.wrap(Citizens.getVersion()));
		sender.sendMessage(ChatColor.GREEN + "  Autori: ");
		sender.sendMessage(ChatColor.YELLOW + "      - fullwall");
		sender.sendMessage(ChatColor.YELLOW + "      - aPunch");
	}

	@Command(
			aliases = "citizens",
			usage = "help (strana)",
			desc = "zobrazi pomocnou stranku Citizens",
			modifiers = "help",
			min = 1,
			max = 2)
	@CommandPermissions("use.basic")
	@CommandRequirements()
	@ServerCommand()
	public static void sendCitizensHelp(CommandContext args,
			CommandSender sender, HumanNPC npc) {
		int page = 1;
		if (args.argsLength() == 2) {
			page = Integer.parseInt(args.getString(1));
		}
		HelpUtils.sendHelpPage(sender, page);
	}

	@CommandRequirements()
	@Command(
			aliases = "citizens",
			usage = "reload",
			desc = "obnovi Citizens",
			modifiers = "reload",
			min = 1,
			max = 1)
	@CommandPermissions("admin")
	@ServerCommand()
	public static void reload(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		Messaging.log("Obnovuji konfiguraci....");
		sender.sendMessage(ChatColor.GREEN + "[" + StringUtils.wrap("Citizens")
				+ "] Obnovuji....");

		UtilityProperties.initialize();
		PropertyManager.loadAll();
		Constants.setupVariables();

		Messaging.log("Obnoveno.");
		sender.sendMessage(ChatColor.GREEN + "[" + StringUtils.wrap("Citizens")
				+ "] Obnoveno.");
	}

	@CommandRequirements()
	@Command(
			aliases = "citizens",
			usage = "save",
			desc = "ulozi Citizens soubory",
			modifiers = "save",
			min = 1,
			max = 1)
	@ServerCommand()
	@CommandPermissions("admin")
	public static void forceSave(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		if (sender instanceof Player)
			Messaging.log("Ukladani...");
		sender.sendMessage(ChatColor.GREEN + "[" + StringUtils.wrap("Citizens")
				+ "] Ukladani...");

		PropertyManager.saveState();

		if (sender instanceof Player)
			Messaging.log("Ulozeno.");
		sender.sendMessage(ChatColor.GREEN + "[" + StringUtils.wrap("Citizens")
				+ "] Ulozeno.");
	}

	@CommandRequirements()
	@Command(
			aliases = { "basic", "npc" },
			usage = "help (strana)",
			desc = "zobrazi (zakladni) Basic NPC pomocnou stranu",
			modifiers = "help",
			min = 1,
			max = 2)
	@CommandPermissions("use.basic")
	@ServerCommand()
	public static void sendBasicHelp(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		int page = 1;
		if (args.argsLength() == 2) {
			page = Integer.parseInt(args.getString(1));
		}
		HelpUtils.sendBasicHelpPage(sender, page);
	}

	@CommandRequirements()
	@Command(
			aliases = "npc",
			usage = "create [jmeno] (text)",
			desc = "vytvori NPC",
			modifiers = "create",
			min = 2)
	@CommandPermissions("create.basic")
	public static void createNPC(CommandContext args, Player player,
			HumanNPC npc) {
		if (UtilityProperties.getNPCCount(player.getName()) >= Constants.maxNPCsPerPlayer
				&& !Permission.isAdmin(player)) {
			player.sendMessage(MessageUtils.reachedNPCLimitMessage);
			return;
		}
		ArrayDeque<String> texts = new ArrayDeque<String>();
		String firstArg = args.getString(1);
		if (args.argsLength() >= 3) {
			texts.add(args.getJoinedStrings(2));
		}
		if (firstArg.length() > 16) {
			player.sendMessage(ChatColor.RED
					+ "Jmeno tohoto NPC bude zkaceno - maximalni delka je 16.");
			firstArg = args.getString(1).substring(0, 16);
		}
		int UID = NPCManager.register(firstArg, player.getLocation(),
				player.getName());
		NPCManager.setText(UID, texts);

		HumanNPC created = NPCManager.get(UID);
		created.getNPCData().setOwner(player.getName());
		Messaging.send(player, created, Constants.creationMessage);

		if (EconomyHandler.useEconomy()) {
			double paid = EconomyHandler.pay(Operation.BASIC_CREATION, player);
			if (paid > 0) {
				player.sendMessage(MessageUtils.getPaidMessage(
						Operation.BASIC_CREATION, paid, firstArg, "", false));
			}
		}

		// TODO Uncomment after BukkitContrib is updated to MC 1.7.2
		// Achievements.award(player, Achievement.NPC_CREATE);
		NPCManager.selectNPC(player, NPCManager.get(UID));
		Messaging.send(player, created, Constants.selectionMessage);
	}

	@Command(
			aliases = "npc",
			usage = "move",
			desc = "premisti NPC",
			modifiers = "move",
			min = 1,
			max = 1)
	@CommandPermissions("modify.basic")
	public static void moveNPC(CommandContext args, Player player, HumanNPC npc) {
		player.sendMessage(StringUtils.wrap(npc.getStrippedName())
				+ " je na ceste do vasi lokace!");
		npc.teleport(player.getLocation());
		npc.getNPCData().setLocation(player.getLocation());
	}

	@Command(
			aliases = "npc",
			usage = "moveTo [x y z](world pitch yaw)",
			desc = "premisti NPC do lokace",
			modifiers = "moveto",
			min = 4,
			max = 4)
	@CommandPermissions("modify.basic")
	public static void moveNPCToLocation(CommandContext args, Player player,
			HumanNPC npc) {
		int index = args.argsLength() - 1;
		double x = 0, y = 0, z = 0;
		float yaw = npc.getLocation().getYaw();
		float pitch = npc.getLocation().getPitch();
		String world = "";
		switch (args.argsLength() - 1) {
		case 6:
			yaw = Float.parseFloat(args.getString(index));
			--index;
		case 5:
			pitch = Float.parseFloat(args.getString(index));
			--index;
		case 4:
			world = args.getString(index);
			--index;
		case 3:
			z = Double.parseDouble(args.getString(index));
			--index;
		case 2:
			y = Double.parseDouble(args.getString(index));
			--index;
		case 1:
			x = Double.parseDouble(args.getString(index));
		}
		if (Bukkit.getServer().getWorld(world) == null) {
			player.sendMessage("Invalid world.");
			return;
		}
		npc.teleport(new Location(Bukkit.getServer().getWorld(world), x, y, z,
				pitch, yaw));
	}

	@Command(
			aliases = "npc",
			usage = "copy",
			desc = "zkopiruje NPC",
			modifiers = "copy",
			min = 1,
			max = 1)
	@CommandPermissions("create.basic")
	public static void copyNPC(CommandContext args, Player player, HumanNPC npc) {
		if (UtilityProperties.getNPCCount(player.getName()) >= Constants.maxNPCsPerPlayer
				&& !Permission.isAdmin(player)) {
			player.sendMessage(MessageUtils.reachedNPCLimitMessage);
			return;
		}
		PropertyManager.save(npc);
		int newUID = NPCManager.register(npc.getName(), player.getLocation(),
				player.getName());
		HumanNPC newNPC = NPCManager.get(newUID);
		newNPC.teleport(player.getLocation());
		newNPC.getNPCData().setLocation(player.getLocation());
		PropertyManager.copyNPCs(npc.getUID(), newUID);
		PropertyManager.load(newNPC);
	}

	@Command(
			aliases = "npc",
			usage = "remove (all)",
			desc = "smaze NPC(cka)",
			modifiers = "remove",
			min = 1,
			max = 2)
	@CommandPermissions("modify.basic")
	public static void removeNPCs(CommandContext args, Player player,
			HumanNPC npc) {
		if (args.argsLength() == 2 && args.getString(1).equalsIgnoreCase("all")) {
			if (Permission.isAdmin(player)) {
				NPCManager.removeAll();
				NPCManager.deselectNPC(player);
				player.sendMessage(ChatColor.GRAY + "NPC(cka) zmizel(a).");
			} else {
				player.sendMessage(MessageUtils.noPermissionsMessage);
			}
			return;
		}
		NPCManager.remove(npc.getUID());
		NPCManager.deselectNPC(player);
		player.sendMessage(StringUtils.wrap(npc.getName(), ChatColor.GRAY)
				+ " zmizel(a).");
	}

	@Command(
			aliases = "npc",
			usage = "rename [jmeno]",
			desc = "prejmenuje NPC",
			modifiers = "rename",
			min = 2,
			max = 2)
	@CommandPermissions("modify.basic")
	public static void renameNPC(CommandContext args, Player player,
			HumanNPC npc) {
		String name = args.getString(1);
		if (name.length() > 16) {
			player.sendMessage(ChatColor.RED
					+ "Maximalni delka jmena je 16 - jmeno NPC bude zkraceno.");
			name = name.substring(0, 16);
		}
		NPCManager.rename(npc.getUID(), name, npc.getOwner());
		player.sendMessage(ChatColor.GREEN + StringUtils.wrap(npc.getName())
				+ "' jmneno nastaveno na " + StringUtils.wrap(name) + ".");
	}

	@Command(
			aliases = "npc",
			usage = "color [kod-barvy]",
			desc = "nastavi barvu jmena NPC",
			modifiers = "color",
			min = 2,
			max = 2)
	@CommandPermissions("modify.basic")
	public static void setNPCColour(CommandContext args, Player player,
			HumanNPC npc) {
		if (!args.getString(1).substring(0, 1).equals("&")) {
			player.sendMessage(ChatColor.RED + "Pouzij & k specifikaci barvy.");
		} else if (args.getString(1).length() != 2) {
			player.sendMessage(ChatColor.GRAY
					+ "Pouzij format &(kod). Priklad - &f = bila.");
		} else {
			int colour = 0xf;
			try {
				colour = Integer.parseInt(args.getString(1).substring(1, 2));
			} catch (NumberFormatException ex) {
				try {
					colour = Integer.parseInt(
							args.getString(1).substring(1, 2), 16);
				} catch (NumberFormatException e) {
					player.sendMessage(ChatColor.RED + "Spatny kod barvy.");
					return;
				}
			}
			npc.getNPCData().setColour(colour);
			NPCManager.setColour(npc.getUID(), npc.getOwner());
			player.sendMessage(StringUtils.wrapFull("{" + npc.getStrippedName()
					+ "}'s barva jmena je ted "
					+ args.getString(1).replace("&", "\u00A7") + "tato}."));
		}
	}

	@Command(
			aliases = "npc",
			usage = "set [text]",
			desc = "nastavi text pro NPC",
			modifiers = "set",
			min = 2)
	@CommandPermissions("modify.basic")
	public static void setNPCText(CommandContext args, Player player,
			HumanNPC npc) {
		String text = args.getJoinedStrings(1);
		ArrayDeque<String> texts = new ArrayDeque<String>();
		texts.add(text);
		NPCManager.setText(npc.getUID(), texts);
		player.sendMessage(StringUtils.wrapFull("{" + npc.getName()
				+ "}'s text byl zmenen na {" + text + "}."));
	}

	@Command(
			aliases = "npc",
			usage = "add [text]",
			desc = "prida text pro NPC",
			modifiers = "add",
			min = 2)
	@CommandPermissions("modify.basic")
	public static void addNPCText(CommandContext args, Player player,
			HumanNPC npc) {
		String text = args.getJoinedStrings(1);
		NPCManager.addText(npc.getUID(), text);
		player.sendMessage(StringUtils.wrap(text) + " byl pridan pro "
				+ StringUtils.wrap(npc.getStrippedName() + "'s") + " text.");
	}

	@Command(
			aliases = "npc",
			usage = "reset",
			desc = "resetuje text NPC",
			modifiers = "reset",
			min = 1,
			max = 1)
	@CommandPermissions("modify.basic")
	public static void resetNPCText(CommandContext args, Player player,
			HumanNPC npc) {
		NPCManager.resetText(npc.getUID());
		player.sendMessage(StringUtils.wrap(npc.getStrippedName() + "'s")
				+ " text byl resetovan!");
	}

	@Command(
			aliases = "npc",
			usage = "item [item]",
			desc = "nastavi predmet v NPC's ruce",
			modifiers = "item",
			min = 2,
			max = 2)
	@CommandPermissions("modify.basic")
	public static void setNPCItemInHand(CommandContext args, Player player,
			HumanNPC npc) {
		NPCDataManager.setItemInHand(player, npc, args.getString(1));
	}

	@Command(
			aliases = "npc",
			usage = "armor [armor] [item]",
			desc = "nastavi brneni pro NPC",
			modifiers = "armor",
			min = 3,
			max = 3)
	@CommandPermissions("modify.basic")
	public static void setNPCArmor(CommandContext args, Player player,
			HumanNPC npc) {
		Material mat = StringUtils.parseMaterial(args.getString(2));
		if (mat == null) {
			player.sendMessage(ChatColor.RED + "Neplatny predmet.");
			return;
		}
		if (mat != Material.AIR && !player.getInventory().contains(mat)) {
			player.sendMessage(ChatColor.RED
					+ "Musis mit predmet v inventari pokud jej chces dat NPC.");
			return;
		}
		if ((mat.getId() < 298 || mat.getId() > 317)
				&& (mat.getId() != 86 && mat.getId() != 91)) {
			player.sendMessage(ChatColor.GRAY
					+ "Tohle nemuze byt pouzito jako brneni.");
			return;
		}
		int slot = player.getInventory().first(mat);
		ItemStack item = NPCDataManager.decreaseItemStack(player.getInventory()
				.getItem(slot));
		player.getInventory().setItem(slot, item);
		ArrayList<Integer> items = npc.getNPCData().getItems();

		if (args.getString(1).contains("helm")) {
			items.set(1, mat.getId());
		} else if (args.getString(1).equalsIgnoreCase("torso")) {
			items.set(2, mat.getId());
		} else if (args.getString(1).contains("leg")) {
			items.set(3, mat.getId());
		} else if (args.getString(1).contains("boot")) {
			items.set(4, mat.getId());
		}
		npc.getNPCData().setItems(items);
		NPCDataManager.addItems(npc, items);

		// Despawn the old NPC, register our new one.
		NPCManager.removeForRespawn(npc.getUID());
		NPCManager.register(npc.getUID(), npc.getOwner());

		player.sendMessage(StringUtils.wrap(npc.getName())
				+ "'s brneni bylo nastaveno na "
				+ StringUtils.wrap(MessageUtils.getMaterialName(mat.getId()))
				+ ".");
	}

	@CommandRequirements()
	@Command(
			aliases = "npc",
			usage = "tp",
			desc = "teleport k NPC",
			modifiers = "tp",
			min = 1,
			max = 1)
	@CommandPermissions("use.basic")
	public static void teleportToNPC(CommandContext args, Player player,
			HumanNPC npc) {
		player.teleport(npc.getNPCData().getLocation());
		player.sendMessage(ChatColor.GREEN + "Byl jsi teleportovan k "
				+ StringUtils.wrap(npc.getStrippedName()) + ". Uzij si to!");
	}

	@Command(
			aliases = "npc",
			usage = "talkclose [true|false]",
			desc = "nastavi aby NPC mluvilo kdyz se k nemu nekdo postavi blizko",
			modifiers = "talkclose",
			min = 2,
			max = 2)
	@CommandPermissions("modify.basic")
	public static void changeNPCTalkWhenClose(CommandContext args,
			Player player, HumanNPC npc) {
		boolean talk = false;
		if (args.getString(1).equals("true")) {
			talk = true;
		}
		npc.getNPCData().setTalkClose(talk);
		if (talk) {
			player.sendMessage(StringUtils.wrap(npc.getStrippedName())
					+ " bude ted mluvit k hracum stojicim blizko.");
		} else if (!talk) {
			player.sendMessage(StringUtils.wrap(npc.getStrippedName())
					+ " nebude uz mluvit k hracum stojicim blizkos.");
		}
	}

	@Command(
			aliases = "npc",
			usage = "lookat [true|false]",
			desc = "nastavi aby se NPC divalo na hrace kdyz se k nemu nekdo blizko",
			modifiers = "lookat",
			min = 2,
			max = 2)
	@CommandPermissions("modify.basic")
	public static void changeNPCLookWhenClose(CommandContext args,
			Player player, HumanNPC npc) {
		boolean look = false;
		if (args.getString(1).equals("true")) {
			look = true;
		}
		npc.getNPCData().setLookClose(look);
		if (look) {
			player.sendMessage(StringUtils.wrap(npc.getStrippedName())
					+ " se odted bude divat na hrace.");
		} else if (!look) {
			player.sendMessage(StringUtils.wrap(npc.getStrippedName())
					+ " se odted nebude divat na hrace.");
		}
	}

	@CommandRequirements(requireSelected = true)
	@Command(
			aliases = "npc",
			usage = "id",
			desc = "zobrazi ID NPC",
			modifiers = "id",
			min = 1,
			max = 1)
	@CommandPermissions("use.basic")
	public static void displayNPCID(CommandContext args, Player player,
			HumanNPC npc) {
		player.sendMessage(ChatColor.GREEN + "ID tohoto NPC je "
				+ StringUtils.wrap("" + npc.getUID()) + ".");
	}

	@Command(
			aliases = "npc",
			usage = "select [id]",
			desc = "vybere NPC podle ID",
			modifiers = "select",
			min = 2,
			max = 2)
	@CommandPermissions("use.basic")
	@CommandRequirements()
	public static void selectNPC(CommandContext args, Player player,
			HumanNPC npc) {
		npc = NPCManager.get(Integer.valueOf(args.getString(1)));
		if (npc == null) {
			player.sendMessage(ChatColor.RED + "NPC s ID "
					+ StringUtils.wrap(args.getString(1), ChatColor.RED)
					+ " neexistuje.");
		} else {
			NPCManager.selectNPC(player, npc);
			Messaging.send(player, npc, Constants.selectionMessage);
		}
	}

	@CommandRequirements(requireSelected = true)
	@Command(
			aliases = "npc",
			usage = "owner",
			desc = "zjisti vlastnika NPC",
			modifiers = "owner",
			min = 1,
			max = 1)
	@CommandPermissions("use.basic")
	public static void getNPCOwner(CommandContext args, Player player,
			HumanNPC npc) {
		player.sendMessage(ChatColor.GREEN + "Vlastnik tohoto NPC je "
				+ StringUtils.wrap(npc.getOwner()) + ".");
	}

	@CommandRequirements(requireSelected = true)
	@Command(
			aliases = "npc",
			usage = "setowner [name]",
			desc = "nastavi vlastnika NPC",
			modifiers = "setowner",
			min = 2,
			max = 2)
	public static void setNPCOwner(CommandContext args, Player player,
			HumanNPC npc) {
		if (Permission.isAdmin(player)
				|| Permission.canModify(player, npc, "basic")) {
			player.sendMessage(ChatColor.GREEN + "Vlastnik NPC "
					+ StringUtils.wrap(npc.getStrippedName()) + " je ted "
					+ StringUtils.wrap(args.getString(1)) + ".");
			npc.getNPCData().setOwner(args.getString(1));
		} else {
			player.sendMessage(MessageUtils.noPermissionsMessage);
		}
	}

	@Command(
			aliases = "npc",
			usage = "/npc [path|waypoints] (reset)",
			desc = "zapne editaci waypointu",
			modifiers = { "path", "waypoints" },
			min = 1,
			max = 2)
	@CommandPermissions("modify.basic")
	public static void editWaypoints(CommandContext args, Player player,
			HumanNPC npc) {
		if (args.length() == 2) {
			Integer editing = NPCDataManager.pathEditors.get(player.getName());
			int UID = npc.getUID();
			if (editing == null) {
				player.sendMessage(ChatColor.AQUA
						+ StringUtils.listify("Ovladani editace waypointu"));
				player.sendMessage(StringUtils.wrap("Left")
						+ " click adds a waypoint, while "
						+ StringUtils.wrap("right") + " click acts as an undo.");
				player.sendMessage(StringUtils.wrap("Repeat")
						+ " this command to finish.");
				editing = UID;
				npc.setPaused(true);
			} else if (editing == UID) {
				player.sendMessage(StringUtils.wrap("Finished")
						+ " editing waypoints.");
				editing = null;
				npc.setPaused(false);
			} else if (editing != UID) {
				player.sendMessage(ChatColor.GRAY + "Now editing "
						+ StringUtils.wrap(npc.getStrippedName())
						+ "'s waypoints.");
				editing = UID;
			}
			NPCDataManager.pathEditors.put(player.getName(), editing);
		} else if (args.length() >= 3 && args.getString(1).equals("reset")) {
			npc.getWaypoints().resetWaypoints();
			player.sendMessage(ChatColor.GREEN + "Waypoints "
					+ StringUtils.wrap("reset") + ".");
		}
	}

	@CommandRequirements()
	@Command(
			aliases = "npc",
			usage = "list (name) (page)",
			desc = "zobrazi seznam NPCs",
			modifiers = "list",
			min = 1,
			max = 3)
	@CommandPermissions("use.basic")
	public static void displayNPCList(CommandContext args, Player player,
			HumanNPC npc) {
		switch (args.argsLength()) {
		case 1:
			MessageUtils.displayNPCList(player, player, npc, "1");
			break;
		case 2:
			if (StringUtils.isNumber(args.getString(1))) {
				MessageUtils.displayNPCList(player, player, npc,
						args.getString(1));
			} else {
				if (ServerUtils.matchPlayer(args.getString(1)) != null) {
					MessageUtils.displayNPCList(player,
							ServerUtils.matchPlayer(args.getString(1)), npc,
							"1");
				} else {
					player.sendMessage(ChatColor.RED
							+ "Nemuzu nalezt hrace.");
				}
			}
			break;
		case 3:
			if (ServerUtils.matchPlayer(args.getString(1)) != null) {
				MessageUtils.displayNPCList(player,
						ServerUtils.matchPlayer(args.getString(1)), npc,
						args.getString(2));
			} else {
				player.sendMessage(ChatColor.RED + "Nemuzu nalezt hrace.");
			}
			break;
		}
	}
}