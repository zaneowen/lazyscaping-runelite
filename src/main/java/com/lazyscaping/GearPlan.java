package com.lazyscaping;

import com.lazyscaping.api.GearSetup;
import com.lazyscaping.ui.GearTarget;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** The state of one gear setup in the panel. Used on the client thread. */
final class GearPlan
{
	/** Minimum game ticks between refreshes after the player's gear or levels change. */
	static final int REFRESH_TICKS = 100;

	final GearTarget target;
	/** The boss or Slayer task id. */
	String selectedId;
	/** "solo" or "group" for bosses fought either way, or null for the boss's usual one. */
	String team;
	/** The picked combat style, or null for the best one for the player's gear. */
	String style;
	/** Upgrades being tried, by gear id: planned as if owned, until the player goes back to their own gear. */
	final Set<String> trying = new LinkedHashSet<>();
	/** The latest setup, for the ready check and bank tag. */
	GearSetup setup;
	/** The owned gear the setup was fetched with. */
	Set<String> fetchedWith = Collections.emptySet();
	boolean stale;
	int lastFetchTick = -REFRESH_TICKS;

	GearPlan(GearTarget target)
	{
		this.target = target;
	}

	void reset()
	{
		style = null;
		team = null;
		trying.clear();
		setup = null;
		fetchedWith = Collections.emptySet();
	}
}
