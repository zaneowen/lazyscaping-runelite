package com.lazyscaping;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LazyScapingPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LazyScapingPlugin.class);
		RuneLite.main(args);
	}
}
