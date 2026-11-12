package com.lazyscaping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.lazyscaping.api.Boss;
import com.lazyscaping.ui.KillPace;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import org.junit.Test;

public class KillTrackerTest
{
	private final List<String> killed = new ArrayList<>();
	private final List<KillPace> paces = new ArrayList<>();
	private final KillTracker tracker = new KillTracker((bossId, pace) ->
	{
		killed.add(bossId);
		paces.add(pace);
	});

	{
		tracker.setBosses(List.of(
			new Boss("vorkath", "Vorkath", null),
			new Boss("tztok_jad", "TzTok-Jad (Fight Caves)", null),
			new Boss("theatre_of_blood", "Theatre of Blood", null),
			new Boss("hueycoatl", "The Hueycoatl", null)));
	}

	private void message(String text)
	{
		tracker.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "", text, "", 0));
	}

	@Test
	public void recognisesKillCountMessages()
	{
		message("Your Vorkath kill count is: <col=ff0000>1,234</col>.");
		message("Your TzTok-Jad kill count is: 5.");
		message("Your completed Theatre of Blood count is: 12.");
		message("Your Hueycoatl kill count is: 3.");
		message("Your Gauntlet completion count is: 40.");
		assertEquals(List.of("vorkath", "tztok_jad", "theatre_of_blood", "hueycoatl", "the_gauntlet"), killed);
	}

	@Test
	public void ignoresOtherMessages()
	{
		message("Your Zulrah kill count is: 10.");
		message("Fight duration: 1:23. Personal best: 1:01");
		tracker.onChatMessage(new ChatMessage(null, ChatMessageType.PUBLICCHAT, "", "Your Vorkath kill count is: 9.", "", 0));
		assertTrue(killed.isEmpty());
	}

	@Test
	public void countsKillsThisSession()
	{
		message("Your Vorkath kill count is: 100.");
		message("Your Vorkath kill count is: 101.");
		assertEquals(2, paces.get(1).getKills());
		tracker.reset();
		message("Your Vorkath kill count is: 102.");
		assertEquals(1, paces.get(2).getKills());
	}
}
