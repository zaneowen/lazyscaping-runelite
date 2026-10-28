package com.lazyscaping.ui;

import java.text.NumberFormat;
import java.util.Locale;

final class Format
{
	private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance(Locale.ROOT);

	private Format()
	{
	}

	static String number(long value)
	{
		return NUMBER.format(value);
	}

	static String gp(long gp)
	{
		if (gp >= 1_000_000_000)
		{
			return String.format(Locale.ROOT, "%.2fB gp", gp / 1e9);
		}
		if (gp >= 1_000_000)
		{
			return String.format(Locale.ROOT, "%.2fM gp", gp / 1e6);
		}
		if (gp >= 10_000)
		{
			return String.format(Locale.ROOT, "%.1fk gp", gp / 1e3);
		}
		return number(gp) + " gp";
	}

	/** "costs 1.20M gp", "makes 300.0k gp", or empty when free. */
	static String cost(long gp)
	{
		if (gp > 0)
		{
			return "costs " + gp(gp);
		}
		return gp < 0 ? "makes " + gp(-gp) : "";
	}

	static String hours(double hours)
	{
		long minutes = Math.round(hours * 60);
		if (minutes < 60)
		{
			return Math.max(1, minutes) + "m";
		}
		return String.format(Locale.ROOT, "%dh %02dm", minutes / 60, minutes % 60);
	}

	static String gpPerXp(double gpPerXp)
	{
		if (Math.abs(gpPerXp) < 0.05)
		{
			return "Free";
		}
		String amount = String.format(Locale.ROOT, "%.1f gp/xp", Math.abs(gpPerXp));
		return gpPerXp > 0 ? "Costs " + amount : "Makes " + amount;
	}

	static String effort(int afk)
	{
		switch (afk)
		{
			case 3:
				return "Very AFK";
			case 2:
				return "Some clicking";
			default:
				return "Click-intensive";
		}
	}
}
