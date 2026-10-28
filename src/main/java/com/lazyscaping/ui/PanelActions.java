package com.lazyscaping.ui;

import net.runelite.api.Skill;

/** What the player does in the panel. Called on the Swing thread. */
public interface PanelActions
{
	/** A goal skill (null to clear the goal) or target level was picked. */
	void goalChanged(Skill skill, int targetLevel);

	/** A training method was picked to plan around, or null for the best plan. */
	void methodPicked(String methodId);

	/** A boss was picked for the gear setup (null to clear it), and optionally a combat style. */
	void bossSelected(String bossId, String style);

	/** A valid budget was set for training, or for gear when {@code gear} is true. */
	void budgetChanged(boolean gear, String budget);

	/** Opens the site with the player's data filled in. */
	void openPlanner();

	void openBossPlan(String bossId);

	void openWiki(String url);

	void refresh();
}
