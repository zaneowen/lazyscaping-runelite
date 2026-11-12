package com.lazyscaping;

import com.google.gson.Gson;
import com.lazyscaping.api.GearItem;
import com.lazyscaping.api.GearSetup;
import com.lazyscaping.api.GearSlot;
import com.lazyscaping.api.InventorySlot;
import com.lazyscaping.api.RunePouchEntry;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;
import net.runelite.api.EquipmentInventorySlot;

/**
 * Turns a gear setup into the text the Inventory Setups plugin imports with
 * "Import setup", so players can keep it in the tool they already use. Plugin
 * Hub plugins can't share classes, so this writes the same JSON that plugin's
 * portable format reads: the setup's worn gear, inventory and rune pouch, and a
 * bank layout.
 */
@Singleton
class SetupExporter
{
	private static final int INVENTORY_SIZE = 28;
	/** Highlights items missing from the setup, as Inventory Setups does by default. */
	private static final Color HIGHLIGHT = Color.RED;
	/** The equipment slots, by the names the site gives them. */
	private static final Map<String, EquipmentInventorySlot> SLOTS = Map.ofEntries(
		Map.entry("Head", EquipmentInventorySlot.HEAD),
		Map.entry("Cape", EquipmentInventorySlot.CAPE),
		Map.entry("Neck", EquipmentInventorySlot.AMULET),
		Map.entry("Ammo", EquipmentInventorySlot.AMMO),
		Map.entry("Weapon", EquipmentInventorySlot.WEAPON),
		Map.entry("Body", EquipmentInventorySlot.BODY),
		Map.entry("Shield", EquipmentInventorySlot.SHIELD),
		Map.entry("Legs", EquipmentInventorySlot.LEGS),
		Map.entry("Hands", EquipmentInventorySlot.GLOVES),
		Map.entry("Feet", EquipmentInventorySlot.BOOTS),
		Map.entry("Ring", EquipmentInventorySlot.RING));

	private final Gson gson;
	private final OwnedItems ownedItems;
	private final BankTagLoader bankTagLoader;

	@Inject
	SetupExporter(Gson gson, OwnedItems ownedItems, BankTagLoader bankTagLoader)
	{
		this.gson = gson;
		this.ownedItems = ownedItems;
		this.bankTagLoader = bankTagLoader;
	}

	/** The setup as Inventory Setups' import text, named like "LazyScaping: Vorkath (Ranged)". */
	String export(GearSetup setup, String name)
	{
		int slots = EquipmentInventorySlot.values().length;
		List<Item> equipment = new ArrayList<>(Collections.nCopies(slots, null));
		for (GearSlot slot : setup.getPicks())
		{
			EquipmentInventorySlot equipmentSlot = SLOTS.get(slot.getSlot());
			Integer id = slot.getItem() == null ? null : itemId(slot.getItem());
			if (equipmentSlot != null && id != null)
			{
				equipment.set(equipmentSlot.getSlotIdx(), new Item(id, null));
			}
		}

		List<Item> inventory = new ArrayList<>(Collections.nCopies(INVENTORY_SIZE, null));
		List<InventorySlot> packed = setup.getInventory();
		for (int i = 0; i < packed.size() && i < INVENTORY_SIZE; i++)
		{
			InventorySlot slot = packed.get(i);
			Integer id = itemId(slot);
			if (id != null)
			{
				inventory.set(i, new Item(id, slot.getQty() > 1 ? slot.getQty() : null));
			}
		}

		List<Item> runePouch = null;
		if (!setup.getRunePouch().isEmpty())
		{
			runePouch = new ArrayList<>();
			for (RunePouchEntry rune : setup.getRunePouch())
			{
				if (rune.getItemId() != null)
				{
					runePouch.add(new Item(rune.getItemId(), rune.getQty() > 1 ? rune.getQty() : null));
				}
			}
		}

		String notes = "Planned by LazyScaping (lazyscaping.app) for " + setup.getStyle().toLowerCase(Locale.ROOT) + ".";
		Setup exported = new Setup(inventory, equipment, runePouch, name, notes, HIGHLIGHT);
		return gson.toJson(new Portable(exported, bankTagLoader.layout(setup)));
	}

	/** The id of the copy the player owns, else the site's id for the item. */
	private Integer itemId(GearItem item)
	{
		Integer owned = item.getId() == null ? null : ownedItems.itemId(item.getId());
		return owned != null ? owned : item.getItemId();
	}

	// The shapes of Inventory Setups' InventorySetupPortable and its parts, with the same field names.

	@AllArgsConstructor
	private static final class Portable
	{
		private final Setup setup;
		private final int[] layout;
	}

	@AllArgsConstructor
	private static final class Setup
	{
		private final List<Item> inv;
		private final List<Item> eq;
		private final List<Item> rp;
		private final String name;
		private final String notes;
		private final Color hc;
	}

	@AllArgsConstructor
	private static final class Item
	{
		private final int id;
		/** Quantity, or null for one. */
		private final Integer q;
	}
}
