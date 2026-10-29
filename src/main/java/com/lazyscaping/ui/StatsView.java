package com.lazyscaping.ui;

import com.lazyscaping.api.GearSetup;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The worn gear's total bonuses, laid out like the in-game equipment stats
 * screen: attack and defence for each style, then strength and prayer. The
 * bonuses that matter for the setup's style are gold.
 */
final class StatsView
{
	private static final String[] STYLES = {"Stab", "Slash", "Crush", "Magic", "Range"};
	private static final String[] OTHER = {"Melee", "Ranged", "Magic", "Prayer"};
	/** Where each group starts in the site's list of bonuses. */
	private static final int ATTACK = 0;
	private static final int DEFENCE = 5;
	private static final int OTHER_START = 10;
	private static final int MAGIC_DAMAGE = 12;
	private static final int PRAYER = 13;
	/** Short names of the bonuses for a change summary, in the site's order. Defences show as one average. */
	private static final String[] SHORT_NAMES = {"Stab", "Slash", "Crush", "Magic", "Range", null, null, null, null, null, "Str", "R.str", "M.dmg", "Prayer"};
	/** What each bonus does, in the site's order. */
	private static final String[] MEANINGS = {
		"Stab attack: how often stab attacks hit",
		"Slash attack: how often slash attacks hit",
		"Crush attack: how often crush attacks hit",
		"Magic attack: how often spells hit",
		"Ranged attack: how often ranged attacks hit",
		"Stab defence: how often stab attacks miss you",
		"Slash defence: how often slash attacks miss you",
		"Crush defence: how often crush attacks miss you",
		"Magic defence: how often spells miss you",
		"Ranged defence: how often ranged attacks miss you",
		"Melee strength: raises your melee max hit",
		"Ranged strength: raises your ranged max hit",
		"Magic damage: raises your spell max hit by this much",
		"Prayer: makes your prayer points last longer",
	};

	private StatsView()
	{
	}

	static void addTo(JPanel parent, GearSetup setup)
	{
		List<Double> stats = setup.getStats();
		if (stats.size() < OTHER_START + OTHER.length)
		{
			return;
		}
		Set<Integer> key = Set.copyOf(setup.getKeyStats());

		parent.add(group("Attack", STYLES, stats, ATTACK, key));
		parent.add(group("Defence", STYLES, stats, DEFENCE, key));
		parent.add(group("Strength &amp; prayer", OTHER, stats, OTHER_START, key));
	}

	/** A titled row of bonuses, each with its name above its value. */
	private static JPanel group(String title, String[] names, List<Double> stats, int start, Set<Integer> key)
	{
		JPanel grid = new JPanel(new GridLayout(2, names.length, 2, 0));
		grid.setOpaque(false);
		for (int i = 0; i < names.length; i++)
		{
			grid.add(cell(names[i], ColorScheme.MEDIUM_GRAY_COLOR, MEANINGS[start + i]));
		}
		for (int i = 0; i < names.length; i++)
		{
			int index = start + i;
			double value = stats.get(index);
			Color color = key.contains(index) ? Html.GOLD : value < 0 ? Html.RED : ColorScheme.LIGHT_GRAY_COLOR;
			grid.add(cell(signed(value) + (index == MAGIC_DAMAGE ? "%" : ""), color, MEANINGS[index]));
		}

		JPanel group = Components.column();
		group.add(Components.spaced(Components.label(Html.muted(title)), 6));
		group.add(Components.spaced(Components.fullWidth(grid), 1));
		return group;
	}

	/**
	 * How a piece of gear changes the bonuses that matter for the style, like
	 * "+12 Range · +4 Def · +3 R.str": the style's {@code key} bonuses, defence
	 * and prayer, with gains in green and losses in red. The five defences show
	 * as their average change, "Def". Empty when none of them change.
	 */
	static String changes(List<Double> change, Collection<Integer> key)
	{
		if (change.size() < OTHER_START + OTHER.length)
		{
			return "";
		}
		List<String> parts = new ArrayList<>();
		for (int i = 0; i < change.size(); i++)
		{
			if (i == DEFENCE)
			{
				double total = 0;
				for (int d = DEFENCE; d < OTHER_START; d++)
				{
					total += change.get(d);
				}
				addChange(parts, Math.round(total / (OTHER_START - DEFENCE)), "", "Def");
			}
			else if (SHORT_NAMES[i] != null && (key.contains(i) || i == PRAYER))
			{
				addChange(parts, change.get(i), i == MAGIC_DAMAGE ? "%" : "", SHORT_NAMES[i]);
			}
		}
		return String.join(" · ", parts);
	}

	private static void addChange(List<String> parts, double value, String unit, String name)
	{
		if (value != 0)
		{
			parts.add(Html.color(value > 0 ? Html.GREEN : Html.RED, Html.keep(signed(value) + unit + " " + name)));
		}
	}

	/** A bonus with its sign, and a decimal only when it has one, like "+8.2" or "-35". */
	static String signed(double value)
	{
		String number = value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
		return value > 0 ? "+" + number : number;
	}

	private static JLabel cell(String text, Color color, String meaning)
	{
		JLabel cell = new JLabel(text, SwingConstants.CENTER);
		cell.setFont(FontManager.getRunescapeSmallFont());
		cell.setForeground(color);
		cell.setToolTipText(meaning);
		return cell;
	}
}
