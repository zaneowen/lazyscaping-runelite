package com.lazyscaping.ui;

import com.lazyscaping.Budget;
import com.lazyscaping.Skills;
import com.lazyscaping.api.Plan;
import com.lazyscaping.api.PlanStep;
import com.lazyscaping.api.TrainingMethod;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;

/** The goal and its progress, the training plan, and every method for the goal's skill. */
class TrainingTab extends JPanel
{
	private static final String NO_SKILL = "Choose a skill";
	private static final int PROGRESS_STEPS = 1000;
	private static final int METHODS_SHOWN = 5;

	private final PanelActions actions;
	private final IconLoader icons;

	private final JComboBox<String> skillBox = new JComboBox<>();
	private final JSpinner targetSpinner = new JSpinner(new SpinnerNumberModel(99, 2, 99, 1));
	private final JProgressBar progressBar = new JProgressBar(0, PROGRESS_STEPS);
	private final JLabel xpLeft = Components.label("");
	private final JLabel rate = Components.label("");
	private final BudgetPicker budget;
	private final JPanel planList = Components.column();
	private final JPanel methodList = Components.column();

	private Skill skill;
	private boolean updating;

	TrainingTab(PanelActions actions, IconLoader icons)
	{
		super(new BorderLayout());
		this.actions = actions;
		this.icons = icons;
		setOpaque(false);
		budget = new BudgetPicker(Budget.TRAINING_PRESETS, "Gp to spend on training methods", b -> actions.budgetChanged(false, b));

		JPanel content = Components.column();
		content.add(buildGoalCard());
		content.add(Components.gap(8));

		JPanel plan = Components.card("Your plan");
		plan.add(budget);
		plan.add(Components.gap(4));
		plan.add(planList);
		content.add(plan);
		content.add(Components.gap(8));

		JPanel methods = Components.card("Training methods");
		methods.add(methodList);
		content.add(methods);
		add(content, BorderLayout.NORTH);
	}

	private JPanel buildGoalCard()
	{
		skillBox.addItem(NO_SKILL);
		Skills.ALL.forEach(s -> skillBox.addItem(s.getName()));
		skillBox.addActionListener(e -> onGoalEdited());
		targetSpinner.addChangeListener(e -> onGoalEdited());
		targetSpinner.setToolTipText("Target level");

		JPanel goalRow = new JPanel(new BorderLayout(6, 0));
		goalRow.setOpaque(false);
		goalRow.add(skillBox, BorderLayout.CENTER);
		goalRow.add(targetSpinner, BorderLayout.EAST);

		progressBar.setStringPainted(true);
		progressBar.setForeground(Html.GOLD);
		progressBar.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel card = Components.card("Training goal");
		card.add(Components.fullWidth(goalRow));
		card.add(Components.fullWidth(progressBar));
		card.add(xpLeft);
		card.add(rate);
		return card;
	}

	void setGoal(Skill skill, int targetLevel)
	{
		this.skill = skill;
		updating = true;
		skillBox.setSelectedItem(skill == null ? NO_SKILL : skill.getName());
		targetSpinner.setValue(targetLevel);
		targetSpinner.setEnabled(skill != null);
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

	private void onGoalEdited()
	{
		if (updating)
		{
			return;
		}
		skill = Skills.byName((String) skillBox.getSelectedItem());
		targetSpinner.setEnabled(skill != null);
		actions.goalChanged(skill, (Integer) targetSpinner.getValue());
	}

	void showProgress(GoalProgress progress)
	{
		progressBar.setVisible(progress != null);
		xpLeft.setVisible(progress != null);
		rate.setVisible(progress != null);
		if (progress == null)
		{
			return;
		}

		long left = Math.max(0, progress.getTargetXp() - progress.getXp());
		long span = Math.max(1, progress.getTargetXp() - progress.getStartXp());
		double done = left == 0 ? 1 : Math.max(0, Math.min(1, (double) (progress.getXp() - progress.getStartXp()) / span));
		progressBar.setValue((int) Math.round(done * PROGRESS_STEPS));
		progressBar.setString(left == 0 ? "Goal reached!" : String.format(Locale.ROOT, "Level %d · %.1f%%", progress.getLevel(), done * 100));

		if (left == 0)
		{
			xpLeft.setText("");
			rate.setText("");
		}
		else
		{
			xpLeft.setText(Format.number(left) + " xp to go");
			rate.setText(progress.getXpPerHour() > 0
				? Format.number(progress.getXpPerHour()) + " xp/hr · done in " + Format.hours((double) left / progress.getXpPerHour())
				: "Train this skill to see your time left.");
		}
	}

	void showPlanMessage(String message)
	{
		Components.showMessage(planList, message);
		Components.showMessage(methodList, null);
	}

	void showPlan(Plan plan)
	{
		planList.removeAll();
		if (plan.isGoalReached() || plan.getSteps().isEmpty())
		{
			planList.add(Components.label(plan.isGoalReached() ? "Goal reached." : "No methods fit this goal yet."));
		}
		else
		{
			String cost = Format.cost(plan.getTotalCost());
			planList.add(Components.label("About " + Format.hours(plan.getTotalHours()) + (cost.isEmpty() ? "" : " · " + cost)));
			if (plan.isOverBudget())
			{
				planList.add(Components.label(Html.color(Html.RED, "Over your budget, so this uses the cheapest methods.")));
			}
			if (plan.getMethod() != null)
			{
				planList.add(Components.label(Html.color(Html.GOLD, "Using the method you picked where it can be.")));
			}
			planList.add(Components.label(Html.muted("Click a step for details.")));
			plan.getSteps().forEach(step -> planList.add(stepRow(step, plan.option(step.getId()))));
		}
		showMethods(plan);
		Components.refresh(planList);
	}

	private JComponent stepRow(PlanStep step, TrainingMethod method)
	{
		String cost = Format.cost(step.getCost());
		String text = Html.white("Level " + step.getFromLevel() + "-" + step.getToLevel()) + "<br>"
			+ Html.color(Html.GOLD, Html.escape(step.getMethod())) + "<br>"
			+ Format.number(step.getXpHr()) + " xp/hr · " + Format.hours(step.getHours()) + (cost.isEmpty() ? "" : " · " + cost)
			+ (method != null ? "<br>" + Html.muted(Format.effort(method.getAfk())) : "");
		return expandableRow(methodPicture(method), text, method == null ? null : MethodDetails.create(method, actions, icons, null));
	}

	private void showMethods(Plan plan)
	{
		methodList.removeAll();
		if (plan.isGoalReached() || plan.getOptions().isEmpty())
		{
			if (!plan.isGoalReached())
			{
				methodList.add(Components.label("No methods for this skill yet."));
			}
			Components.refresh(methodList);
			return;
		}

		methodList.add(Components.label(Html.muted("Click a method for details. Pick one to plan around it.")));
		if (plan.getMethod() != null)
		{
			methodList.add(Components.gap(4));
			methodList.add(Components.fullWidth(pickButton("Back to the best plan", null, Components.TEXT_WIDTH)));
		}
		// the first few are shown; the rest, mostly locked, are behind a link
		JPanel more = Components.column();
		List<TrainingMethod> options = plan.getOptions();
		for (int i = 0; i < options.size(); i++)
		{
			TrainingMethod method = options.get(i);
			(i < METHODS_SHOWN ? methodList : more).add(methodRow(method, method.getId().equals(plan.getMethod())));
		}
		if (more.getComponentCount() > 0)
		{
			int hidden = more.getComponentCount();
			JLabel header = Components.spaced(Components.label(Html.color(Html.GOLD, Html.underline("Show " + hidden + " more method" + (hidden == 1 ? "" : "s")))), 8);
			methodList.add(Components.expandable(header, more));
		}
		Components.refresh(methodList);
	}

	private JComponent methodRow(TrainingMethod method, boolean picked)
	{
		Color color = picked ? Html.GOLD
			: method.isAvailable() ? Html.GREEN
			: method.isPickable() ? Color.WHITE
			: ColorScheme.MEDIUM_GRAY_COLOR;
		StringBuilder text = new StringBuilder(Html.color(color, Html.escape(method.getName()) + (picked ? " (picked)" : "")))
			.append("<br>Level ").append(method.getLevel())
			.append(" · ").append(Format.number(method.getXpHr())).append(" xp/hr")
			.append(" · ").append(Format.gpPerXp(method.getGpPerXp()));
		if (!method.isPickable() && !method.getBlockers().isEmpty())
		{
			text.append("<br>").append(Html.color(Html.RED, Html.escape(String.join(", ", method.getBlockers()))));
		}
		else if (!method.isAvailable())
		{
			text.append("<br>").append(Html.muted("From level " + method.getLevel()));
		}

		JButton button = picked ? pickButton("Back to the best plan", null, MethodDetails.WIDTH)
			: method.isPickable() ? pickButton("Use this method", method.getId(), MethodDetails.WIDTH)
			: null;
		return expandableRow(methodPicture(method), text.toString(), MethodDetails.create(method, actions, icons, button));
	}

	/** What the method makes or uses, or the skill's icon for methods without a recipe. */
	private JLabel methodPicture(TrainingMethod method)
	{
		JLabel picture = Components.picture();
		if (method != null && method.getIcon() != null)
		{
			icons.loadItem(method.getIcon(), 1, false, picture);
		}
		else if (skill != null)
		{
			icons.loadSkill(skill, picture);
		}
		return picture;
	}

	private JButton pickButton(String text, String methodId, int width)
	{
		JButton button = Components.button(text, width);
		button.addActionListener(e -> actions.methodPicked(methodId));
		return button;
	}

	/** A pictured row that expands to its details, if any, when clicked. */
	private static JComponent expandableRow(JLabel picture, String html, JPanel details)
	{
		JPanel header = Components.spaced(Components.pictureRow(picture, html), 8);
		return details == null ? header : Components.expandable(header, details);
	}
}
