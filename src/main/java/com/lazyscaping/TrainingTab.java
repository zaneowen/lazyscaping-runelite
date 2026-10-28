package com.lazyscaping;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;

/** The Training tab: the goal and its progress, the plan, and every method for the skill. */
class TrainingTab extends JPanel
{
	private static final String CHOOSE_SKILL = "Choose a skill";
	/** How far method details are indented under their header, and the width they wrap at. */
	private static final int DETAIL_INDENT = 8;
	private static final int DETAIL_WIDTH = Ui.WRAP_WIDTH - DETAIL_INDENT;

	private final LazyScapingPanel.Actions actions;
	private final JComboBox<String> skillBox = new JComboBox<>();
	private final JSpinner targetSpinner = new JSpinner(new SpinnerNumberModel(99, 2, 99, 1));
	private final JProgressBar progressBar = new JProgressBar(0, 1000);
	private final JLabel xpLeft = Ui.wrapped("");
	private final JLabel rate = Ui.wrapped("");
	private final BudgetPicker budget;
	private final JPanel planList = Ui.column();
	private final JPanel methodList = Ui.column();
	/** Set while the goal is changed in code, so it isn't reported as the player's choice. */
	private boolean settingGoal;

	TrainingTab(LazyScapingPanel.Actions actions)
	{
		this.actions = actions;
		setLayout(new BorderLayout());
		setOpaque(false);
		budget = new BudgetPicker(Budget.TRAINING_PRESETS, "Gp to spend on training methods", b -> actions.budgetChanged(false, b));

		JPanel tab = Ui.column();
		JPanel goal = Ui.card("Training goal");
		skillBox.addItem(CHOOSE_SKILL);
		for (Skill skill : Account.SKILLS)
		{
			skillBox.addItem(skill.getName());
		}
		skillBox.addActionListener(e -> goalEdited());
		targetSpinner.addChangeListener(e -> goalEdited());
		targetSpinner.setToolTipText("Target level");
		JPanel goalRow = new JPanel(new BorderLayout(6, 0));
		goalRow.setOpaque(false);
		goalRow.add(skillBox, BorderLayout.CENTER);
		goalRow.add(targetSpinner, BorderLayout.EAST);
		goal.add(Ui.full(goalRow));
		progressBar.setStringPainted(true);
		progressBar.setForeground(Ui.GOLD);
		progressBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		goal.add(Ui.full(progressBar));
		goal.add(xpLeft);
		goal.add(rate);
		tab.add(goal);
		tab.add(Ui.gap(8));

		JPanel plan = Ui.card("Your plan");
		plan.add(budget);
		plan.add(Ui.gap(4));
		plan.add(planList);
		tab.add(plan);
		tab.add(Ui.gap(8));

		JPanel methods = Ui.card("Training methods");
		methods.add(methodList);
		tab.add(methods);
		add(tab, BorderLayout.NORTH);
	}

	/** Shows a goal without reporting it back as a change. */
	void setGoal(Skill skill, int targetLevel)
	{
		settingGoal = true;
		skillBox.setSelectedItem(skill == null ? CHOOSE_SKILL : skill.getName());
		targetSpinner.setValue(targetLevel);
		targetSpinner.setEnabled(skill != null);
		settingGoal = false;
	}

	private void goalEdited()
	{
		if (settingGoal)
		{
			return;
		}
		Skill skill = Account.skillNamed((String) skillBox.getSelectedItem());
		targetSpinner.setEnabled(skill != null);
		actions.goalChanged(skill, (Integer) targetSpinner.getValue());
	}

	void setBudget(String text)
	{
		budget.setBudget(text);
	}

	void setCoins(long coins)
	{
		budget.setCoins(coins);
	}

	void showProgress(LazyScapingPanel.Progress p)
	{
		progressBar.setVisible(p != null);
		xpLeft.setVisible(p != null);
		rate.setVisible(p != null);
		if (p == null)
		{
			return;
		}
		long left = Math.max(0, p.targetXp - p.xp);
		long span = Math.max(1, p.targetXp - p.fromXp);
		double done = left == 0 ? 1 : Math.max(0, Math.min(1, (double) (p.xp - p.fromXp) / span));
		progressBar.setValue((int) Math.round(done * 1000));
		progressBar.setString(left == 0 ? "Goal reached!" : String.format(Locale.ROOT, "Level %d · %.1f%%", p.level, done * 100));
		xpLeft.setText(left == 0 ? "" : Ui.NUMBER.format(left) + " xp to go");
		if (left == 0)
		{
			rate.setText("");
		}
		else if (p.xpHr > 0)
		{
			rate.setText(Ui.NUMBER.format(p.xpHr) + " xp/hr · done in " + Ui.hours((double) left / p.xpHr));
		}
		else
		{
			rate.setText("Train this skill to see your time left.");
		}
	}

	void showPlan(LazyScapingApi.Plan plan)
	{
		planList.removeAll();
		if (plan.goalReached || plan.steps == null || plan.steps.isEmpty())
		{
			planList.add(Ui.wrapped(plan.goalReached ? "Goal reached." : "No methods fit this goal yet."));
		}
		else
		{
			planList.add(Ui.wrapped("About " + Ui.hours(plan.totalHours) + costText(" · ", plan.totalCost)));
			if (plan.overBudget)
			{
				planList.add(Ui.wrapped(Ui.colored(Ui.MISSING, "Over your budget, so this uses the cheapest methods.")));
			}
			if (plan.method != null)
			{
				planList.add(Ui.wrapped(Ui.colored(Ui.GOLD, "Using the method you picked where it can be.")));
			}
			planList.add(Ui.wrapped(Ui.muted("Click a step for details.")));
			for (LazyScapingApi.PlanStep step : plan.steps)
			{
				LazyScapingApi.MethodOption option = optionFor(plan, step.id);
				String text = Ui.white("Level " + step.fromLevel + "-" + step.toLevel) + "<br>"
					+ Ui.colored(Ui.GOLD, Ui.escape(step.method)) + "<br>"
					+ Ui.NUMBER.format(step.xpHr) + " xp/hr · " + Ui.hours(step.hours) + costText(" · ", step.cost)
					+ (option != null ? "<br>" + Ui.muted(effort(option.afk)) : "");
				JLabel header = (JLabel) Ui.spaced(Ui.wrapped(text), 8);
				planList.add(option == null ? header : Ui.expandable(header, methodDetails(option, null)));
			}
		}
		showMethods(plan);
		Ui.refresh(planList);
	}

	void showPlanMessage(String message)
	{
		Ui.showMessage(planList, message);
		Ui.showMessage(methodList, null);
	}

	/** Every method for the skill, each with its details and a button to plan around it. */
	private void showMethods(LazyScapingApi.Plan plan)
	{
		methodList.removeAll();
		if (plan.goalReached || plan.options == null || plan.options.isEmpty())
		{
			if (!plan.goalReached)
			{
				methodList.add(Ui.wrapped("No methods for this skill yet."));
			}
			Ui.refresh(methodList);
			return;
		}
		methodList.add(Ui.wrapped(Ui.muted("Click a method for details. Pick one to plan around it.")));
		if (plan.method != null)
		{
			methodList.add(Ui.gap(4));
			methodList.add(Ui.full(pickButton("Back to the best plan", null, Ui.WRAP_WIDTH)));
		}
		for (LazyScapingApi.MethodOption o : plan.options)
		{
			boolean picked = o.id.equals(plan.method);
			Color color = picked ? Ui.GOLD : o.available ? Ui.READY : o.pickable ? Color.WHITE : ColorScheme.MEDIUM_GRAY_COLOR;
			StringBuilder text = new StringBuilder(Ui.colored(color, Ui.escape(o.name) + (picked ? " (picked)" : "")))
				.append("<br>Level ").append(o.level).append(" · ").append(Ui.NUMBER.format(o.xpHr)).append(" xp/hr · ").append(gpPerXp(o.gpPerXp));
			if (!o.pickable && o.blockers != null && !o.blockers.isEmpty())
			{
				text.append("<br>").append(Ui.colored(Ui.MISSING, Ui.escape(String.join(", ", o.blockers))));
			}
			else if (!o.available)
			{
				text.append("<br>").append(Ui.muted("From level " + o.level));
			}
			JLabel header = (JLabel) Ui.spaced(Ui.wrapped(text.toString()), 8);
			JButton button = picked ? pickButton("Back to the best plan", null, DETAIL_WIDTH)
				: o.pickable ? pickButton("Use this method", o.id, DETAIL_WIDTH)
				: null;
			methodList.add(Ui.expandable(header, methodDetails(o, button)));
		}
		Ui.refresh(methodList);
	}

	/** A method's effort, cost, notes and how-to guide, then `button` (to pick or unpick it) if there is one. */
	private JPanel methodDetails(LazyScapingApi.MethodOption o, JButton button)
	{
		JPanel details = Ui.column();
		details.setBorder(new EmptyBorder(4, DETAIL_INDENT, 2, 0));
		String about = effort(o.afk) + " · " + gpPerXp(o.gpPerXp) + (o.members ? " · Members" : "") + (o.ironFriendly ? "" : " · Hard for ironmen");
		details.add(Ui.wrapped(about, DETAIL_WIDTH));
		if (o.notes != null)
		{
			details.add(Ui.spaced(Ui.wrapped(Ui.escape(o.notes), DETAIL_WIDTH), 4));
		}
		LazyScapingApi.MethodGuide g = o.guide;
		if (g != null)
		{
			if (g.where != null)
			{
				details.add(Ui.spaced(Ui.wrapped(Ui.white("Where:") + " " + Ui.escape(g.where), DETAIL_WIDTH), 4));
			}
			addList(details, "Getting there", g.travel, false);
			addList(details, "Bring", g.bring, false);
			addList(details, "Needs", g.needs, false);
			addList(details, "How to", g.steps, true);
			if (g.wiki != null)
			{
				JLabel wiki = Ui.wrapped(Ui.colored(Ui.GOLD, "<u>Full guide on the OSRS Wiki</u>"), DETAIL_WIDTH);
				Ui.onClick(wiki, () -> actions.openUrl(g.wiki));
				details.add(Ui.spaced(wiki, 4));
			}
		}
		if (button != null)
		{
			details.add(Ui.gap(4));
			details.add(Ui.full(button));
		}
		return details;
	}

	/** A button that picks a method to plan around, or goes back to the best plan when `methodId` is null. */
	private JButton pickButton(String text, String methodId, int width)
	{
		JButton button = Ui.button(text, width);
		button.addActionListener(e -> actions.methodPicked(methodId));
		return button;
	}

	private static void addList(JPanel details, String title, List<String> items, boolean numbered)
	{
		if (items == null || items.isEmpty())
		{
			return;
		}
		StringBuilder text = new StringBuilder(Ui.white(title + ":"));
		for (int i = 0; i < items.size(); i++)
		{
			text.append("<br>").append(numbered ? (i + 1) + ". " : "- ").append(Ui.escape(items.get(i)));
		}
		details.add(Ui.spaced(Ui.wrapped(text.toString(), DETAIL_WIDTH), 4));
	}

	private static LazyScapingApi.MethodOption optionFor(LazyScapingApi.Plan plan, String id)
	{
		if (plan.options != null)
		{
			for (LazyScapingApi.MethodOption o : plan.options)
			{
				if (o.id.equals(id))
				{
					return o;
				}
			}
		}
		return null;
	}

	/** " · costs 1.2M gp", " · makes 300k gp", or nothing when it's free. */
	private static String costText(String separator, long cost)
	{
		return cost > 0 ? separator + "costs " + Ui.gp(cost) : cost < 0 ? separator + "makes " + Ui.gp(-cost) : "";
	}

	private static String effort(int afk)
	{
		return afk >= 3 ? "Very AFK" : afk == 2 ? "Some clicking" : "Click-intensive";
	}

	private static String gpPerXp(double gpPerXp)
	{
		if (Math.abs(gpPerXp) < 0.05)
		{
			return "Free";
		}
		String amount = String.format(Locale.ROOT, "%.1f gp/xp", Math.abs(gpPerXp));
		return gpPerXp > 0 ? "Costs " + amount : "Makes " + amount;
	}
}
