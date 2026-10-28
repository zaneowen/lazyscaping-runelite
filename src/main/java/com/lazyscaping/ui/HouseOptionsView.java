package com.lazyscaping.ui;

import com.lazyscaping.HouseTeleport;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Tick boxes for what the player's house has and whether their elite Lumbridge
 * Diary is done, which change the teleports a gear setup packs. Collapsed behind
 * a link that says how many are ticked.
 */
class HouseOptionsView
{
	private final Map<HouseTeleport, JCheckBox> houseBoxes = new EnumMap<>(HouseTeleport.class);
	private final JCheckBox diaryBox = checkBox("Elite Lumbridge Diary");
	private final JLabel header = Components.label("");
	private final JComponent component;
	private boolean updating;

	HouseOptionsView(PanelActions actions)
	{
		diaryBox.addActionListener(e ->
		{
			if (!updating)
			{
				actions.lumbridgeEliteChanged(diaryBox.isSelected());
			}
		});

		JPanel details = Components.column();
		details.setBorder(new EmptyBorder(2, 4, 0, 0));
		details.add(diaryBox);
		details.add(Components.spaced(Components.label(Html.white("Your house has:")), 4));
		for (HouseTeleport teleport : HouseTeleport.values())
		{
			JCheckBox box = checkBox(teleport.getLabel());
			box.addActionListener(e ->
			{
				if (!updating)
				{
					actions.houseTeleportChanged(teleport, box.isSelected());
				}
			});
			houseBoxes.put(teleport, box);
			details.add(box);
		}
		details.add(Components.spaced(Components.label(Html.muted("Setups use a house tablet instead of the teleports these replace.")), 4));

		component = Components.expandable(Components.spaced(header, 6), details);
		set(Set.of(), false, false);
	}

	JComponent getComponent()
	{
		return component;
	}

	/** Shows what's ticked. {@code diaryDetected} ticks and locks the diary box when the game says it's done. */
	void set(Set<HouseTeleport> house, boolean diaryTicked, boolean diaryDetected)
	{
		updating = true;
		houseBoxes.forEach((teleport, box) -> box.setSelected(house.contains(teleport)));
		diaryBox.setSelected(diaryTicked || diaryDetected);
		diaryBox.setEnabled(!diaryDetected);
		diaryBox.setToolTipText(diaryDetected ? "Done, as detected in game. Fairy rings need no dramen staff." : "Tick once it's done: fairy rings then need no dramen staff.");
		updating = false;

		int ticked = house.size() + (diaryTicked || diaryDetected ? 1 : 0);
		header.setText(Html.color(Html.GOLD, Html.underline("Your house and diary")) + (ticked > 0 ? " " + Html.muted("(" + ticked + " set)") : ""));
	}

	private static JCheckBox checkBox(String text)
	{
		JCheckBox box = new JCheckBox(text);
		box.setOpaque(false);
		box.setFont(FontManager.getRunescapeSmallFont());
		box.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return box;
	}
}
