package com.lazyscaping.api;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

/** For a boss fought solo or in a group: the team size the setup is for, and the choices. */
@Getter
public class TeamOptions
{
	/** "solo" or "group". */
	private String selected;
	private List<Option> options = Collections.emptyList();

	@Getter
	public static class Option
	{
		private String id;
		/** Like "Solo", "Duo" or "2–5 players". */
		private String label;
		/** What changes for this team size. */
		private String summary;

		@Override
		public String toString()
		{
			return label;
		}
	}
}
