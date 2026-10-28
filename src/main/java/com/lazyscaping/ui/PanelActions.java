package com.lazyscaping.ui;

import com.lazyscaping.HouseTeleport;
import net.runelite.api.Skill;

/** What the player does in the panel. Called on the Swing thread. */
public interface PanelActions
{
	/** A goal skill (null to clear the goal) or target level was picked. */
	void goalChanged(Skill skill, int targetLevel);

	/** A training method was picked to plan around, or null for the best plan. */
	void methodPicked(String methodId);

	/** A boss was picked for the Bossing setup, or null to clear it. */
	void bossSelected(String bossId);

	/** A Slayer task was picked for the Slayer setup, or null to clear it. */
	void taskSelected(String taskId);

	/** The player said whether to plan Slayer gear with a Slayer helmet (i). */
	void slayerHelmetChanged(boolean planWithHelmet);

	/** A house teleport was ticked or unticked. */
	void houseTeleportChanged(HouseTeleport teleport, boolean has);

	/** The elite Lumbridge Diary box was ticked or unticked. */
	void lumbridgeEliteChanged(boolean done);

	/** A combat style was picked for a setup. */
	void styleSelected(GearTarget target, String style);

	/** A valid budget was set for training, or for gear when {@code gear} is true. */
	void budgetChanged(boolean gear, String budget);

	/** Shows a setup's items as a bank tag. */
	void showInBank(GearTarget target);

	/** Opens the site with the player's data filled in. */
	void openPlanner();

	/** Opens a setup's full plan on the site. */
	void openGearPlan(GearTarget target);

	void openWiki(String url);

	void refresh();
}
