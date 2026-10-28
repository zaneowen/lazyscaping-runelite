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

	@ConfigSection(
		name = "Jewellery box",
		description = "The jewellery in your house's jewellery box",
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

	@ConfigSection(
		name = "House portals",
		description = "The portals in your house's portal chamber or portal nexus",
		position = 30,
		closedByDefault = true
	)
	String portalSection = "portals";

	@ConfigItem(
		keyName = "houseVarrockPortal",
		name = "Varrock",
		description = "Your house has a Varrock portal",
		section = portalSection,
		position = 31
	)
	default boolean houseVarrockPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseFaladorPortal",
		name = "Falador",
		description = "Your house has a Falador portal",
		section = portalSection,
		position = 32
	)
	default boolean houseFaladorPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseBarrowsPortal",
		name = "Barrows",
		description = "Your house has a Barrows portal",
		section = portalSection,
		position = 33
	)
	default boolean houseBarrowsPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseWaterbirthPortal",
		name = "Waterbirth Island",
		description = "Your house has a Waterbirth Island portal",
		section = portalSection,
		position = 34
	)
	default boolean houseWaterbirthPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseTrollheimPortal",
		name = "Trollheim",
		description = "Your house has a Trollheim portal",
		section = portalSection,
		position = 35
	)
	default boolean houseTrollheimPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseAnnakarlPortal",
		name = "Annakarl",
		description = "Your house has an Annakarl portal",
		section = portalSection,
		position = 36
	)
	default boolean houseAnnakarlPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseCarrallangerPortal",
		name = "Carrallanger",
		description = "Your house has a Carrallanger portal",
		section = portalSection,
		position = 37
	)
	default boolean houseCarrallangerPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseGhorrockPortal",
		name = "Ghorrock",
		description = "Your house has a Ghorrock portal",
		section = portalSection,
		position = 38
	)
	default boolean houseGhorrockPortal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseLassarPortal",
		name = "Lassar",
		description = "Your house has a Lassar portal",
		section = portalSection,
		position = 39
	)
	default boolean houseLassarPortal()
	{
		return false;
	}
}
