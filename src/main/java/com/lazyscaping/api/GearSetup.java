package com.lazyscaping.api;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

/** The best gear setup for a boss or Slayer task from what the player owns, plus what a budget buys. */
@Getter
public class GearSetup
{
	private Boss boss;
	/** Set when the setup is for a Slayer task. */
	private SlayerTask task;
	private String style;
	/** Styles the boss can be fought with. */
	private List<String> styles = Collections.emptyList();
	/** Set when no gear can be brought, saying why. */
	private String noGear;
	private List<GearSlot> picks = Collections.emptyList();
	private List<GearSwitch> switches = Collections.emptyList();
	private GearSlot spec;
	/** One entry per inventory slot, in packing order. */
	private List<InventorySlot> inventory = Collections.emptyList();
	private int inventorySize;
	private List<RunePouchEntry> runePouch = Collections.emptyList();
	/** Food left out so the switches fit. */
	private int foodCut;
	/** Items that don't fit in one inventory. */
	private int overflow;
	private Travel travel;
	/** GE cost of supplies for one trip. */
	private long tripCost;
	/** The budget used, or null for unlimited. */
	private Long budget;
	private boolean ironman;
	/** Gp spent on gear. */
	private long gearCost;
	/** Supplies alone cost more than the budget, so no gear was bought. */
	private boolean suppliesOverBudget;
}
