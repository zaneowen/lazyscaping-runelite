package com.lazyscaping;

import com.lazyscaping.api.GearItem;
import com.lazyscaping.api.GearSetup;
import com.lazyscaping.api.GearSlot;
import com.lazyscaping.api.InventorySlot;
import com.lazyscaping.ui.ReadyCheck;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;

/**
 * Compares what the player is wearing and carrying with a gear setup. Gear is
 * matched by the site's gear ids, so any variant counts; supplies are matched by
 * name, with any dose of a potion counting. Must be used on the client thread.
 */
@Singleton
class ReadyChecker
{
	private final Client client;
	private final ItemManager itemManager;

	@Inject
	ReadyChecker(Client client, ItemManager itemManager)
	{
		this.client = client;
		this.itemManager = itemManager;
	}

	ReadyCheck check(GearSetup setup, GearCatalog catalog)
	{
		Map<String, Integer> gearHeld = new HashMap<>();
		Map<String, Integer> itemsHeld = new HashMap<>();
		for (int containerId : new int[]{InventoryID.WORN, InventoryID.INV})
		{
			ItemContainer container = client.getItemContainer(containerId);
			if (container == null)
			{
				continue;
			}
			for (Item item : container.getItems())
			{
				if (item.getId() <= 0)
				{
					continue;
				}
				String name = itemManager.getItemComposition(itemManager.canonicalize(item.getId())).getName();
				String gearId = catalog.match(name);
				if (gearId != null)
				{
					gearHeld.merge(gearId, 1, Integer::sum);
				}
				itemsHeld.merge(ItemNames.anyDose(name), item.getQuantity(), Integer::sum);
			}
		}

		Map<String, Need> needs = new LinkedHashMap<>();
		for (GearSlot slot : setup.getPicks())
		{
			GearItem item = slot.getItem();
			if (item != null && item.getId() != null)
			{
				needs.computeIfAbsent("gear:" + item.getId(), k -> new Need(item.getName())).quantity++;
			}
		}
		for (InventorySlot slot : setup.getInventory())
		{
			boolean gear = slot.getKind() == InventorySlot.Kind.SWITCH || slot.getKind() == InventorySlot.Kind.SPEC;
			String key = gear && slot.getId() != null ? "gear:" + slot.getId() : "item:" + ItemNames.anyDose(slot.getName());
			needs.computeIfAbsent(key, k -> new Need(slot.getName())).quantity += Math.max(1, slot.getQty());
		}

		List<ReadyCheck.Missing> missing = new ArrayList<>();
		needs.forEach((key, need) ->
		{
			int held = key.startsWith("gear:")
				? gearHeld.getOrDefault(key.substring("gear:".length()), 0)
				: itemsHeld.getOrDefault(key.substring("item:".length()), 0);
			if (held < need.quantity)
			{
				missing.add(new ReadyCheck.Missing(need.name, need.quantity - held));
			}
		});
		return new ReadyCheck(missing);
	}

	private static final class Need
	{
		private final String name;
		private int quantity;

		private Need(String name)
		{
			this.name = name;
		}
	}
}
