package com.lazyscaping.api;

import lombok.Getter;

/** An item the panel can draw: by its game item id when it has one, else by the site's picture. */
@Getter
public class ItemRef
{
	private String name;
	private Integer itemId;
	/** Path of the site's picture, like {@code /images/wiki/fire-cape.png}. */
	private String icon;
}
