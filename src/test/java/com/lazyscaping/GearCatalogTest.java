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
	public void matchesOtherVersionsOfGear()
	{
		GearCatalog bundled = GearCatalog.bundled(GSON);
		assertEquals("amulet_of_glory", bundled.match("Amulet of glory(4)"));
		assertEquals("amulet_of_glory", bundled.match("Amulet of glory (t4)"));
		assertEquals("rune_platebody", bundled.match("Rune platebody (h3)"));
		assertEquals("dragon_dagger", bundled.match("Dragon dagger(p++)"));
		assertEquals("ancestral_hat", bundled.match("Twisted ancestral hat"));
		assertEquals("torva_platebody", bundled.match("Sanguine torva platebody"));
		assertEquals("infinity_top", bundled.match("Dark infinity top"));
		assertEquals("masori_body", bundled.match("Masori body (f)"));
		assertEquals("infernal_cape", bundled.match("Infernal cape (l)"));
		assertEquals("dizanas_quiver", bundled.match("Blessed Dizana's quiver (l)"));
		assertEquals("avernic_defender", bundled.match("Ghommal's avernic defender 6 (l)"));
		assertEquals("slayer_helmet_i", bundled.match("Radiant slayer helmet (i)"));
	}

	@Test
	public void matchesVersionsNamedUnlikeTheirItem()
	{
		assertEquals("abyssal_whip", catalog.match("Volcanic abyssal whip", "Abyssal whip"));
		assertNull(catalog.match("Abyssal whip (broken)", "Abyssal whip"));
		assertNull(catalog.match("Torva platebody (damaged)"));
	}
	@Test
	public void ignoresOtherItems()
	{
		assertNull(catalog.match("Lobster"));
		assertNull(catalog.match(""));
	}

	@Test
	public void matchesSlayerTasksByGameName()
	{
		GearCatalog bundled = GearCatalog.bundled(GSON);
		assertEquals("abyssal_demons", bundled.matchTask("Abyssal demons").getId());
		assertEquals("kalphites", bundled.matchTask("Kalphite").getId());
		assertEquals("metal_dragons", bundled.matchTask("Iron dragons").getId());
		assertNull(bundled.matchTask("Chickens"));
		assertNull(bundled.matchTask(null));
	}

	@Test
	public void loadsBundledCatalog()
	{
		GearCatalog bundled = GearCatalog.bundled(GSON);
		assertEquals("abyssal_whip", bundled.match("Abyssal whip"));
		assertEquals("toxic_blowpipe", bundled.match("Toxic blowpipe (empty)"));
		assertEquals("slayer_helmet_i", bundled.match("Hydra slayer helmet (i)"));
		assertTrue(bundled.getBosses().stream().anyMatch(b -> "vorkath".equals(b.getId()) && "Vorkath".equals(b.getName())));
		assertTrue(bundled.getBosses().stream().allMatch(b -> bundled.getBossGroups().contains(b.getGroup())));
	}
}
