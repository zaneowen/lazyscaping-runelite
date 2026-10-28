package com.lazyscaping.api;

import lombok.Getter;

/** An item a training method uses or makes, per action. */
@Getter
public class Material extends ItemRef
{
	private int qty;
}
