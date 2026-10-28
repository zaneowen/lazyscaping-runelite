package com.lazyscaping.api;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
public class Plan
{
	private boolean goalReached;
	private double totalHours;
	private long totalCost;
	/** Even the cheapest methods cost more than the budget. */
	private boolean overBudget;
	/** The picked method's id, when the plan could use it. */
	private String method;
	private List<PlanStep> steps = Collections.emptyList();
	/** Every method for the skill, available ones first. */
	private List<TrainingMethod> options = Collections.emptyList();

	public TrainingMethod option(String id)
	{
		return options.stream().filter(o -> o.getId().equals(id)).findFirst().orElse(null);
	}
}
