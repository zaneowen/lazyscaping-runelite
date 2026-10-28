package com.lazyscaping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BudgetTest
{
	@Test
	public void parsesAmountsLikeTheSite()
	{
		assertEquals(Long.valueOf(5_000_000), Budget.parseGp("5m"));
		assertEquals(Long.valueOf(250_000), Budget.parseGp("250K"));
		assertEquals(Long.valueOf(1_200_000_000), Budget.parseGp("1.2b"));
		assertEquals(Long.valueOf(1_000), Budget.parseGp(" 1,000 "));
		assertEquals(Long.valueOf(0), Budget.parseGp("Free"));
		assertNull(Budget.parseGp("lots"));
		assertNull(Budget.parseGp("5mm"));
	}

	@Test
	public void resolvesPresets()
	{
		assertNull(Budget.resolve("Unlimited", 0));
		assertEquals(Long.valueOf(12_345), Budget.resolve("My coins", 12_345));
		assertEquals(Long.valueOf(10_000_000), Budget.resolve("10M", 0));
	}

	@Test
	public void validatesInput()
	{
		assertTrue(Budget.isValid("50M"));
		assertTrue(Budget.isValid("unlimited"));
		assertTrue(Budget.isValid("My coins"));
		assertFalse(Budget.isValid("abc"));
		assertFalse(Budget.isValid(""));
	}
}
