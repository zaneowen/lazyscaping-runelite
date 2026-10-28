package com.lazyscaping;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(LazyScapingConfig.GROUP)
public interface LazyScapingConfig extends Config
{
	String GROUP = "lazyscaping";

	enum Priority
	{
		FASTEST("fastest", "Fastest"),
		CHEAPEST("cheapest", "Cheapest"),
		AFK("afk", "AFK");

		final String apiValue;
		private final String label;

		Priority(String apiValue, String label)
		{
			this.apiValue = apiValue;
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "liveRecommendations",
		name = "Live recommendations",
		description = "Fetch training plans and boss suggestions from lazyscaping.app for the panel. Sends your levels,<br>"
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
}
