package com.lazyscaping.api;

import lombok.Getter;

/** A setup's damage against a boss, from the wiki's combat formulas. */
@Getter
public class Dps
{
	private int maxHit;
	/** 0 to 1: the chance each attack hits. */
	private double accuracy;
	private double dps;
	/** Seconds to kill the boss from full health. */
	private long killSeconds;
	/** How it attacks, like "Slash", "Rapid" or "Fire Surge". */
	private String attack;
}
