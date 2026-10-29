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
	/** Set for bosses fought solo or in a group. */
	private TeamOptions team;
	private String style;
	/** Styles the boss can be fought with. */
	private List<String> styles = Collections.emptyList();
	/** The style the player's gear suits best, or null when the boss has one style. */
	private String bestStyle;
	private FightNotes notes;
	/** Null when the boss has no money making guide. */
	private Profit profit;
	/** The upgrades that do the most for their price, one per slot. */
	private List<UpgradeOption> upgrades = Collections.emptyList();
	/** Set when no gear can be brought, saying why. */
	private String noGear;
	private List<GearSlot> picks = Collections.emptyList();
	/**
	 * Total bonuses of the worn gear, as on the in-game equipment stats screen:
	 * five attack bonuses, five defence bonuses, then melee strength, ranged
	 * strength, magic damage % and prayer.
	 */
	private List<Double> stats = Collections.emptyList();
	/** Indexes into {@link #stats} of the bonuses that matter most for the style. */
	private List<Integer> keyStats = Collections.emptyList();
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
