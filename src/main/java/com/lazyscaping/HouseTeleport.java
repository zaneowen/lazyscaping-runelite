package com.lazyscaping;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Things in a player-owned house that can replace a teleport in a gear setup's inventory, each ticked in the settings. */
@RequiredArgsConstructor
enum HouseTeleport
{
	FAIRY_RING("fairy_ring", LazyScapingConfig::houseFairyRing),
	SPIRIT_TREE("spirit_tree", LazyScapingConfig::houseSpiritTree),
	XERICS_TALISMAN("xerics_talisman", LazyScapingConfig::houseXericsTalisman),
	DIGSITE_PENDANT("digsite_pendant", LazyScapingConfig::houseDigsitePendant),
	RING_OF_DUELING("jewellery_dueling", LazyScapingConfig::houseRingOfDueling),
	GAMES_NECKLACE("jewellery_games", LazyScapingConfig::houseGamesNecklace),
	VARROCK("varrock_portal", LazyScapingConfig::houseVarrockPortal),
	FALADOR("falador_portal", LazyScapingConfig::houseFaladorPortal),
	BARROWS("barrows_portal", LazyScapingConfig::houseBarrowsPortal),
	WATERBIRTH("waterbirth_portal", LazyScapingConfig::houseWaterbirthPortal),
	TROLLHEIM("trollheim_portal", LazyScapingConfig::houseTrollheimPortal),
	ANNAKARL("annakarl_portal", LazyScapingConfig::houseAnnakarlPortal),
	CARRALLANGER("carrallanger_portal", LazyScapingConfig::houseCarrallangerPortal),
	GHORROCK("ghorrock_portal", LazyScapingConfig::houseGhorrockPortal),
	LASSAR("lassar_portal", LazyScapingConfig::houseLassarPortal);

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
