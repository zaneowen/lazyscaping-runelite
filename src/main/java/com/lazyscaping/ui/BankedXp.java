package com.lazyscaping.ui;

import java.util.Map;
import lombok.Value;

/** The xp the materials in the player's bank and inventory are worth, for the methods of one skill. */
@Value
public class BankedXp
{
	/** Banked xp by method id, for methods whose materials the player has. */
	Map<String, Long> byMethod;
	/** The method the most xp is banked for, or null when nothing is. */
	String bestMethod;
	long bestXp;
	/** The skill's level now, and after using up the best method's materials. */
	int fromLevel;
	int toLevel;
}
