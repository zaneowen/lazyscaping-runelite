package com.lazyscaping.ui;

import com.lazyscaping.Budget;
import com.lazyscaping.api.GearSetup;
import com.lazyscaping.api.GearSlot;
import com.lazyscaping.api.GearSwitch;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A gear setup with its style and budget pickers: the worn gear, what a budget
 * buys, the packed inventory and how to get there. Shared by the Bossing and
 * Slayer tabs.
 */
class GearSetupView
{
	private final PanelActions actions;
	private final IconLoader icons;

	private final JComboBox<String> styleBox = new JComboBox<>();
	private final JComponent styleRow = Components.labeledRow("Style", styleBox);
	private final BudgetPicker budget;
	private final JButton bankButton = Components.button("Show setup in bank");
	private final JLabel bankMessage = Components.label("");
	private final JPanel setupPanel = Components.column();
	private final JPanel panel = Components.column();

	private boolean updating;

	GearSetupView(GearTarget target, PanelActions actions, IconLoader icons)
	{
		this.actions = actions;
		this.icons = icons;
		budget = new BudgetPicker(Budget.GEAR_PRESETS, "Gp to spend on gear upgrades and supplies", b -> actions.budgetChanged(true, b));

		styleBox.setToolTipText("Combat style");
		styleBox.addActionListener(e ->
		{
			if (!updating)
			{
				actions.styleSelected(target, (String) styleBox.getSelectedItem());
			}
		});
		styleRow.setVisible(false);

		bankButton.setToolTipText("Tags the setup's gear and supplies in your bank, laid out like your inventory");
		bankButton.addActionListener(e -> actions.showInBank(target));
		bankMessage.setVisible(false);

		panel.add(styleRow);
		panel.add(budget);
		panel.add(setupPanel);
		panel.add(Components.spaced(Components.textButton("Open the full plan on lazyscaping.app", () -> actions.openGearPlan(target)), 10));
		showMessage(null);
	}

	JPanel getComponent()
	{
		return panel;
	}

	void setBudget(String text)
	{
		budget.setBudget(text);
	}

	void setCoins(long coins)
	{
		budget.setCoins(coins);
	}

	/** Hides the style picker until the next setup arrives, since styles differ between bosses. */
	void resetStyle()
	{
		styleRow.setVisible(false);
	}

	void showMessage(String message)
	{
		Components.showMessage(setupPanel, message);
	}

	void showBankMessage(String message)
	{
		bankMessage.setText(message == null ? "" : Html.muted(Html.escape(message)));
		bankMessage.setVisible(message != null);
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
		setupPanel.add(Components.spaced(Components.label(Html.muted(Html.escape(setup.getStyle()) + " · " + gearSource(setup))), 6));
		if (setup.getNoGear() != null)
		{
			setupPanel.add(Components.spaced(Components.label(Html.escape(setup.getNoGear())), 4));
		}
		else
		{
			addSummary(setup);
			setupPanel.add(Components.gap(6));
			setupPanel.add(Components.fullWidth(bankButton));
			setupPanel.add(bankMessage);

			setupPanel.add(Components.subheading("Worn gear"));
			setupPanel.add(Components.spaced(EquipmentView.create(setup.getPicks(), icons), 4));
			setupPanel.add(Components.spaced(Components.label(Html.muted("Hover a slot for details. Faded items are upgrades to get.")), 4));
			setupPanel.add(gearDetails(setup));
		}

		if (!setup.getInventory().isEmpty())
		{
			InventoryView.addTo(setupPanel, setup, icons);
		}
		TravelView.addTo(setupPanel, setup.getTravel(), actions, icons);
		Components.refresh(setupPanel);
	}

	private void addSummary(GearSetup setup)
	{
		if (setup.isSuppliesOverBudget())
		{
			setupPanel.add(Components.spaced(Components.label(Html.color(Html.RED, "Supplies cost more than your budget, so no gear was bought.")), 4));
		}
		if (setup.getGearCost() > 0)
		{
			String of = setup.getBudget() != null ? " of " + Format.gp(setup.getBudget()) : "";
			setupPanel.add(Components.spaced(Components.label("Buying upgrades: " + Html.color(Html.GOLD, Format.gp(setup.getGearCost())) + of), 4));
		}
		setupPanel.add(Components.spaced(Components.label(emptySlotsSummary(setup)), 4));
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

	/** Every slot of the worn gear, switches and spec weapon with prices and upgrades, behind a link. */
	private JComponent gearDetails(GearSetup setup)
	{
		JPanel details = Components.column();
		setup.getPicks().forEach(slot -> details.add(slotRow(slot)));
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

		JLabel header = Components.spaced(Components.label(Html.color(Html.GOLD, Html.underline("Gear details, prices and upgrades"))), 6);
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
