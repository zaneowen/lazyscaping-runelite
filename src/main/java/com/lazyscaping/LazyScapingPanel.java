package com.lazyscaping;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/** The side panel: the player's training goal and plan, and bosses they're ready for. Only touched on the Swing thread. */
class LazyScapingPanel extends PluginPanel
{
	interface Actions
	{
		/** The player picked a skill (null to clear the goal) or target level. */
		void goalChanged(Skill skill, int targetLevel);

		void openTraining();

		void openBosses();

		void openBoss(String bossId);

		void refresh();
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
	private static final Color READY = new Color(0x7bc96f);
	private static final Color CLOSE = new Color(0xd9a23a);
	private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance(Locale.ROOT);

	private final Actions actions;
	private final JLabel status = wrapped("");
	private final JComboBox<String> skillBox = new JComboBox<>();
	private final JSpinner targetSpinner = new JSpinner(new SpinnerNumberModel(99, 2, 99, 1));
	private final JProgressBar progressBar = new JProgressBar(0, 1000);
	private final JLabel xpLeft = small("");
	private final JLabel rate = small("");
	private final JPanel planList = column();
	private final JPanel bossList = column();
	private final JLabel liveNotice = wrapped("Turn on <b>Live recommendations</b> in this plugin's settings to see your training plan and boss suggestions here.");
	/** Set while the goal controls are changed in code, so they don't report it as the player's choice. */
	private boolean settingGoal;

	LazyScapingPanel(Actions actions)
	{
		this.actions = actions;
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout());

		JPanel content = column();
		JLabel title = new JLabel("LazyScaping");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(CLOSE);
		content.add(title);
		content.add(status);
		content.add(gap());

		JButton sync = new JButton("Open my planner on lazyscaping.app");
		sync.setToolTipText("Opens the site with your name, gear and quests filled in");
		sync.addActionListener(e -> actions.openTraining());
		content.add(full(sync));
		content.add(gap());

		content.add(heading("Training goal"));
		skillBox.addItem(CHOOSE_SKILL);
		for (Skill skill : LazyScapingPlugin.SKILLS)
		{
			skillBox.addItem(skill.getName());
		}
		skillBox.addActionListener(e -> goalEdited());
		targetSpinner.addChangeListener(e -> goalEdited());
		JPanel goalRow = new JPanel(new BorderLayout(6, 0));
		goalRow.add(skillBox, BorderLayout.CENTER);
		goalRow.add(targetSpinner, BorderLayout.EAST);
		content.add(full(goalRow));
		progressBar.setStringPainted(true);
		progressBar.setForeground(CLOSE);
		progressBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.add(full(progressBar));
		content.add(xpLeft);
		content.add(rate);
		content.add(gap());
		content.add(planList);
		content.add(gap());

		content.add(heading("Bosses for you"));
		content.add(bossList);
		JButton bosses = new JButton("All boss suggestions");
		bosses.addActionListener(e -> actions.openBosses());
		content.add(full(bosses));
		content.add(gap());

		content.add(liveNotice);
		JButton refresh = new JButton("Refresh");
		refresh.addActionListener(e -> actions.refresh());
		content.add(full(refresh));

		add(content, BorderLayout.NORTH);
		setLoggedIn(false);
	}

	void setLoggedIn(boolean loggedIn)
	{
		status.setText(loggedIn ? "" : "Log in to see your goal and suggestions.");
		status.setVisible(!loggedIn);
		if (!loggedIn)
		{
			showProgress(null);
			showPlanMessage(null);
			showBossesMessage(null);
		}
	}

	void setLiveEnabled(boolean enabled)
	{
		liveNotice.setVisible(!enabled);
		if (!enabled)
		{
			showPlanMessage(null);
			showBossesMessage(null);
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
		Skill skill = selectedSkill();
		targetSpinner.setEnabled(skill != null);
		actions.goalChanged(skill, (Integer) targetSpinner.getValue());
	}

	private Skill selectedSkill()
	{
		return LazyScapingPlugin.skillNamed((String) skillBox.getSelectedItem());
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
			planList.add(small(plan.goalReached ? "" : "No methods fit this goal yet."));
		}
		else
		{
			planList.add(small("LazyScaping plan · about " + hours(plan.totalHours)
				+ (plan.totalCost > 0 ? " · costs " + gp(plan.totalCost) : plan.totalCost < 0 ? " · makes " + gp(-plan.totalCost) : "")));
			for (LazyScapingApi.PlanStep step : plan.steps)
			{
				JLabel label = wrapped("<b>" + step.fromLevel + "-" + step.toLevel + "</b> " + escape(step.method)
					+ "<br><font color='#a8a29e'>" + NUMBER.format(step.xpHr) + " xp/hr · " + hours(step.hours) + "</font>");
				label.setToolTipText(step.notes);
				label.setBorder(new EmptyBorder(4, 0, 0, 0));
				planList.add(label);
			}
		}
		refreshLayout(planList);
	}

	void showPlanMessage(String message)
	{
		planList.removeAll();
		if (message != null)
		{
			planList.add(small(message));
		}
		refreshLayout(planList);
	}

	void showBosses(LazyScapingApi.Bosses bosses)
	{
		bossList.removeAll();
		if (bosses.bosses == null || bosses.bosses.isEmpty())
		{
			bossList.add(small("No bosses fit your levels and gear yet."));
		}
		else
		{
			for (LazyScapingApi.BossSuggestion boss : bosses.bosses)
			{
				bossList.add(bossRow(boss));
			}
		}
		refreshLayout(bossList);
	}

	void showBossesMessage(String message)
	{
		bossList.removeAll();
		if (message != null)
		{
			bossList.add(small(message));
		}
		refreshLayout(bossList);
	}

	private JComponent bossRow(LazyScapingApi.BossSuggestion boss)
	{
		boolean ready = "ready".equals(boss.readiness);
		StringBuilder detail = new StringBuilder(ready ? "Ready" : "Almost").append(" · ").append(escape(boss.style));
		if (boss.missing != null)
		{
			for (LazyScapingApi.MissingLevel m : boss.missing)
			{
				detail.append(" · ").append(m.need).append(' ').append(escape(m.skill));
			}
		}
		if (boss.gearShort)
		{
			detail.append(" · better gear");
		}
		JLabel label = wrapped("<font color='" + hex(ready ? READY : CLOSE) + "'>" + escape(boss.name) + "</font>"
			+ "<br><font color='#a8a29e'>" + detail + "</font>");
		label.setBorder(new EmptyBorder(3, 0, 3, 0));
		label.setToolTipText("Open the gear plan for " + boss.name);
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				actions.openBoss(boss.id);
			}
		});
		return label;
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

	private static String gp(long gp)
	{
		if (gp >= 1_000_000)
		{
			return String.format(Locale.ROOT, "%.1fM gp", gp / 1_000_000.0);
		}
		if (gp >= 1_000)
		{
			return Math.round(gp / 1_000.0) + "k gp";
		}
		return gp + " gp";
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
		return panel;
	}

	/** Stretches a component across the panel. */
	private static JComponent full(JComponent c)
	{
		JPanel row = new JPanel(new GridLayout(1, 1));
		row.setBorder(new EmptyBorder(2, 0, 2, 0));
		row.add(c);
		return row;
	}

	private static JLabel heading(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(Color.WHITE);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		return label;
	}

	private static JLabel small(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	/** A label whose text wraps to the panel's width. Text may contain HTML. */
	private static JLabel wrapped(String html)
	{
		JLabel label = new JLabel("<html><body style='width:" + (PANEL_WIDTH - 40) + "px'>" + html + "</body></html>")
		{
			@Override
			public void setText(String text)
			{
				super.setText(text.startsWith("<html>") || text.isEmpty() ? text : "<html><body style='width:" + (PANEL_WIDTH - 40) + "px'>" + text + "</body></html>");
			}
		};
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	private static JComponent gap()
	{
		JPanel gap = new JPanel();
		gap.setBorder(new EmptyBorder(6, 0, 0, 0));
		return gap;
	}
}
