package com.lazyscaping;

import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.ConfigManager;

/**
 * Teleports in a player-owned house, each ticked in the settings. The site
 * swaps a setup's teleport for a house teleport when one of these replaces it,
 * and ignores the rest.
 */
@RequiredArgsConstructor
enum HouseTeleport
{
	FAIRY_RING("fairy_ring", "houseFairyRing"),
	SPIRIT_TREE("spirit_tree", "houseSpiritTree"),
	XERICS_TALISMAN("xerics_talisman", "houseXericsTalisman"),
	DIGSITE_PENDANT("digsite_pendant", "houseDigsitePendant"),
	MOUNTED_GLORY("mounted_glory", "houseMountedGlory"),
	MYTHICAL_CAPE("mythical_cape", "houseMythicalCape"),
	RING_OF_DUELING("jewellery_dueling", "houseRingOfDueling"),
	GAMES_NECKLACE("jewellery_games", "houseGamesNecklace"),
	COMBAT_BRACELET("jewellery_combat", "houseCombatBracelet"),
	SKILLS_NECKLACE("jewellery_skills", "houseSkillsNecklace"),
	RING_OF_WEALTH("jewellery_wealth", "houseRingOfWealth"),
	AMULET_OF_GLORY("jewellery_glory", "houseAmuletOfGlory"),
	ANNAKARL_PORTAL("annakarl_portal", "houseAnnakarlPortal"),
	APE_ATOLL_DUNGEON_PORTAL("ape_atoll_dungeon_portal", "houseApeAtollDungeonPortal"),
	ARCEUUS_LIBRARY_PORTAL("arceuus_library_portal", "houseArceuusLibraryPortal"),
	BARBARIAN_OUTPOST_PORTAL("barbarian_outpost_portal", "houseBarbarianOutpostPortal"),
	BARROWS_PORTAL("barrows_portal", "houseBarrowsPortal"),
	BATTLEFRONT_PORTAL("battlefront_portal", "houseBattlefrontPortal"),
	CAMELOT_PORTAL("camelot_portal", "houseCamelotPortal"),
	CARRALLANGER_PORTAL("carrallanger_portal", "houseCarrallangerPortal"),
	CATHERBY_PORTAL("catherby_portal", "houseCatherbyPortal"),
	CIVITAS_PORTAL("civitas_portal", "houseCivitasPortal"),
	DAREEYAK_PORTAL("dareeyak_portal", "houseDareeyakPortal"),
	DRAYNOR_MANOR_PORTAL("draynor_manor_portal", "houseDraynorManorPortal"),
	EAST_ARDOUGNE_PORTAL("ardougne_portal", "houseEastArdougnePortal"),
	FALADOR_PORTAL("falador_portal", "houseFaladorPortal"),
	FENKENSTRAIN_PORTAL("fenkenstrain_portal", "houseFenkenstrainPortal"),
	FISHING_GUILD_PORTAL("fishing_guild_portal", "houseFishingGuildPortal"),
	GHORROCK_PORTAL("ghorrock_portal", "houseGhorrockPortal"),
	HARMONY_ISLAND_PORTAL("harmony_island_portal", "houseHarmonyIslandPortal"),
	ICE_PLATEAU_PORTAL("ice_plateau_portal", "houseIcePlateauPortal"),
	KHARYRLL_PORTAL("kharyrll_portal", "houseKharyrllPortal"),
	KOUREND_CASTLE_PORTAL("kourend_castle_portal", "houseKourendCastlePortal"),
	LASSAR_PORTAL("lassar_portal", "houseLassarPortal"),
	LUMBRIDGE_PORTAL("lumbridge_portal", "houseLumbridgePortal"),
	LUNAR_ISLE_PORTAL("lunar_isle_portal", "houseLunarIslePortal"),
	MARIM_PORTAL("marim_portal", "houseMarimPortal"),
	MIND_ALTAR_PORTAL("mind_altar_portal", "houseMindAltarPortal"),
	OURANIA_CAVE_PORTAL("ourania_cave_portal", "houseOuraniaCavePortal"),
	PADDEWWA_PORTAL("paddewwa_portal", "housePaddewwaPortal"),
	PORT_KHAZARD_PORTAL("port_khazard_portal", "housePortKhazardPortal"),
	SALVE_GRAVEYARD_PORTAL("salve_graveyard_portal", "houseSalveGraveyardPortal"),
	SENNTISTEN_PORTAL("senntisten_portal", "houseSenntistenPortal"),
	FORGOTTEN_CEMETERY_PORTAL("forgotten_cemetery_portal", "houseForgottenCemeteryPortal"),
	TROLL_STRONGHOLD_PORTAL("troll_stronghold_portal", "houseTrollStrongholdPortal"),
	TROLLHEIM_PORTAL("trollheim_portal", "houseTrollheimPortal"),
	VARROCK_PORTAL("varrock_portal", "houseVarrockPortal"),
	WATERBIRTH_PORTAL("waterbirth_portal", "houseWaterbirthPortal"),
	WATCHTOWER_PORTAL("watchtower_portal", "houseWatchtowerPortal"),
	WEISS_PORTAL("weiss_portal", "houseWeissPortal"),
	WEST_ARDOUGNE_PORTAL("west_ardougne_portal", "houseWestArdougnePortal");

	/** The name lazyscaping.app's API uses. */
	@Getter
	private final String apiName;
	/** The key of its tick box in {@link LazyScapingConfig}. */
	@Getter
	private final String configKey;

	/** What the settings say the player's house has. */
	static Set<HouseTeleport> owned(ConfigManager configManager)
	{
		Set<HouseTeleport> owned = EnumSet.noneOf(HouseTeleport.class);
		for (HouseTeleport teleport : values())
		{
			if (teleport.isTicked(configManager))
			{
				owned.add(teleport);
			}
		}
		return owned;
	}

	boolean isTicked(ConfigManager configManager)
	{
		return Boolean.TRUE.equals(configManager.getConfiguration(LazyScapingConfig.GROUP, configKey, Boolean.class));
	}

	/** Whether it's a portal, which can be in a portal chamber or a portal nexus. */
	boolean isPortal()
	{
		return apiName.endsWith("_portal");
	}
}
