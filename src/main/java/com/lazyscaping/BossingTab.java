package com.lazyscaping;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

/** The Bossing tab: pick a boss to see the best gear setup from what you own, and what a budget buys. */
class BossingTab extends JPanel
{
	private static final GearCatalog.Boss CHOOSE_BOSS = new GearCatalog.Boss(null, "Choose a boss");
	/** Item pictures are drawn in a box this size, like the game's inventory. */
	private static final Dimension ICON_SIZE = new Dimension(36, 32);
	/** Text next to an item picture wraps narrower. */
	private static final int ICON_TEXT_WIDTH = Ui.WRAP_WIDTH - (ICON_SIZE.width + 6) * 3 / 4;

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
			addSlots(setup.picks);
			if (setup.switches != null)
			{
				for (LazyScapingApi.GearSwitch sw : setup.switches)
				{
					gearList.add(Ui.subheading(sw.style + " switch"));
					addSlots(sw.picks);
				}
			}
			if (setup.spec != null)
			{
				gearList.add(Ui.subheading("Special attack"));
				addSlots(List.of(setup.spec));
			}
		}
		if (setup.supplies != null && !setup.supplies.isEmpty())
		{
			gearList.add(Ui.subheading("Inventory · " + Ui.gp(setup.tripCost) + " a trip"));
			setup.supplies.forEach(line -> gearList.add(lineRow(line)));
		}
		if (setup.utility != null && !setup.utility.isEmpty())
		{
			gearList.add(Ui.subheading("Also bring"));
			gearList.add(Ui.wrapped(Ui.muted("Hover an item to see why.")));
			setup.utility.forEach(line -> gearList.add(lineRow(line)));
		}
		Ui.refresh(gearList);
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
		String text = "Still empty: " + empty + " slot" + (empty == 1 ? "" : "s") + ", " + Ui.colored(Ui.GOLD, Ui.gp(cost))
			+ (untradeable > 0 ? " + " + untradeable + " untradeable" : "");
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

	private void addSlots(List<LazyScapingApi.GearSlot> slots)
	{
		if (slots != null)
		{
			slots.forEach(s -> gearList.add(slotRow(s)));
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

	/** A supply or utility item: its picture (with the quantity on it, like a stack), name and cost. */
	private JComponent lineRow(LazyScapingApi.ItemLine line)
	{
		JLabel picture = picture();
		icons.load(line, line.qty, false, picture);
		String text = (line.qty > 1 ? line.qty + " x " : "") + Ui.escape(line.name)
			+ (line.cost != null && line.cost > 0 ? "<br>" + Ui.muted(Ui.gp(line.cost)) : "");
		JComponent row = itemRow(picture, text);
		if (line.why != null)
		{
			row.setToolTipText(Ui.html(Ui.escape(line.why), Ui.WRAP_WIDTH));
			picture.setToolTipText(row.getToolTipText());
		}
		return row;
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
		JLabel label = Ui.wrapped(text, ICON_TEXT_WIDTH);
		JPanel row = new JPanel(new BorderLayout(6, 0))
		{
			@Override
			public void setToolTipText(String tip)
			{
				super.setToolTipText(tip);
				label.setToolTipText(tip);
			}
		};
		row.setOpaque(false);
		row.add(picture, BorderLayout.WEST);
		row.add(label, BorderLayout.CENTER);
		return Ui.spaced(row, 4);
	}
}
