package com.lazyscaping;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(LazyScapingConfig.GROUP)
public interface LazyScapingConfig extends Config
{
	String GROUP = "lazyscaping";

	@Getter
	@RequiredArgsConstructor
	enum Priority
	{
		FASTEST("Fastest", "fastest"),
		CHEAPEST("Cheapest", "cheapest"),
		AFK("AFK", "afk");

		private final String label;
		/** The name lazyscaping.app's API uses. */
		private final String apiName;

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "liveRecommendations",
		name = "Live recommendations",
		description = "Fetch training plans and boss gear setups from lazyscaping.app for the panel. Sends your levels,<br>"
			+ "the gear you own and your quest unlocks. Nothing is stored.",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		position = 1
	)
	default boolean liveRecommendations()
	{
		return false;
	}

	@ConfigItem(
		keyName = "priority",
		name = "Plan priority",
		description = "What the training plan favours: the most xp per hour, the lowest cost, or AFK methods",
		position = 2
	)
	default Priority priority()
	{
		return Priority.FASTEST;
	}

	@ConfigSection(
		name = "Achievement diaries",
		description = "Diaries that change your gear setups. They're also detected in game.",
		position = 3
	)
	String diarySection = "diaries";

	@ConfigItem(
		keyName = "lumbridgeElite",
		name = "Elite Lumbridge Diary",
		description = "You've done the elite Lumbridge & Draynor Diary, so fairy rings don't need a dramen staff",
		section = diarySection,
		position = 4
	)
	default boolean lumbridgeElite()
	{
		return false;
	}

	@ConfigSection(
		name = "Your house",
		description = "What your house has. Gear setups bring a house teleport instead of the teleports these replace.",
		position = 10
	)
	String houseSection = "house";

	@ConfigItem(
		keyName = "houseFairyRing",
		name = "Fairy ring",
		description = "Your house has a fairy ring, used when one gets you to a boss",
		section = houseSection,
		position = 11
	)
	default boolean houseFairyRing()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseSpiritTree",
		name = "Spirit tree",
		description = "Your house has a spirit tree, used when one gets you to a boss",
		section = houseSection,
		position = 12
	)
	default boolean houseSpiritTree()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseXericsTalisman",
		name = "Mounted Xeric's talisman",
		description = "Your house has a mounted Xeric's talisman",
		section = houseSection,
		position = 13
	)
	default boolean houseXericsTalisman()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseDigsitePendant",
		name = "Mounted digsite pendant",
		description = "Your house has a mounted digsite pendant",
		section = houseSection,
		position = 14
	)
	default boolean houseDigsitePendant()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseMountedGlory",
		name = "Mounted amulet of glory",
		description = "Your house has a mounted amulet of glory",
		section = houseSection,
		position = 15
	)
	default boolean houseMountedGlory()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseMythicalCape",
		name = "Mounted mythical cape",
		description = "Your house has a mounted mythical cape",
		section = houseSection,
		position = 16
	)
	default boolean houseMythicalCape()
	{
		return false;
	}

	@ConfigSection(
		name = "Jewellery box",
		description = "The teleports in your house's jewellery box. A basic box has the first two, a fancy box the first four and an ornate box all six.",
		position = 20
	)
	String jewellerySection = "jewelleryBox";

	@ConfigItem(
		keyName = "houseRingOfDueling",
		name = "Ring of dueling",
		description = "Your jewellery box has ring of dueling teleports",
		section = jewellerySection,
		position = 21
	)
	default boolean houseRingOfDueling()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseGamesNecklace",
		name = "Games necklace",
		description = "Your jewellery box has games necklace teleports",
		section = jewellerySection,
		position = 22
	)
	default boolean houseGamesNecklace()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseCombatBracelet",
		name = "Combat bracelet",
		description = "Your jewellery box has combat bracelet teleports",
		section = jewellerySection,
		position = 23
	)
	default boolean houseCombatBracelet()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseSkillsNecklace",
		name = "Skills necklace",
		description = "Your jewellery box has skills necklace teleports",
		section = jewellerySection,
		position = 24
	)
	default boolean houseSkillsNecklace()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseRingOfWealth",
		name = "Ring of wealth",
		description = "Your jewellery box has ring of wealth teleports",
		section = jewellerySection,
		position = 25
	)
	default boolean houseRingOfWealth()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseAmuletOfGlory",
		name = "Amulet of glory",
		description = "Your jewellery box has amulet of glory teleports",
		section = jewellerySection,
		position = 26
	)
	default boolean houseAmuletOfGlory()
	{
		return false;
	}

	@ConfigSection(
		name = "House portals",
		description = "The teleports in your house's portal chamber or portal nexus",
		position = 40,
		closedByDefault = true
	)
	String portalSection = "portals";

	@ConfigItem(
		keyName = "houseAnnakarlPortal",
		name = "Annakarl",
		description = "Your house has an Annakarl portal",
		section = portalSection,
		position = 41
	)
	default boolean houseAnnakarlPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseApeAtollDungeonPortal",
		name = "Ape Atoll Dungeon",
		description = "Your house has an Ape Atoll Dungeon portal",
		section = portalSection,
		position = 42
	)
	default boolean houseApeAtollDungeonPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseArceuusLibraryPortal",
		name = "Arceuus Library",
		description = "Your house has an Arceuus Library portal",
		section = portalSection,
		position = 43
	)
	default boolean houseArceuusLibraryPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseBarbarianOutpostPortal",
		name = "Barbarian Outpost",
		description = "Your house has a Barbarian Outpost portal",
		section = portalSection,
		position = 44
	)
	default boolean houseBarbarianOutpostPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseBarrowsPortal",
		name = "Barrows",
		description = "Your house has a Barrows portal",
		section = portalSection,
		position = 45
	)
	default boolean houseBarrowsPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseBattlefrontPortal",
		name = "Battlefront",
		description = "Your house has a Battlefront portal",
		section = portalSection,
		position = 46
	)
	default boolean houseBattlefrontPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseCamelotPortal",
		name = "Camelot or Seers' Village",
		description = "Your house has a Camelot or Seers' Village portal",
		section = portalSection,
		position = 47
	)
	default boolean houseCamelotPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseCarrallangerPortal",
		name = "Carrallanger",
		description = "Your house has a Carrallanger portal",
		section = portalSection,
		position = 48
	)
	default boolean houseCarrallangerPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseCatherbyPortal",
		name = "Catherby",
		description = "Your house has a Catherby portal",
		section = portalSection,
		position = 49
	)
	default boolean houseCatherbyPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseCivitasPortal",
		name = "Civitas illa Fortis",
		description = "Your house has a Civitas illa Fortis portal",
		section = portalSection,
		position = 50
	)
	default boolean houseCivitasPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseDareeyakPortal",
		name = "Dareeyak",
		description = "Your house has a Dareeyak portal",
		section = portalSection,
		position = 51
	)
	default boolean houseDareeyakPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseDraynorManorPortal",
		name = "Draynor Manor",
		description = "Your house has a Draynor Manor portal",
		section = portalSection,
		position = 52
	)
	default boolean houseDraynorManorPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseEastArdougnePortal",
		name = "East Ardougne",
		description = "Your house has an East Ardougne portal",
		section = portalSection,
		position = 53
	)
	default boolean houseEastArdougnePortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseFaladorPortal",
		name = "Falador",
		description = "Your house has a Falador portal",
		section = portalSection,
		position = 54
	)
	default boolean houseFaladorPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseFenkenstrainPortal",
		name = "Fenkenstrain's Castle",
		description = "Your house has a Fenkenstrain's Castle portal",
		section = portalSection,
		position = 55
	)
	default boolean houseFenkenstrainPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseFishingGuildPortal",
		name = "Fishing Guild",
		description = "Your house has a Fishing Guild portal",
		section = portalSection,
		position = 56
	)
	default boolean houseFishingGuildPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseGhorrockPortal",
		name = "Ghorrock",
		description = "Your house has a Ghorrock portal",
		section = portalSection,
		position = 57
	)
	default boolean houseGhorrockPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseHarmonyIslandPortal",
		name = "Harmony Island",
		description = "Your house has a Harmony Island portal",
		section = portalSection,
		position = 58
	)
	default boolean houseHarmonyIslandPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseIcePlateauPortal",
		name = "Ice Plateau",
		description = "Your house has an Ice Plateau portal",
		section = portalSection,
		position = 59
	)
	default boolean houseIcePlateauPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseKharyrllPortal",
		name = "Kharyrll",
		description = "Your house has a Kharyrll portal",
		section = portalSection,
		position = 60
	)
	default boolean houseKharyrllPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseKourendCastlePortal",
		name = "Kourend Castle",
		description = "Your house has a Kourend Castle portal",
		section = portalSection,
		position = 61
	)
	default boolean houseKourendCastlePortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseLassarPortal",
		name = "Lassar",
		description = "Your house has a Lassar portal",
		section = portalSection,
		position = 62
	)
	default boolean houseLassarPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseLumbridgePortal",
		name = "Lumbridge",
		description = "Your house has a Lumbridge portal",
		section = portalSection,
		position = 63
	)
	default boolean houseLumbridgePortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseLunarIslePortal",
		name = "Lunar Isle",
		description = "Your house has a Lunar Isle portal",
		section = portalSection,
		position = 64
	)
	default boolean houseLunarIslePortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseMarimPortal",
		name = "Marim",
		description = "Your house has a Marim portal",
		section = portalSection,
		position = 65
	)
	default boolean houseMarimPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseMindAltarPortal",
		name = "Mind Altar",
		description = "Your house has a Mind Altar portal",
		section = portalSection,
		position = 66
	)
	default boolean houseMindAltarPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseOuraniaCavePortal",
		name = "Ourania Cave",
		description = "Your house has an Ourania Cave portal",
		section = portalSection,
		position = 67
	)
	default boolean houseOuraniaCavePortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "housePaddewwaPortal",
		name = "Paddewwa",
		description = "Your house has a Paddewwa portal",
		section = portalSection,
		position = 68
	)
	default boolean housePaddewwaPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "housePortKhazardPortal",
		name = "Port Khazard",
		description = "Your house has a Port Khazard portal",
		section = portalSection,
		position = 69
	)
	default boolean housePortKhazardPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseSalveGraveyardPortal",
		name = "Salve Graveyard",
		description = "Your house has a Salve Graveyard portal",
		section = portalSection,
		position = 70
	)
	default boolean houseSalveGraveyardPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseSenntistenPortal",
		name = "Senntisten",
		description = "Your house has a Senntisten portal",
		section = portalSection,
		position = 71
	)
	default boolean houseSenntistenPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseForgottenCemeteryPortal",
		name = "The Forgotten Cemetery",
		description = "Your house has a The Forgotten Cemetery portal",
		section = portalSection,
		position = 72
	)
	default boolean houseForgottenCemeteryPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseTrollStrongholdPortal",
		name = "Troll Stronghold",
		description = "Your house has a Troll Stronghold portal",
		section = portalSection,
		position = 73
	)
	default boolean houseTrollStrongholdPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseTrollheimPortal",
		name = "Trollheim",
		description = "Your house has a Trollheim portal",
		section = portalSection,
		position = 74
	)
	default boolean houseTrollheimPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseVarrockPortal",
		name = "Varrock or Grand Exchange",
		description = "Your house has a Varrock or Grand Exchange portal",
		section = portalSection,
		position = 75
	)
	default boolean houseVarrockPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseWaterbirthPortal",
		name = "Waterbirth Island",
		description = "Your house has a Waterbirth Island portal",
		section = portalSection,
		position = 76
	)
	default boolean houseWaterbirthPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseWatchtowerPortal",
		name = "Watchtower or Yanille",
		description = "Your house has a Watchtower or Yanille portal",
		section = portalSection,
		position = 77
	)
	default boolean houseWatchtowerPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseWeissPortal",
		name = "Weiss",
		description = "Your house has a Weiss portal",
		section = portalSection,
		position = 78
	)
	default boolean houseWeissPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseWestArdougnePortal",
		name = "West Ardougne",
		description = "Your house has a West Ardougne portal",
		section = portalSection,
		position = 79
	)
	default boolean houseWestArdougnePortal()
	{
		return false;
	}
}
