package com.lazyscaping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class GearCatalogTest
{
	private final GearCatalog catalog = new GearCatalog(Map.of(
		"abyssal_whip", List.of("abyssal whip"),
		"trident_of_the_seas", List.of("trident of the seas", "uncharged trident"),
		"osmumtens_fang", List.of("osmumten's fang"),
		"ahrims_robetop", List.of("ahrim's robetop")
	), List.of());

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
	public void bundledCatalogLoads()
	{
		GearCatalog bundled = GearCatalog.bundled(new Gson());
		assertEquals("abyssal_whip", bundled.match("Abyssal whip"));
		assertEquals("toxic_blowpipe", bundled.match("Toxic blowpipe (empty)"));
		assertEquals(true, bundled.bosses().stream().anyMatch(b -> "vorkath".equals(b.id) && "Vorkath".equals(b.name)));
	}
}
