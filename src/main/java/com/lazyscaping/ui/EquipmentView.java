package com.lazyscaping.ui;

import com.lazyscaping.api.GearSlot;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/** The worn gear of a setup, laid out like the game's equipment screen. Hover a slot for its details. */
final class EquipmentView
{
	private static final int COLUMNS = 3;
	/** Slot names by grid position, row by row; null positions are blank. */
	private static final String[] LAYOUT = {
		null, "Head", null,
		"Cape", "Neck", "Ammo",
		"Weapon", "Body", "Shield",
		null, "Legs", null,
		"Hands", "Feet", "Ring",
	};
	private static final Color OWNED = Html.GREEN.darker();
	private static final Color BOUGHT = Html.GOLD;
	private static final Color MISSING_REQUIRED = Html.RED;

	private EquipmentView()
	{
	}

	static JPanel create(List<GearSlot> picks, IconLoader icons)
	{
		Map<String, GearSlot> bySlot = new HashMap<>();
		picks.forEach(slot -> bySlot.put(slot.getSlot(), slot));

		List<JLabel> cells = new ArrayList<>();
		for (String slotName : LAYOUT)
		{
			cells.add(slotName == null ? Components.itemCell(null) : cell(slotName, bySlot.get(slotName), icons));
		}
		return Components.itemGrid(COLUMNS, cells);
	}

	private static JLabel cell(String slotName, GearSlot slot, IconLoader icons)
	{
		if (slot == null || slot.getStatus() == GearSlot.Status.TWO_HANDED || (slot.getItem() == null && slot.getNext() == null))
		{
			JLabel cell = Components.itemCell(ColorScheme.DARK_GRAY_COLOR);
			String reason = slot != null && slot.getStatus() == GearSlot.Status.TWO_HANDED ? "Two-handed weapon" : "Empty";
			cell.setToolTipText(Html.wrap(Html.white(slotName) + "<br>" + reason, Components.TEXT_WIDTH));
			return cell;
		}

		JLabel cell;
		if (slot.getItem() != null)
		{
			cell = Components.itemCell(slot.getStatus() == GearSlot.Status.BUY ? BOUGHT : OWNED);
			icons.loadItem(slot.getItem(), 1, false, cell);
		}
		else
		{
			cell = Components.itemCell(slot.isMissingRequired() ? MISSING_REQUIRED : ColorScheme.MEDIUM_GRAY_COLOR);
			icons.loadItem(slot.getNext(), 1, true, cell);
		}
		cell.setToolTipText(Html.wrap(tooltip(slotName, slot), Components.TEXT_WIDTH));
		return cell;
	}

	private static String tooltip(String slotName, GearSlot slot)
	{
		StringBuilder html = new StringBuilder(Html.white(Html.escape(slotName))).append("<br>");
		if (slot.getItem() != null)
		{
			html.append(Html.escape(slot.getItem().getName()));
			html.append("<br>").append(slot.getStatus() == GearSlot.Status.BUY ? "Buy for " + Format.gp(slot.getCost()) : "You own this");
			if (slot.getNext() != null)
			{
				html.append("<br>Upgrade: ").append(Html.escape(slot.getNext().getName())).append(price(slot.getNext().getPrice()));
			}
		}
		else
		{
			html.append(slot.isMissingRequired() ? "Needed: " : "Get: ").append(Html.escape(slot.getNext().getName()))
				.append(price(slot.getNext().getPrice()));
		}
		return html.toString();
	}

	private static String price(Long price)
	{
		return price != null ? " (" + Format.gp(price) + ")" : " (untradeable)";
	}
}
