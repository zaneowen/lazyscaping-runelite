package com.lazyscaping.api;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

/** Gear for an extra combat style, swapped in from the inventory. */
@Getter
public class GearSwitch
{
	private String style;
	private List<GearSlot> picks = Collections.emptyList();
}
