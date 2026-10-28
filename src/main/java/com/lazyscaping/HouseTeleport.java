package com.lazyscaping;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Teleports in a player-owned house, each ticked in the settings. The site
 * swaps a setup's teleport for a house teleport when one of these replaces it,
 * and ignores the rest.
 */
@RequiredArgsConstructor
enum HouseTeleport
{
	FAIRY_RING("fairy_ring", LazyScapingConfig::houseFairyRing),
	SPIRIT_TREE("spirit_tree", LazyScapingConfig::houseSpiritTree),
	XERICS_TALISMAN("xerics_talisman", LazyScapingConfig::houseXericsTalisman),
	DIGSITE_PENDANT("digsite_pendant", LazyScapingConfig::houseDigsitePendant),
	MOUNTED_GLORY("mounted_glory", LazyScapingConfig::houseMountedGlory),
	MYTHICAL_CAPE("mythical_cape", LazyScapingConfig::houseMythicalCape),
	RING_OF_DUELING("jewellery_dueling", LazyScapingConfig::houseRingOfDueling),
	GAMES_NECKLACE("jewellery_games", LazyScapingConfig::houseGamesNecklace),
	COMBAT_BRACELET("jewellery_combat", LazyScapingConfig::houseCombatBracelet),
	SKILLS_NECKLACE("jewellery_skills", LazyScapingConfig::houseSkillsNecklace),
	RING_OF_WEALTH("jewellery_wealth", LazyScapingConfig::houseRingOfWealth),
	AMULET_OF_GLORY("jewellery_glory", LazyScapingConfig::houseAmuletOfGlory),
	ANNAKARL_PORTAL("annakarl_portal", LazyScapingConfig::houseAnnakarlPortal),
	APE_ATOLL_DUNGEON_PORTAL("ape_atoll_dungeon_portal", LazyScapingConfig::houseApeAtollDungeonPortal),
	ARCEUUS_LIBRARY_PORTAL("arceuus_library_portal", LazyScapingConfig::houseArceuusLibraryPortal),
	BARBARIAN_OUTPOST_PORTAL("barbarian_outpost_portal", LazyScapingConfig::houseBarbarianOutpostPortal),
	BARROWS_PORTAL("barrows_portal", LazyScapingConfig::houseBarrowsPortal),
	BATTLEFRONT_PORTAL("battlefront_portal", LazyScapingConfig::houseBattlefrontPortal),
	CAMELOT_PORTAL("camelot_portal", LazyScapingConfig::houseCamelotPortal),
	CARRALLANGER_PORTAL("carrallanger_portal", LazyScapingConfig::houseCarrallangerPortal),
	CATHERBY_PORTAL("catherby_portal", LazyScapingConfig::houseCatherbyPortal),
	CIVITAS_PORTAL("civitas_portal", LazyScapingConfig::houseCivitasPortal),
	DAREEYAK_PORTAL("dareeyak_portal", LazyScapingConfig::houseDareeyakPortal),
	DRAYNOR_MANOR_PORTAL("draynor_manor_portal", LazyScapingConfig::houseDraynorManorPortal),
	EAST_ARDOUGNE_PORTAL("ardougne_portal", LazyScapingConfig::houseEastArdougnePortal),
	FALADOR_PORTAL("falador_portal", LazyScapingConfig::houseFaladorPortal),
	FENKENSTRAIN_PORTAL("fenkenstrain_portal", LazyScapingConfig::houseFenkenstrainPortal),
	FISHING_GUILD_PORTAL("fishing_guild_portal", LazyScapingConfig::houseFishingGuildPortal),
	GHORROCK_PORTAL("ghorrock_portal", LazyScapingConfig::houseGhorrockPortal),
	HARMONY_ISLAND_PORTAL("harmony_island_portal", LazyScapingConfig::houseHarmonyIslandPortal),
	ICE_PLATEAU_PORTAL("ice_plateau_portal", LazyScapingConfig::houseIcePlateauPortal),
	KHARYRLL_PORTAL("kharyrll_portal", LazyScapingConfig::houseKharyrllPortal),
	KOUREND_CASTLE_PORTAL("kourend_castle_portal", LazyScapingConfig::houseKourendCastlePortal),
	LASSAR_PORTAL("lassar_portal", LazyScapingConfig::houseLassarPortal),
	LUMBRIDGE_PORTAL("lumbridge_portal", LazyScapingConfig::houseLumbridgePortal),
	LUNAR_ISLE_PORTAL("lunar_isle_portal", LazyScapingConfig::houseLunarIslePortal),
	MARIM_PORTAL("marim_portal", LazyScapingConfig::houseMarimPortal),
	MIND_ALTAR_PORTAL("mind_altar_portal", LazyScapingConfig::houseMindAltarPortal),
	OURANIA_CAVE_PORTAL("ourania_cave_portal", LazyScapingConfig::houseOuraniaCavePortal),
	PADDEWWA_PORTAL("paddewwa_portal", LazyScapingConfig::housePaddewwaPortal),
	PORT_KHAZARD_PORTAL("port_khazard_portal", LazyScapingConfig::housePortKhazardPortal),
	SALVE_GRAVEYARD_PORTAL("salve_graveyard_portal", LazyScapingConfig::houseSalveGraveyardPortal),
	SENNTISTEN_PORTAL("senntisten_portal", LazyScapingConfig::houseSenntistenPortal),
	FORGOTTEN_CEMETERY_PORTAL("forgotten_cemetery_portal", LazyScapingConfig::houseForgottenCemeteryPortal),
	TROLL_STRONGHOLD_PORTAL("troll_stronghold_portal", LazyScapingConfig::houseTrollStrongholdPortal),
	TROLLHEIM_PORTAL("trollheim_portal", LazyScapingConfig::houseTrollheimPortal),
	VARROCK_PORTAL("varrock_portal", LazyScapingConfig::houseVarrockPortal),
	WATERBIRTH_PORTAL("waterbirth_portal", LazyScapingConfig::houseWaterbirthPortal),
	WATCHTOWER_PORTAL("watchtower_portal", LazyScapingConfig::houseWatchtowerPortal),
	WEISS_PORTAL("weiss_portal", LazyScapingConfig::houseWeissPortal),
	WEST_ARDOUGNE_PORTAL("west_ardougne_portal", LazyScapingConfig::houseWestArdougnePortal);

	/** The name lazyscaping.app's API uses. */
	@Getter
	private final String apiName;
	private final Predicate<LazyScapingConfig> ticked;

	/** What the settings say the player's house has. */
	static Set<HouseTeleport> owned(LazyScapingConfig config)
	{
		Set<HouseTeleport> owned = EnumSet.noneOf(HouseTeleport.class);
		for (HouseTeleport teleport : values())
		{
			if (teleport.ticked.test(config))
			{
				owned.add(teleport);
			}
		}
		return owned;
	}
}
