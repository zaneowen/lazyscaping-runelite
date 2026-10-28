package com.lazyscaping.api;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A Slayer task the site has a plan for. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SlayerTaskRef
{
	private String id;
	private String name;
	/** In-game task names that differ from the site's, like "Iron dragons" for metal dragons. */
	private List<String> gameNames = Collections.emptyList();

	@Override
	public String toString()
	{
		return name;
	}
}
