package com.citizens.NPCTypes.Questers.QuestTypes;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPlaceEvent;

import com.citizens.NPCTypes.Questers.Objectives.Objectives.ObjectiveCycler;
import com.citizens.NPCTypes.Questers.Quests.QuestIncrementer;
import com.citizens.Resources.NPClib.HumanNPC;

public class BuildQuest extends QuestIncrementer {
	public BuildQuest(HumanNPC npc, Player player, String questName,
			ObjectiveCycler objectives) {
		super(npc, player, questName, objectives);
	}

	@Override
	public void updateProgress(Event event) {
		if (event instanceof BlockPlaceEvent) {
			if (((BlockPlaceEvent) event).getBlockPlaced().getType() == this.objective
					.getMaterial()) {
				this.getProgress().incrementCompleted(1);
			}
		}
	}

	@Override
	public boolean isCompleted() {
		return this.getProgress().getAmount() >= this.objective.getAmount();
	}
}