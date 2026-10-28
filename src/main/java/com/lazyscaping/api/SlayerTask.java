package com.lazyscaping.api;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

/** Where and how to do a Slayer task. */
@Getter
public class SlayerTask
{
	private String id;
	private String name;
	private int slayer;
	/** A quest or unlock needed before it's assigned. */
	private String requires;
	private List<String> masters = Collections.emptyList();
	/** The best places to do it, best first. */
	private List<SlayerSpot> spots = Collections.emptyList();
	private List<String> otherPlaces = Collections.emptyList();
	/** Items the task can't be done without. */
	private List<String> needs = Collections.emptyList();
	private List<String> tips = Collections.emptyList();
	/** Advice for skipping or blocking it. */
	private String skip;
	/** A boss that counts towards the task. */
	private String boss;
}
