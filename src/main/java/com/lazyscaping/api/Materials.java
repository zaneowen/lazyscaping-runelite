package com.lazyscaping.api;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
public class Materials
{
	/** Xp each action gives. */
	private double xp;
	private List<Material> inputs = Collections.emptyList();
	private List<Material> outputs = Collections.emptyList();
}
