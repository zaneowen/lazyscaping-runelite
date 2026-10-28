package com.lazyscaping;

import com.lazyscaping.api.GearSetup;
import com.lazyscaping.ui.GearTarget;
import java.util.Collections;
import java.util.Set;

/** The state of one gear setup in the panel. Used on the client thread. */
final class GearPlan
{
	/** Minimum game ticks between refreshes after the player's gear or levels change. */
	static final int REFRESH_TICKS = 100;

	final GearTarget target;
	/** The boss or Slayer task id. */
	String selectedId;
	/** The picked combat style, or null for the best one for the player's gear. */
	String style;
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
		setup = null;
		fetchedWith = Collections.emptySet();
	}
}
