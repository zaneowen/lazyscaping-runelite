package com.lazyscaping.api;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

/** How to get to a boss. */
@Getter
public class Travel
{
	private List<Teleport> teleports = Collections.emptyList();
	/** Other ways there, like a house portal or a fairy ring. */
	private List<String> routes = Collections.emptyList();
	private String quest;
	/** Other requirements, like a key. */
	private String access;
	/** The routes use fairy rings, which need a dramen staff until the elite Lumbridge & Draynor Diary. */
	private boolean fairyRings;
	/** The boss's OSRS Wiki strategy page. */
	private String wiki;

	public boolean isEmpty()
	{
		return teleports.isEmpty() && routes.isEmpty() && quest == null && access == null && wiki == null;
	}
}
