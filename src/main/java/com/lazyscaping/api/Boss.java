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

	@Override
	public String toString()
	{
		return name;
	}
}
