package com.lazyscaping.api;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
public class TrainingMethod
{
	private String id;
	private String name;
	private int level;
	private int xpHr;
	/** Positive costs gp per xp, negative makes gp. */
	private double gpPerXp;
	/** 1 is click-intensive, 3 is very AFK. */
	private int afk;
	private boolean members;
	private boolean ironFriendly;
	private String notes;
	/** What the method makes or uses up, or null when it has no recipe. */
	private ItemRef icon;
	private Materials materials;
	private MethodGuide guide;
	private boolean available;
	/** Why it can't be used yet, like "Needs level 70". */
	private List<String> blockers = Collections.emptyList();
	/** Nothing but level stands in the way before the goal, so it can be planned around. */
	private boolean pickable;
}
