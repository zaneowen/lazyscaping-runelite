package com.lazyscaping;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/** Choices and remembered data saved per account (RuneScape profile), in the plugin's config group. */
@Singleton
class ProfileSettings
{
	static final String BANK_ITEMS = "bankItems";
	static final String BANK_COINS = "bankCoins";
	static final String GOAL_SKILL = "goalSkill";
	static final String GOAL_LEVEL = "goalLevel";
	static final String GOAL_FROM_XP = "goalFromXp";
	static final String GOAL_METHOD = "goalMethod";
	static final String GEAR_BOSS = "gearBoss";
	static final String TRAINING_BUDGET = "trainingBudget";
	static final String GEAR_BUDGET = "gearBudget";

	@Inject
	private ConfigManager configManager;

	String get(String key)
	{
		return configManager.getRSProfileConfiguration(LazyScapingConfig.GROUP, key);
	}

	long getLong(String key, long fallback)
	{
		String value = get(key);
		try
		{
			return value == null ? fallback : Long.parseLong(value);
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}

	/** Saves a value, or forgets it when it's null. Unchanged values aren't written again. */
	void set(String key, Object value)
	{
		if (value == null)
		{
			configManager.unsetRSProfileConfiguration(LazyScapingConfig.GROUP, key);
		}
		else if (!String.valueOf(value).equals(get(key)))
		{
			configManager.setRSProfileConfiguration(LazyScapingConfig.GROUP, key, value);
		}
	}
}
