package com.lazyscaping.api;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

/** How a boss fight goes: tips, and which spellbook and thralls to use. */
@Getter
public class FightNotes
{
	private List<String> tips = Collections.emptyList();
	/** The spellbook to be on for the setup's style, or null when any works. */
	private String spellbook;
	/** "Yes", "Optional" or "No". */
	private String thralls;
	/** Why that spellbook and thralls. */
	private String magic;
}
