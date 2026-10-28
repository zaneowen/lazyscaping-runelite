package com.lazyscaping;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ItemNamesTest
{
	@Test
	public void ignoresPotionDoses()
	{
		assertEquals("prayer potion", ItemNames.anyDose("Prayer potion(4)"));
		assertEquals("prayer potion", ItemNames.anyDose("Prayer potion(1)"));
		assertEquals("extended super antifire", ItemNames.anyDose("Extended super antifire(3)"));
	}

	@Test
	public void keepsOtherNames()
	{
		assertEquals("shark", ItemNames.anyDose("Shark"));
		assertEquals("teleport to house (tablet)", ItemNames.anyDose("Teleport to house (tablet)"));
	}
}
