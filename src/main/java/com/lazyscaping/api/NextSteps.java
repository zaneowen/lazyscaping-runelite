package com.lazyscaping.api;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

/** What the player could do next across their account, from lazyscaping.app. */
@Getter
public class NextSteps
{
	private List<Step> steps = Collections.emptyList();

	@Getter
	public static class Step
	{
		/** "boss", "level", "gear" or "quest". */
		private String kind;
		/** Short and actionable, like "Train Ranged to 80". */
		private String title;
		/** Why, in a sentence or two. */
		private String detail;
		/** The boss the step is about or leads to, or null. */
		private String bossId;
		/** For a level step: the skill's name, like "Ranged", and the level to reach. */
		private String skill;
		private Integer level;
	}
}
