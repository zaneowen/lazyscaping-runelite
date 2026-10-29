package com.lazyscaping.ui;

import com.lazyscaping.api.GearSetup;
import com.lazyscaping.api.InventorySlot;
import com.lazyscaping.api.RunePouchEntry;
import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.RequiredArgsConstructor;
import net.runelite.client.ui.ColorScheme;

/** A gear setup's inventory: a grid packed like the game's, then its contents grouped by kind. */
final class InventoryView
{
	private static final int COLUMNS = 4;
	private static final int DEFAULT_SIZE = 28;
	private static final String RUNE_POUCH = "Rune pouch";

	@RequiredArgsConstructor
	private enum Kind
	{
		SWITCH("Switches", "switch item", "switch items", new Color(0x6fa8dc)),
		SPEC("Special attack", "spec weapon", "spec weapons", Html.GOLD),
		UTILITY("Teleports and tools", "teleport or tool", "teleports and tools", new Color(0xb39ddb)),
		POTION("Potions", "potion", "potions", Html.GREEN),
		FOOD("Food", "food", "food", new Color(0xd98c5f));

		private final String heading;
		private final String singular;
		private final String plural;
		private final Color color;

		static Kind of(InventorySlot.Kind kind)
		{
			if (kind == null)
			{
				return UTILITY;
			}
			switch (kind)
			{
				case SWITCH:
					return SWITCH;
				case SPEC:
					return SPEC;
				case POTION:
					return POTION;
				case FOOD:
					return FOOD;
				default:
					return UTILITY;
			}
		}
	}

	private InventoryView()
	{
	}

	static void addTo(JPanel parent, GearSetup setup, IconLoader icons)
	{
		List<InventorySlot> slots = setup.getInventory();
		int size = setup.getInventorySize() > 0 ? setup.getInventorySize() : DEFAULT_SIZE;
		parent.add(Components.subheading("Inventory"));
		parent.add(Components.label(Html.wrapCentered(Html.muted(Html.parts(slots.size() + "/" + size + " slots", "supplies " + Format.gp(setup.getTripCost()) + " a trip")),
			Components.TEXT_WIDTH)));
		parent.add(Components.label(Html.wrapCentered(slotCounts(slots), Components.TEXT_WIDTH)));
		parent.add(Components.spaced(grid(slots, size, setup.getRunePouch(), icons), 6));

		if (setup.getFoodCut() > 0)
		{
			parent.add(Components.spaced(Components.label(Html.wrapCentered(Html.muted(setup.getFoodCut() + " food left out so your switches fit."), Components.TEXT_WIDTH)), 4));
		}
		if (setup.getOverflow() > 0)
		{
			String items = setup.getOverflow() == 1 ? " more item won't" : " more items won't";
			parent.add(Components.spaced(Components.label(Html.wrapCentered(Html.color(Html.RED, setup.getOverflow() + items + " fit in one inventory."), Components.TEXT_WIDTH)), 4));
		}
		parent.add(contents(slots, setup.getRunePouch()));
	}

	/** How many slots each kind of item takes, coloured like the slot borders. */
	private static String slotCounts(List<InventorySlot> slots)
	{
		Map<Kind, Integer> counts = new EnumMap<>(Kind.class);
		slots.forEach(slot -> counts.merge(Kind.of(slot.getKind()), 1, Integer::sum));
		return counts.entrySet().stream()
			.map(e -> Html.color(e.getKey().color, Html.keep(e.getValue() + " " + (e.getValue() == 1 ? e.getKey().singular : e.getKey().plural))))
			.collect(Collectors.joining(" · "));
	}

	private static JPanel grid(List<InventorySlot> slots, int size, List<RunePouchEntry> runePouch, IconLoader icons)
	{
		List<JLabel> cells = new ArrayList<>();
		for (int i = 0; i < size; i++)
		{
			cells.add(cell(i < slots.size() ? slots.get(i) : null, runePouch, icons));
		}
		return Components.itemGrid(COLUMNS, cells);
	}

	private static JLabel cell(InventorySlot slot, List<RunePouchEntry> runePouch, IconLoader icons)
	{
		if (slot == null)
		{
			return Components.itemCell(ColorScheme.DARK_GRAY_COLOR);
		}

		JLabel cell = Components.itemCell(Kind.of(slot.getKind()).color.darker());
		icons.loadItem(slot, Math.max(1, slot.getQty()), false, cell);
		cell.setToolTipText(Html.wrap(tooltip(slot, runePouch), Components.TEXT_WIDTH));
		return cell;
	}

	private static String tooltip(InventorySlot slot, List<RunePouchEntry> runePouch)
	{
		StringBuilder tip = new StringBuilder(Html.white(Html.escape(slot.getName())));
		if (slot.getKind() == InventorySlot.Kind.SWITCH && slot.getStyle() != null)
		{
			tip.append("<br>").append(Html.escape(slot.getStyle())).append(" switch");
		}
		else if (slot.getKind() == InventorySlot.Kind.SPEC)
		{
			tip.append("<br>Special attack weapon");
		}

		if (slot.isBought())
		{
			tip.append("<br>").append(Html.color(Html.GOLD, "Buy" + (slot.getPrice() != null ? ": " + Format.gp(slot.getPrice()) : "")));
		}
		else if (isSupply(slot) && slot.getPrice() != null && slot.getPrice() > 0)
		{
			tip.append("<br>").append(Format.gp(slot.getPrice() * Math.max(1, slot.getQty())));
		}

		if (slot.getWhy() != null)
		{
			tip.append("<br>").append(Html.escape(slot.getWhy()));
		}
		if (RUNE_POUCH.equals(slot.getName()) && !runePouch.isEmpty())
		{
			tip.append("<br>Holds:");
			runePouch.forEach(rune -> tip.append("<br>- ").append(rune.getQty()).append(" x ").append(Html.escape(rune.getName())));
		}
		return tip.toString();
	}

	/** The inventory's items grouped as packed: each switch, the spec weapon, teleports and tools, potions and food. */
	private static JPanel contents(List<InventorySlot> slots, List<RunePouchEntry> runePouch)
	{
		Map<String, Kind> groupKinds = new LinkedHashMap<>();
		Map<String, Map<String, Integer>> groups = new LinkedHashMap<>();
		for (InventorySlot slot : slots)
		{
			Kind kind = Kind.of(slot.getKind());
			String group = kind == Kind.SWITCH && slot.getStyle() != null ? slot.getStyle() + " switch" : kind.heading;
			groupKinds.putIfAbsent(group, kind);
			// a stack counts its items; repeated slots count the slots
			groups.computeIfAbsent(group, g -> new LinkedHashMap<>())
				.merge(displayName(slot, runePouch), Math.max(1, slot.getQty()), Integer::sum);
		}

		JPanel list = Components.column();
		groups.forEach((group, items) ->
		{
			StringBuilder html = new StringBuilder(Html.color(groupKinds.get(group).color, Html.escape(group)));
			items.forEach((name, count) -> html.append("<br>").append(count > 1 ? count + " x " : "").append(Html.escape(name)));
			JPanel entry = Components.entry();
			entry.add(Components.label(html.toString(), Components.ENTRY_WIDTH));
			list.add(entry);
		});
		return list;
	}

	private static String displayName(InventorySlot slot, List<RunePouchEntry> runePouch)
	{
		if (!RUNE_POUCH.equals(slot.getName()) || runePouch.isEmpty())
		{
			return slot.getName();
		}

		List<String> runes = new ArrayList<>();
		runePouch.forEach(rune -> runes.add(rune.getQty() + " " + rune.getName()));
		return slot.getName() + " (" + String.join(", ", runes) + ")";
	}

	private static boolean isSupply(InventorySlot slot)
	{
		return slot.getKind() != InventorySlot.Kind.SWITCH && slot.getKind() != InventorySlot.Kind.SPEC;
	}
}
