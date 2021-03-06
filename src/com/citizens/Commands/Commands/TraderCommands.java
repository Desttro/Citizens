package com.citizens.Commands.Commands;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.citizens.Economy.EconomyHandler;
import com.citizens.Economy.Payment;
import com.citizens.Economy.ServerEconomyInterface;
import com.citizens.NPCTypes.Traders.Check;
import com.citizens.NPCTypes.Traders.ItemPrice;
import com.citizens.NPCTypes.Traders.Stockable;
import com.citizens.NPCTypes.Traders.TraderNPC;
import com.citizens.Resources.NPClib.HumanNPC;
import com.citizens.Resources.sk89q.Command;
import com.citizens.Resources.sk89q.CommandContext;
import com.citizens.Resources.sk89q.CommandPermissions;
import com.citizens.Resources.sk89q.CommandRequirements;
import com.citizens.Resources.sk89q.ServerCommand;
import com.citizens.Utils.HelpUtils;
import com.citizens.Utils.MessageUtils;
import com.citizens.Utils.Messaging;
import com.citizens.Utils.PageUtils;
import com.citizens.Utils.PageUtils.PageInstance;
import com.citizens.Utils.StringUtils;

@CommandRequirements(
		requireSelected = true,
		requireOwnership = true,
		requiredType = "trader")
public class TraderCommands {

	@CommandRequirements()
	@Command(
			aliases = "trader",
			usage = "help",
			desc = "view the trader help page",
			modifiers = "help",
			min = 1,
			max = 1)
	@CommandPermissions("use.trader")
	@ServerCommand()
	public static void sendTraderHelp(CommandContext args,
			CommandSender sender, HumanNPC npc) {
		HelpUtils.sendTraderHelp(sender);
	}

	/**
	 * Display a trader's balance
	 * 
	 * @param player
	 * @param npc
	 */
	@Command(
			aliases = "trader",
			usage = "money (give|take) (amount)",
			desc = "control a trader's balance",
			modifiers = "money",
			min = 1,
			max = 3)
	@CommandPermissions("use.trader")
	public static void controlMoney(CommandContext args, Player player,
			HumanNPC npc) {
		if (!EconomyHandler.useEconPlugin()) {
			player.sendMessage(MessageUtils.noEconomyMessage);
			return;
		}
		switch (args.argsLength()) {
		case 1:
			player.sendMessage(StringUtils.wrap(npc.getName())
					+ " has "
					+ StringUtils.wrap(ServerEconomyInterface.format(npc
							.getBalance())) + ".");
			break;
		case 3:
			double amount;
			try {
				amount = Double.parseDouble(args.getString(2));
			} catch (NumberFormatException e) {
				player.sendMessage(ChatColor.RED
						+ "Invalid balance change amount entered.");
				return;
			}
			if (args.getString(1).contains("g")) {
				if (EconomyHandler.canBuy(new Payment(amount, true), player)) {
					EconomyHandler.pay(new Payment(-amount, true), npc, -1);
					EconomyHandler.pay(new Payment(amount, true), player, -1);
					player.sendMessage(ChatColor.GREEN
							+ "Gave "
							+ StringUtils.wrap(ServerEconomyInterface
									.format(amount))
							+ " to "
							+ StringUtils.wrap(npc.getStrippedName())
							+ ". Your balance is now "
							+ StringUtils.wrap(ServerEconomyInterface
									.getFormattedBalance(player.getName()),
									ChatColor.GREEN) + ".");
				} else {
					player.sendMessage(ChatColor.RED
							+ "You don't have enough money for that! Need "
							+ StringUtils.wrap(ServerEconomyInterface
									.format(amount
											- ServerEconomyInterface
													.getBalance(player
															.getName())),
									ChatColor.RED) + " more.");
				}
			} else if (args.getString(1).contains("t")) {
				if (EconomyHandler.canBuy(new Payment(amount, true), npc)) {
					EconomyHandler.pay(new Payment(amount, true), npc, -1);
					EconomyHandler.pay(new Payment(-amount, true), player, -1);
					player.sendMessage(ChatColor.GREEN
							+ "Took "
							+ StringUtils.wrap(ServerEconomyInterface
									.format(amount))
							+ " from "
							+ StringUtils.wrap(npc.getStrippedName())
							+ ". Your balance is now "
							+ StringUtils.wrap(ServerEconomyInterface
									.getFormattedBalance(player.getName()))
							+ ".");
				} else {
					player.sendMessage(ChatColor.RED
							+ "The trader doesn't have enough money for that! It needs "
							+ StringUtils.wrap(
									ServerEconomyInterface.format(amount
											- npc.getBalance()), ChatColor.RED)
							+ " more in its balance.");
				}
			} else {
				player.sendMessage(ChatColor.RED + "Invalid argument type "
						+ StringUtils.wrap(args.getString(1), ChatColor.RED)
						+ ".");
			}
			break;
		default:
			Messaging.sendError(player, "Incorrect syntax. See /trader help");
			break;
		}

	}

	/**
	 * Displays a list of buying/selling items for the selected trader npc.
	 * 
	 * @param player
	 * @param npc
	 * @param args
	 * @param selling
	 */
	@CommandRequirements(requiredType = "trader", requireSelected = true)
	@Command(
			aliases = "trader",
			usage = "list [buy|sell]",
			desc = "view a trader's buying/selling list",
			modifiers = "list",
			min = 2,
			max = 3)
	@CommandPermissions("use.trader")
	public static void displayList(CommandContext args, Player player,
			HumanNPC npc) {
		if (!args.getString(1).contains("s")
				&& !args.getString(1).contains("b")) {
			player.sendMessage(ChatColor.RED + "Not a valid list type.");
			return;
		}
		boolean selling = args.getString(1).contains("s");
		TraderNPC trader = npc.getToggleable("trader");
		ArrayList<Stockable> stock = trader.getStockables(!selling);
		int page = 1;
		if (args.argsLength() == 3)
			page = args.getInteger(2);
		String keyword = "Buying ";
		if (selling)
			keyword = "Selling ";
		if (stock.size() == 0) {
			player.sendMessage(ChatColor.GRAY + "This trader isn't "
					+ keyword.toLowerCase() + "any items.");
			return;
		}
		PageInstance instance = PageUtils.newInstance(player);
		instance.push("");
		for (Stockable stockable : stock) {
			if (stockable == null)
				continue;
			instance.push(ChatColor.GREEN
					+ keyword
					+ MessageUtils.getStockableMessage(stockable,
							ChatColor.GREEN) + ".");
		}
		if (page <= instance.maxPages()) {
			instance.header(ChatColor.YELLOW
					+ StringUtils.listify(ChatColor.GREEN + "Trader "
							+ StringUtils.wrap(keyword) + "List (Page %x/%y)"
							+ ChatColor.YELLOW));
			instance.process(page);
		} else {
			player.sendMessage(MessageUtils.getMaxPagesMessage(page,
					instance.maxPages()));
		}
	}

	/**
	 * Sets whether the selected trader will have unlimited stock or not.
	 * 
	 * @param npc
	 * @param sender
	 * @param unlimited
	 */
	@Command(
			aliases = "trader",
			usage = "unlimited [true|false]",
			desc = "change the unlimited status of a trader",
			modifiers = { "unlimited", "unlim", "unl" },
			min = 2,
			max = 2)
	@CommandPermissions("admin")
	public static void changeUnlimited(CommandContext args, Player player,
			HumanNPC npc) {
		String unlimited = args.getString(1);
		TraderNPC trader = npc.getToggleable("trader");
		if (unlimited.equalsIgnoreCase("true")
				|| unlimited.equalsIgnoreCase("on")) {
			trader.setUnlimited(true);
			player.sendMessage(ChatColor.GREEN
					+ "The trader will now have unlimited stock!");
		} else if (unlimited.equalsIgnoreCase("false")
				|| unlimited.equalsIgnoreCase("off")) {
			trader.setUnlimited(false);
			player.sendMessage(ChatColor.GREEN
					+ "The trader has stopped having unlimited stock.");
		} else {
			player.sendMessage(ChatColor.GREEN
					+ "Incorrect unlimited type entered. Valid values are true, on, false, off.");
		}
	}

	/**
	 * Adds an item to be stocked by the selected trader.
	 * 
	 * @param player
	 * @param npc
	 * @param item
	 * @param price
	 * @param selling
	 */
	@Command(
			aliases = "trader",
			usage = "buy/sell [item] [price]",
			desc = "change the stock of a trader",
			modifiers = { "buy", "sell" },
			min = 3,
			max = 4)
	@CommandPermissions("modify.trader")
	public static void changeTraderStock(CommandContext args, Player player,
			HumanNPC npc) {
		// TODO this is horrible, clean it up
		String item = args.getString(1);
		String price = args.getString(2);
		boolean selling = args.getString(0).contains("bu");
		TraderNPC trader = npc.getToggleable("trader");
		String keyword = "buying";
		if (!selling) {
			keyword = "selling";
		}

		if (args.length() == 4 && item.contains("edit")) {
			ItemStack stack = parseItemStack(player, price, false);
			if (stack == null)
				return;
			if (trader.getStockable(stack.getTypeId(), stack.getDurability(),
					selling) == null) {
				player.sendMessage(ChatColor.RED
						+ "The trader is not currently " + keyword
						+ " that item.");
				return;
			} else {
				String cost = args.getString(3);
				trader.getStockable(stack.getTypeId(), stack.getDurability(),
						selling).setPrice(createItemPrice(player, cost));
				player.sendMessage(ChatColor.GREEN
						+ "Edited "
						+ StringUtils.wrap(StringUtils.capitalise(stack
								.getType().name().toLowerCase())) + "'s price.");
			}
			return;
		}
		if (item.contains("rem")) {
			ItemStack stack = parseItemStack(player, price, false);
			if (stack == null)
				return;
			if (trader.getStockable(stack.getTypeId(), stack.getDurability(),
					selling) == null) {
				player.sendMessage(ChatColor.RED
						+ "The trader is not currently " + keyword
						+ " that item.");
				return;
			} else {
				trader.removeStockable(stack.getTypeId(),
						stack.getDurability(), selling);
				player.sendMessage(ChatColor.GREEN + "Removed "
						+ StringUtils.wrap(stack.getType().name())
						+ " from the trader's " + keyword + " list.");
			}
			return;
		}
		if (item.contains("clear")) {
			int count = 0;
			for (Check check : trader.getStocking().keySet()) {
				if (check.isSelling() == selling) {
					trader.removeStockable(check);
					++count;
				}
			}
			player.sendMessage(ChatColor.GREEN + "Cleared "
					+ StringUtils.wrap(count)
					+ StringUtils.pluralise(" item", count)
					+ " from the trader's " + StringUtils.wrap(keyword)
					+ " list.");
			return;
		}
		selling = !selling;
		ItemStack stack = parseItemStack(player, item, false);
		if (stack == null)
			return;
		ItemPrice itemPrice = createItemPrice(player, price);
		if (itemPrice == null)
			return;
		Stockable s = new Stockable(stack, itemPrice, true);
		keyword = "buying";
		if (selling) {
			keyword = "selling";
			s.setSelling(false);
		}
		if (trader.isStocked(s)) {
			player.sendMessage(ChatColor.RED
					+ "Already "
					+ keyword
					+ " that at "
					+ MessageUtils.getStockableMessage(trader.getStockable(s),
							ChatColor.RED) + ".");
			return;
		}
		trader.addStockable(s);
		player.sendMessage(ChatColor.GREEN + "The trader is now " + keyword
				+ " " + MessageUtils.getStockableMessage(s, ChatColor.GREEN)
				+ ".");
	}

	@Command(
			aliases = "trader",
			usage = "clear [buy|sell]",
			desc = "clear the stock of a trader",
			modifiers = { "clear" },
			min = 2,
			max = 2)
	@CommandPermissions("modify.trader")
	public static void clearTraderStock(CommandContext args, Player player,
			HumanNPC npc) {
		boolean selling = args.getString(1).contains("bu");
		TraderNPC trader = npc.getToggleable("trader");
		String keyword = "buying";
		if (!selling) {
			keyword = "selling";
		}
		int count = 0;
		for (Check check : trader.getStocking().keySet()) {
			if (check.isSelling() == selling) {
				trader.removeStockable(check);
				++count;
			}
		}
		player.sendMessage(ChatColor.GREEN + "Cleared "
				+ StringUtils.wrap(count)
				+ StringUtils.pluralise(" item", count) + " from the trader's "
				+ StringUtils.wrap(keyword) + " list.");
		return;
	}

	private static ItemPrice createItemPrice(Player player, String price) {
		ItemStack cost = parseItemStack(player, price, true);
		boolean econPlugin = false;
		if (cost == null) {
			econPlugin = true;
		}
		ItemPrice itemPrice;
		if (!econPlugin) {
			itemPrice = new ItemPrice(cost);
		} else {
			itemPrice = new ItemPrice(Double.parseDouble(price));
		}
		itemPrice.setEconPlugin(econPlugin);
		return itemPrice;
	}

	private static ItemStack parseItemStack(Player player, String item,
			boolean price) {
		String[] split = item.split(":");
		ItemStack stack = null;
		if ((price && split.length != 1) || !price) {
			stack = parseItemStack(split);
			if (!price && stack == null) {
				player.sendMessage(ChatColor.RED
						+ "Invalid item ID or name specified.");
			}
		}
		if (price && stack == null && !EconomyHandler.useEconPlugin()) {
			player.sendMessage(ChatColor.GRAY
					+ "This server is not using an economy plugin, so the price cannot be "
					+ "that kind of value. If you meant to use an item as currency, "
					+ "please format it like so: item ID:amount(:data).");
			return null;
		}
		return stack;
	}

	/**
	 * Creates an ItemStack from the given string ItemStack format.
	 * 
	 * @param split
	 * @return
	 */
	private static ItemStack parseItemStack(String[] split) {
		try {
			int amount = 1;
			short data = 0;
			Material mat = StringUtils.parseMaterial(split[0]);
			if (mat == null) {
				return null;
			}
			switch (split.length) {
			case 3:
				data = Short.parseShort(split[2]);
			case 2:
				amount = Integer.parseInt(split[1]);
			default:
				break;
			}
			ItemStack stack = new ItemStack(mat, amount);
			stack.setDurability(data);
			return stack;
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}