package com.lazyscaping.ui;

import static org.junit.Assert.assertEquals;
import java.util.List;
import org.junit.Test;

public class PanelLayoutTest
{
	@Test
	public void fitsPhrasesOnOneRowWhenTheyFit()
	{
		// one unit per character, so the separator " · " is 3 wide
		List<List<String>> rows = Components.fitRows(List.of("26/28 slots", "supplies 185.6k gp a trip"), String::length, 40);
		assertEquals(List.of(List.of("26/28 slots", "supplies 185.6k gp a trip")), rows);
	}

	@Test
	public void breaksRowsBetweenPhrasesOnly()
	{
		List<List<String>> rows = Components.fitRows(List.of("1 spec weapon", "4 teleports and tools", "21 potions"), String::length, 35);
		assertEquals(List.of(List.of("1 spec weapon"), List.of("4 teleports and tools", "21 potions")), rows);
	}

	@Test
	public void givesAPhraseTooWideForAnyRowItsOwnRow()
	{
		List<List<String>> rows = Components.fitRows(List.of("short", "a phrase far too wide to fit", "end"), String::length, 10);
		assertEquals(List.of(List.of("short"), List.of("a phrase far too wide to fit"), List.of("end")), rows);
	}

	@Test
	public void fitsNothingIntoNoRows()
	{
		assertEquals(List.of(), Components.fitRows(List.of(), String::length, 10));
	}

	@Test
	public void signsStatsAndOnlyShowsDecimalsWhenThereAreSome()
	{
		assertEquals("+146", StatsView.signed(146));
		assertEquals("-35", StatsView.signed(-35));
		assertEquals("0", StatsView.signed(0));
		assertEquals("+8.2", StatsView.signed(8.2));
	}

	@Test
	public void summarisesTheStatChangesThatMatterForTheStyle()
	{
		List<Double> change = List.of(0.0, 0.0, 0.0, -2.0, 12.0, 5.0, 5.0, 5.0, 5.0, 0.0, 0.0, 3.0, 0.0, 1.0);
		// ranged: range attack and ranged strength matter, magic doesn't
		String text = StatsView.changes(change, List.of(4, 11)).replaceAll("<[^>]+>", "").replace("&nbsp;", " ");
		assertEquals("+12 Range · +4 Def · +3 R.str · +1 Prayer", text);
		assertEquals("", StatsView.changes(List.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0), List.of(4, 11)));
	}

	@Test
	public void linksItemsToTheirWikiPage()
	{
		assertEquals("https://oldschool.runescape.wiki/w/Dizana%27s_quiver", Components.wikiUrl("Dizana's quiver"));
		assertEquals("https://oldschool.runescape.wiki/w/Ruby_dragon_bolts_%28e%29", Components.wikiUrl("Ruby dragon bolts (e)"));
	}

	@Test
	public void keepsPartsTogetherAndEscapesThem()
	{
		assertEquals("a&nbsp;b · c", Html.parts("a b", "", null, "c"));
		assertEquals("&lt;b&gt;", Html.escape("<b>"));
	}
}
