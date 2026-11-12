package com.lazyscaping;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
import net.runelite.client.game.ItemVariationMapping;

/**
 * Tracks the gear, coins and item quantities the player owns. The bank is only available while
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

	/** How many of each item the bank holds, by canonical item id, as last seen. */
	private Map<Integer, Long> bankQuantities = Collections.emptyMap();
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

	/** Loads the bank saved for the current profile, as "id:quantity" entries. */
	void load()
	{
		Map<Integer, Long> quantities = new TreeMap<>();
		String saved = settings.get(ProfileSettings.BANK_ITEMS);
		if (saved != null && !saved.isEmpty())
		{
			for (String entry : saved.split(","))
			{
				// banks saved before quantities were kept have only the id
				String[] parts = entry.split(":", 2);
				try
				{
					quantities.put(Integer.parseInt(parts[0]), parts.length > 1 ? Long.parseLong(parts[1]) : 1L);
				}
				catch (NumberFormatException ignored)
				{
					// skip corrupt entries
				}
			}
		}
		bankQuantities = quantities;
		bankCoins = settings.getLong(ProfileSettings.BANK_COINS, 0);
		bankGear = null;
	}

	void onBankChanged(ItemContainer bank)
	{
		Map<Integer, Long> quantities = new TreeMap<>();
		for (Item item : bank.getItems())
		{
			if (item.getId() > 0)
			{
				quantities.merge(itemManager.canonicalize(item.getId()), (long) item.getQuantity(), Long::sum);
			}
		}

		if (!quantities.equals(bankQuantities))
		{
			if (!quantities.keySet().equals(bankQuantities.keySet()))
			{
				bankGear = null;
			}
			bankQuantities = quantities;
			settings.set(ProfileSettings.BANK_ITEMS, quantities.entrySet().stream()
				.map(e -> e.getKey() + ":" + e.getValue())
				.collect(Collectors.joining(",")));
		}
		bankCoins = coinsIn(bank);
		settings.set(ProfileSettings.BANK_COINS, bankCoins);
	}

	/** How many of an item the player has in their bank (as last seen) and inventory. */
	long quantity(int itemId)
	{
		long carried = 0;
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory != null)
		{
			for (Item item : inventory.getItems())
			{
				if (item.getId() > 0 && itemManager.canonicalize(item.getId()) == itemId)
				{
					carried += item.getQuantity();
				}
			}
		}
		return bankQuantities.getOrDefault(itemId, 0L) + carried;
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
			bankGear = matchGear(bankQuantities.keySet(), catalog);
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
			int baseId = ItemVariationMapping.map(itemId);
			String baseName = baseId != itemId ? itemManager.getItemComposition(baseId).getName() : null;
			String gearId = catalog.match(itemManager.getItemComposition(itemId).getName(), baseName);
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
