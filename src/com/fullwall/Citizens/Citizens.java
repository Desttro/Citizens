package com.fullwall.Citizens;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import com.fullwall.Citizens.Commands.CommandHandler;
import com.fullwall.Citizens.Listeners.CustomListen;
import com.fullwall.Citizens.Listeners.EntityListen;
import com.fullwall.Citizens.Listeners.PluginListen;
import com.fullwall.Citizens.Listeners.WorldListen;
import com.fullwall.Citizens.NPCTypes.Bandits.BanditTask;
import com.fullwall.Citizens.NPCTypes.Guards.GuardTask;
import com.fullwall.Citizens.NPCTypes.Healers.HealerTask;
import com.fullwall.Citizens.NPCTypes.Wizards.WizardTask;
import com.fullwall.Citizens.NPCs.BasicNPCHandler;
import com.fullwall.Citizens.NPCs.NPCManager;
import com.fullwall.Citizens.Properties.PropertyManager;
import com.fullwall.Citizens.Properties.Properties.UtilityProperties;
import com.fullwall.Citizens.Utils.Messaging;
import com.fullwall.resources.redecouverte.NPClib.HumanNPC;
import com.nijikokun.register.payment.Method;

/**
 * Citizens for Bukkit
 * 
 * @author fullwall
 */
public class Citizens extends JavaPlugin {
	private final EntityListen entityListener = new EntityListen(this);
	private final CustomListen customListener = new CustomListen(this);
	private final WorldListen worldListener = new WorldListen(this);
	private final PluginListen serverListener = new PluginListen(this);
	public final BasicNPCHandler basicNPCHandler = new BasicNPCHandler(this);

	public static Citizens plugin;

	public static Method economy;

	public static String separatorChar = "/";

	private static final String codename = "Realist";
	private static final String letter = "f";
	private static String version = "1.0.8" + letter;

	/**
	 * Returns the current version of Citizens
	 * 
	 * @return
	 */
	private static String getVersion() {
		return version;
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
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Get the health regeneration rate for a healer based on its level
	 * 
	 * @return
	 */
	private int getHealthRegenRate() {
		int delay = 0;
		if (!NPCManager.getList().isEmpty()) {
			for (Entry<Integer, HumanNPC> entry : NPCManager.getList()
					.entrySet()) {
				delay = Constants.healerHealthRegenIncrement
						* (11 - (entry.getValue().getHealer().getLevel()));
			}
		} else {
			delay = 12000;
		}
		return delay;
	}

	@Override
	public void onDisable() {
		// Save the local copy of our files to disk.
		PropertyManager.stateSave();

		basicNPCHandler.despawnAll();

		Messaging.log("version [" + getVersion() + "] (" + codename
				+ ") disabled");
	}

	@Override
	public void onEnable() {
		// Register our commands.
		CommandHandler commandHandler = new CommandHandler(this);
		commandHandler.registerCommands();

		// Register our events.
		entityListener.registerEvents();
		customListener.registerEvents();
		worldListener.registerEvents();
		serverListener.registerEvents();

		// Register files.
		PropertyManager.registerProperties();

		// set up settings and economy file variables
		Constants.setupVariables();

		plugin = this;

		// Reinitialise existing NPCs. Scheduled tasks run once all plugins are
		// loaded. This means that multiworld will now work without hacky
		// workarounds.
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
				new TickTask(this, Constants.npcRange), Constants.tickDelay,
				Constants.tickDelay);
		getServer().getScheduler().scheduleSyncRepeatingTask(this,
				new GuardTask(this), Constants.tickDelay, Constants.tickDelay);
		getServer().getScheduler().scheduleSyncRepeatingTask(this,
				new BanditTask(this), Constants.tickDelay, Constants.tickDelay);
		if (Constants.useSaveTask) {
			getServer().getScheduler().scheduleSyncRepeatingTask(this,
					new Runnable() {
						@Override
						public void run() {
							Messaging.log("Saving npc files to disk...");
							PropertyManager.stateSave();
							Messaging.log("Saved.");
						}
					}, Constants.saveDelay, Constants.saveDelay);
		}

		Messaging.log("version [" + getVersion() + "] (" + codename
				+ ") loaded");
	}

	@Override
	public void onLoad() {
	}

	private void setupNPCs() {
		// Start reloading old NPCs from the config files.
		String[] list = PropertyManager.getBasic().locations.getString("list")
				.split(",");
		if (list.length > 0 && !list[0].isEmpty()) {
			for (String name : list) {
				Location loc = PropertyManager.getBasic().getLocation(
						Integer.valueOf(name.split("_")[0]));
				if (loc != null) {
					NPCManager.register(
							Integer.valueOf(name.split("_")[0]),
							PropertyManager.getBasic().getOwner(
									Integer.valueOf(name.split("_")[0])));
					ArrayDeque<String> text = PropertyManager.getBasic()
							.getText(Integer.valueOf(name.split("_")[0]));
					if (text != null) {
						NPCManager.setText(Integer.valueOf(name.split("_")[0]),
								text);
					}
				} else {
					PropertyManager.getBasic().deleteNameFromList(name);
				}
			}
		}
		Messaging.log("Loaded " + NPCManager.GlobalUIDs.size() + " NPCs.");
		getServer().getScheduler().scheduleSyncRepeatingTask(this,
				new HealerTask(), getHealthRegenRate(), getHealthRegenRate());
		getServer().getScheduler().scheduleSyncRepeatingTask(this,
				new WizardTask(), Constants.wizardManaRegenRate,
				Constants.wizardManaRegenRate);
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
			boolean isShift = false;
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

	/**
	 * Checks if UID is being used by an NPC.
	 * 
	 * @param UID
	 *            to check.
	 * @return Whether it is used.
	 */
	public boolean validateUID(int UID) {
		if (NPCManager.GlobalUIDs.containsKey(UID)) {
			return true;
		}
		return false;
	}
}