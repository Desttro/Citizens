package com.citizens.Utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.citizens.Resources.NPClib.HumanNPC;

public class Messaging {
	private static final Logger log = Logger.getLogger("Minecraft");
	private final static boolean debug = false;
	private final static String[] colours = { "black", "dblue", "dgreen",
			"dteal", "dred", "purple", "gold", "gray", "dgray", "blue",
			"bgreen", "teal", "red", "pink", "yellow", "white" };

	public static void send(Player player, HumanNPC npc, String message) {
		message = colourise(StringUtils.colourise(message));
		message = message.replace("<h>", "" + player.getHealth());
		message = message.replace("<name>", player.getName());
		message = message.replace("<world>", player.getWorld().getName());
		if (npc != null) {
			message = message.replace("<npc>", npc.getStrippedName());
			message = message.replace("<npcid>", "" + npc.getUID());
		}
		player.sendMessage(message);
	}

	static void send(CommandSender sender, HumanNPC npc, String message) {
		if (sender instanceof Player) {
			send((Player) sender, npc, message);
			return;
		}
		message = colourise(StringUtils.colourise(message));
		if (npc != null) {
			message = message.replace("<npc>", npc.getStrippedName());
			message = message.replace("<npcid>", "" + npc.getUID());
		}
		sender.sendMessage(message);
	}

	private static String colourise(String message) {
		byte index = 0;
		for (String colour : colours) {
			message = message.replaceAll("<" + colour + ">",
					"" + ChatColor.getByCode(index));
			++index;
		}
		for (int colour = 0; colour <= 16; ++colour) {
			message = message.replaceAll("<" + colour + ">",
					"" + ChatColor.getByCode(colour));
		}
		message = message.replaceAll("<g>", "" + ChatColor.GREEN);
		message = message.replaceAll("<y>", "" + ChatColor.YELLOW);
		return message;
	}

	public static void log(Object... messages) {
		StringBuilder builder = new StringBuilder();
		for (Object string : messages) {
			builder.append(string.toString() + " ");
		}
		log(builder.toString(), Level.INFO);
	}

	public static void log(Object message, Level level) {
		log.log(level, "[Citizens] " + message);
	}

	public static void log(Object message) {
		log(message, Level.INFO);
	}

	public static void debug(Object message) {
		if (debug) {
			log(message);
		}
	}

	public static void sendError(Player player, String error) {
		send(player, null, ChatColor.RED + error);
	}

	public static void sendError(CommandSender sender, String error) {
		send(sender, null, ChatColor.RED + error);
	}

	public static void sendUncertain(String name, String message) {
		Player player = Bukkit.getServer().getPlayer(name);
		if (player != null)
			send(player, null, message);
	}
}