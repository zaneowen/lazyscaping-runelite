package com.lazyscaping.ui;

import lombok.Value;

/** The player's real pace at a boss this session, from their kill count messages. */
@Value
public class KillPace
{
	/** Kills this session. */
	int kills;
	/** Kills an hour between the first and last kill, or 0 until there are two. */
	double kph;
}
