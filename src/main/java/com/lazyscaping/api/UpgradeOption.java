package com.lazyscaping.api;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

/** An upgrade worth buying for a setup, with what it adds. */
@Getter
public class UpgradeOption extends GearItem
{
	/** The slot it goes in, like "Legs". */
	private String slot;
	/** How much closer the setup gets to the best for its style, in percentage points. */
	private int gearGain;
	/** Extra gp an hour, or null when the boss has no money making guide. */
	private Long gpPerHour;
	/** The change in each equipment bonus over what it replaces, in the order of {@link GearSetup#getStats()}. */
	private List<Double> stats = Collections.emptyList();
}
