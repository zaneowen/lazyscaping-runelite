package com.lazyscaping.ui;

import com.lazyscaping.api.Boss;
import com.lazyscaping.api.GearSetup;
import com.lazyscaping.api.Plan;
import com.lazyscaping.api.SlayerTaskRef;
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

/** The side panel: a header with the sync button, then Training, Bossing and Slayer tabs. */
public class LazyScapingPanel extends PluginPanel
{
	private final TrainingTab training;
	private final BossingTab bossing;
	private final SlayerTab slayer;
	private final JLabel status = Components.label("");
	private final JLabel liveNotice = Components.label(
		"Turn on <b>Live recommendations</b> in this plugin's settings to see your training plan and gear setups.");

	public LazyScapingPanel(PanelActions actions, IconLoader icons)
	{
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout());
		training = new TrainingTab(actions, icons);
		bossing = new BossingTab(actions, icons);
		slayer = new SlayerTab(actions, icons);

		JLabel title = new JLabel("LazyScaping");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Html.GOLD);

		JButton sync = Components.button("Open my planner on lazyscaping.app");
		sync.setToolTipText("Opens the site with your name, gear and quests filled in");
		sync.addActionListener(e -> actions.openPlanner());

		JButton refresh = Components.button("Refresh");
		refresh.addActionListener(e -> actions.refresh());

		JPanel display = new JPanel(new BorderLayout());
		MaterialTabGroup tabs = new MaterialTabGroup(display);
		tabs.setLayout(new GridLayout(1, 3, 2, 0));
		tabs.setBorder(new EmptyBorder(0, 0, 8, 0));
		MaterialTab trainingTab = new MaterialTab("Training", tabs, training);
		tabs.addTab(trainingTab);
		tabs.addTab(new MaterialTab("Bossing", tabs, bossing));
		tabs.addTab(new MaterialTab("Slayer", tabs, slayer));
		tabs.select(trainingTab);

		JPanel content = Components.column();
		content.add(title);
		content.add(status);
		content.add(Components.gap(6));
		content.add(Components.fullWidth(sync));
		content.add(Components.gap(8));
		content.add(tabs);
		content.add(display);
		content.add(Components.gap(8));
		content.add(liveNotice);
		content.add(Components.gap(4));
		content.add(Components.fullWidth(refresh));
		add(content, BorderLayout.NORTH);

		setLoggedIn(false);
	}

	public void setLoggedIn(boolean loggedIn)
	{
		status.setText(loggedIn ? "" : "Log in to see your goal and gear setups.");
		status.setVisible(!loggedIn);
		if (!loggedIn)
		{
			training.showProgress(null);
			training.showPlanMessage(null);
			bossing.getSetupView().showMessage(null);
			slayer.showMessage(null);
			slayer.setCurrentTask(null);
		}
	}

	public void setLiveEnabled(boolean enabled)
	{
		liveNotice.setVisible(!enabled);
		if (!enabled)
		{
			training.showPlanMessage("Needs Live recommendations.");
			bossing.getSetupView().showMessage("Needs Live recommendations.");
			slayer.showMessage("Needs Live recommendations.");
		}
	}

	/** Shows the player's saved choices without reporting them back as changes. */
	public void setChoices(Skill goalSkill, int goalLevel, String bossId, String taskId, String trainingBudget, String gearBudget)
	{
		training.setGoal(goalSkill, goalLevel);
		training.setBudget(trainingBudget);
		bossing.selectBoss(bossId);
		slayer.selectTask(taskId);
		setup(GearTarget.BOSS).setBudget(gearBudget);
		setup(GearTarget.SLAYER).setBudget(gearBudget);
	}

	public void setGoal(Skill skill, int targetLevel)
	{
		training.setGoal(skill, targetLevel);
	}

	/** Keeps both gear budget pickers on the same budget. */
	public void setGearBudget(String budget)
	{
		setup(GearTarget.BOSS).setBudget(budget);
		setup(GearTarget.SLAYER).setBudget(budget);
	}

	public void setCoins(long coins)
	{
		training.setCoins(coins);
		setup(GearTarget.BOSS).setCoins(coins);
		setup(GearTarget.SLAYER).setCoins(coins);
	}

	public void setBossOptions(List<Boss> bosses)
	{
		bossing.setBossOptions(bosses);
	}

	public void setTaskOptions(List<SlayerTaskRef> tasks)
	{
		slayer.setTaskOptions(tasks);
	}

	/** Shows the game's current Slayer task, like "87 x Abyssal demons, in the Catacombs", or null for none. */
	public void setCurrentTask(String description, String taskId)
	{
		slayer.setCurrentTask(description);
		if (taskId != null)
		{
			slayer.selectTask(taskId);
		}
	}

	public void showProgress(GoalProgress progress)
	{
		training.showProgress(progress);
	}

	public void showPlan(Plan plan)
	{
		training.showPlan(plan);
	}

	public void showPlanMessage(String message)
	{
		training.showPlanMessage(message);
	}

	public void showGearSetup(GearTarget target, GearSetup setup)
	{
		if (target == GearTarget.SLAYER)
		{
			slayer.showSetup(setup);
		}
		else
		{
			bossing.getSetupView().showSetup(setup);
		}
	}

	public void showGearMessage(GearTarget target, String message)
	{
		if (target == GearTarget.SLAYER)
		{
			slayer.showMessage(message);
		}
		else
		{
			bossing.getSetupView().showMessage(message);
		}
	}

	public void showReadyCheck(GearTarget target, ReadyCheck check)
	{
		setup(target).showReadyCheck(check);
	}

	public void showBankMessage(GearTarget target, String message)
	{
		setup(target).showBankMessage(message);
	}

	private GearSetupView setup(GearTarget target)
	{
		return target == GearTarget.SLAYER ? slayer.getSetupView() : bossing.getSetupView();
	}
}
