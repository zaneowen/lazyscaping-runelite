package com.lazyscaping;

import com.lazyscaping.api.Boss;
import com.lazyscaping.ui.KillPace;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.util.Text;

/**
 * Times the player's kills from the game's kill count messages, like "Your
 * Vorkath kill count is: 123.", to work out their real pace at each boss this
 * session. Used on the client thread.
 */
class KillTracker
{
	/** "Your X kill count is: N", "Your completed X count is: N" and "Your X completion count is: N". */
	private static final Pattern KILL_COUNT = Pattern.compile("^Your (?:completed )?(.+?) (?:kill |completion )?count is: ?([\\d,]+)");
	/** A boss name's part in brackets, like "(Fight Caves)", which the game's messages leave out. */
	private static final Pattern BRACKETS = Pattern.compile("\\s*\\(.*\\)$");
	/** Kill count names that aren't the boss's name, by the name in the message. */
	private static final Map<String, String> MESSAGE_NAMES = Map.of(
		"gauntlet", "the_gauntlet",
		"corrupted gauntlet", "corrupted_gauntlet",
		"barrows chest", "barrows",
		"lunar chest", "moons_of_peril");

	private final Map<String, String> bossIdsByName = new HashMap<>();
	private final Map<String, long[]> kills = new HashMap<>();
	private final BiConsumer<String, KillPace> onKill;

	/** @param onKill told of the boss id and its new pace after each kill */
	KillTracker(BiConsumer<String, KillPace> onKill)
	{
		this.onKill = onKill;
	}

	/** The bosses whose kill counts to recognise, by name. */
	void setBosses(List<Boss> bosses)
	{
		bossIdsByName.clear();
		for (Boss boss : bosses)
		{
			bossIdsByName.put(key(boss.getName()), boss.getId());
		}
	}

	void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		Matcher match = KILL_COUNT.matcher(Text.removeTags(event.getMessage()));
		if (!match.find())
		{
			return;
		}
		String name = key(match.group(1));
		String bossId = MESSAGE_NAMES.getOrDefault(name, bossIdsByName.get(name));
		if (bossId == null)
		{
			return;
		}

		// kills this session, the first kill's time and the last kill's time
		long now = System.currentTimeMillis();
		long[] stats = kills.computeIfAbsent(bossId, id -> new long[]{0, now, now});
		stats[0]++;
		stats[2] = now;
		onKill.accept(bossId, pace(stats));
	}

	/** Forgets this session's kills, like after logging out. */
	void reset()
	{
		kills.clear();
	}

	/** Kills an hour from the time between the first and last kill, once there are a few to go on. */
	private static KillPace pace(long[] stats)
	{
		int count = (int) stats[0];
		double hours = (stats[2] - stats[1]) / 3_600_000.0;
		double kph = count > 1 && hours > 0 ? (count - 1) / hours : 0;
		return new KillPace(count, kph);
	}

	/** A name to match on: without a bracketed part or a leading "the", in lower case. */
	private static String key(String name)
	{
		String key = BRACKETS.matcher(name).replaceAll("").trim().toLowerCase(Locale.ROOT);
		return key.startsWith("the ") ? key.substring(4) : key;
	}
}
