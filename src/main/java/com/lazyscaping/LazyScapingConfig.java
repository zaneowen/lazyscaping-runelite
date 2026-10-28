package com.lazyscaping;

import java.util.EnumSet;
import java.util.Set;
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
		name = "Your account",
		description = "Things the plugin can't always see, used to plan your gear setups",
		position = 3
	)
	String accountSection = "account";

	@ConfigItem(
		keyName = "lumbridgeElite",
		name = "Elite Lumbridge Diary",
		description = "You've done the elite Lumbridge & Draynor Diary, so fairy rings don't need a dramen staff.<br>"
			+ "It's also detected in game.",
		section = accountSection,
		position = 4
	)
	default boolean lumbridgeElite()
	{
		return false;
	}

	@ConfigItem(
		keyName = "houseTeleports",
		name = "House teleports",
		description = "What your house has. Gear setups use a house teleport instead of the teleports these replace,<br>"
			+ "and a fairy ring or spirit tree replaces a boss's teleport when it can get you there.",
		section = accountSection,
		position = 5
	)
	default Set<HouseTeleport> houseTeleports()
	{
		return EnumSet.noneOf(HouseTeleport.class);
	}
}
