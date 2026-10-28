package com.lazyscaping;

import com.lazyscaping.api.GearItem;
import com.lazyscaping.api.GearSetup;
import com.lazyscaping.api.GearSlot;
import com.lazyscaping.api.InventorySlot;
import com.lazyscaping.api.ItemRef;
import com.lazyscaping.api.RunePouchEntry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.Layout;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;

/**
 * Shows a gear setup in the bank as a temporary bank tag, laid out with the worn
 * gear on top and the inventory below it as it's packed. Must be used on the
 * client thread.
 */
@Singleton
class BankTagLoader
{
	static final String TAG = "lazyscaping";

	private static final int BANK_COLUMNS = 8;
	private static final int INVENTORY_COLUMNS = 4;
	/** The worn gear takes the first two bank rows. */
	private static final int INVENTORY_START = 2 * BANK_COLUMNS;
	private static final int INVENTORY_ROWS = 7;
	/** Runes from the rune pouch sit to the right of the inventory. */
	private static final int RUNE_COLUMN = INVENTORY_COLUMNS + 1;

	private final Client client;
	private final ItemManager itemManager;
	private final TagManager tagManager;
	private final LayoutManager layoutManager;
	private final BankTagsService bankTagsService;
	private final OwnedItems ownedItems;

	private boolean pending;

	@Inject
	BankTagLoader(Client client, ItemManager itemManager, TagManager tagManager, LayoutManager layoutManager,
		BankTagsService bankTagsService, OwnedItems ownedItems)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.tagManager = tagManager;
		this.layoutManager = layoutManager;
		this.bankTagsService = bankTagsService;
		this.ownedItems = ownedItems;
	}

	/**
	 * Tags the setup's items and opens the tag if the bank is open.
	 *
	 * @return false if the bank is closed, in which case the tag opens next time it is
	 */
	boolean show(GearSetup setup)
	{
		int[] layout = new int[INVENTORY_START + INVENTORY_ROWS * BANK_COLUMNS];
		Arrays.fill(layout, -1);
		Set<Integer> itemIds = new HashSet<>();
		Set<String> names = new HashSet<>();

		int position = 0;
		for (GearSlot slot : setup.getPicks())
		{
			if (slot.getItem() != null && position < INVENTORY_START)
			{
				position = place(layout, position, slot.getItem(), itemIds, names) ? position + 1 : position;
			}
		}

		List<InventorySlot> inventory = setup.getInventory();
		for (int i = 0; i < inventory.size() && i < INVENTORY_ROWS * INVENTORY_COLUMNS; i++)
		{
			place(layout, inventoryPosition(i / INVENTORY_COLUMNS, i % INVENTORY_COLUMNS), inventory.get(i), itemIds, names);
		}

		List<RunePouchEntry> runes = setup.getRunePouch();
		for (int i = 0; i < runes.size() && i < INVENTORY_ROWS; i++)
		{
			Integer runeId = runes.get(i).getItemId();
			if (runeId != null)
			{
				layout[inventoryPosition(i, RUNE_COLUMN)] = runeId;
				itemIds.add(runeId);
			}
		}

		tagManager.registerTag(TAG, itemId -> itemIds.contains(itemId) || names.contains(anyDoseName(itemId)));
		layoutManager.saveLayout(new Layout(TAG, layout));

		if (!isBankOpen())
		{
			pending = true;
			return false;
		}
		open();
		return true;
	}

	/** Opens a tag that was waiting for the bank. */
	void onBankOpened()
	{
		if (pending)
		{
			open();
		}
	}

	void remove()
	{
		pending = false;
		if (TAG.equals(bankTagsService.getActiveTag()))
		{
			bankTagsService.closeBankTag();
		}
		tagManager.unregisterTag(TAG);
		layoutManager.removeLayout(TAG);
	}

	private void open()
	{
		pending = false;
		// other doses of the setup's potions are tagged too, and go below the layout
		bankTagsService.openBankTag(TAG, BankTagsService.OPTION_ITEMS_NOT_IN_LAYOUT_AT_BOTTOM);
	}

	private boolean place(int[] layout, int position, ItemRef item, Set<Integer> itemIds, Set<String> names)
	{
		Integer itemId = item.getItemId();
		if (item instanceof GearItem && ((GearItem) item).getId() != null)
		{
			Integer owned = ownedItems.itemId(((GearItem) item).getId());
			if (owned != null)
			{
				itemId = owned;
			}
		}
		if (itemId == null)
		{
			return false;
		}

		layout[position] = itemId;
		itemIds.add(itemId);
		if (item.getName() != null)
		{
			names.add(ItemNames.anyDose(item.getName()));
		}
		return true;
	}

	private String anyDoseName(int itemId)
	{
		return ItemNames.anyDose(itemManager.getItemComposition(itemId).getName());
	}

	private boolean isBankOpen()
	{
		Widget bank = client.getWidget(InterfaceID.Bankmain.ITEMS);
		return bank != null && !bank.isHidden();
	}

	private static int inventoryPosition(int row, int column)
	{
		return INVENTORY_START + row * BANK_COLUMNS + column;
	}
}
