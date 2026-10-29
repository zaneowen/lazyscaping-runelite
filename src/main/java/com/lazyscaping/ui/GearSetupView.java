package com.lazyscaping.ui;

import com.lazyscaping.Budget;
import com.lazyscaping.api.GearSetup;
import com.lazyscaping.api.GearSlot;
import com.lazyscaping.api.FightNotes;
import com.lazyscaping.api.GearSwitch;
import com.lazyscaping.api.Profit;
import com.lazyscaping.api.TeamOptions;
import com.lazyscaping.api.UpgradeOption;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

/**
 * A gear setup with its team, style and budget pickers: the worn gear, what a budget
 * buys, the packed inventory and how to get there. Shared by the Bossing and
 * Slayer tabs.
 */
class GearSetupView
{
	private final GearTarget target;
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
	/** The style the shown setup's gear suits best, or null. */
	private String bestStyle;
	/** Gear ids of the upgrades being tried in the setup shown. */
	private Set<String> trying = Set.of();

	GearSetupView(GearTarget target, PanelActions actions, IconLoader icons)
	{
		this.target = target;
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
		// marks the style the player's gear suits best
		ListCellRenderer<? super String> styleRenderer = styleBox.getRenderer();
		styleBox.setRenderer((list, value, index, selected, focused) ->
		{
			Component cell = styleRenderer.getListCellRendererComponent(list, value, index, selected, focused);
			if (value != null && value.equals(bestStyle) && cell instanceof JLabel)
			{
				((JLabel) cell).setText(value + " (best fit)");
			}
			return cell;
		});
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
				teamSummary.setText(Html.wrapCentered(Html.muted(Html.escape(option.getSummary())), Components.TEXT_WIDTH));
			}
		}
	}

	/** Shows a setup. {@code trying} holds the gear ids of upgrades being tried, which are marked. */
	void showSetup(GearSetup setup, Set<String> trying)
	{
		this.trying = trying;
		bestStyle = setup.getBestStyle();
		updating = true;
		showTeam(setup.getTeam());
		styleBox.removeAllItems();
		setup.getStyles().forEach(styleBox::addItem);
		styleBox.setSelectedItem(setup.getStyle());
		styleRow.setVisible(setup.getStyles().size() > 1);
		updating = false;
		budget.setVisible(!setup.isIronman());

		setupPanel.removeAll();
		if (!trying.isEmpty())
		{
			setupPanel.add(tryingBar(setup));
		}
		setupPanel.add(Components.section("Overview", overview(setup), actions));
		if (target == GearTarget.BOSS && setup.getNotes() != null)
		{
			setupPanel.add(fightTips(setup.getNotes()));
		}

		if (setup.getNoGear() == null)
		{
			JPanel worn = Components.column();
			setup.getPicks().forEach(slot -> worn.add(slotRow(slot)));
			worn.add(bestUpgrades(setup));
			worn.add(switchDetails(setup));
			setupPanel.add(Components.section("Worn gear", worn, actions));
			addSection("Equipment stats", body -> StatsView.addTo(body, setup));
		}
		if (!setup.getInventory().isEmpty())
		{
			addSection("Inventory", body -> InventoryView.addTo(body, setup, icons, actions));
		}
		addSection("Getting there", body -> TravelView.addTo(body, setup.getTravel(), actions, icons));
		Components.refresh(setupPanel);
	}

	/** Adds a foldable section, if {@code fill} puts anything in it. */
	private void addSection(String title, Consumer<JPanel> fill)
	{
		JPanel body = Components.column();
		fill.accept(body);
		if (body.getComponentCount() > 0)
		{
			setupPanel.add(Components.section(title, body, actions));
		}
	}

	/** The setup at a glance: where the gear comes from, what it costs, how fast it kills, and the bank tag button. */
	private JPanel overview(GearSetup setup)
	{
		JPanel overview = Components.column();
		if (!styleRow.isVisible())
		{
			overview.add(Components.fact("Style", Html.escape(setup.getStyle())));
		}
		else if (setup.getBestStyle() != null && !setup.getBestStyle().equals(setup.getStyle()))
		{
			overview.add(Components.fact("Tip", Html.color(Html.GOLD, Html.escape(setup.getBestStyle())) + " suits your gear better"));
		}
		overview.add(Components.fact("Gear", gearSource(setup)));
		if (setup.getNoGear() != null)
		{
			overview.add(Components.spaced(Components.label(Html.escape(setup.getNoGear())), 6));
			return overview;
		}

		if (setup.getGearCost() > 0)
		{
			String of = setup.getBudget() != null ? " " + Html.muted(Html.keep("of " + Format.gp(setup.getBudget()))) : "";
			overview.add(Components.fact("Buying", Html.color(Html.GOLD, Html.keep(Format.gp(setup.getGearCost()))) + of));
		}
		overview.add(Components.fact("Slots", emptySlotsSummary(setup)));
		addProfit(overview, setup.getProfit());
		if (setup.isSuppliesOverBudget())
		{
			overview.add(Components.spaced(Components.label(Html.color(Html.RED, "Supplies cost more than your budget, so no gear was bought.")), 6));
		}
		overview.add(Components.gap(8));
		overview.add(Components.fullWidth(bankButton));
		overview.add(Components.fullWidth(bankMessage));
		return overview;
	}

	/** Kills and gp an hour from the boss's money making guide, and what tried upgrades add. */
	private void addProfit(JPanel overview, Profit profit)
	{
		if (profit == null)
		{
			return;
		}
		Profit.Gain gain = profit.getTryingGain();
		String kph = Html.keep(String.format(Locale.ROOT, "%.1f", profit.getKph()) + " kills/hr");
		if (gain != null && gain.getKph() != 0)
		{
			kph += " " + signed(String.format(Locale.ROOT, "%+.1f", gain.getKph()), gain.getKph() > 0);
		}
		String gp = Html.color(Html.GOLD, Html.keep(Format.gp(profit.getGpPerHour()) + "/hr"));
		if (gain != null && gain.getGpPerHour() != 0)
		{
			gp += " " + signed((gain.getGpPerHour() > 0 ? "+" : "-") + Format.gp(Math.abs(gain.getGpPerHour())), gain.getGpPerHour() > 0);
		}

		String about = "Estimated from the wiki's money making guide (" + profit.getGuide() + "), at the pace your gear and levels allow";
		JComponent pace = Components.fact("Pace", kph);
		JComponent money = Components.fact("Profit", gp);
		pace.setToolTipText(about);
		money.setToolTipText(about);
		overview.add(pace);
		overview.add(money);
	}

	private static String signed(String text, boolean up)
	{
		return Html.color(up ? Html.GREEN : Html.RED, Html.keep("(" + text + ")"));
	}

	/** The fight's tips, spellbook and thralls, folded away until clicked. */
	private JComponent fightTips(FightNotes notes)
	{
		JPanel details = Components.column();
		details.add(Components.gap(2));
		details.add(Components.fact("Book", Html.escape(notes.getSpellbook() != null ? notes.getSpellbook() : "Any")));
		if (notes.getThralls() != null)
		{
			details.add(Components.fact("Thralls", Html.escape(notes.getThralls())));
		}
		if (notes.getMagic() != null)
		{
			details.add(Components.spaced(Components.label(Html.muted(Html.escape(notes.getMagic()))), 4));
		}
		for (String tip : notes.getTips())
		{
			JPanel entry = Components.entry();
			entry.add(Components.label(Html.escape(tip), Components.ENTRY_WIDTH));
			details.add(entry);
		}
		return Components.disclosure("Fight tips", details);
	}

	/** The upgrades that do the most for their price, each with what it adds and a link to try it. */
	private JComponent bestUpgrades(GearSetup setup)
	{
		JPanel list = Components.column();
		for (UpgradeOption upgrade : setup.getUpgrades())
		{
			JLabel picture = Components.picture();
			icons.loadItem(upgrade, 1, false, picture);
			Components.linkToWiki(picture, upgrade.getName(), actions);

			StringBuilder html = new StringBuilder(Html.muted(Html.escape(upgrade.getSlot()))).append("<br>")
				.append(Html.color(Html.GOLD, Html.escape(upgrade.getName())));
			if (upgrade.getPrice() != null)
			{
				html.append("<br>").append(Html.muted(Format.gp(upgrade.getPrice())));
			}
			html.append("<br>").append(gainText(upgrade));

			JComponent link = trying.contains(upgrade.getId()) ? null
				: Components.textButton("Try upgrade", () -> actions.tryUpgrade(target, upgrade.getId()));
			list.add(Components.listRow(Components.pictureRow(picture, html.toString(), link)));
		}
		return list.getComponentCount() == 0 ? list : Components.disclosure("Best upgrades", list);
	}

	private static String gainText(UpgradeOption upgrade)
	{
		if (upgrade.getGpPerHour() == null)
		{
			return Html.color(Html.GREEN, Html.keep("+" + upgrade.getGearGain() + "% closer to the best"));
		}
		String gain = Html.color(Html.GREEN, Html.keep("+" + Format.gp(upgrade.getGpPerHour()) + "/hr"));
		Double payback = upgrade.getPaybackHours();
		if (payback == null)
		{
			return gain;
		}
		String when = payback < 1 ? "pays for itself within the hour" : "pays for itself in " + Format.hours(payback);
		return gain + ", " + Html.muted(when);
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

	/** Says which upgrades are being tried and what they'd cost, with a button back to the player's own gear. */
	private JComponent tryingBar(GearSetup setup)
	{
		long cost = 0;
		int count = 0;
		for (GearSlot slot : allSlots(setup))
		{
			if (isTried(slot))
			{
				count++;
				cost += slot.getItem().getPrice() == null ? 0 : slot.getItem().getPrice();
			}
		}

		JPanel bar = Components.column();
		bar.setOpaque(true);
		bar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bar.setBorder(new EmptyBorder(6, 6, 6, 6));
		String what = count == 1 ? "Trying 1 upgrade" : "Trying " + count + " upgrades";
		bar.add(Components.label(Html.wrapCentered(Html.color(Html.GOLD, what) + (cost > 0 ? " " + Html.keep("worth " + Format.gp(cost)) : ""), Components.TEXT_WIDTH - 8)));
		bar.add(Components.gap(4));
		JButton back = Components.button("Back to my gear", Components.TEXT_WIDTH - 8);
		back.setToolTipText("Plan with only the gear you own again");
		back.addActionListener(e -> actions.stopTrying(target));
		bar.add(Components.fullWidth(back));
		return Components.spaced(Components.fullWidth(bar), 6);
	}

	private boolean isTried(GearSlot slot)
	{
		return slot.getItem() != null && slot.getItem().getId() != null && trying.contains(slot.getItem().getId());
	}

	/** Links to try a slot's next upgrade and to undo a tried one, or null when there are none. */
	private JComponent slotLinks(GearSlot slot)
	{
		List<JComponent> links = new ArrayList<>();
		if (isTried(slot))
		{
			JLabel undo = Components.textButton("Undo", () -> actions.untryUpgrade(target, slot.getItem().getId()));
			undo.setToolTipText("Stop trying " + slot.getItem().getName());
			links.add(undo);
		}
		if (slot.getNext() != null && slot.getNext().getId() != null && slot.getStatus() != GearSlot.Status.TWO_HANDED)
		{
			JLabel tryIt = Components.textButton("Try upgrade", () -> actions.tryUpgrade(target, slot.getNext().getId()));
			tryIt.setToolTipText("See the setup as if you had " + slot.getNext().getName());
			links.add(tryIt);
		}
		return links.isEmpty() ? null : Components.links(links.toArray(new JComponent[0]));
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
			if (isTried(slot))
			{
				Long price = slot.getItem().getPrice();
				html.append(Html.color(Html.GOLD, "Trying: " + Html.escape(slot.getItem().getName())))
					.append("<br>").append(Html.muted(price != null ? Format.gp(price) : "Untradeable"));
			}
			else if (slot.getStatus() == GearSlot.Status.BUY)
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
			Components.linkToWiki(picture, slot.getItem().getName(), actions);
		}
		else if (slot.getNext() != null)
		{
			String label = slot.isMissingRequired() ? "Needed: " : "Get: ";
			html.append(Html.color(slot.isMissingRequired() ? Html.RED : Html.GOLD, label + Html.escape(slot.getNext().getName())))
				.append("<br>").append(Html.muted(slot.getNext().getPrice() != null ? Format.gp(slot.getNext().getPrice()) : "Untradeable"));
			icons.loadItem(slot.getNext(), 1, true, picture);
			Components.linkToWiki(picture, slot.getNext().getName(), actions);
		}
		else
		{
			html.append(Html.muted("Empty"));
		}
		return Components.listRow(Components.pictureRow(picture, html.toString(), slotLinks(slot)));
	}
}
