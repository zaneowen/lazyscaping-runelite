package com.lazyscaping;

import java.util.Locale;
import java.util.regex.Pattern;

final class ItemNames
{
	private static final Pattern DOSE = Pattern.compile("\\s*\\(\\d\\)$");

	private ItemNames()
	{
	}

	/** A name that's the same for every dose of a potion, so a 3-dose potion counts as a 4-dose one. */
	static String anyDose(String name)
	{
		return DOSE.matcher(name.trim().toLowerCase(Locale.ROOT)).replaceFirst("");
	}
}
