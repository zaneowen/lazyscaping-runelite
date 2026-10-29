package com.lazyscaping.ui;

import com.lazyscaping.api.GearSetup;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.List;
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

	private StatsView()
	{
	}

	static void addTo(JPanel parent, GearSetup setup)
	{
		List<Integer> stats = setup.getStats();
		if (stats.size() < OTHER_START + OTHER.length)
		{
			return;
		}
		Set<Integer> key = Set.copyOf(setup.getKeyStats());

		parent.add(Components.subheading("Equipment stats"));
		parent.add(group("Attack", STYLES, stats, ATTACK, key));
		parent.add(group("Defence", STYLES, stats, DEFENCE, key));
		parent.add(group("Strength &amp; prayer", OTHER, stats, OTHER_START, key));
	}

	/** A titled row of bonuses, each with its name above its value. */
	private static JPanel group(String title, String[] names, List<Integer> stats, int start, Set<Integer> key)
	{
		JPanel grid = new JPanel(new GridLayout(2, names.length, 2, 0));
		grid.setOpaque(false);
		for (String name : names)
		{
			grid.add(cell(name, ColorScheme.MEDIUM_GRAY_COLOR));
		}
		for (int i = 0; i < names.length; i++)
		{
			int index = start + i;
			int value = stats.get(index);
			Color color = key.contains(index) ? Html.GOLD : value < 0 ? Html.RED : ColorScheme.LIGHT_GRAY_COLOR;
			grid.add(cell((value > 0 ? "+" : "") + value + (index == MAGIC_DAMAGE ? "%" : ""), color));
		}

		JPanel group = Components.column();
		group.add(Components.spaced(Components.label(Html.muted(title)), 6));
		group.add(Components.spaced(Components.fullWidth(grid), 1));
		return group;
	}

	private static JLabel cell(String text, Color color)
	{
		JLabel cell = new JLabel(text, SwingConstants.CENTER);
		cell.setFont(FontManager.getRunescapeSmallFont());
		cell.setForeground(color);
		return cell;
	}
}
