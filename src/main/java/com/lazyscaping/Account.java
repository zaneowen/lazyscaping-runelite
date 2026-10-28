package com.lazyscaping;

import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/** What the planners need to know about the logged-in account. Read on the client thread. */
@Singleton
class Account
{
	/** Skills with levels. Overall is deprecated in the API, so it's left out by name. */
	static final List<Skill> SKILLS = Arrays.stream(Skill.values())
		.filter(s -> !"Overall".equals(s.getName()))
		.collect(Collectors.toUnmodifiableList());

	/** The site's planner unlocks (see UNLOCKS in src/lib/methods.ts) that come from finishing a quest. */
	private static final Map<String, Quest> QUEST_UNLOCKS = new LinkedHashMap<>();

	static
	{
		QUEST_UNLOCKS.put("mm2", Quest.MONKEY_MADNESS_II);
		QUEST_UNLOCKS.put("dt1", Quest.DESERT_TREASURE_I);
		QUEST_UNLOCKS.put("lunar", Quest.LUNAR_DIPLOMACY);
		QUEST_UNLOCKS.put("sote", Quest.SONG_OF_THE_ELVES);
		QUEST_UNLOCKS.put("sotf", Quest.SINS_OF_THE_FATHER);
		QUEST_UNLOCKS.put("feud", Quest.THE_FEUD);
		QUEST_UNLOCKS.put("tote", Quest.TEMPLE_OF_THE_EYE);
		QUEST_UNLOCKS.put("boneVoyage", Quest.BONE_VOYAGE);
		QUEST_UNLOCKS.put("familyCrest", Quest.FAMILY_CREST);
		QUEST_UNLOCKS.put("touristTrap", Quest.THE_TOURIST_TRAP);
		QUEST_UNLOCKS.put("taiBwo", Quest.TAI_BWO_WANNAI_TRIO);
		QUEST_UNLOCKS.put("ghostsAhoy", Quest.GHOSTS_AHOY);
		QUEST_UNLOCKS.put("troubledTortugans", Quest.TROUBLED_TORTUGANS);
	}

	@Inject
	private Client client;

	static Skill skillNamed(String name)
	{
		for (Skill skill : SKILLS)
		{
			if (skill.getName().equals(name))
			{
				return skill;
			}
		}
		return null;
	}

	/** The player's name, or null before it has loaded. */
	String name()
	{
		if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
		{
			return null;
		}
		// Names with spaces come through with non-breaking spaces.
		return client.getLocalPlayer().getName().replace(' ', ' ');
	}

	int level(Skill skill)
	{
		return client.getRealSkillLevel(skill);
	}

	int xp(Skill skill)
	{
		return client.getSkillExperience(skill);
	}

	/** Real levels by skill name, as the site's API takes them. */
	JsonObject levels()
	{
		JsonObject levels = new JsonObject();
		for (Skill skill : SKILLS)
		{
			levels.addProperty(skill.getName(), client.getRealSkillLevel(skill));
		}
		return levels;
	}

	/** Whether each quest unlock the site plans with is done, by the site's unlock key. */
	Map<String, Boolean> questUnlocks()
	{
		Map<String, Boolean> out = new LinkedHashMap<>();
		QUEST_UNLOCKS.forEach((key, quest) -> out.put(key, quest.getState(client) == QuestState.FINISHED));
		return out;
	}

	/** On a members world, or with membership days left. */
	boolean members()
	{
		return client.getWorldType().contains(WorldType.MEMBERS) || client.getVarpValue(VarPlayerID.ACCOUNT_CREDIT) > 0;
	}

	/** Any kind of ironman, group ironmen included: they can't buy from the GE. */
	boolean ironman()
	{
		return client.getVarbitValue(VarbitID.IRONMAN) != 0;
	}

	/** The hiscores the account is on, as the site names them. Group ironmen are on the normal hiscores. */
	String hiscoresType()
	{
		switch (client.getVarbitValue(VarbitID.IRONMAN))
		{
			case 1:
				return "ironman";
			case 2:
				return "ultimate";
			case 3:
				return "hardcore";
			default:
				return "normal";
		}
	}
}
