package com.citizens;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.citizens.Commands.CommandHandler;
import com.citizens.Implementations.OperationPurchaser;
import com.citizens.Interfaces.NPCType;
import com.citizens.Listeners.EntityListen;
import com.citizens.Listeners.PlayerListen;
import com.citizens.Listeners.ServerListen;
import com.citizens.Listeners.WorldListen;
import com.citizens.NPCTypes.Blacksmiths.BlacksmithNPC;
import com.citizens.NPCTypes.Guards.GuardNPC;
import com.citizens.NPCTypes.Guards.GuardTask;
import com.citizens.NPCTypes.Healers.HealerNPC;
import com.citizens.NPCTypes.Healers.HealerTask;
import com.citizens.NPCTypes.Landlords.LandlordNPC;
import com.citizens.NPCTypes.Questers.QuesterNPC;
import com.citizens.NPCTypes.Questers.Quests.QuestManager;
import com.citizens.NPCTypes.Traders.TraderNPC;
import com.citizens.NPCTypes.Wizards.WizardNPC;
import com.citizens.NPCTypes.Wizards.WizardTask;
import com.citizens.NPCs.NPCManager;
import com.citizens.NPCs.NPCTypeManager;
import com.citizens.Properties.PropertyHandler;
import com.citizens.Properties.PropertyManager;
import com.citizens.Properties.Properties.BlacksmithProperties;
import com.citizens.Properties.Properties.GuardProperties;
import com.citizens.Properties.Properties.HealerProperties;
import com.citizens.Properties.Properties.LandlordProperties;
import com.citizens.Properties.Properties.QuesterProperties;
import com.citizens.Properties.Properties.TraderProperties;
import com.citizens.Properties.Properties.UtilityProperties;
import com.citizens.Properties.Properties.WizardProperties;
import com.citizens.Resources.NPClib.HumanNPC;
import com.citizens.Resources.nijikokun.register.payment.Method;
import com.citizens.Resources.sk89q.CitizensCommandsManager;
import com.citizens.Resources.sk89q.CommandPermissionsException;
import com.citizens.Resources.sk89q.CommandUsageException;
import com.citizens.Resources.sk89q.MissingNestedCommandException;
import com.citizens.Resources.sk89q.RequirementMissingException;
import com.citizens.Resources.sk89q.ServerCommandException;
import com.citizens.Resources.sk89q.UnhandledCommandException;
import com.citizens.Resources.sk89q.WrappedCommandException;
import com.citizens.Utils.MessageUtils;
import com.citizens.Utils.Messaging;
import com.citizens.Utils.StringUtils;

/**
 * Citizens - NPCs for Bukkit
 */
public class Citizens extends JavaPlugin {
	public static Citizens plugin;

	public static Method economy;

	public static final String separatorChar = "/";

	private static final String codename = "Odyssey";
	private static final String letter = "b";
	private static final String version = "1.0.9" + letter;

	public static CitizensCommandsManager<Player> commands = new CitizensCommandsManager<Player>();

	public static boolean initialized = false;

	@Override
	public void onEnable() {
		plugin = this;
	
		// Register NPC types.
		registerTypes();
	
		// Register our commands.
		CommandHandler.registerCommands();
	
		// Register our events.
		new EntityListen().registerEvents();
		new WorldListen().registerEvents();
		new ServerListen().registerEvents();
		new PlayerListen().registerEvents();
	
		// Register files.
		PropertyManager.registerProperties();
	
		// Initialize Permissions.
		Permission.initialize(Bukkit.getServer());
	
		// Load settings.
		Constants.setupVariables();
	
		// schedule Creature tasks
		getServer().getScheduler().scheduleSyncRepeatingTask(this,
				new CreatureTask(), Constants.spawnTaskDelay,
				Constants.spawnTaskDelay);
		getServer().getScheduler().scheduleSyncRepeatingTask(this,
				new CreatureTask.CreatureTick(), 0, 1);
	
		// Reinitialize existing NPCs. Scheduled tasks run once all plugins are
		// loaded -> gives multiworld support.
		if (getServer().getScheduler().scheduleSyncDelayedTask(this,
				new Runnable() {
					@Override
					public void run() {
						setupNPCs();
					}
				}) == -1) {
			Messaging
					.log("Issue with scheduled loading of pre-existing NPCs. There may be a multiworld error.");
			setupNPCs();
		}
	
		// Schedule tasks TODO - Genericify
		getServer().getScheduler().scheduleSyncRepeatingTask(this,
				new TickTask(Constants.npcRange), Constants.tickDelay,
				Constants.tickDelay);
		getServer().getScheduler().scheduleSyncRepeatingTask(this,
				new GuardTask(), Constants.tickDelay, Constants.tickDelay);
		if (Constants.useSaveTask) {
			getServer().getScheduler().scheduleSyncRepeatingTask(this,
					new Runnable() {
						@Override
						public void run() {
							Messaging.log("Saving npc files to disk...");
							PropertyManager.saveState();
							Messaging.log("Saved.");
						}
					}, Constants.saveDelay, Constants.saveDelay);
		}
	
		QuestManager.initialize();
		Messaging.log("version [" + getVersion() + "] (" + codename
				+ ") loaded");
	}

	@Override
	public void onDisable() {
		// Save the local copy of our files to disk.
		PropertyManager.saveState();
		NPCManager.despawnAll();
		CreatureTask.despawnAll();

		Messaging.log("version [" + getVersion() + "] (" + codename
				+ ") disabled");
	}

	@Override
	public void onLoad() {
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		Player player = null;
		if (sender instanceof Player)
			player = (Player) sender;
		try {
			// must put command into split.
			String[] split = new String[args.length + 1];
			System.arraycopy(args, 0, split, 1, args.length);
			split[0] = command.getName().toLowerCase();

			String modifier = "";
			if (args.length > 0)
				modifier = args[0];

			// No command found!
			if (!commands.hasCommand(split[0], modifier)) {
				if (!modifier.isEmpty()) {
					boolean value = handleMistake(sender, split[0], modifier);
					return value;
				}
			}

			HumanNPC npc = null;
			if (player != null && NPCManager.validateSelected(player)) {
				npc = NPCManager.get(NPCManager.getSelected(player));
			}
			try {
				commands.execute(split, player, player == null ? sender
						: player, npc);
			} catch (ServerCommandException e) {
				sender.sendMessage(e.getMessage());
			} catch (CommandPermissionsException e) {
				Messaging.sendError(sender, MessageUtils.noPermissionsMessage);
			} catch (MissingNestedCommandException e) {
				Messaging.sendError(player, e.getUsage());
			} catch (CommandUsageException e) {
				Messaging.sendError(player, e.getMessage());
				Messaging.sendError(player, e.getUsage());
			} catch (RequirementMissingException e) {
				Messaging.sendError(player, e.getMessage());
			} catch (WrappedCommandException e) {
				throw e.getCause();
			} catch (UnhandledCommandException e) {
				return false;
			}
		} catch (NumberFormatException e) {
			Messaging.sendError(player, "That is not a valid number.");
		} catch (Throwable excp) {
			excp.printStackTrace();
			Messaging.sendError(player,
					"Please report this error: [See console]");
			Messaging.sendError(player,
					excp.getClass().getName() + ": " + excp.getMessage());
		}
		return true;
	}

	/**
	 * Get the current version of Citizens
	 * 
	 * @return
	 */
	public static String getVersion() {
		return version;
	}

	private boolean handleMistake(CommandSender sender, String command,
			String modifier) {
		String[] modifiers = commands.getAllCommandModifiers(command);
		Map<Integer, String> values = new TreeMap<Integer, String>();
		int i = 0;
		for (String string : modifiers) {
			values.put(StringUtils.getLevenshteinDistance(modifier, string),
					modifiers[i]);
			++i;
		}
		int best = 0;
		boolean stop = false;
		Set<String> possible = new HashSet<String>();
		for (Entry<Integer, String> entry : values.entrySet()) {
			if (!stop) {
				best = entry.getKey();
				stop = true;
			} else if (entry.getKey() > best) {
				break;
			}
			possible.add(entry.getValue());
		}
		if (possible.size() > 0) {
			sender.sendMessage(ChatColor.GRAY + "Spatny prikaz. Mel jsi na mysli:");
			for (String string : possible) {
				sender.sendMessage(StringUtils.wrap("    /") + command + " "
						+ StringUtils.wrap(string));
			}
			return true;
		}
		return false;
	}

	private void registerTypes() {
		OperationPurchaser purchaser = new OperationPurchaser();
		NPCTypeManager.registerType(new NPCType("blacksmith",
				new BlacksmithProperties(), purchaser, BlacksmithNPC.class),
				true);
		NPCTypeManager.registerType(new NPCType("guard", new GuardProperties(),
				purchaser, GuardNPC.class), true);
		NPCTypeManager.registerType(new NPCType("healer",
				new HealerProperties(), purchaser, HealerNPC.class), true);
		NPCTypeManager.registerType(new NPCType("landlord",
				new LandlordProperties(), purchaser, LandlordNPC.class), true);
		NPCTypeManager.registerType(new NPCType("quester",
				new QuesterProperties(), purchaser, QuesterNPC.class), true);
		NPCTypeManager.registerType(new NPCType("trader",
				new TraderProperties(), purchaser, TraderNPC.class), true);
		NPCTypeManager.registerType(new NPCType("wizard",
				new WizardProperties(), purchaser, WizardNPC.class), true);
	}

	private void setupNPCs() {
		// TODO REMOVE AFTER 1.0.9 IS RELEASED
		if (Constants.convertOld) {
			Messaging.log("Converting old nodes to new save system...");
			PropertyHandler locations = new PropertyHandler(
					"plugins/Citizens/Basic NPCs/Citizens.locations", false);
			String[] list = locations.getString("list").split(",");
			if (list.length > 0 && !list[0].isEmpty()) {
				for (String name : list) {
					int UID = Integer.parseInt(name.split("_")[0]);
					Conversion.convert(UID, name.split("_")[1]);
				}
			}
			Messaging
					.log("Finished conversion. You must delete all old files manually.");
			UtilityProperties.getSettings().setBoolean("general.convert-old",
					false);
			PropertyManager.getNPCProfiles().save();
		}
		PropertyManager.getNPCProfiles().load();
		StringBuilder UIDList = new StringBuilder();
		List<Integer> sorted = PropertyManager.getNPCProfiles().getIntegerKeys(
				null);
		Collections.sort(sorted);
		int max = sorted.size() == 0 ? 0 : sorted.get(sorted.size() - 1), count = 0;
		while (count <= max) {
			if (PropertyManager.getNPCProfiles().pathExists(count)) {
				UIDList.append(count + ",");
			}
			++count;
		}
		String[] values = UIDList.toString().split(",");

		if (values.length > 0 && !values[0].isEmpty()) {
			for (String value : values) {
				int UID = Integer.parseInt(value);
				Location loc = PropertyManager.getBasic().getLocation(UID);
				if (loc != null) {
					NPCManager.register(UID, PropertyManager.getBasic()
							.getOwner(UID));
					ArrayDeque<String> text = PropertyManager.getBasic()
							.getText(UID);
					if (text != null) {
						NPCManager.setText(UID, text);
					}
				}
			}
		}
		Messaging.log("Loaded " + NPCManager.GlobalUIDs.size() + " NPCs.");
		getServer().getScheduler().scheduleSyncRepeatingTask(this,
				new HealerTask(), HealerTask.getHealthRegenRate(),
				HealerTask.getHealthRegenRate());
		getServer().getScheduler().scheduleSyncRepeatingTask(this,
				new WizardTask(), Constants.wizardManaRegenRate,
				Constants.wizardManaRegenRate);
		initialized = true;
	}

	/**
	 * A method used for iConomy support.
	 * 
	 * @param iConomy
	 *            plugin
	 * @return
	 */
	public static boolean setMethod(Method method) {
		if (economy == null) {
			economy = method;
			return true;
		}
		return false;
	}

	/**
	 * Returns whether the given item ID is usable as a tool.
	 * 
	 * @param key
	 * @param type
	 * @param sneaking
	 * 
	 * @return Whether the ID is used for a tool.
	 */
	public boolean validateTool(String key, int type, boolean sneaking) {
		if (Constants.useItemList) {
			String[] items = UtilityProperties.getSettings().getString(key)
					.split(",");
			List<String> item = Arrays.asList(items);
			if (item.contains("*")) {
				return true;
			}
			boolean isShift;
			for (String s : item) {
				isShift = false;
				if (s.contains("SHIFT-")) {
					s = s.replace("SHIFT-", "");
					isShift = true;
				}
				if (Integer.parseInt(s) == type && isShift == sneaking) {
					return true;
				}
			}
			return false;
		}
		return true;
	}
}