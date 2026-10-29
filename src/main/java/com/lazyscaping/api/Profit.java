package com.lazyscaping.api;

import lombok.Getter;

/** Kills and gp an hour for a setup, from the boss's money making guide on the wiki. */
@Getter
public class Profit
{
	private double kph;
	private long gpPerHour;
	/** The guide's setup, like "Dragon hunter crossbow". */
	private String guide;
	private String wiki;
	/** What the upgrades being tried add, or null when none are. */
	private Gain tryingGain;

	@Getter
	public static class Gain
	{
		private double kph;
		private long gpPerHour;
	}
}
