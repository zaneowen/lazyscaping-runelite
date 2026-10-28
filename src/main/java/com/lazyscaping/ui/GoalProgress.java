package com.lazyscaping.ui;

import lombok.Value;

/** Progress toward the training goal, read from the client. */
@Value
public class GoalProgress
{
	int level;
	long xp;
	/** Xp when the goal was set. */
	long startXp;
	long targetXp;
	/** From the XP Tracker; 0 while the skill isn't being trained. */
	int xpPerHour;
}
