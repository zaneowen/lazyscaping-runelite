package com.lazyscaping.api;

import lombok.Getter;

/** The OSRS Wiki's advice on a Slayer task. */
@Getter
public class SlayerAdvice
{
	/** Whether to do, skip or block it, like "Do for money; block for XP". */
	private String verdict;
	/** Slayer xp an hour, by style when it differs, or null. */
	private String xpHr;
}
