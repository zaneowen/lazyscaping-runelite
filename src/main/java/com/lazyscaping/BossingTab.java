package com.lazyscaping;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;

/** The Bossing tab: pick a boss to see the best gear setup from what you own, and what a budget buys. */
class BossingTab extends JPanel
{
	private static final GearCatalog.Boss CHOOSE_BOSS = new GearCatalog.Boss(null, "Choose a boss");
	/** Item pictures are drawn in a box this size, like the game's inventory. */
	private static final Dimension ICON_SIZE = new Dimension(36, 32);
	/** Text next to an item picture wraps narrower. */
	private static final int ICON_TEXT_WIDTH = Ui.WRAP_WIDTH - (ICON_SIZE.width + 6) * 3 / 4;
	/** The inventory is 4 slots wide, like the game's. */
	private static final int INVENTORY_COLUMNS = 4;
	/** An inventory slot: an item picture plus a 1px border each side. */
	private static final Dimension CELL_SIZE = new Dimension(ICON_SIZE.width + 2, ICON_SIZE.height + 2);

	private final LazyScapingPanel.Actions actions;
	private final LazyScapingPanel.Icons icons;
	private final JComboBox<GearCatalog.Boss> bossBox = new JComboBox<>();
	private final JComboBox<String> styleBox = new JComboBox<>();
	/** Holds the style picker; shown when the boss can be fought more than one way. */
	private final JComponent styleRow = Ui.full(styleBox);
	/** Hidden for ironmen, who can't buy from the GE. */
	private final BudgetPicker budget;
	private final JPanel gearList = Ui.column();
	/** Set while the pickers are changed in code, so they don't report it as the player's choice. */
	private boolean settingBoss;

	BossingTab(LazyScapingPanel.Actions actions, LazyScapingPanel.Icons icons)
	{
		this.actions = actions;
		this.icons = icons;
		setLayout(new BorderLayout());
		setOpaque(false);
		budget = new BudgetPicker(Budget.GEAR_PRESETS, "Gp to spend on gear upgrades and supplies", b -> actions.budgetChanged(true, b));

		JPanel setup = Ui.card("Gear setup");
		setup.add(Ui.wrapped(Ui.muted("Pick a boss to see the best setup from your gear.")));
		setup.add(Ui.gap(4));
		bossBox.addItem(CHOOSE_BOSS);
		// Sized like this, not by the longest boss name, so it fits the panel.
		bossBox.setPrototypeDisplayValue(CHOOSE_BOSS);
		// Long names wrap in the list instead of being cut off.
		ListCellRenderer<? super GearCatalog.Boss> renderer = bossBox.getRenderer();
		bossBox.setRenderer((list, boss, index, selected, focused) ->
		{
			Component c = renderer.getListCellRendererComponent(list, boss, index, selected, focused);
			if (index >= 0 && boss != null && c instanceof JLabel)
			{
				((JLabel) c).setText(Ui.html(Ui.escape(boss.name), Ui.WRAP_WIDTH));
			}
			return c;
		});
		bossBox.setMaximumRowCount(15);
		bossBox.addActionListener(e -> bossEdited(false));
		styleBox.addActionListener(e -> bossEdited(true));
		styleBox.setToolTipText("Combat style");
		styleRow.setVisible(false);
		setup.add(Ui.full(bossBox));
		setup.add(styleRow);
		setup.add(budget);
		setup.add(gearList);
		setup.add(Ui.gap(4));
		JButton fullPlan = Ui.button("Full gear plan on lazyscaping.app");
		fullPlan.addActionListener(e ->
		{
			GearCatalog.Boss boss = (GearCatalog.Boss) bossBox.getSelectedItem();
			if (boss != null && boss.id != null)
			{
				actions.openBoss(boss.id);
			}
		});
		setup.add(Ui.full(fullPlan));
		add(setup, BorderLayout.NORTH);
	}

	/** The bosses to pick from, keeping the current pick. */
	void setBossOptions(List<GearCatalog.Boss> bosses)
	{
		GearCatalog.Boss selected = (GearCatalog.Boss) bossBox.getSelectedItem();
		settingBoss = true;
		bossBox.removeAllItems();
		bossBox.addItem(CHOOSE_BOSS);
		bosses.forEach(bossBox::addItem);
		settingBoss = false;
		selectBoss(selected == null ? null : selected.id);
	}

	/** Shows a boss in the picker without reporting it back as a change. */
	void selectBoss(String bossId)
	{
		settingBoss = true;
		bossBox.setSelectedItem(CHOOSE_BOSS);
		for (int i = 0; i < bossBox.getItemCount(); i++)
		{
			if (Objects.equals(bossBox.getItemAt(i).id, bossId))
			{
				bossBox.setSelectedIndex(i);
			}
		}
		styleRow.setVisible(false);
		settingBoss = false;
	}

	private void bossEdited(boolean styleChanged)
	{
		if (settingBoss)
		{
			return;
		}
		GearCatalog.Boss boss = (GearCatalog.Boss) bossBox.getSelectedItem();
		if (!styleChanged)
		{
			styleRow.setVisible(false);
		}
		actions.bossSelected(boss == null ? null : boss.id, styleChanged ? (String) styleBox.getSelectedItem() : null);
	}

	void setBudget(String text)
	{
		budget.setBudget(text);
	}

	void setCoins(long coins)
	{
		budget.setCoins(coins);
	}

	void showGearMessage(String message)
	{
		Ui.showMessage(gearList, message);
	}

	void showGear(LazyScapingApi.GearSetup setup)
	{
		settingBoss = true;
		styleBox.removeAllItems();
		if (setup.styles != null)
		{
			setup.styles.forEach(styleBox::addItem);
		}
		styleBox.setSelectedItem(setup.style);
		styleRow.setVisible(setup.styles != null && setup.styles.size() > 1);
		settingBoss = false;
		budget.setVisible(!setup.ironman);

		gearList.removeAll();
		String name = setup.boss != null ? setup.boss.name : "";
		String source = setup.ironman ? "from the gear you own (ironmen can't buy)"
			: setup.budget != null && setup.budget == 0 ? "from the gear you own"
			: "from your gear, plus " + (setup.budget == null ? "any upgrades" : "up to " + Ui.gp(setup.budget));
		gearList.add(Ui.wrapped(Ui.white(Ui.escape(name)) + "<br>" + Ui.escape(setup.style) + " · " + source));
		if (setup.noGear != null)
		{
			gearList.add(Ui.wrapped(Ui.escape(setup.noGear)));
		}
		else
		{
			if (setup.suppliesOverBudget)
			{
				gearList.add(Ui.wrapped(Ui.colored(Ui.MISSING, "Supplies cost more than your budget, so no gear was bought.")));
			}
			if (setup.gearCost > 0)
			{
				gearList.add(Ui.wrapped("Buying upgrades: " + Ui.colored(Ui.GOLD, Ui.gp(setup.gearCost))
					+ (setup.budget != null ? " of " + Ui.gp(setup.budget) : "")));
			}
			gearList.add(emptySlotsSummary(setup));
			addSlots(gearList, setup.picks);
			gearList.add(switchDetails(setup));
		}
		if (setup.inventory != null && !setup.inventory.isEmpty())
		{
			addInventory(setup);
			addInventoryList(setup);
		}
		addTravel(setup.travel);
		Ui.refresh(gearList);
	}

	/** Each switch and spec slot with what to get next, behind a click since the inventory already shows them. */
	private JComponent switchDetails(LazyScapingApi.GearSetup setup)
	{
		JPanel details = Ui.column();
		if (setup.switches != null)
		{
			for (LazyScapingApi.GearSwitch sw : setup.switches)
			{
				details.add(Ui.subheading(sw.style + " switch"));
				addSlots(details, sw.picks);
			}
		}
		if (setup.spec != null)
		{
			details.add(Ui.subheading("Special attack"));
			addSlots(details, List.of(setup.spec));
		}
		if (details.getComponentCount() == 0)
		{
			return details;
		}
		JLabel header = (JLabel) Ui.spaced(Ui.wrapped(Ui.colored(Ui.GOLD, "<u>Switch and spec upgrades</u>")), 10);
		return Ui.expandable(header, details);
	}

	/** The inventory as you'd pack it, with how many slots each kind of item takes. */
	private void addInventory(LazyScapingApi.GearSetup setup)
	{
		int size = setup.inventorySize > 0 ? setup.inventorySize : 28;
		gearList.add(Ui.subheading("Inventory · " + setup.inventory.size() + "/" + size + " slots · " + Ui.gp(setup.tripCost) + " a trip"));

		Map<InventoryKind, Integer> counts = new EnumMap<>(InventoryKind.class);
		setup.inventory.forEach(slot -> counts.merge(InventoryKind.of(slot.kind), 1, Integer::sum));
		List<String> parts = new ArrayList<>();
		counts.forEach((kind, count) -> parts.add(Ui.colored(kind.color, count + " " + (count == 1 ? kind.one : kind.many))));
		gearList.add(Ui.wrapped(String.join(" · ", parts)));

		JPanel grid = new JPanel(new GridLayout(0, INVENTORY_COLUMNS, 3, 3));
		grid.setOpaque(false);
		for (int i = 0; i < size; i++)
		{
			grid.add(inventoryCell(i < setup.inventory.size() ? setup.inventory.get(i) : null, setup.runePouch));
		}
		// Left-aligned at its own width, rather than stretched across the card.
		JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		holder.setOpaque(false);
		holder.add(grid);
		gearList.add(Ui.spaced(holder, 6));

		if (setup.foodCut > 0)
		{
			gearList.add(Ui.spaced(Ui.wrapped(Ui.muted(setup.foodCut + " food left out so your switches fit.")), 4));
		}
		if (setup.overflow > 0)
		{
			gearList.add(Ui.spaced(Ui.wrapped(Ui.colored(Ui.MISSING, setup.overflow + " more item" + (setup.overflow == 1 ? "" : "s") + " won't fit in one inventory.")), 4));
		}
		gearList.add(Ui.spaced(Ui.wrapped(Ui.muted("Hover an item for details.")), 4));
	}

	/** What's in the inventory, by kind: each switch, the spec weapon, teleports and tools, potions and food, with counts. */
	private void addInventoryList(LazyScapingApi.GearSetup setup)
	{
		// Group -> item -> how many, in the order they're packed.
		Map<String, Map<String, Integer>> groups = new LinkedHashMap<>();
		Map<String, InventoryKind> groupKinds = new LinkedHashMap<>();
		for (LazyScapingApi.InventorySlot slot : setup.inventory)
		{
			InventoryKind kind = InventoryKind.of(slot.kind);
			String group = kind == InventoryKind.SWITCH && slot.style != null ? slot.style + " switch" : kind.heading;
			groupKinds.putIfAbsent(group, kind);
			String name = slot.name;
			if ("Rune pouch".equals(name) && setup.runePouch != null && !setup.runePouch.isEmpty())
			{
				List<String> runes = new ArrayList<>();
				setup.runePouch.forEach(r -> runes.add(r.qty + " " + r.name));
				name += " (" + String.join(", ", runes) + ")";
			}
			// A stack in one slot counts its items; repeated slots count the slots.
			groups.computeIfAbsent(group, g -> new LinkedHashMap<>()).merge(name, Math.max(1, slot.qty), Integer::sum);
		}

		JPanel list = Ui.column();
		groups.forEach((group, items) ->
		{
			StringBuilder text = new StringBuilder(Ui.colored(groupKinds.get(group).color, Ui.escape(group)));
			items.forEach((name, count) -> text.append("<br>").append(count > 1 ? count + " x " : "").append(Ui.escape(name)));
			list.add(Ui.spaced(Ui.wrapped(text.toString()), 6));
		});
		gearList.add(list);
	}

	/** How to get to the boss: requirements, the teleports in the plan, other routes and the wiki guide. */
	private void addTravel(LazyScapingApi.Travel travel)
	{
		if (travel == null)
		{
			return;
		}
		boolean hasTeleports = travel.teleports != null && !travel.teleports.isEmpty();
		boolean hasRoutes = travel.routes != null && !travel.routes.isEmpty();
		if (!hasTeleports && !hasRoutes && travel.quest == null && travel.access == null && travel.wiki == null)
		{
			return;
		}
		gearList.add(Ui.subheading("Getting there"));
		if (travel.quest != null)
		{
			gearList.add(Ui.spaced(Ui.wrapped(Ui.white("Quest:") + " " + Ui.escape(travel.quest)), 4));
		}
		if (travel.access != null)
		{
			gearList.add(Ui.spaced(Ui.wrapped(Ui.white("Needs:") + " " + Ui.escape(travel.access)), 4));
		}
		if (hasTeleports)
		{
			for (LazyScapingApi.Teleport teleport : travel.teleports)
			{
				JLabel picture = picture();
				icons.load(teleport, 1, false, picture);
				gearList.add(itemRow(picture, Ui.escape(teleport.name) + "<br>" + Ui.muted(Ui.escape(teleport.where))));
			}
		}
		if (hasRoutes)
		{
			StringBuilder text = new StringBuilder(Ui.white("Other ways:"));
			travel.routes.forEach(r -> text.append("<br>- ").append(Ui.escape(r)));
			gearList.add(Ui.spaced(Ui.wrapped(text.toString()), 6));
			String note = (travel.fairyRings ? "Fairy rings need a dramen staff until you finish the elite Lumbridge & Draynor Diary. " : "")
				+ "In your house, a portal nexus or jewellery box" + (travel.fairyRings ? " (or fairy ring)" : "") + " turns one house tablet into most of these teleports.";
			gearList.add(Ui.spaced(Ui.wrapped(Ui.muted(Ui.escape(note))), 4));
		}
		if (travel.wiki != null)
		{
			JLabel wiki = Ui.wrapped(Ui.colored(Ui.GOLD, "<u>Strategy guide on the OSRS Wiki</u>"));
			Ui.onClick(wiki, () -> actions.openUrl(travel.wiki));
			gearList.add(Ui.spaced(wiki, 6));
		}
	}

	/** One inventory slot: the item's picture (with its stack size), bordered by what kind of item it is. */
	private JComponent inventoryCell(LazyScapingApi.InventorySlot slot, List<LazyScapingApi.RuneLine> runePouch)
	{
		JLabel cell = new JLabel();
		cell.setPreferredSize(CELL_SIZE);
		cell.setHorizontalAlignment(SwingConstants.CENTER);
		cell.setOpaque(true);
		cell.setBackground(ColorScheme.DARK_GRAY_COLOR);
		if (slot == null)
		{
			cell.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
			return cell;
		}
		InventoryKind kind = InventoryKind.of(slot.kind);
		cell.setBorder(BorderFactory.createLineBorder(kind.border));
		icons.load(slot, Math.max(1, slot.qty), false, cell);

		StringBuilder tip = new StringBuilder(Ui.white(Ui.escape(slot.name)));
		if ("switch".equals(slot.kind) && slot.style != null)
		{
			tip.append("<br>").append(Ui.escape(slot.style)).append(" switch");
		}
		else if ("spec".equals(slot.kind))
		{
			tip.append("<br>Special attack weapon");
		}
		if (slot.bought)
		{
			tip.append("<br>").append(Ui.colored(Ui.GOLD, "Buy" + (slot.price != null ? ": " + Ui.gp(slot.price) : "")));
		}
		else if (!"switch".equals(slot.kind) && !"spec".equals(slot.kind) && slot.price != null && slot.price > 0)
		{
			tip.append("<br>").append(Ui.gp(slot.price * Math.max(1, slot.qty)));
		}
		if (slot.why != null)
		{
			tip.append("<br>").append(Ui.escape(slot.why));
		}
		if ("Rune pouch".equals(slot.name) && runePouch != null && !runePouch.isEmpty())
		{
			tip.append("<br>Holds:");
			runePouch.forEach(r -> tip.append("<br>- ").append(r.qty).append(" x ").append(Ui.escape(r.name)));
		}
		cell.setToolTipText(Ui.html(tip.toString(), Ui.WRAP_WIDTH));
		return cell;
	}

	/** What an inventory slot holds, with how it's labelled and outlined. */
	private enum InventoryKind
	{
		SWITCH("switch", "Switches", "switch item", "switch items", new Color(0x6fa8dc)),
		SPEC("spec", "Special attack", "spec weapon", "spec weapons", Ui.GOLD),
		UTILITY("utility", "Teleports and tools", "teleport or tool", "teleports and tools", new Color(0xb39ddb)),
		POTION("potion", "Potions", "potion", "potions", new Color(0x7bc96f)),
		FOOD("food", "Food", "food", "food", new Color(0xd98c5f));

		private final String key;
		/** Heads the kind's items in the list under the inventory. */
		private final String heading;
		private final String one;
		private final String many;
		private final Color color;
		private final Color border;

		InventoryKind(String key, String heading, String one, String many, Color color)
		{
			this.key = key;
			this.heading = heading;
			this.one = one;
			this.many = many;
			this.color = color;
			this.border = color.darker();
		}

		static InventoryKind of(String key)
		{
			for (InventoryKind kind : values())
			{
				if (kind.key.equals(key))
				{
					return kind;
				}
			}
			return UTILITY;
		}
	}

	/** What filling the slots that are still empty would cost on the GE. */
	private static JComponent emptySlotsSummary(LazyScapingApi.GearSetup setup)
	{
		long cost = 0;
		int untradeable = 0;
		int empty = 0;
		for (LazyScapingApi.GearSlot s : allSlots(setup))
		{
			if (s.item == null && s.next != null && !"twoHanded".equals(s.status))
			{
				empty++;
				if (s.next.price != null)
				{
					cost += s.next.price;
				}
				else
				{
					untradeable++;
				}
			}
		}
		if (empty == 0)
		{
			return Ui.wrapped(Ui.colored(Ui.READY, "Every slot is filled."));
		}
		int tradeable = empty - untradeable;
		String text = "Still empty: " + empty + " slot" + (empty == 1 ? "" : "s")
			+ (tradeable > 0 ? ", " + Ui.colored(Ui.GOLD, Ui.gp(cost)) + " to fill" + (untradeable > 0 ? " (+ " + untradeable + " untradeable)" : "")
			: untradeable == 1 ? ", needing an untradeable item" : ", needing untradeable items");
		return Ui.spaced(Ui.wrapped(text), 4);
	}

	private static List<LazyScapingApi.GearSlot> allSlots(LazyScapingApi.GearSetup setup)
	{
		List<LazyScapingApi.GearSlot> out = new ArrayList<>();
		if (setup.picks != null)
		{
			out.addAll(setup.picks);
		}
		if (setup.switches != null)
		{
			setup.switches.forEach(sw -> out.addAll(sw.picks));
		}
		if (setup.spec != null)
		{
			out.add(setup.spec);
		}
		return out;
	}

	private void addSlots(JPanel target, List<LazyScapingApi.GearSlot> slots)
	{
		if (slots != null)
		{
			slots.forEach(s -> target.add(slotRow(s)));
		}
	}

	/** One gear slot: the item's picture, then the slot, item and what to get next. */
	private JComponent slotRow(LazyScapingApi.GearSlot s)
	{
		JLabel picture = picture();
		StringBuilder text = new StringBuilder(Ui.muted(Ui.escape(s.slot))).append("<br>");
		if ("twoHanded".equals(s.status))
		{
			text.append(Ui.muted("Two-handed weapon"));
		}
		else if (s.item != null)
		{
			if ("buy".equals(s.status))
			{
				// Bought with the budget.
				text.append(Ui.colored(Ui.GOLD, "Buy: " + Ui.escape(s.item.name))).append("<br>").append(Ui.muted(Ui.gp(s.cost)));
			}
			else
			{
				text.append(Ui.colored(Ui.READY, Ui.escape(s.item.name)));
			}
			if (s.next != null)
			{
				text.append("<br>").append(Ui.muted("Upgrade: " + Ui.escape(s.next.name) + (s.next.price != null ? " · " + Ui.gp(s.next.price) : "")));
			}
			icons.load(s.item, 1, false, picture);
		}
		else if (s.next != null)
		{
			text.append(Ui.colored(s.missingRequired ? Ui.MISSING : Ui.GOLD, (s.missingRequired ? "Needed: " : "Get: ") + Ui.escape(s.next.name)))
				.append("<br>").append(Ui.muted(s.next.price != null ? Ui.gp(s.next.price) : "Untradeable"));
			// The item to get, faded, since you don't have it yet.
			icons.load(s.next, 1, true, picture);
		}
		else
		{
			text.append(Ui.muted("Empty"));
		}
		return itemRow(picture, text.toString());
	}

	private static JLabel picture()
	{
		JLabel picture = new JLabel();
		picture.setPreferredSize(ICON_SIZE);
		picture.setMinimumSize(ICON_SIZE);
		picture.setHorizontalAlignment(SwingConstants.CENTER);
		picture.setVerticalAlignment(SwingConstants.TOP);
		return picture;
	}

	/** An item's picture with text beside it. */
	private static JComponent itemRow(JLabel picture, String text)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.add(picture, BorderLayout.WEST);
		row.add(Ui.wrapped(text, ICON_TEXT_WIDTH), BorderLayout.CENTER);
		return Ui.spaced(row, 4);
	}
}
