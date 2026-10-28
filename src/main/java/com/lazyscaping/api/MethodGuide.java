package com.lazyscaping.api;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

/** How to do a training method, from the site's method guides. */
@Getter
public class MethodGuide
{
	private String where;
	private List<String> travel = Collections.emptyList();
	private List<String> bring = Collections.emptyList();
	private List<String> needs = Collections.emptyList();
	private List<String> steps = Collections.emptyList();
	/** The OSRS Wiki page with the full details. */
	private String wiki;
}
