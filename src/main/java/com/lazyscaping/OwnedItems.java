package com.lazyscaping;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;

/**
 * The gear and coins the player owns: the bank as last seen (the client only
 * has it while it's open, so it's saved per account), plus what's worn and in
 * the inventory. Used on the client thread.
 */
@Singleton
class OwnedItems
{
	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ProfileSettings settings;

	private Set<Integer> bankIds = Set.of();
	private long bankCoins;
	/** The bank's gear (gear id -> item id), matched once per bank change, not on every inventory change. */
	private Map<String, Integer> bankGear;
	private GearCatalog bankGearCatalog;
	/** The item id of each piece of gear owned, so pictures show your version. Read on the Swing thread. */
	private volatile Map<String, Integer> itemIds = Map.of();

	/** Reads the bank remembered for the logged-in account. */
	void load()
	{
		String saved = settings.get(ProfileSettings.BANK_ITEMS);
		Set<Integer> ids = new TreeSet<>();
		if (saved != null && !saved.isEmpty())
		{
			for (String id : saved.split(","))
			{
				try
				{
					ids.add(Integer.parseInt(id));
				}
				catch (NumberFormatException ignored)
				{
				}
			}
		}
		bankIds = ids;
		bankCoins = settings.getLong(ProfileSettings.BANK_COINS, 0);
		bankGear = null;
	}

	/** Remembers the bank's items and coins when it changes. */
	void bankChanged(ItemContainer bank)
	{
		if (bank == null)
		{
			return;
		}
		Set<Integer> ids = new TreeSet<>();
		for (Item item : bank.getItems())
		{
			if (item.getId() > 0)
			{
				ids.add(itemManager.canonicalize(item.getId()));
			}
		}
		if (!ids.equals(bankIds))
		{
			bankIds = ids;
			bankGear = null;
			settings.set(ProfileSettings.BANK_ITEMS, ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
		}
		bankCoins = coinsIn(bank);
		settings.set(ProfileSettings.BANK_COINS, bankCoins);
	}

	/** Coins and platinum tokens in the bank (as last seen) and inventory, for the "My coins" budget. */
	long coins()
	{
		return bankCoins + coinsIn(client.getItemContainer(InventoryID.INV));
	}

	/** The site's gear ids for gear in the bank, worn or in the inventory. */
	Set<String> gear(GearCatalog catalog)
	{
		if (bankGear == null || bankGearCatalog != catalog)
		{
			bankGear = match(bankIds, catalog);
			bankGearCatalog = catalog;
		}
		Set<Integer> carried = new TreeSet<>();
		for (int containerId : new int[]{InventoryID.WORN, InventoryID.INV})
		{
			ItemContainer container = client.getItemContainer(containerId);
			if (container != null)
			{
				for (Item item : container.getItems())
				{
					if (item.getId() > 0)
					{
						carried.add(itemManager.canonicalize(item.getId()));
					}
				}
			}
		}
		Map<String, Integer> all = new HashMap<>(bankGear);
		// What you're wearing or carrying shows over the bank's copy.
		all.putAll(match(carried, catalog));
		itemIds = all;
		return new TreeSet<>(all.keySet());
	}

	/** The item id of a piece of owned gear, or null. Safe on any thread. */
	Integer itemId(String gearId)
	{
		return itemIds.get(gearId);
	}

	private Map<String, Integer> match(Set<Integer> ids, GearCatalog catalog)
	{
		Map<String, Integer> out = new HashMap<>();
		for (int id : ids)
		{
			String gear = catalog.match(itemManager.getItemComposition(id).getName());
			if (gear != null)
			{
				out.put(gear, id);
			}
		}
		return out;
	}

	private static long coinsIn(ItemContainer container)
	{
		long gp = 0;
		if (container != null)
		{
			for (Item item : container.getItems())
			{
				if (item.getId() == ItemID.COINS)
				{
					gp += item.getQuantity();
				}
				else if (item.getId() == ItemID.PLATINUM)
				{
					gp += item.getQuantity() * 1_000L;
				}
			}
		}
		return gp;
	}
}
