package com.lazyscaping.api;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

/** One equipment slot of a gear setup. */
@Getter
public class GearSlot
{
	public enum Status
	{
		@SerializedName("owned") OWNED,
		/** Bought with the budget. */
		@SerializedName("buy") BUY,
		@SerializedName("none") NONE,
		/** Taken by a two-handed weapon. */
		@SerializedName("twoHanded") TWO_HANDED,
	}

	private String slot;
	private Status status;
	/** Gp spent on the slot when it's bought. */
	private long cost;
	/** The best item owned or bought for the slot, or null. */
	private GearItem item;
	/** The next item to work towards, or null. */
	private GearItem next;
	/** The boss needs a specific item here, like an anti-dragon shield, and none is owned. */
	private boolean missingRequired;
}
