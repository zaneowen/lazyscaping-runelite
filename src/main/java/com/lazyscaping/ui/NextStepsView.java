package com.lazyscaping.ui;

import com.lazyscaping.api.NextSteps;
import java.awt.Color;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.api.Skill;

/**
 * What the player could do next across their account: bosses they're ready
 * for, levels that get them to bosses, gear to earn and quests that open
 * faster training. A boss step opens that boss, and a level step sets it as
 * the training goal.
 */
class NextStepsView
{
	private static final Map<String, String> KIND_LABELS = Map.of("boss", "Boss", "level", "Level", "gear", "Earn", "quest", "Quest");
	private static final Map<String, Color> KIND_COLOURS = Map.of(
		"boss", Html.RED,
		"level", Html.GREEN,
		"gear", Html.GOLD,
		"quest", new Color(0x7fb8e6));

	/** The most useful steps, so the card doesn't push the training goal too far down. */
	private static final int MAX_SHOWN = 4;

	private final JPanel card = Components.card("What to do next");
	private final JPanel list = Components.column();
	private final Consumer<String> openBoss;
	private final BiConsumer<Skill, Integer> setGoal;

	NextStepsView(Consumer<String> openBoss, BiConsumer<Skill, Integer> setGoal)
	{
		this.openBoss = openBoss;
		this.setGoal = setGoal;
		card.add(Components.intro("From your levels, the gear you own and your quests."));
		card.add(list);
		card.setVisible(false);
	}

	JComponent getComponent()
	{
		return card;
	}

	/** Shows the steps, or hides the card when there are none. */
	void show(NextSteps steps)
	{
		list.removeAll();
		if (steps != null)
		{
			steps.getSteps().stream().limit(MAX_SHOWN).forEach(step -> list.add(stepEntry(step)));
		}
		card.setVisible(list.getComponentCount() > 0);
		Components.refresh(card);
	}

	private JComponent stepEntry(NextSteps.Step step)
	{
		Color colour = KIND_COLOURS.getOrDefault(step.getKind(), Html.MUTED);
		String kind = KIND_LABELS.getOrDefault(step.getKind(), "");
		JLabel title = Components.label(Html.color(colour, Html.escape(kind)) + " " + Html.white(Html.escape(step.getTitle())), Components.ENTRY_WIDTH);
		Runnable action = action(step);
		if (action != null)
		{
			Components.onClick(title, action);
			title.setToolTipText("level".equals(step.getKind()) ? "Set it as your training goal" : "Open it in the Bossing tab");
		}

		JPanel entry = Components.entry();
		entry.add(title);
		entry.add(Components.spaced(Components.label(Html.muted(Html.escape(step.getDetail())), Components.ENTRY_WIDTH), 2));
		return entry;
	}

	/** What clicking a step does, or null when there's nothing to open. */
	private Runnable action(NextSteps.Step step)
	{
		if ("level".equals(step.getKind()) && step.getSkill() != null && step.getLevel() != null)
		{
			Skill skill = skill(step.getSkill());
			return skill == null ? null : () -> setGoal.accept(skill, step.getLevel());
		}
		if (step.getBossId() != null && "boss".equals(step.getKind()))
		{
			return () -> openBoss.accept(step.getBossId());
		}
		return null;
	}

	private static Skill skill(String name)
	{
		try
		{
			return Skill.valueOf(name.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}
}
