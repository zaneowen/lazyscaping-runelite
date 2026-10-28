package com.lazyscaping;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Skill;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

/**
 * The side panel: a header with the sync button, then a Training tab (goal,
 * plan and methods) and a Bossing tab (gear setups). Only touched on the Swing
 * thread.
 */
class LazyScapingPanel extends PluginPanel
{
	/** What the player does in the panel. Called on the Swing thread. */
	interface Actions
	{
		/** The player picked a skill (null to clear the goal) or target level. */
		void goalChanged(Skill skill, int targetLevel);

		/** Opens the site with the player's data filled in. */
		void openPlanner();

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

	private final TrainingTab training;
	private final BossingTab bossing;
	private final JLabel status = Ui.wrapped("");
	private final JLabel liveNotice = Ui.wrapped("Turn on <b>Live recommendations</b> in this plugin's settings to see your training plan and boss gear setups.");

	LazyScapingPanel(Actions actions, Icons icons)
	{
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout());
		training = new TrainingTab(actions);
		bossing = new BossingTab(actions, icons);

		JPanel content = Ui.column();
		JLabel title = new JLabel("LazyScaping");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Ui.GOLD);
		content.add(title);
		content.add(status);
		content.add(Ui.gap(6));
		JButton sync = Ui.button("Open my planner on lazyscaping.app");
		sync.setToolTipText("Opens the site with your name, gear and quests filled in");
		sync.addActionListener(e -> actions.openPlanner());
		content.add(Ui.full(sync));
		content.add(Ui.gap(8));

		JPanel display = new JPanel(new BorderLayout());
		MaterialTabGroup tabs = new MaterialTabGroup(display);
		tabs.setLayout(new GridLayout(1, 2, 4, 0));
		tabs.setBorder(new EmptyBorder(0, 0, 8, 0));
		MaterialTab trainingTab = new MaterialTab("Training", tabs, training);
		tabs.addTab(trainingTab);
		tabs.addTab(new MaterialTab("Bossing", tabs, bossing));
		tabs.select(trainingTab);
		content.add(tabs);
		content.add(display);

		content.add(Ui.gap(8));
		content.add(liveNotice);
		content.add(Ui.gap(4));
		JButton refresh = Ui.button("Refresh");
		refresh.addActionListener(e -> actions.refresh());
		content.add(Ui.full(refresh));

		add(content, BorderLayout.NORTH);
		setLoggedIn(false);
	}

	void setLoggedIn(boolean loggedIn)
	{
		status.setText(loggedIn ? "" : "Log in to see your goal and gear setups.");
		status.setVisible(!loggedIn);
		if (!loggedIn)
		{
			training.showProgress(null);
			training.showPlanMessage(null);
			bossing.showGearMessage(null);
		}
	}

	void setLiveEnabled(boolean enabled)
	{
		liveNotice.setVisible(!enabled);
		if (!enabled)
		{
			training.showPlanMessage("Needs Live recommendations.");
			bossing.showGearMessage("Needs Live recommendations.");
		}
	}

	/** Shows the player's saved choices without reporting them back as changes. */
	void setChoices(Skill goalSkill, int goalLevel, String bossId, String trainingBudget, String gearBudget)
	{
		training.setGoal(goalSkill, goalLevel);
		training.setBudget(trainingBudget);
		bossing.selectBoss(bossId);
		bossing.setBudget(gearBudget);
	}

	void setGoal(Skill skill, int targetLevel)
	{
		training.setGoal(skill, targetLevel);
	}

	/** The coins "My coins" budgets stand for. */
	void setCoins(long coins)
	{
		training.setCoins(coins);
		bossing.setCoins(coins);
	}

	void setBossOptions(List<GearCatalog.Boss> bosses)
	{
		bossing.setBossOptions(bosses);
	}

	void showProgress(Progress progress)
	{
		training.showProgress(progress);
	}

	void showPlan(LazyScapingApi.Plan plan)
	{
		training.showPlan(plan);
	}

	void showPlanMessage(String message)
	{
		training.showPlanMessage(message);
	}

	void showGear(LazyScapingApi.GearSetup setup)
	{
		bossing.showGear(setup);
	}

	void showGearMessage(String message)
	{
		bossing.showGearMessage(message);
	}
}
