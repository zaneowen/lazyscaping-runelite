package com.lazyscaping.ui;

import com.lazyscaping.Budget;
import com.lazyscaping.api.Boss;
import com.lazyscaping.api.GearSetup;
import com.lazyscaping.api.GearSlot;
import com.lazyscaping.api.GearSwitch;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

/** Pick a boss to see the best gear setup from what you own, and what a budget buys. */
class BossingTab extends JPanel
{
	private static final Boss NO_BOSS = new Boss(null, "Choose a boss");
	private static final int VISIBLE_BOSSES = 15;

	private final PanelActions actions;
	private final IconLoader icons;

	private final JComboBox<Boss> bossBox = new JComboBox<>();
	private final JComboBox<String> styleBox = new JComboBox<>();
	private final JComponent styleRow = Components.fullWidth(styleBox);
	private final BudgetPicker budget;
	private final JPanel setupPanel = Components.column();

	private boolean updating;

	BossingTab(PanelActions actions, IconLoader icons)
	{
		super(new BorderLayout());
		this.actions = actions;
		this.icons = icons;
		setOpaque(false);
		budget = new BudgetPicker(Budget.GEAR_PRESETS, "Gp to spend on gear upgrades and supplies", b -> actions.budgetChanged(true, b));

		bossBox.addItem(NO_BOSS);
		// size by the placeholder rather than the longest boss name, so the box fits the panel
		bossBox.setPrototypeDisplayValue(NO_BOSS);
		bossBox.setRenderer(wrappingRenderer(bossBox.getRenderer()));
		bossBox.setMaximumRowCount(VISIBLE_BOSSES);
		bossBox.addActionListener(e -> onBossEdited(false));

		styleBox.setToolTipText("Combat style");
		styleBox.addActionListener(e -> onBossEdited(true));
		styleRow.setVisible(false);

		JButton fullPlan = Components.button("Full gear plan on lazyscaping.app");
		fullPlan.addActionListener(e ->
		{
			Boss boss = (Boss) bossBox.getSelectedItem();
			if (boss != null && boss.getId() != null)
			{
				actions.openBossPlan(boss.getId());
			}
		});

		JPanel card = Components.card("Gear setup");
		card.add(Components.label(Html.muted("Pick a boss to see the best setup from your gear.")));
		card.add(Components.gap(4));
		card.add(Components.fullWidth(bossBox));
		card.add(styleRow);
		card.add(budget);
		card.add(setupPanel);
		card.add(Components.gap(4));
		card.add(Components.fullWidth(fullPlan));
		add(card, BorderLayout.NORTH);
	}

	/** Wraps long boss names in the open list instead of cutting them off. */
	private static ListCellRenderer<? super Boss> wrappingRenderer(ListCellRenderer<? super Boss> renderer)
	{
		return (list, boss, index, selected, focused) ->
		{
			Component component = renderer.getListCellRendererComponent(list, boss, index, selected, focused);
			if (index >= 0 && boss != null && component instanceof JLabel)
			{
				((JLabel) component).setText(Html.wrap(Html.escape(boss.getName()), Components.TEXT_WIDTH));
			}
			return component;
		};
	}

	void setBossOptions(List<Boss> bosses)
	{
		Boss selected = (Boss) bossBox.getSelectedItem();
		updating = true;
		bossBox.removeAllItems();
		bossBox.addItem(NO_BOSS);
		bosses.forEach(bossBox::addItem);
		updating = false;
		selectBoss(selected == null ? null : selected.getId());
	}

	void selectBoss(String bossId)
	{
		updating = true;
		bossBox.setSelectedItem(NO_BOSS);
		for (int i = 0; i < bossBox.getItemCount(); i++)
		{
			if (Objects.equals(bossBox.getItemAt(i).getId(), bossId))
			{
				bossBox.setSelectedIndex(i);
				break;
			}
		}
		styleRow.setVisible(false);
		updating = false;
	}

	void setBudget(String text)
	{
		budget.setBudget(text);
	}

	void setCoins(long coins)
	{
		budget.setCoins(coins);
	}

	private void onBossEdited(boolean styleChanged)
	{
		if (updating)
		{
			return;
		}
		if (!styleChanged)
		{
			styleRow.setVisible(false);
		}
		Boss boss = (Boss) bossBox.getSelectedItem();
		actions.bossSelected(boss == null ? null : boss.getId(), styleChanged ? (String) styleBox.getSelectedItem() : null);
	}

	void showMessage(String message)
	{
		Components.showMessage(setupPanel, message);
	}

	void showSetup(GearSetup setup)
	{
		updating = true;
		styleBox.removeAllItems();
		setup.getStyles().forEach(styleBox::addItem);
		styleBox.setSelectedItem(setup.getStyle());
		styleRow.setVisible(setup.getStyles().size() > 1);
		updating = false;
		budget.setVisible(!setup.isIronman());

		setupPanel.removeAll();
		String bossName = setup.getBoss() != null ? setup.getBoss().getName() : "";
		setupPanel.add(Components.label(Html.white(Html.escape(bossName)) + "<br>" + Html.escape(setup.getStyle()) + " · " + gearSource(setup)));

		if (setup.getNoGear() != null)
		{
			setupPanel.add(Components.label(Html.escape(setup.getNoGear())));
		}
		else
		{
			if (setup.isSuppliesOverBudget())
			{
				setupPanel.add(Components.label(Html.color(Html.RED, "Supplies cost more than your budget, so no gear was bought.")));
			}
			if (setup.getGearCost() > 0)
			{
				String of = setup.getBudget() != null ? " of " + Format.gp(setup.getBudget()) : "";
				setupPanel.add(Components.label("Buying upgrades: " + Html.color(Html.GOLD, Format.gp(setup.getGearCost())) + of));
			}
			setupPanel.add(Components.spaced(Components.label(emptySlotsSummary(setup)), 4));
			setup.getPicks().forEach(slot -> setupPanel.add(slotRow(slot)));
			setupPanel.add(switchDetails(setup));
		}

		if (!setup.getInventory().isEmpty())
		{
			InventoryView.addTo(setupPanel, setup, icons);
		}
		TravelView.addTo(setupPanel, setup.getTravel(), actions, icons);
		Components.refresh(setupPanel);
	}

	private static String gearSource(GearSetup setup)
	{
		if (setup.isIronman())
		{
			return "from the gear you own (ironmen can't buy)";
		}
		if (setup.getBudget() != null && setup.getBudget() == 0)
		{
			return "from the gear you own";
		}
		return "from your gear, plus " + (setup.getBudget() == null ? "any upgrades" : "up to " + Format.gp(setup.getBudget()));
	}

	/** What filling the slots that are still empty would cost on the GE. */
	private static String emptySlotsSummary(GearSetup setup)
	{
		long cost = 0;
		int empty = 0;
		int untradeable = 0;
		for (GearSlot slot : allSlots(setup))
		{
			if (slot.getItem() == null && slot.getNext() != null && slot.getStatus() != GearSlot.Status.TWO_HANDED)
			{
				empty++;
				if (slot.getNext().getPrice() != null)
				{
					cost += slot.getNext().getPrice();
				}
				else
				{
					untradeable++;
				}
			}
		}

		if (empty == 0)
		{
			return Html.color(Html.GREEN, "Every slot is filled.");
		}
		String slots = "Still empty: " + empty + (empty == 1 ? " slot" : " slots");
		if (empty == untradeable)
		{
			return slots + (untradeable == 1 ? ", needing an untradeable item" : ", needing untradeable items");
		}
		return slots + ", " + Html.color(Html.GOLD, Format.gp(cost)) + " to fill" + (untradeable > 0 ? " (+ " + untradeable + " untradeable)" : "");
	}

	private static List<GearSlot> allSlots(GearSetup setup)
	{
		List<GearSlot> slots = new ArrayList<>(setup.getPicks());
		setup.getSwitches().forEach(s -> slots.addAll(s.getPicks()));
		if (setup.getSpec() != null)
		{
			slots.add(setup.getSpec());
		}
		return slots;
	}

	/** Each switch and spec slot with what to get next, behind a link since the inventory already shows them. */
	private JComponent switchDetails(GearSetup setup)
	{
		JPanel details = Components.column();
		for (GearSwitch gearSwitch : setup.getSwitches())
		{
			details.add(Components.subheading(gearSwitch.getStyle() + " switch"));
			gearSwitch.getPicks().forEach(slot -> details.add(slotRow(slot)));
		}
		if (setup.getSpec() != null)
		{
			details.add(Components.subheading("Special attack"));
			details.add(slotRow(setup.getSpec()));
		}
		if (details.getComponentCount() == 0)
		{
			return details;
		}

		JLabel header = Components.spaced(Components.label(Html.color(Html.GOLD, Html.underline("Switch and spec upgrades"))), 10);
		return Components.expandable(header, details);
	}

	/** One equipment slot: the item's picture, then the item and what to get next. */
	private JComponent slotRow(GearSlot slot)
	{
		JLabel picture = Components.picture();
		StringBuilder html = new StringBuilder(Html.muted(Html.escape(slot.getSlot()))).append("<br>");

		if (slot.getStatus() == GearSlot.Status.TWO_HANDED)
		{
			html.append(Html.muted("Two-handed weapon"));
		}
		else if (slot.getItem() != null)
		{
			if (slot.getStatus() == GearSlot.Status.BUY)
			{
				html.append(Html.color(Html.GOLD, "Buy: " + Html.escape(slot.getItem().getName())))
					.append("<br>").append(Html.muted(Format.gp(slot.getCost())));
			}
			else
			{
				html.append(Html.color(Html.GREEN, Html.escape(slot.getItem().getName())));
			}
			if (slot.getNext() != null)
			{
				String price = slot.getNext().getPrice() != null ? " · " + Format.gp(slot.getNext().getPrice()) : "";
				html.append("<br>").append(Html.muted("Upgrade: " + Html.escape(slot.getNext().getName()) + price));
			}
			icons.loadItem(slot.getItem(), 1, false, picture);
		}
		else if (slot.getNext() != null)
		{
			String label = slot.isMissingRequired() ? "Needed: " : "Get: ";
			html.append(Html.color(slot.isMissingRequired() ? Html.RED : Html.GOLD, label + Html.escape(slot.getNext().getName())))
				.append("<br>").append(Html.muted(slot.getNext().getPrice() != null ? Format.gp(slot.getNext().getPrice()) : "Untradeable"));
			icons.loadItem(slot.getNext(), 1, true, picture);
		}
		else
		{
			html.append(Html.muted("Empty"));
		}
		return Components.pictureRow(picture, html.toString());
	}
}
