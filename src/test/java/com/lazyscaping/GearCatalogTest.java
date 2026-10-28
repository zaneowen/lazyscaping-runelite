package com.lazyscaping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.lazyscaping.api.Catalog;
import org.junit.Test;

public class GearCatalogTest
{
	private static final Gson GSON = new Gson();

	private final GearCatalog catalog = new GearCatalog(GSON.fromJson("{\"gear\": {"
		+ "\"abyssal_whip\": [\"abyssal whip\"],"
		+ "\"trident_of_the_seas\": [\"trident of the seas\", \"uncharged trident\"],"
		+ "\"osmumtens_fang\": [\"osmumten's fang\"],"
		+ "\"ahrims_robetop\": [\"ahrim's robetop\"]"
		+ "}}", Catalog.class));

	@Test
	public void matchesNamesAndAliases()
	{
		assertEquals("abyssal_whip", catalog.match("Abyssal whip"));
		assertEquals("trident_of_the_seas", catalog.match("Uncharged trident"));
		assertEquals("osmumtens_fang", catalog.match("Osmumten\u2019s  fang"));
	}

	@Test
	public void stripsVariantAndDegradeSuffixes()
	{
		assertEquals("abyssal_whip", catalog.match("Abyssal whip (or)"));
		assertEquals("trident_of_the_seas", catalog.match("Trident of the seas (full)"));
		assertEquals("ahrims_robetop", catalog.match("Ahrim's robetop 75"));
	}

	@Test
	public void ignoresOtherItems()
	{
		assertNull(catalog.match("Lobster"));
		assertNull(catalog.match(""));
	}

	@Test
	public void loadsBundledCatalog()
	{
		GearCatalog bundled = GearCatalog.bundled(GSON);
		assertEquals("abyssal_whip", bundled.match("Abyssal whip"));
		assertEquals("toxic_blowpipe", bundled.match("Toxic blowpipe (empty)"));
		assertTrue(bundled.getBosses().stream().anyMatch(b -> "vorkath".equals(b.getId()) && "Vorkath".equals(b.getName())));
	}
}
