package com.lazyscaping.ui;

import com.lazyscaping.api.ItemRef;
import javax.swing.JLabel;
import net.runelite.api.Skill;

/** Loads pictures into labels asynchronously. */
public interface IconLoader
{
	/**
	 * Shows an item's picture. A quantity above one is drawn on it like an
	 * inventory stack; faded pictures are used for items the player doesn't have yet.
	 */
	void loadItem(ItemRef item, int quantity, boolean faded, JLabel target);

	void loadSkill(Skill skill, JLabel target);
}
