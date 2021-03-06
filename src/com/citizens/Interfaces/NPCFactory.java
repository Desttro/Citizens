package com.citizens.Interfaces;

import java.lang.reflect.Constructor;

import com.citizens.Resources.NPClib.HumanNPC;

public class NPCFactory {
	private Constructor<? extends Toggleable> constructor = null;

	public NPCFactory(Class<? extends Toggleable> clazz) {
		try {
			this.constructor = clazz.getConstructor(HumanNPC.class);
		} catch (Exception ex) {
		}
	}

	public Toggleable create(HumanNPC npc) {
		try {
			return constructor.newInstance(npc);
		} catch (Exception ex) {
			return null;
		}
	}
}