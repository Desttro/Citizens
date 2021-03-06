package com.citizens.Properties.Properties;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.citizens.Constants;
import com.citizens.Interfaces.Saveable;
import com.citizens.NPCTypes.Guards.GuardManager.GuardType;
import com.citizens.NPCTypes.Guards.GuardNPC;
import com.citizens.NPCs.NPCTypeManager;
import com.citizens.Properties.PropertyManager;
import com.citizens.Resources.NPClib.HumanNPC;

public class GuardProperties extends PropertyManager implements Saveable {
	private static final String isGuard = ".guard.toggle";
	private static final String type = ".guard.type";
	private static final String radius = ".guard.radius";
	private static final String blacklist = ".guard.blacklist";
	private static final String whitelist = ".guard.whitelist";
	private static final String aggressive = ".guard.aggressive";

	private void saveProtectionRadius(int UID, double rad) {
		profiles.setDouble(UID + radius, rad);
	}

	private double getProtectionRadius(int UID) {
		return profiles.getDouble(UID + radius,
				Constants.defaultBouncerProtectionRadius);
	}

	private void saveAggressive(int UID, boolean aggro) {
		profiles.setBoolean(UID + aggressive, aggro);
	}

	private boolean getAggressive(int UID) {
		return profiles.getBoolean(UID + aggressive);
	}

	private GuardType getGuardType(int UID) {
		return GuardType.parse(profiles.getString(UID + type));
	}

	private void saveGuardType(int UID, GuardType guardType) {
		profiles.setString(UID + type, guardType.name());
	}

	private Set<String> getBlacklist(int UID) {
		String save = profiles.getString(UID + blacklist);
		Set<String> mobs = new HashSet<String>();
		Collections.addAll(mobs, save.split(","));
		return mobs;
	}

	private void saveBlacklist(int UID, Set<String> mobs) {
		StringBuilder save = new StringBuilder();
		for (String mob : mobs) {
			save.append(mob + ",");
		}
		profiles.setString(UID + blacklist, save.toString());
	}

	private Set<String> getWhitelist(int UID) {
		String save = profiles.getString(UID + whitelist);
		Set<String> players = new HashSet<String>();
		Collections.addAll(players, save.split(","));
		return players;
	}

	private void saveWhitelist(int UID, Set<String> players) {
		StringBuilder save = new StringBuilder();
		for (String player : players) {
			save.append(player + ",");
		}
		profiles.setString(UID + whitelist, save.toString());
	}

	@Override
	public void saveState(HumanNPC npc) {
		if (exists(npc)) {
			boolean is = npc.isType("guard");
			setEnabled(npc, is);
			if (is) {
				GuardNPC guard = npc.getToggleable("guard");
				saveGuardType(npc.getUID(), guard.getGuardType());
				saveBlacklist(npc.getUID(), guard.getBlacklist());
				saveWhitelist(npc.getUID(), guard.getWhitelist());
				saveProtectionRadius(npc.getUID(), guard.getProtectionRadius());
				saveAggressive(npc.getUID(), guard.isAggressive());
			}
		}
	}

	@Override
	public void loadState(HumanNPC npc) {
		if (getEnabled(npc)) {
			npc.registerType("guard", NPCTypeManager.getFactory("guard"));
			GuardNPC guard = npc.getToggleable("guard");
			guard.setGuardType(getGuardType(npc.getUID()));
			guard.setBlacklist(getBlacklist(npc.getUID()));
			guard.setWhitelist(getWhitelist(npc.getUID()));
			guard.setProtectionRadius(getProtectionRadius(npc.getUID()));
			guard.setAggressive(getAggressive(npc.getUID()));
		}
		saveState(npc);
	}

	@Override
	public void register(HumanNPC npc) {
		setEnabled(npc, true);
	}

	@Override
	public void setEnabled(HumanNPC npc, boolean value) {
		profiles.setBoolean(npc.getUID() + isGuard, value);
	}

	@Override
	public boolean getEnabled(HumanNPC npc) {
		return profiles.getBoolean(npc.getUID() + isGuard);
	}

	@Override
	public void copy(int UID, int nextUID) {
		if (profiles.pathExists(UID + isGuard)) {
			profiles.setString(nextUID + isGuard,
					profiles.getString(UID + isGuard));
		}
		if (profiles.pathExists(UID + type)) {
			profiles.setString(nextUID + type, profiles.getString(UID + type));
		}
		if (profiles.pathExists(UID + blacklist)) {
			profiles.setString(nextUID + blacklist,
					profiles.getString(UID + blacklist));
		}
		if (profiles.pathExists(UID + whitelist)) {
			profiles.setString(nextUID + whitelist,
					profiles.getString(UID + whitelist));
		}
		if (profiles.pathExists(UID + radius)) {
			profiles.setString(nextUID + radius,
					profiles.getString(UID + radius));
		}
		if (profiles.pathExists(UID + aggressive)) {
			profiles.setString(nextUID + aggressive,
					profiles.getString(UID + aggressive));
		}
	}
}