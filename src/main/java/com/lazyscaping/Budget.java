package com.lazyscaping;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Gp budgets typed or picked in the panel, read like the site's parseGp (src/lib/format.ts). */
final class Budget
{
	static final String FREE = "Free";
	static final String UNLIMITED = "Unlimited";
	/** The coins in your bank (as last seen) and inventory. */
	static final String MY_COINS = "My coins";

	static final String[] TRAINING_PRESETS = {FREE, "1M", "10M", "50M", UNLIMITED, MY_COINS};
	static final String[] GEAR_PRESETS = {FREE, "1M", "10M", "50M", "250M", UNLIMITED, MY_COINS};

	private static final Pattern AMOUNT = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*([kmb]?)$");

	private Budget()
	{
	}

	static boolean isValid(String text)
	{
		return text != null && (isUnlimited(text) || isMyCoins(text) || parseGp(text) != null);
	}

	static boolean isUnlimited(String text)
	{
		return text.trim().equalsIgnoreCase(UNLIMITED);
	}

	static boolean isMyCoins(String text)
	{
		return text.trim().equalsIgnoreCase(MY_COINS);
	}

	/** Gp for an amount like "5m", "250k", "1.2b", "1,000" or "Free", or null if it isn't one. */
	static Long parseGp(String text)
	{
		String s = text.trim().toLowerCase(Locale.ROOT).replace(",", "");
		if (s.equals(FREE.toLowerCase(Locale.ROOT)))
		{
			return 0L;
		}
		Matcher m = AMOUNT.matcher(s);
		if (!m.matches())
		{
			return null;
		}
		double mult = m.group(2).equals("k") ? 1e3 : m.group(2).equals("m") ? 1e6 : m.group(2).equals("b") ? 1e9 : 1;
		return Math.round(Double.parseDouble(m.group(1)) * mult);
	}

	/** The budget in gp, or null for unlimited. `coins` is used for "My coins". */
	static Long resolve(String text, long coins)
	{
		if (text == null || isUnlimited(text))
		{
			return null;
		}
		if (isMyCoins(text))
		{
			return coins;
		}
		Long gp = parseGp(text);
		return gp == null ? null : gp;
	}
}
