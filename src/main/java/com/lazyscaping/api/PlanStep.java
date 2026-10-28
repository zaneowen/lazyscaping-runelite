package com.lazyscaping.api;

import lombok.Getter;

@Getter
public class PlanStep
{
	/** The method's id, matching one of the plan's methods. */
	private String id;
	private String method;
	private int fromLevel;
	private int toLevel;
	private int xpHr;
	private double hours;
	private long cost;
}
