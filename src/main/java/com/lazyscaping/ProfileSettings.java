package com.lazyscaping;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/** Choices and remembered data stored per RuneScape profile. */
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

	private final ConfigManager configManager;

	@Inject
	ProfileSettings(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	String get(String key)
	{
		return configManager.getRSProfileConfiguration(LazyScapingConfig.GROUP, key);
	}

	long getLong(String key, long defaultValue)
	{
		String value = get(key);
		if (value == null)
		{
			return defaultValue;
		}

		try
		{
			return Long.parseLong(value);
		}
		catch (NumberFormatException e)
		{
			return defaultValue;
		}
	}

	/** Stores a value, or removes it when null. */
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
