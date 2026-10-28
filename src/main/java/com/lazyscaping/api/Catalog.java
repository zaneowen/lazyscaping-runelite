package com.lazyscaping.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/** The gear and bosses the site's planners know. */
@Getter
public class Catalog
{
	/** Every in-game name that matches each piece of gear, normalized, by gear id. */
	private Map<String, List<String>> gear = Collections.emptyMap();
	private List<Boss> bosses = Collections.emptyList();
}
