package com.lazyscaping.api;

import lombok.Getter;

@Getter
public class Teleport extends ItemRef
{
	/** Where it takes you, and the rest of the way. */
	private String where;
}
