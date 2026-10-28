package com.lazyscaping;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/** Reads what the planners need to know about the logged-in account. Must be used on the client thread. */
@Singleton
class Account
{
	/** Training unlocks that come from quests, keyed by the site's unlock ids. */
	private static final Map<String, Quest> QUEST_UNLOCKS = ImmutableMap.<String, Quest>builder()
		.put("mm2", Quest.MONKEY_MADNESS_II)
		.put("dt1", Quest.DESERT_TREASURE_I)
		.put("lunar", Quest.LUNAR_DIPLOMACY)
		.put("sote", Quest.SONG_OF_THE_ELVES)
		.put("sotf", Quest.SINS_OF_THE_FATHER)
		.put("feud", Quest.THE_FEUD)
		.put("tote", Quest.TEMPLE_OF_THE_EYE)
		.put("boneVoyage", Quest.BONE_VOYAGE)
		.put("familyCrest", Quest.FAMILY_CREST)
		.put("touristTrap", Quest.THE_TOURIST_TRAP)
		.put("taiBwo", Quest.TAI_BWO_WANNAI_TRIO)
		.put("ghostsAhoy", Quest.GHOSTS_AHOY)
		.put("troubledTortugans", Quest.TROUBLED_TORTUGANS)
		.build();

	private final Client client;

	@Inject
	Account(Client client)
	{
		this.client = client;
	}

	/** The player's name, or null before it has loaded. */
	String name()
	{
		Player player = client.getLocalPlayer();
		if (player == null || player.getName() == null)
		{
			return null;
		}
		return player.getName().replace('\u00A0', ' ');
	}

	int level(Skill skill)
	{
		return client.getRealSkillLevel(skill);
	}

	int xp(Skill skill)
	{
		return client.getSkillExperience(skill);
	}

	JsonObject levels()
	{
		JsonObject levels = new JsonObject();
		for (Skill skill : Skills.ALL)
		{
			levels.addProperty(skill.getName(), client.getRealSkillLevel(skill));
		}
		return levels;
	}

	/** Whether each quest unlock is done, by unlock id. */
	Map<String, Boolean> questUnlocks()
	{
		Map<String, Boolean> unlocks = new LinkedHashMap<>();
		QUEST_UNLOCKS.forEach((id, quest) -> unlocks.put(id, quest.getState(client) == QuestState.FINISHED));
		return unlocks;
	}

	/** On a members world, or with membership days left. */
	boolean isMember()
	{
		return client.getWorldType().contains(WorldType.MEMBERS) || client.getVarpValue(VarPlayerID.ACCOUNT_CREDIT) > 0;
	}

	/** Any kind of ironman, including group ironmen. */
	boolean isIronman()
	{
		return client.getVarbitValue(VarbitID.IRONMAN) != 0;
	}

	/** The hiscores the account appears on, as the site names them. Group ironmen use the normal hiscores. */
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
