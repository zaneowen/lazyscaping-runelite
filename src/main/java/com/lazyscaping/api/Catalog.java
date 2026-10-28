package com.lazyscaping.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/** The gear, bosses and Slayer tasks the site's planners know. */
@Getter
public class Catalog
{
	/** Every in-game name that matches each piece of gear, normalized, by gear id. */
	private Map<String, List<String>> gear = Collections.emptyMap();
	private List<Boss> bosses = Collections.emptyList();
	/** The bosses' groups, in the order the site lists them. */
	private List<String> bossGroups = Collections.emptyList();
	private List<SlayerTaskRef> slayerTasks = Collections.emptyList();
}
