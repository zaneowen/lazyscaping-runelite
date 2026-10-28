package com.lazyscaping;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The gear the LazyScaping planners know, by every in-game name that matches it.
 * Matching works like the site's bank import (src/lib/bankImport.ts): names are
 * normalized, then charged, degraded and decorated suffixes are stripped.
 * A copy ships with the plugin; with live recommendations on, the plugin
 * fetches the current one from /api/plugin/catalog.
 */
class GearCatalog
{
	private static final Logger log = LoggerFactory.getLogger(GearCatalog.class);

	private static final Pattern VARIANT_SUFFIX = Pattern.compile("\\s*\\((uncharged|empty|full|inactive|c|or|t|g|i|u|deadman|last man standing)\\)$");
	private static final Pattern DEGRADE_SUFFIX = Pattern.compile("\\s+(0|25|50|75|100)$");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	/** Normalized name -> gear id. */
	private final Map<String, String> byName = new HashMap<>();
	private final List<Boss> bosses;

	/** A boss the site has gear plans for. */
	static class Boss
	{
		final String id;
		final String name;

		Boss(String id, String name)
		{
			this.id = id;
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	private static class Response
	{
		Map<String, List<String>> gear;
		List<Boss> bosses;
	}

	GearCatalog(Map<String, List<String>> gear, List<Boss> bosses)
	{
		gear.forEach((id, names) -> names.forEach(name -> byName.put(normalize(name), id)));
		this.bosses = bosses;
	}

	static GearCatalog fromJson(Gson gson, JsonObject json)
	{
		Response response = gson.fromJson(json, Response.class);
		return new GearCatalog(response.gear == null ? Map.of() : response.gear, response.bosses == null ? List.of() : response.bosses);
	}

	List<Boss> bosses()
	{
		return bosses;
	}

	/** The copy bundled with the plugin, or an empty catalog if it can't be read. */
	static GearCatalog bundled(Gson gson)
	{
		try (InputStream in = GearCatalog.class.getResourceAsStream("gear-catalog.json");
			Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			return fromJson(gson, gson.fromJson(reader, JsonObject.class));
		}
		catch (Exception e)
		{
			log.warn("Couldn't read the bundled gear catalog", e);
			return new GearCatalog(Map.of(), List.of());
		}
	}

	static String normalize(String name)
	{
		String lower = name.trim().toLowerCase(Locale.ROOT).replace('\u2019', '\'').replace('`', '\'');
		return WHITESPACE.matcher(lower).replaceAll(" ");
	}

	/** The gear id for an in-game item name, or null if the planners don't use it. */
	String match(String itemName)
	{
		String name = normalize(itemName);
		for (int i = 0; i < 3 && !name.isEmpty(); i++)
		{
			String hit = byName.get(name);
			if (hit != null)
			{
				return hit;
			}
			String stripped = VARIANT_SUFFIX.matcher(DEGRADE_SUFFIX.matcher(name).replaceFirst("")).replaceFirst("");
			if (stripped.equals(name))
			{
				break;
			}
			name = stripped;
		}
		return null;
	}

	/** The gear ids for a set of item names, sorted and once each. */
	Set<String> matchAll(Collection<String> itemNames)
	{
		Set<String> out = new TreeSet<>();
		for (String name : itemNames)
		{
			String id = match(name);
			if (id != null)
			{
				out.add(id);
			}
		}
		return out;
	}
}
