package com.lazyscaping.ui;

import java.util.List;
import lombok.Value;

/** What the player is missing from a gear setup, out of what they're wearing and carrying. */
@Value
public class ReadyCheck
{
	@Value
	public static class Missing
	{
		String name;
		int quantity;
	}

	List<Missing> missing;

	public boolean isComplete()
	{
		return missing.isEmpty();
	}
}
