package com.lazyscaping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lazyscaping.api.GearSetup;
import java.awt.Color;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;

public class SetupExporterTest
{
	/** The Gson RuneLite gives plugins, which Inventory Setups also reads with. */
	private static final Gson GSON = RuneLiteAPI.GSON;

	private static final String SETUP = "{"
		+ "\"style\": \"Ranged\","
		+ "\"picks\": ["
		+ "  {\"slot\": \"Weapon\", \"status\": \"owned\", \"item\": {\"id\": \"dragon_hunter_crossbow\", \"itemId\": 21012}},"
		+ "  {\"slot\": \"Ammo\", \"status\": \"buy\", \"item\": {\"id\": \"ruby_dragon_bolts_e\", \"itemId\": 21944}},"
		+ "  {\"slot\": \"Shield\", \"status\": \"none\"}"
		+ "],"
		+ "\"inventory\": ["
		+ "  {\"kind\": \"potion\", \"name\": \"Prayer potion(4)\", \"itemId\": 2434, \"qty\": 1},"
		+ "  {\"kind\": \"utility\", \"name\": \"Rune pouch\", \"itemId\": 12791, \"qty\": 1}"
		+ "],"
		+ "\"runePouch\": [{\"name\": \"Fire rune\", \"itemId\": 554, \"qty\": 60}]"
		+ "}";

	private JsonObject export()
	{
		OwnedItems owned = new OwnedItems(null, null, null);
		SetupExporter exporter = new SetupExporter(GSON, owned, new BankTagLoader(null, null, null, null, null, owned));
		String text = exporter.export(GSON.fromJson(SETUP, GearSetup.class), "LazyScaping: Vorkath (Ranged)");
		return GSON.fromJson(text, JsonObject.class);
	}

	@Test
	public void writesInventorySetupsPortableFormat()
	{
		JsonObject portable = export();
		JsonObject setup = portable.getAsJsonObject("setup");
		assertEquals("LazyScaping: Vorkath (Ranged)", setup.get("name").getAsString());
		assertTrue(portable.get("layout").isJsonArray());

		JsonArray equipment = setup.getAsJsonArray("eq");
		assertEquals(EquipmentInventorySlot.values().length, equipment.size());
		assertEquals(21012, equipment.get(EquipmentInventorySlot.WEAPON.getSlotIdx()).getAsJsonObject().get("id").getAsInt());
		assertEquals(21944, equipment.get(EquipmentInventorySlot.AMMO.getSlotIdx()).getAsJsonObject().get("id").getAsInt());
		assertTrue(equipment.get(EquipmentInventorySlot.SHIELD.getSlotIdx()).isJsonNull());

		JsonArray inventory = setup.getAsJsonArray("inv");
		assertEquals(28, inventory.size());
		assertEquals(2434, inventory.get(0).getAsJsonObject().get("id").getAsInt());
		assertTrue(inventory.get(2).isJsonNull());

		JsonObject fire = setup.getAsJsonArray("rp").get(0).getAsJsonObject();
		assertEquals(554, fire.get("id").getAsInt());
		assertEquals(60, fire.get("q").getAsInt());
	}

	@Test
	public void writesAColourInventorySetupsReadsBack()
	{
		Color colour = GSON.fromJson(export().getAsJsonObject("setup").get("hc"), Color.class);
		assertEquals(Color.RED, colour);
	}
}
