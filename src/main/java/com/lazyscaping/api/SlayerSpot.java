package com.lazyscaping.api;

import lombok.Getter;

/** A good place to do a Slayer task. */
@Getter
public class SlayerSpot
{
	private String where;
	private String how;
	private boolean multi;
	private boolean cannon;
	private boolean wilderness;
}
