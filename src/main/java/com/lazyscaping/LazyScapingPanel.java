package com.lazyscaping;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

/**
 * The side panel, with a Training tab (goal, plan and methods) and a Bossing tab
 * (gear setups). Only touched on the Swing thread.
 */
class LazyScapingPanel extends PluginPanel
{
	interface Actions
	{
		/** The player picked a skill (null to clear the goal) or target level. */
		void goalChanged(Skill skill, int targetLevel);

		void openTraining();

		void openBoss(String bossId);

		/** The player picked a boss for the gear setup (null to clear it), and maybe a style. */
		void bossSelected(String bossId, String style);

		/** The player set a valid budget for training (or for gear when `gear` is true). */
		void budgetChanged(boolean gear, String budget);

		/** The player picked a training method to plan around, or null for the best plan. */
		void methodPicked(String methodId);

		void openUrl(String url);

		void refresh();
	}

	/** Draws an item's picture into a label, once it's loaded. */
	interface Icons
	{
		void load(LazyScapingApi.GearItem item, boolean dimmed, JLabel target);
	}

	/** Progress toward the goal, read from the client. */
	static class Progress
	{
		int level;
		long xp;
		/** Xp when the goal was set. */
		long fromXp;
		long targetXp;
		/** From the XP Tracker; 0 when the skill isn't being trained right now. */
		int xpHr;
	}

	private static final String CHOOSE_SKILL = "Choose a skill";
	private static final GearCatalog.Boss CHOOSE_BOSS = new GearCatalog.Boss(null, "Choose a boss");
	private static final Color READY = new Color(0x7bc96f);
	private static final Color GOLD = new Color(0xd9a23a);
	private static final Color MISSING = new Color(0xe0605a);
	private static final String MUTED = "#a8a29e";
	private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance(Locale.ROOT);
	/** Space around a card's contents. */
	private static final int CARD_PADDING = 8;
	/**
	 * Width text wraps at inside a card, in CSS px: the panel minus its border, the
	 * card's padding and some slack. Swing's HTML renderer draws CSS px about 4/3
	 * larger, hence the scaling.
	 */
	private static final int WRAP_WIDTH = (PANEL_WIDTH - 30 - 2 * CARD_PADDING) * 3 / 4;
	/** Item pictures are drawn in a box this size, like the game's inventory. */
	private static final Dimension ICON_SIZE = new Dimension(36, 32);
	/** Text next to an item picture wraps narrower. */
	private static final int ICON_TEXT_WIDTH = WRAP_WIDTH - (ICON_SIZE.width + 6) * 3 / 4;
	/** How far method details are indented under their header, and the width they wrap at. */
	private static final int DETAIL_INDENT = 8;
	private static final int DETAIL_WIDTH = WRAP_WIDTH - DETAIL_INDENT;

	private final Actions actions;
	private final Icons icons;
	private final JLabel status = wrapped("");
	private final JComboBox<String> skillBox = new JComboBox<>();
	private final JSpinner targetSpinner = new JSpinner(new SpinnerNumberModel(99, 2, 99, 1));
	private final JProgressBar progressBar = new JProgressBar(0, 1000);
	private final JLabel xpLeft = wrapped("");
	private final JLabel rate = wrapped("");
	private final JComboBox<String> trainingBudget = budgetBox(Budget.TRAINING_PRESETS);
	private final JLabel trainingBudgetNote = wrapped("");
	private final JPanel planList = column();
	private final JPanel methodList = column();
	private final JComboBox<String> gearBudget = budgetBox(Budget.GEAR_PRESETS);
	private final JLabel gearBudgetNote = wrapped("");
	/** Holds the gear budget; hidden for ironmen, who can't buy from the GE. */
	private JComponent gearBudgetRow;
	/** Coins in the bank and inventory, for the "My coins" budget. */
	private long coins;
	private final JComboBox<GearCatalog.Boss> bossBox = new JComboBox<>();
	private final JComboBox<String> styleBox = new JComboBox<>();
	/** Holds the style picker; shown when the boss can be fought more than one way. */
	private JComponent styleRow;
	private final JPanel gearList = column();
	private final JLabel liveNotice = wrapped("Turn on <b>Live recommendations</b> in this plugin's settings to see your training plan and boss gear setups.");
	/** Set while the goal controls are changed in code, so they don't report it as the player's choice. */
	private boolean settingGoal;
	/** The same for the gear setup's boss and style pickers. */
	private boolean settingBoss;
	/** The same for the budget pickers. */
	private boolean settingBudget;

	LazyScapingPanel(Actions actions, Icons icons)
	{
		this.actions = actions;
		this.icons = icons;
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout());

		JPanel content = column();
		JLabel title = new JLabel("LazyScaping");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(GOLD);
		content.add(title);
		content.add(status);
		content.add(gap(6));
		JButton sync = button("Open my planner on lazyscaping.app");
		sync.setToolTipText("Opens the site with your name, gear and quests filled in");
		sync.addActionListener(e -> actions.openTraining());
		content.add(full(sync));
		content.add(gap(8));

		// Training and Bossing each get a tab.
		JPanel display = new JPanel(new BorderLayout());
		MaterialTabGroup tabs = new MaterialTabGroup(display);
		tabs.setLayout(new GridLayout(1, 2, 4, 0));
		tabs.setBorder(new EmptyBorder(0, 0, 8, 0));
		MaterialTab training = new MaterialTab("Training", tabs, trainingTab());
		MaterialTab bossing = new MaterialTab("Bossing", tabs, bossingTab());
		tabs.addTab(training);
		tabs.addTab(bossing);
		tabs.select(training);
		content.add(tabs);
		content.add(display);

		content.add(gap(8));
		content.add(liveNotice);
		content.add(gap(4));
		JButton refresh = button("Refresh");
		refresh.addActionListener(e -> actions.refresh());
		content.add(full(refresh));

		styleRow.setVisible(false);
		add(content, BorderLayout.NORTH);
		setLoggedIn(false);
	}

	private JComponent trainingTab()
	{
		JPanel tab = column();

		JPanel goal = card("Training goal");
		skillBox.addItem(CHOOSE_SKILL);
		for (Skill skill : LazyScapingPlugin.SKILLS)
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
		goal.add(full(goalRow));
		progressBar.setStringPainted(true);
		progressBar.setForeground(GOLD);
		progressBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		goal.add(full(progressBar));
		goal.add(xpLeft);
		goal.add(rate);
		tab.add(goal);
		tab.add(gap(8));

		JPanel plan = card("Your plan");
		plan.add(budgetRow(trainingBudget, false, "Gp to spend on training methods"));
		plan.add(trainingBudgetNote);
		plan.add(gap(4));
		plan.add(planList);
		tab.add(plan);
		tab.add(gap(8));

		JPanel methods = card("Training methods");
		methods.add(methodList);
		tab.add(methods);
		return tab;
	}

	private JComponent bossingTab()
	{
		JPanel tab = column();

		JPanel setup = card("Gear setup");
		setup.add(wrapped("<font color='" + MUTED + "'>Pick a boss to see the best setup from your gear.</font>"));
		setup.add(gap(4));
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
				((JLabel) c).setText(html(escape(boss.name), WRAP_WIDTH));
			}
			return c;
		});
		bossBox.setMaximumRowCount(15);
		bossBox.addActionListener(e -> bossEdited(false));
		styleBox.addActionListener(e -> bossEdited(true));
		styleBox.setToolTipText("Combat style");
		setup.add(full(bossBox));
		styleRow = full(styleBox);
		setup.add(styleRow);
		JPanel budget = column();
		budget.add(budgetRow(gearBudget, true, "Gp to spend on gear upgrades and supplies"));
		budget.add(gearBudgetNote);
		gearBudgetRow = budget;
		setup.add(gearBudgetRow);
		setup.add(gearList);
		setup.add(gap(4));
		JButton fullPlan = button("Full gear plan on lazyscaping.app");
		fullPlan.addActionListener(e ->
		{
			GearCatalog.Boss boss = (GearCatalog.Boss) bossBox.getSelectedItem();
			if (boss != null && boss.id != null)
			{
				actions.openBoss(boss.id);
			}
		});
		setup.add(full(fullPlan));
		tab.add(setup);
		return tab;
	}

	void setLoggedIn(boolean loggedIn)
	{
		status.setText(loggedIn ? "" : "Log in to see your goal and gear setups.");
		status.setVisible(!loggedIn);
		if (!loggedIn)
		{
			showProgress(null);
			showPlanMessage(null);
			showGearMessage(null);
		}
	}

	void setLiveEnabled(boolean enabled)
	{
		liveNotice.setVisible(!enabled);
		if (!enabled)
		{
			showPlanMessage("Needs Live recommendations.");
			showGearMessage("Needs Live recommendations.");
		}
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
		Skill skill = LazyScapingPlugin.skillNamed((String) skillBox.getSelectedItem());
		targetSpinner.setEnabled(skill != null);
		actions.goalChanged(skill, (Integer) targetSpinner.getValue());
	}

	/** A labelled, editable budget picker: a preset or an amount like "25m". */
	private JComponent budgetRow(JComboBox<String> box, boolean gear, String tooltip)
	{
		box.setToolTipText(tooltip + ". Pick one, or type an amount like 25m.");
		box.addActionListener(e -> budgetEdited(box, gear));
		JLabel label = new JLabel("Budget");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.add(label, BorderLayout.WEST);
		row.add(box, BorderLayout.CENTER);
		return full(row);
	}

	private static JComboBox<String> budgetBox(String[] presets)
	{
		JComboBox<String> box = new JComboBox<>(presets);
		box.setEditable(true);
		return box;
	}

	private void budgetEdited(JComboBox<String> box, boolean gear)
	{
		if (settingBudget)
		{
			return;
		}
		Object value = box.getSelectedItem();
		String text = value == null ? "" : value.toString().trim();
		updateBudgetNote(box, gear ? gearBudgetNote : trainingBudgetNote);
		if (Budget.isValid(text))
		{
			actions.budgetChanged(gear, text);
		}
	}

	/** Shows budgets without reporting them back as changes. */
	void setBudgets(String training, String gear)
	{
		settingBudget = true;
		trainingBudget.setSelectedItem(training);
		gearBudget.setSelectedItem(gear);
		settingBudget = false;
		updateBudgetNote(trainingBudget, trainingBudgetNote);
		updateBudgetNote(gearBudget, gearBudgetNote);
	}

	/** The coins "My coins" stands for. */
	void setCoins(long coins)
	{
		this.coins = coins;
		updateBudgetNote(trainingBudget, trainingBudgetNote);
		updateBudgetNote(gearBudget, gearBudgetNote);
	}

	private void updateBudgetNote(JComboBox<String> box, JLabel note)
	{
		Object value = box.getSelectedItem();
		String text = value == null ? "" : value.toString().trim();
		if (!Budget.isValid(text))
		{
			note.setText("<font color='" + hex(MISSING) + "'>Type an amount like 25m, or Unlimited.</font>");
		}
		else if (Budget.isMyCoins(text))
		{
			note.setText(gp(coins) + " in your bank and inventory" + (coins == 0 ? " (open your bank once)" : ""));
		}
		else
		{
			note.setText("");
		}
		note.setVisible(!note.getText().isEmpty());
	}

	/** The bosses the gear setup can be picked for, keeping the current pick. */
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

	/** Shows a boss in the gear setup picker without reporting it back as a change. */
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

	void showProgress(Progress p)
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
		xpLeft.setText(left == 0 ? "" : NUMBER.format(left) + " xp to go");
		if (left == 0)
		{
			rate.setText("");
		}
		else if (p.xpHr > 0)
		{
			rate.setText(NUMBER.format(p.xpHr) + " xp/hr · done in " + hours((double) left / p.xpHr));
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
			planList.add(wrapped(plan.goalReached ? "Goal reached." : "No methods fit this goal yet."));
		}
		else
		{
			String cost = plan.totalCost > 0 ? " · costs " + gp(plan.totalCost) : plan.totalCost < 0 ? " · makes " + gp(-plan.totalCost) : "";
			planList.add(wrapped("About " + hours(plan.totalHours) + cost));
			if (plan.overBudget)
			{
				planList.add(wrapped("<font color='" + hex(MISSING) + "'>Over your budget, so this uses the cheapest methods.</font>"));
			}
			if (plan.method != null)
			{
				planList.add(wrapped("<font color='" + hex(GOLD) + "'>Using the method you picked where it can be.</font>"));
			}
			planList.add(wrapped("<font color='" + MUTED + "'>Click a step for details.</font>"));
			for (LazyScapingApi.PlanStep step : plan.steps)
			{
				LazyScapingApi.MethodOption option = optionFor(plan, step.id);
				StringBuilder text = new StringBuilder("<font color='#ffffff'>Level ").append(step.fromLevel).append('-').append(step.toLevel).append("</font><br>")
					.append("<font color='").append(hex(GOLD)).append("'>").append(escape(step.method)).append("</font><br>")
					.append(NUMBER.format(step.xpHr)).append(" xp/hr · ").append(hours(step.hours));
				if (step.cost != 0)
				{
					text.append(" · ").append(step.cost > 0 ? "costs " + gp(step.cost) : "makes " + gp(-step.cost));
				}
				if (option != null)
				{
					text.append("<br><font color='").append(MUTED).append("'>").append(effort(option.afk)).append("</font>");
				}
				JLabel header = wrapped(text.toString());
				header.setBorder(new EmptyBorder(8, 0, 0, 0));
				planList.add(option == null ? header : expandable(header, methodDetails(option)));
			}
		}
		showMethods(plan);
		refreshLayout(planList);
	}

	void showPlanMessage(String message)
	{
		showMessage(planList, message);
		showMessage(methodList, null);
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

	/** Every method for the skill, each with its details and a button to plan around it. */
	private void showMethods(LazyScapingApi.Plan plan)
	{
		methodList.removeAll();
		if (plan.options == null || plan.options.isEmpty() || plan.goalReached)
		{
			methodList.add(wrapped(plan.goalReached ? "" : "No methods for this skill yet."));
			refreshLayout(methodList);
			return;
		}
		methodList.add(wrapped("<font color='" + MUTED + "'>Click a method for details. Pick one to plan around it.</font>"));
		if (plan.method != null)
		{
			JButton best = button("Back to the best plan");
			best.addActionListener(e -> actions.methodPicked(null));
			methodList.add(gap(4));
			methodList.add(full(best));
		}
		for (LazyScapingApi.MethodOption o : plan.options)
		{
			boolean picked = o.id.equals(plan.method);
			Color color = picked ? GOLD : o.available ? READY : o.pickable ? Color.WHITE : ColorScheme.MEDIUM_GRAY_COLOR;
			StringBuilder text = new StringBuilder("<font color='").append(hex(color)).append("'>").append(escape(o.name))
				.append(picked ? " (picked)" : "").append("</font><br>Level ").append(o.level).append(" · ")
				.append(NUMBER.format(o.xpHr)).append(" xp/hr · ").append(gpPerXp(o.gpPerXp));
			if (!o.pickable && o.blockers != null && !o.blockers.isEmpty())
			{
				text.append("<br><font color='").append(hex(MISSING)).append("'>").append(escape(String.join(", ", o.blockers))).append("</font>");
			}
			else if (!o.available)
			{
				text.append("<br><font color='").append(MUTED).append("'>From level ").append(o.level).append("</font>");
			}
			JLabel header = wrapped(text.toString());
			header.setBorder(new EmptyBorder(8, 0, 0, 0));

			JPanel details = methodDetails(o);
			if (o.pickable && !picked)
			{
				JButton use = button("Use this method", DETAIL_WIDTH);
				use.addActionListener(e -> actions.methodPicked(o.id));
				details.add(gap(4));
				details.add(full(use));
			}
			else if (picked)
			{
				JButton clear = button("Back to the best plan", DETAIL_WIDTH);
				clear.addActionListener(e -> actions.methodPicked(null));
				details.add(gap(4));
				details.add(full(clear));
			}
			methodList.add(expandable(header, details));
		}
		refreshLayout(methodList);
	}

	/** A method's effort, cost, notes and how-to guide. */
	private JPanel methodDetails(LazyScapingApi.MethodOption o)
	{
		JPanel details = column();
		details.setBorder(new EmptyBorder(4, DETAIL_INDENT, 2, 0));
		StringBuilder text = new StringBuilder(effort(o.afk)).append(" · ").append(gpPerXp(o.gpPerXp));
		if (o.members)
		{
			text.append(" · Members");
		}
		if (!o.ironFriendly)
		{
			text.append(" · Hard for ironmen");
		}
		details.add(wrapped(text.toString(), DETAIL_WIDTH));
		if (o.notes != null)
		{
			details.add(spaced(wrapped(escape(o.notes), DETAIL_WIDTH)));
		}
		LazyScapingApi.MethodGuide g = o.guide;
		if (g != null)
		{
			if (g.where != null)
			{
				details.add(spaced(wrapped("<font color='#ffffff'>Where:</font> " + escape(g.where), DETAIL_WIDTH)));
			}
			addList(details, "Getting there", g.travel, false);
			addList(details, "Bring", g.bring, false);
			addList(details, "Needs", g.needs, false);
			addList(details, "How to", g.steps, true);
			if (g.wiki != null)
			{
				JLabel wiki = wrapped("<font color='" + hex(GOLD) + "'><u>Full guide on the OSRS Wiki</u></font>", DETAIL_WIDTH);
				wiki.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				wiki.addMouseListener(new MouseAdapter()
				{
					@Override
					public void mouseClicked(MouseEvent e)
					{
						actions.openUrl(g.wiki);
					}
				});
				details.add(spaced(wiki));
			}
		}
		return details;
	}

	private void addList(JPanel details, String title, List<String> items, boolean numbered)
	{
		if (items == null || items.isEmpty())
		{
			return;
		}
		StringBuilder text = new StringBuilder("<font color='#ffffff'>").append(title).append(":</font>");
		for (int i = 0; i < items.size(); i++)
		{
			text.append("<br>").append(numbered ? (i + 1) + ". " : "- ").append(escape(items.get(i)));
		}
		details.add(spaced(wrapped(text.toString(), DETAIL_WIDTH)));
	}

	private static JComponent spaced(JComponent c)
	{
		c.setBorder(new EmptyBorder(4, 0, 0, 0));
		return c;
	}

	/** A header that shows or hides its details when clicked. */
	private JComponent expandable(JLabel header, JComponent details)
	{
		JPanel box = column();
		details.setVisible(false);
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		header.setToolTipText("Click for details");
		header.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				details.setVisible(!details.isVisible());
				box.revalidate();
				box.repaint();
			}
		});
		box.add(header);
		box.add(details);
		return box;
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
		gearBudgetRow.setVisible(!setup.ironman);

		gearList.removeAll();
		String name = setup.boss != null ? setup.boss.name : "";
		String source = setup.ironman ? "from the gear you own (ironmen can't buy)"
			: setup.budget != null && setup.budget == 0 ? "from the gear you own"
			: "from your gear, plus " + (setup.budget == null ? "any upgrades" : "up to " + gp(setup.budget));
		gearList.add(wrapped("<font color='#ffffff'>" + escape(name) + "</font><br>" + escape(setup.style) + " · " + source));
		if (setup.noGear != null)
		{
			gearList.add(wrapped(escape(setup.noGear)));
		}
		else
		{
			if (setup.suppliesOverBudget)
			{
				gearList.add(wrapped("<font color='" + hex(MISSING) + "'>Supplies cost more than your budget, so no gear was bought.</font>"));
			}
			if (setup.gearCost > 0)
			{
				gearList.add(wrapped("Buying upgrades: <font color='" + hex(GOLD) + "'>" + gp(setup.gearCost) + "</font>"
					+ (setup.budget != null ? " of " + gp(setup.budget) : "")));
			}
			gearList.add(costSummary(setup));
			addSlots(setup.picks);
			if (setup.switches != null)
			{
				for (LazyScapingApi.GearSwitch sw : setup.switches)
				{
					gearList.add(subheading(sw.style + " switch"));
					addSlots(sw.picks);
				}
			}
			if (setup.spec != null)
			{
				gearList.add(subheading("Special attack"));
				addSlots(List.of(setup.spec));
			}
		}
		if (setup.supplies != null && !setup.supplies.isEmpty())
		{
			gearList.add(subheading("Inventory · " + gp(setup.tripCost) + " a trip"));
			gearList.add(wrapped(lines(setup.supplies)));
		}
		if (setup.utility != null && !setup.utility.isEmpty())
		{
			gearList.add(subheading("Also bring"));
			JLabel utility = wrapped(lines(setup.utility));
			StringBuilder why = new StringBuilder("<html>");
			for (LazyScapingApi.ItemLine line : setup.utility)
			{
				why.append("<b>").append(escape(line.name)).append(":</b> ").append(escape(line.why)).append("<br>");
			}
			utility.setToolTipText(why.append("</html>").toString());
			gearList.add(utility);
		}
		refreshLayout(gearList);
	}

	void showGearMessage(String message)
	{
		showMessage(gearList, message);
	}

	/** What filling the empty slots would cost on the GE. */
	private JComponent costSummary(LazyScapingApi.GearSetup setup)
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
			return wrapped("<font color='" + hex(READY) + "'>Every slot is filled.</font>");
		}
		String text = "Still empty: " + empty + " slot" + (empty == 1 ? "" : "s") + ", <font color='" + hex(GOLD) + "'>" + gp(cost) + "</font>";
		if (untradeable > 0)
		{
			text += " + " + untradeable + " untradeable";
		}
		JLabel label = wrapped(text);
		label.setBorder(new EmptyBorder(4, 0, 2, 0));
		return label;
	}

	private static List<LazyScapingApi.GearSlot> allSlots(LazyScapingApi.GearSetup setup)
	{
		List<LazyScapingApi.GearSlot> out = new java.util.ArrayList<>();
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
		if (slots == null)
		{
			return;
		}
		for (LazyScapingApi.GearSlot s : slots)
		{
			gearList.add(slotRow(s));
		}
	}

	/** One gear slot: the item's picture, then the slot, item and what to get next. */
	private JComponent slotRow(LazyScapingApi.GearSlot s)
	{
		JLabel picture = new JLabel();
		picture.setPreferredSize(ICON_SIZE);
		picture.setMinimumSize(ICON_SIZE);
		picture.setHorizontalAlignment(SwingConstants.CENTER);
		picture.setVerticalAlignment(SwingConstants.TOP);

		StringBuilder text = new StringBuilder("<font color='").append(MUTED).append("'>").append(escape(s.slot)).append("</font><br>");
		if ("twoHanded".equals(s.status))
		{
			text.append("<font color='").append(MUTED).append("'>Two-handed weapon</font>");
		}
		else if (s.item != null && "buy".equals(s.status))
		{
			// Bought with the budget.
			text.append("<font color='").append(hex(GOLD)).append("'>Buy: ").append(escape(s.item.name)).append("</font><br><font color='")
				.append(MUTED).append("'>").append(gp(s.cost)).append("</font>");
			if (s.next != null)
			{
				text.append("<br><font color='").append(MUTED).append("'>Upgrade: ").append(escape(s.next.name)).append(price(s.next)).append("</font>");
			}
			icons.load(s.item, false, picture);
		}
		else if (s.item != null)
		{
			text.append("<font color='").append(hex(READY)).append("'>").append(escape(s.item.name)).append("</font>");
			if (s.next != null)
			{
				text.append("<br><font color='").append(MUTED).append("'>Upgrade: ").append(escape(s.next.name)).append(price(s.next)).append("</font>");
			}
			icons.load(s.item, false, picture);
		}
		else if (s.next != null)
		{
			Color color = s.missingRequired ? MISSING : GOLD;
			text.append("<font color='").append(hex(color)).append("'>").append(s.missingRequired ? "Needed: " : "Get: ")
				.append(escape(s.next.name)).append("</font><br><font color='").append(MUTED).append("'>")
				.append(s.next.price != null ? gp(s.next.price) : "Untradeable").append("</font>");
			// The item to get, faded, since you don't have it yet.
			icons.load(s.next, true, picture);
		}
		else
		{
			text.append("<font color='").append(MUTED).append("'>Empty</font>");
		}

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(4, 0, 0, 0));
		row.add(picture, BorderLayout.WEST);
		row.add(wrapped(text.toString(), ICON_TEXT_WIDTH), BorderLayout.CENTER);
		return row;
	}

	private static String price(LazyScapingApi.GearItem item)
	{
		return item.price != null ? " · " + gp(item.price) : "";
	}

	private static String lines(List<LazyScapingApi.ItemLine> lines)
	{
		StringBuilder out = new StringBuilder();
		for (LazyScapingApi.ItemLine line : lines)
		{
			if (out.length() > 0)
			{
				out.append("<br>");
			}
			if (line.qty > 1)
			{
				out.append(line.qty).append(" x ");
			}
			out.append(escape(line.name));
			if (line.cost != null && line.cost > 0)
			{
				out.append(" <font color='").append(MUTED).append("'>· ").append(gp(line.cost)).append("</font>");
			}
		}
		return out.toString();
	}

	private void showMessage(JPanel list, String message)
	{
		list.removeAll();
		if (message != null)
		{
			list.add(wrapped(message));
		}
		refreshLayout(list);
	}

	private void refreshLayout(JComponent changed)
	{
		changed.revalidate();
		changed.repaint();
	}

	private static String hours(double hours)
	{
		long minutes = Math.round(hours * 60);
		if (minutes < 60)
		{
			return Math.max(1, minutes) + "m";
		}
		return minutes / 60 + "h " + String.format(Locale.ROOT, "%02d", minutes % 60) + "m";
	}

	static String gp(long gp)
	{
		if (gp >= 1_000_000_000)
		{
			return String.format(Locale.ROOT, "%.2fB gp", gp / 1_000_000_000.0);
		}
		if (gp >= 1_000_000)
		{
			return String.format(Locale.ROOT, "%.2fM gp", gp / 1_000_000.0);
		}
		if (gp >= 10_000)
		{
			return String.format(Locale.ROOT, "%.1fk gp", gp / 1_000.0);
		}
		return NUMBER.format(gp) + " gp";
	}

	private static String escape(String text)
	{
		return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String hex(Color c)
	{
		return String.format("#%06x", c.getRGB() & 0xffffff);
	}

	/** Stacks components top to bottom, all left-aligned (BoxLayout misplaces mixed alignments). */
	private static JPanel column()
	{
		JPanel panel = new JPanel()
		{
			@Override
			protected void addImpl(Component comp, Object constraints, int index)
			{
				if (comp instanceof JComponent)
				{
					((JComponent) comp).setAlignmentX(LEFT_ALIGNMENT);
				}
				super.addImpl(comp, constraints, index);
			}
		};
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);
		return panel;
	}

	/** A shaded box with a title, for one section of a tab. */
	private static JPanel card(String title)
	{
		JPanel card = column();
		card.setOpaque(true);
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(new EmptyBorder(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING));
		JLabel heading = new JLabel(title);
		heading.setFont(FontManager.getRunescapeBoldFont());
		heading.setForeground(Color.WHITE);
		heading.setBorder(new EmptyBorder(0, 0, 6, 0));
		card.add(heading);
		return card;
	}

	/** Stretches a component across the panel. */
	private static JComponent full(JComponent c)
	{
		JPanel row = new JPanel(new GridLayout(1, 1));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(2, 0, 2, 0));
		row.add(c);
		return row;
	}

	private static JLabel subheading(String text)
	{
		JLabel label = wrapped("<font color='#ffffff'>" + escape(text) + "</font>");
		label.setBorder(new EmptyBorder(10, 0, 0, 0));
		return label;
	}

	/** Text (which may contain HTML) wrapped to a width in CSS px. */
	private static String html(String text, int width)
	{
		return "<html><body style='width:" + width + "px'>" + text + "</body></html>";
	}

	/** A button whose label wraps instead of being cut off. */
	private static JButton button(String text)
	{
		return button(text, WRAP_WIDTH);
	}

	/** A button whose label wraps instead of being cut off, in a space `width` wide (CSS px). */
	private static JButton button(String text, int width)
	{
		// Narrower than other text, to leave room for the button's padding.
		return new JButton("<html><body style='width:" + (width - 15) + "px;text-align:center'>" + escape(text) + "</body></html>");
	}

	private static JLabel wrapped(String text)
	{
		return wrapped(text, WRAP_WIDTH);
	}

	/** A label whose text wraps to a width. Text may contain HTML. */
	private static JLabel wrapped(String text, int width)
	{
		JLabel label = new JLabel(html(text, width))
		{
			@Override
			public void setText(String text)
			{
				super.setText(text == null || text.isEmpty() || text.startsWith("<html>") ? text : html(text, width));
			}
		};
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	private static JComponent gap(int height)
	{
		JPanel gap = new JPanel();
		gap.setOpaque(false);
		gap.setBorder(new EmptyBorder(height, 0, 0, 0));
		gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		return gap;
	}
}
