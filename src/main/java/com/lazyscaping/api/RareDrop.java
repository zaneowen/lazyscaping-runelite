package com.lazyscaping.api;

import lombok.Getter;

/** One of a boss's rare drops, or its pet. */
@Getter
public class RareDrop
{
	private String name;
	/** As the wiki writes it, like "1/5,000". */
	private String rarity;
	/** The chance a kill, from 0 to 1. */
	private double chance;
}
