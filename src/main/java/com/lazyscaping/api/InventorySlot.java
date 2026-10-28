package com.lazyscaping.api;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class InventorySlot extends GearItem
{
	public enum Kind
	{
		@SerializedName("switch") SWITCH,
		@SerializedName("spec") SPEC,
		@SerializedName("utility") UTILITY,
		@SerializedName("potion") POTION,
		@SerializedName("food") FOOD,
	}

	private Kind kind;
	/** The combat style of a switch. */
	private String style;
	private boolean bought;
	/** Stack size, like a pile of teleport tablets. */
	private int qty;
	/** Why a teleport or tool is brought. */
	private String why;
}
