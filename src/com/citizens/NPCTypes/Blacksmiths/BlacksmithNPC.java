package com.citizens.NPCTypes.Blacksmiths;

import org.bukkit.entity.Player;

import com.citizens.Permission;
import com.citizens.Economy.EconomyHandler.Operation;
import com.citizens.Interfaces.Clickable;
import com.citizens.Interfaces.Toggleable;
import com.citizens.Resources.NPClib.HumanNPC;
import com.citizens.Utils.MessageUtils;

public class BlacksmithNPC extends Toggleable implements Clickable {

	/**
	 * Blacksmith NPC object
	 * 
	 * @param npc
	 */
	public BlacksmithNPC(HumanNPC npc) {
		super(npc);
	}

	@Override
	public String getType() {
		return "blacksmith";
	}

	@Override
	public void onLeftClick(Player player, HumanNPC npc) {
	}

	@Override
	public void onRightClick(Player player, HumanNPC npc) {
		if (Permission.canUse(player, npc, getType())) {
			Operation op = null;
			if (BlacksmithManager.validateTool(player.getItemInHand())) {
				op = Operation.BLACKSMITH_TOOLREPAIR;
			} else if (BlacksmithManager.validateArmor(player.getItemInHand())) {
				op = Operation.BLACKSMITH_ARMORREPAIR;
			}
			if (op != null) {
				BlacksmithManager.buyRepair(player, npc, op);
			}
		} else {
			player.sendMessage(MessageUtils.noPermissionsMessage);
		}
	}
}