package com.lazyscaping.ui;

import com.lazyscaping.Budget;
import com.lazyscaping.api.GearSetup;
import com.lazyscaping.api.GearSlot;
import com.lazyscaping.api.GearSwitch;
import com.lazyscaping.api.TeamOptions;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A gear setup with its team, style and budget pickers: the worn gear, what a budget
 * buys, the packed inventory and how to get there. Shared by the Bossing and
 * Slayer tabs.
 */
class GearSetupView
{
	private final PanelActions actions;
	private final IconLoader icons;

	private final JComboBox<TeamOptions.Option> teamBox = new JComboBox<>();
	private final JComponent teamRow = Components.labeledRow("Team", teamBox);
	/** What changes for the picked team size. */
	private final JLabel teamSummary = Components.label("");
	private final JComboBox<String> styleBox = new JComboBox<>();
	private final JComponent styleRow = Components.labeledRow("Style", styleBox);
	private final BudgetPicker budget;
	private final JButton bankButton = Components.button("Show setup in bank");
	private final JLabel bankMessage = Components.centeredLabel();
	private final JPanel setupPanel = Components.column();
	private final JPanel panel = Components.column();

	private boolean updating;

	GearSetupView(GearTarget target, PanelActions actions, IconLoader icons)
	{
		this.actions = actions;
		this.icons = icons;
		budget = new BudgetPicker(Budget.GEAR_PRESETS, "Gp to spend on gear upgrades and supplies", b -> actions.budgetChanged(true, b));

		teamBox.setToolTipText("Solo or in a group: the gear and supplies differ");
		teamBox.addActionListener(e ->
		{
			TeamOptions.Option option = (TeamOptions.Option) teamBox.getSelectedItem();
			if (!updating && option != null)
			{
				actions.teamSelected(target, option.getId());
			}
		});
		teamRow.setVisible(false);
		teamSummary.setVisible(false);

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

		panel.add(teamRow);
		panel.add(Components.spaced(teamSummary, 2));
		panel.add(Components.gap(2));
		panel.add(styleRow);
		panel.add(budget);
		panel.add(setupPanel);
		panel.add(Components.spaced(Components.centered(Components.textButton("Open the full plan on lazyscaping.app", () -> actions.openGearPlan(target))), 10));
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

	/** Hides the team and style pickers until the next setup arrives, since they differ between bosses. */
	void resetStyle()
	{
		teamRow.setVisible(false);
		teamSummary.setVisible(false);
		styleRow.setVisible(false);
	}

	void showMessage(String message)
	{
		Components.showMessage(setupPanel, message);
	}

	void showError(String message)
	{
		Components.showError(setupPanel, message, actions::refresh);
	}

	void showBankMessage(String message)
	{
		bankMessage.setText(message == null ? "" : "<html><body style='width:" + Components.TEXT_WIDTH + "px;text-align:center'>" + Html.muted(Html.escape(message)) + "</body></html>");
		bankMessage.setVisible(message != null);
	}

	private void showTeam(TeamOptions team)
	{
		teamBox.removeAllItems();
		teamRow.setVisible(team != null);
		teamSummary.setVisible(team != null);
		if (team == null)
		{
			return;
		}
		for (TeamOptions.Option option : team.getOptions())
		{
			teamBox.addItem(option);
			if (option.getId().equals(team.getSelected()))
			{
				teamBox.setSelectedItem(option);
				teamSummary.setText(Html.muted(Html.escape(option.getSummary())));
			}
		}
	}

	void showSetup(GearSetup setup)
	{
		updating = true;
		showTeam(setup.getTeam());
		styleBox.removeAllItems();
		setup.getStyles().forEach(styleBox::addItem);
		styleBox.setSelectedItem(setup.getStyle());
		styleRow.setVisible(setup.getStyles().size() > 1);
		updating = false;
		budget.setVisible(!setup.isIronman());

		setupPanel.removeAll();
		setupPanel.add(Components.gap(4));
		if (!styleRow.isVisible())
		{
			setupPanel.add(Components.fact("Style", Html.escape(setup.getStyle())));
		}
		setupPanel.add(Components.fact("Gear", gearSource(setup)));
		if (setup.getNoGear() != null)
		{
			setupPanel.add(Components.spaced(Components.label(Html.escape(setup.getNoGear())), 6));
		}
		else
		{
			addSummary(setup);
			setupPanel.add(Components.gap(8));
			setupPanel.add(Components.fullWidth(bankButton));
			setupPanel.add(Components.fullWidth(bankMessage));

			setupPanel.add(Components.subheading("Worn gear"));
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

	private void addSummary(GearSetup setup)
	{
		if (setup.getGearCost() > 0)
		{
			String of = setup.getBudget() != null ? " " + Html.muted(Html.keep("of " + Format.gp(setup.getBudget()))) : "";
			setupPanel.add(Components.fact("Buying", Html.color(Html.GOLD, Html.keep(Format.gp(setup.getGearCost()))) + of));
		}
		setupPanel.add(Components.fact("Slots", emptySlotsSummary(setup)));
		if (setup.isSuppliesOverBudget())
		{
			setupPanel.add(Components.spaced(Components.label(Html.color(Html.RED, "Supplies cost more than your budget, so no gear was bought.")), 6));
		}
	}

	/** Where the gear comes from. */
	private static String gearSource(GearSetup setup)
	{
		if (setup.isIronman())
		{
			return "What you own " + Html.muted("(ironmen can't buy)");
		}
		if (setup.getBudget() != null && setup.getBudget() == 0)
		{
			return "What you own";
		}
		String extra = setup.getBudget() == null ? "any upgrades" : Html.keep("up to " + Format.gp(setup.getBudget()));
		return "What you own, plus " + extra;
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
			return Html.color(Html.GREEN, "All filled");
		}
		String slots = Html.keep(empty + " empty");
		if (empty == untradeable)
		{
			return slots + Html.muted(", " + (untradeable == 1 ? "needs an untradeable" : "need untradeables"));
		}
		return slots + Html.muted(",") + " " + Html.color(Html.GOLD, Html.keep(Format.gp(cost))) + " " + Html.keep("to fill")
			+ (untradeable > 0 ? " " + Html.muted(Html.keep("(+" + untradeable + " untradeable)")) : "");
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

		return Components.disclosure("Switch and spec upgrades", details);
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
					.append("<br>").append(Html.color(Html.GOLD, Format.gp(slot.getCost())));
			}
			else
			{
				html.append(Html.color(Html.GREEN, Html.escape(slot.getItem().getName())));
			}
			if (slot.getNext() != null)
			{
				html.append("<br>").append(Html.muted("Next: " + Html.escape(slot.getNext().getName())));
				if (slot.getNext().getPrice() != null)
				{
					html.append("<br>").append(Html.muted(Format.gp(slot.getNext().getPrice())));
				}
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
		return Components.spaced(Components.pictureRow(picture, html.toString()), 8);
	}
}
