package com.lazyscaping;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.api.Skill;

public final class Skills
{
	/** Every skill with a level. Overall is deprecated in the API, so it's matched by name. */
	public static final List<Skill> ALL = Arrays.stream(Skill.values())
		.filter(s -> !"Overall".equals(s.getName()))
		.collect(Collectors.toUnmodifiableList());

	private Skills()
	{
	}

	/** The skill with this display name, or null. */
	public static Skill byName(String name)
	{
		return ALL.stream().filter(s -> s.getName().equals(name)).findFirst().orElse(null);
	}
}
