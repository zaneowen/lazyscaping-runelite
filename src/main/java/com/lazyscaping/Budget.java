package com.lazyscaping;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A gp budget as picked or typed, read the same way as the site's {@code parseGp}. */
public final class Budget
{
	public static final String FREE = "Free";
	public static final String UNLIMITED = "Unlimited";
	/** The coins in the player's bank and inventory. */
	public static final String MY_COINS = "My coins";

	public static final String[] TRAINING_PRESETS = {FREE, "1M", "10M", "50M", UNLIMITED, MY_COINS};
	public static final String[] GEAR_PRESETS = {FREE, "1M", "10M", "50M", "250M", UNLIMITED, MY_COINS};

	private static final Pattern AMOUNT = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([kmb]?)");

	private Budget()
	{
	}

	public static boolean isValid(String text)
	{
		return text != null && (isUnlimited(text) || isMyCoins(text) || parseGp(text) != null);
	}

	public static boolean isUnlimited(String text)
	{
		return text.trim().equalsIgnoreCase(UNLIMITED);
	}

	public static boolean isMyCoins(String text)
	{
		return text.trim().equalsIgnoreCase(MY_COINS);
	}

	/** Parses an amount like "5m", "250k", "1.2b", "1,000" or "Free", or returns null. */
	public static Long parseGp(String text)
	{
		String s = text.trim().toLowerCase(Locale.ROOT).replace(",", "");
		if (s.equalsIgnoreCase(FREE))
		{
			return 0L;
		}

		Matcher m = AMOUNT.matcher(s);
		if (!m.matches())
		{
			return null;
		}

		double multiplier;
		switch (m.group(2))
		{
			case "k":
				multiplier = 1e3;
				break;
			case "m":
				multiplier = 1e6;
				break;
			case "b":
				multiplier = 1e9;
				break;
			default:
				multiplier = 1;
		}
		return Math.round(Double.parseDouble(m.group(1)) * multiplier);
	}

	/** The budget in gp, or null for unlimited. */
	public static Long resolve(String text, long coins)
	{
		if (text == null || isUnlimited(text))
		{
			return null;
		}
		return isMyCoins(text) ? Long.valueOf(coins) : parseGp(text);
	}
}
