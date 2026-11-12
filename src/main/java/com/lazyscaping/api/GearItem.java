package com.lazyscaping.api;

import lombok.Getter;

/** A piece of gear the planners know. */
@Getter
public class GearItem extends ItemRef
{
	/** The site's gear id, used to match it against what the player owns. */
	private String id;
	/** GE price, or null when untradeable. Ammo is priced for one trip. */
	private Long price;
	/** How to get it without the GE, like where it drops and how often, or null. */
	private String howToGet;
}
