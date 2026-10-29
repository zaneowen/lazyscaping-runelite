package com.lazyscaping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class HouseReaderTest
{
	@Test
	public void readsNexusMenuLines()
	{
		assertEquals(HouseTeleport.VARROCK_PORTAL, HouseReader.nexusDestination("<col=ffffff>1</col> : Varrock"));
		assertEquals(HouseTeleport.CAMELOT_PORTAL, HouseReader.nexusDestination("Seers' Village"));
		assertEquals(HouseTeleport.FENKENSTRAIN_PORTAL, HouseReader.nexusDestination("Fenkenstrain's Castle"));
		assertEquals(HouseTeleport.CIVITAS_PORTAL, HouseReader.nexusDestination("Civitas illa Fortis"));
		assertEquals(HouseTeleport.XERICS_TALISMAN, HouseReader.nexusDestination("Xeric's Lookout"));
	}

	@Test
	public void prefersTheLongerName()
	{
		assertEquals(HouseTeleport.WEST_ARDOUGNE_PORTAL, HouseReader.nexusDestination("West Ardougne"));
		assertEquals(HouseTeleport.EAST_ARDOUGNE_PORTAL, HouseReader.nexusDestination("Ardougne"));
		assertEquals(HouseTeleport.APE_ATOLL_DUNGEON_PORTAL, HouseReader.nexusDestination("Ape Atoll Dungeon"));
		assertEquals(HouseTeleport.MARIM_PORTAL, HouseReader.nexusDestination("Marim"));
		assertEquals(HouseTeleport.TROLL_STRONGHOLD_PORTAL, HouseReader.nexusDestination("Troll Stronghold"));
		assertEquals(HouseTeleport.TROLLHEIM_PORTAL, HouseReader.nexusDestination("Trollheim"));
	}

	@Test
	public void ignoresOtherText()
	{
		assertNull(HouseReader.nexusDestination("Portal Nexus"));
		assertNull(HouseReader.nexusDestination("Scry"));
		assertNull(HouseReader.nexusDestination(""));
	}
}
