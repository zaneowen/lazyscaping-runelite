package com.lazyscaping.ui;

import com.lazyscaping.api.Boss;
import com.lazyscaping.api.GearSetup;
import com.lazyscaping.api.Plan;
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

/** The side panel: a header with the sync button, then Training and Bossing tabs. */
public class LazyScapingPanel extends PluginPanel
{
	private final TrainingTab training;
	private final BossingTab bossing;
	private final JLabel status = Components.label("");
	private final JLabel liveNotice = Components.label(
		"Turn on <b>Live recommendations</b> in this plugin's settings to see your training plan and boss gear setups.");

	public LazyScapingPanel(PanelActions actions, IconLoader icons)
	{
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout());
		training = new TrainingTab(actions, icons);
		bossing = new BossingTab(actions, icons);

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
		tabs.setLayout(new GridLayout(1, 2, 4, 0));
		tabs.setBorder(new EmptyBorder(0, 0, 8, 0));
		MaterialTab trainingTab = new MaterialTab("Training", tabs, training);
		tabs.addTab(trainingTab);
		tabs.addTab(new MaterialTab("Bossing", tabs, bossing));
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
			bossing.showMessage(null);
		}
	}

	public void setLiveEnabled(boolean enabled)
	{
		liveNotice.setVisible(!enabled);
		if (!enabled)
		{
			training.showPlanMessage("Needs Live recommendations.");
			bossing.showMessage("Needs Live recommendations.");
		}
	}

	/** Shows the player's saved choices without reporting them back as changes. */
	public void setChoices(Skill goalSkill, int goalLevel, String bossId, String trainingBudget, String gearBudget)
	{
		training.setGoal(goalSkill, goalLevel);
		training.setBudget(trainingBudget);
		bossing.selectBoss(bossId);
		bossing.setBudget(gearBudget);
	}

	public void setGoal(Skill skill, int targetLevel)
	{
		training.setGoal(skill, targetLevel);
	}

	public void setCoins(long coins)
	{
		training.setCoins(coins);
		bossing.setCoins(coins);
	}

	public void setBossOptions(List<Boss> bosses)
	{
		bossing.setBossOptions(bosses);
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

	public void showGearSetup(GearSetup setup)
	{
		bossing.showSetup(setup);
	}

	public void showGearMessage(String message)
	{
		bossing.showMessage(message);
	}
}
