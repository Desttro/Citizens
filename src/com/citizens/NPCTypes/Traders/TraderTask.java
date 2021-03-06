package com.citizens.NPCTypes.Traders;

import java.util.HashMap;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.InventoryPlayer;
import net.minecraft.server.Packet103SetSlot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftInventoryPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.citizens.Economy.EconomyHandler;
import com.citizens.Economy.Payment;
import com.citizens.Economy.ServerEconomyInterface;
import com.citizens.NPCTypes.Traders.TraderManager.Mode;
import com.citizens.Resources.NPClib.HumanNPC;
import com.citizens.Utils.MessageUtils;
import com.citizens.Utils.StringUtils;

public class TraderTask implements Runnable {
	private final HumanNPC npc;
	private final CraftPlayer player;
	private int taskID;
	private int prevTraderSlot = -1;
	private int prevPlayerSlot = -1;
	private final PlayerInventory previousTraderInv;
	private final PlayerInventory previousPlayerInv;
	private final Mode mode;
	private boolean stop;
	private boolean said;

	/**
	 * Gets run every tick, checks the inventory for changes.
	 * 
	 * @param npc
	 * @param player
	 * @param plugin
	 * @param mode
	 */
	public TraderTask(HumanNPC npc, Player player, Mode mode) {
		this.npc = npc;
		this.player = (CraftPlayer) player;
		// Create the inventory objects
		this.previousTraderInv = new CraftInventoryPlayer(new InventoryPlayer(
				null));
		this.previousPlayerInv = new CraftInventoryPlayer(new InventoryPlayer(
				null));
		// clone the items to the newly created inventory objects
		clonePlayerInventory(npc.getInventory(), this.previousTraderInv);
		clonePlayerInventory(player.getInventory(), this.previousPlayerInv);

		this.mode = mode;
		sendJoinMessage();
	}

	@Override
	public synchronized void run() {
		if (stop) {
			return;
		}
		if ((npc == null || player == null
				|| checkContainer(player.getHandle()) || !player.isOnline())
				&& !said) {
			kill();
			return;
		}
		if (mode == Mode.STOCK) {
			return;
		}
		stop = true;
		int count = 0;
		boolean found = false;
		net.minecraft.server.ItemStack inhand = player.getHandle().inventory
				.j();
		for (ItemStack i : npc.getInventory().getContents()) {
			if (!previousTraderInv.getItem(count).equals(i) && inhand == null) {
				rewind();
				break;
			}
			if (!previousTraderInv.getItem(count).equals(i)
					&& previousTraderInv.getItem(count).getTypeId() == inhand.id) {
				found = true;
				handleTraderClick(count, npc.getInventory());
				break;
			}
			count += 1;
		}

		count = 0;
		if (!found) {
			for (ItemStack i : player.getInventory().getContents()) {
				if (!previousPlayerInv.getItem(count).equals(i)
						&& inhand == null) {
					rewind();
					break;
				}
				if (!previousPlayerInv.getItem(count).equals(i)
						&& previousPlayerInv.getItem(count).getTypeId() == inhand.id) {
					handlePlayerClick(count, player.getInventory());
					break;
				}
				count += 1;
			}
		}

		clonePlayerInventory(npc.getInventory(), this.previousTraderInv);
		clonePlayerInventory(player.getInventory(), this.previousPlayerInv);

		// Set the itemstack in the player's cursor to null.
		player.getHandle().inventory.b((net.minecraft.server.ItemStack) null);
		// Get rid of the picture on the cursor.
		Packet103SetSlot packet = new Packet103SetSlot(-1, -1, null);
		player.getHandle().netServerHandler.sendPacket(packet);
		stop = false;
	}

	public void addID(int ID) {
		this.taskID = ID;
	}

	public void kill() {
		TraderNPC trader = npc.getToggleable("trader");
		trader.setFree(true);
		if (!said) {
			sendLeaveMessage();
			said = true;
		}
		int index = TraderManager.tasks.indexOf(taskID);
		if (index != -1) {
			TraderManager.tasks.remove(TraderManager.tasks.indexOf(taskID));
		}
		run();
		Bukkit.getServer().getScheduler().cancelTask(taskID);
	}

	private boolean checkContainer(EntityPlayer handle) {
		return handle.activeContainer == handle.defaultContainer;
	}

	@SuppressWarnings("deprecation")
	private void handleTraderClick(int slot, PlayerInventory npcInv) {
		npcInv.setItem(slot, previousTraderInv.getItem(slot));
		Stockable stockable = getStockable(npcInv.getItem(slot), "sold", false);
		if (stockable == null) {
			return;
		}
		if (prevTraderSlot != slot) {
			prevTraderSlot = slot;
			sendStockableMessage(stockable);
			return;
		}
		prevTraderSlot = slot;
		prevPlayerSlot = -1;
		if (checkMiscellaneous(npcInv, stockable, true)) {
			return;
		}
		// /trader sell 1:1 1:5
		// stocking is 1 stone
		// price is 5 stone
		ItemStack buying = stockable.getStocking().clone();
		EconomyHandler.pay(new Payment(stockable.getPrice()), player, -1);
		if (mode != Mode.INFINITE) {
			EconomyHandler.pay(new Payment(buying), npc, slot);
		}
		HashMap<Integer, ItemStack> unbought = player.getInventory().addItem(
				buying);
		if (unbought.size() >= 1) {
			rewind();
			player.sendMessage(ChatColor.RED
					+ "Not enough room in your inventory to add "
					+ MessageUtils.getStackString(buying, ChatColor.RED) + ".");
			return;
		}
		if (!stockable.isEconPlugin() && mode != Mode.INFINITE) {
			ItemStack temp = stockable.getPrice().getItemStack().clone();
			unbought = npc.getInventory().addItem(temp);
			if (unbought.size() >= 1) {
				rewind();
				player.sendMessage(ChatColor.RED
						+ "Not enough room in the npc's inventory to add "
						+ MessageUtils.getStackString(stockable.getPrice()
								.getItemStack(), ChatColor.RED) + ".");
				return;
			}
		} else {
			double price = stockable.getPrice().getPrice();
			npc.setBalance(npc.getBalance() + price);
		}
		npc.getPlayer().updateInventory();
		player.updateInventory();
		player.sendMessage(ChatColor.GREEN + "Transaction successful.");
	}

	@SuppressWarnings("deprecation")
	private void handlePlayerClick(int slot, PlayerInventory playerInv) {
		playerInv.setItem(slot, previousPlayerInv.getItem(slot));
		Stockable stockable = getStockable(playerInv.getItem(slot), "bought",
				true);
		if (stockable == null) {
			return;
		}
		if (prevPlayerSlot != slot) {
			prevPlayerSlot = slot;
			sendStockableMessage(stockable);
			return;
		}
		prevPlayerSlot = slot;
		prevTraderSlot = -1;
		if (checkMiscellaneous(playerInv, stockable, false)) {
			return;
		}
		ItemStack selling = stockable.getStocking().clone();
		if (mode != Mode.INFINITE) {
			EconomyHandler.pay(new Payment(stockable.getPrice()), npc, -1);
		}
		EconomyHandler.pay(new Payment(selling), player, slot);
		HashMap<Integer, ItemStack> unsold = new HashMap<Integer, ItemStack>();
		if (mode != Mode.INFINITE) {
			unsold = npc.getInventory().addItem(selling);
		}
		if (unsold.size() >= 1) {
			rewind();
			player.sendMessage(ChatColor.RED
					+ "Not enough room available to add "
					+ MessageUtils.getStackString(selling, ChatColor.RED)
					+ " to the trader's stock.");
			return;
		}
		if (!stockable.isEconPlugin()) {
			ItemStack temp = stockable.getPrice().getItemStack().clone();
			unsold = player.getInventory().addItem(temp);
			if (unsold.size() >= 1) {
				rewind();
				player.sendMessage(ChatColor.RED
						+ "Not enough room in your inventory to add "
						+ MessageUtils.getStackString(stockable.getPrice()
								.getItemStack(), ChatColor.RED) + ".");
				return;
			}
		} else {
			double price = stockable.getPrice().getPrice();
			ServerEconomyInterface.add(player.getName(), price);
		}
		npc.getPlayer().updateInventory();
		player.updateInventory();
		player.sendMessage(ChatColor.GREEN + "Transaction successful.");
	}

	@SuppressWarnings("deprecation")
	private void rewind() {
		player.getInventory().setContents(previousPlayerInv.getContents());
		npc.getInventory().setContents(previousTraderInv.getContents());
		player.updateInventory();
		npc.getPlayer().updateInventory();
	}

	private boolean checkMiscellaneous(PlayerInventory inv,
			Stockable stockable, boolean buying) {
		ItemStack stocking = stockable.getStocking();
		if (buying) {
			if (!EconomyHandler.canBuy(new Payment(stocking), npc)) {
				sendNoMoneyMessage(stocking, true);
				return true;
			}
			if (!EconomyHandler.canBuy(new Payment(stockable.getPrice()),
					player)) {
				sendNoMoneyMessage(stocking, false);
				return true;
			}
		} else {
			if (!EconomyHandler.canBuy(new Payment(stocking), player)) {
				sendNoMoneyMessage(stocking, true);
				return true;
			}
			if (!EconomyHandler.canBuy(new Payment(stockable.getPrice()), npc)) {
				sendNoMoneyMessage(stocking, false);
				return true;
			}
		}
		return false;
	}

	private void sendNoMoneyMessage(ItemStack stocking, boolean selling) {
		selling = !selling;
		String start = "The trader doesn't";
		if (selling) {
			start = "You don't";
		}
		player.sendMessage(ChatColor.RED + start
				+ " have enough money available to buy "
				+ MessageUtils.getStackString(stocking) + ".");
	}

	private void sendStockableMessage(Stockable stockable) {
		String[] message = MessageUtils.getStockableMessage(stockable,
				ChatColor.AQUA).split("for");
		player.sendMessage(ChatColor.AQUA + "Item: " + message[0].trim());
		player.sendMessage(ChatColor.AQUA + "Price: " + message[1].trim());
		player.sendMessage(ChatColor.GOLD + "Click to confirm.");
	}

	private Stockable getStockable(ItemStack item, String keyword,
			boolean selling) {
		// durability needs to be reset to 0 for tools / weapons / armor
		short durability = item.getDurability();
		if (isTool(item.getTypeId())) {
			durability = 0;
		}
		TraderNPC trader = npc.getToggleable("trader");
		if (!trader.isStocked(item.getTypeId(), durability, selling)) {
			player.sendMessage(StringUtils.wrap(
					MessageUtils.getItemName(item.getTypeId()), ChatColor.RED)
					+ " isn't being " + keyword + " here.");
			return null;
		}
		return trader.getStockable(item.getTypeId(), durability, selling);
	}

	private void sendJoinMessage() {
		switch (mode) {
		case INFINITE:
		case NORMAL:
			player.sendMessage(ChatColor.GREEN + "Transaction log");
			player.sendMessage(ChatColor.GOLD
					+ "-------------------------------");
			break;
		case STOCK:
			player.sendMessage(ChatColor.GOLD + "Stocking of "
					+ StringUtils.wrap(npc.getStrippedName(), ChatColor.GOLD)
					+ " started.");
			break;
		}
	}

	private void sendLeaveMessage() {
		switch (mode) {
		case INFINITE:
		case NORMAL:
			player.sendMessage(ChatColor.GOLD
					+ "-------------------------------");
			break;
		case STOCK:
			player.sendMessage(ChatColor.GOLD + "Stocking of "
					+ StringUtils.wrap(npc.getStrippedName(), ChatColor.GOLD)
					+ " finished.");
			break;
		}
	}

	private ItemStack cloneItemStack(ItemStack source) {
		if (source == null) {// sanity check
			return null;
		}
		return new ItemStack(source.getType(), source.getAmount(),
				source.getDurability(), (source.getData() != null ? source
						.getData().getData() : null));
	}

	/**
	 * Clones the first passed PlayerInventory object to the second one.
	 * 
	 * @param source
	 * @param target
	 */
	private void clonePlayerInventory(PlayerInventory source,
			PlayerInventory target) {
		ItemStack[] contents = new ItemStack[source.getContents().length];
		System.arraycopy(source.getContents(), 0, contents, 0, contents.length);
		target.setContents(contents);

		target.setHelmet(cloneItemStack(source.getHelmet()));
		target.setChestplate(cloneItemStack(source.getChestplate()));
		target.setLeggings(cloneItemStack(source.getLeggings()));
		target.setBoots(cloneItemStack(source.getBoots()));
	}

	/**
	 * Determines if the passed itemId is a tool, weapon, or armor. (ie. has
	 * durability)
	 * 
	 * @param itemId
	 */
	private boolean isTool(int id) {
		return ((id >= 256 && id <= 259) || (id >= 267 && id <= 279)
				|| (id >= 283 && id <= 286) || (id >= 290 && id <= 294)
				|| (id >= 298 && id <= 317) || id == 346);
	}
}
