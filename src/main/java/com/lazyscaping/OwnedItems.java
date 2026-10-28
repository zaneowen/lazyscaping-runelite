package com.lazyscaping;

import java.util.Collections;
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
 * Tracks the gear and coins the player owns. The bank is only available while
 * it's open, so its contents are saved to the profile. Used on the client thread,
 * except {@link #itemId}.
 */
@Singleton
class OwnedItems
{
	private static final int PLATINUM_TOKEN_VALUE = 1000;

	private final Client client;
	private final ItemManager itemManager;
	private final ProfileSettings settings;

	private Set<Integer> bankItemIds = Collections.emptySet();
	private long bankCoins;
	/** Gear in the bank, by gear id, matched when the bank changes rather than on every inventory change. */
	private Map<String, Integer> bankGear;
	private GearCatalog bankGearCatalog;
	/** Item id of each piece of owned gear, by gear id. */
	private volatile Map<String, Integer> ownedItemIds = Collections.emptyMap();

	@Inject
	OwnedItems(Client client, ItemManager itemManager, ProfileSettings settings)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.settings = settings;
	}

	/** Loads the bank saved for the current profile. */
	void load()
	{
		Set<Integer> ids = new TreeSet<>();
		String saved = settings.get(ProfileSettings.BANK_ITEMS);
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
					// skip corrupt entries
				}
			}
		}
		bankItemIds = ids;
		bankCoins = settings.getLong(ProfileSettings.BANK_COINS, 0);
		bankGear = null;
	}

	void onBankChanged(ItemContainer bank)
	{
		Set<Integer> ids = new TreeSet<>();
		for (Item item : bank.getItems())
		{
			if (item.getId() > 0)
			{
				ids.add(itemManager.canonicalize(item.getId()));
			}
		}

		if (!ids.equals(bankItemIds))
		{
			bankItemIds = ids;
			bankGear = null;
			settings.set(ProfileSettings.BANK_ITEMS, ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
		}
		bankCoins = coinsIn(bank);
		settings.set(ProfileSettings.BANK_COINS, bankCoins);
	}

	/** Coins and platinum tokens in the bank (as last seen) and inventory. */
	long coins()
	{
		return bankCoins + coinsIn(client.getItemContainer(InventoryID.INV));
	}

	/** Gear ids of the gear in the bank, worn or in the inventory. */
	Set<String> gear(GearCatalog catalog)
	{
		if (bankGear == null || bankGearCatalog != catalog)
		{
			bankGear = matchGear(bankItemIds, catalog);
			bankGearCatalog = catalog;
		}

		Set<Integer> carried = new TreeSet<>();
		addItemIds(carried, client.getItemContainer(InventoryID.WORN));
		addItemIds(carried, client.getItemContainer(InventoryID.INV));

		Map<String, Integer> owned = new HashMap<>(bankGear);
		// prefer the copy being worn or carried, for its picture
		owned.putAll(matchGear(carried, catalog));
		ownedItemIds = owned;
		return new TreeSet<>(owned.keySet());
	}

	/** The item id of a piece of owned gear, or null. Safe to call from any thread. */
	Integer itemId(String gearId)
	{
		return ownedItemIds.get(gearId);
	}

	private void addItemIds(Set<Integer> ids, ItemContainer container)
	{
		if (container == null)
		{
			return;
		}
		for (Item item : container.getItems())
		{
			if (item.getId() > 0)
			{
				ids.add(itemManager.canonicalize(item.getId()));
			}
		}
	}

	private Map<String, Integer> matchGear(Set<Integer> itemIds, GearCatalog catalog)
	{
		Map<String, Integer> gear = new HashMap<>();
		for (int itemId : itemIds)
		{
			String gearId = catalog.match(itemManager.getItemComposition(itemId).getName());
			if (gearId != null)
			{
				gear.put(gearId, itemId);
			}
		}
		return gear;
	}

	private static long coinsIn(ItemContainer container)
	{
		if (container == null)
		{
			return 0;
		}

		long coins = 0;
		for (Item item : container.getItems())
		{
			if (item.getId() == ItemID.COINS)
			{
				coins += item.getQuantity();
			}
			else if (item.getId() == ItemID.PLATINUM)
			{
				coins += (long) item.getQuantity() * PLATINUM_TOKEN_VALUE;
			}
		}
		return coins;
	}
}
