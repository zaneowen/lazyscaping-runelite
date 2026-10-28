package com.lazyscaping.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Boss
{
	private String id;
	private String name;
	/** The site's boss group, like "Raids", or null when the catalog predates groups. */
	private String group;

	@Override
	public String toString()
	{
		return name;
	}
}
