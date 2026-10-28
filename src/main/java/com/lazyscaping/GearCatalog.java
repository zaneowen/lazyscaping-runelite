package com.lazyscaping;

import com.google.gson.Gson;
import com.lazyscaping.api.Boss;
import com.lazyscaping.api.Catalog;
import com.lazyscaping.api.SlayerTaskRef;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Matches in-game names to what the site's planners know. Item names match gear
 * the same way as the site's bank import: names are normalized, then charged,
 * degraded and decorated suffixes are stripped. Task names match Slayer tasks.
 */
@Slf4j
class GearCatalog
{
	/**
	 * Versions of an item that count as owning it: charged or not, ornament kits,
	 * locked, imbued, heraldic, poisoned, fortified and recoloured, and teleport
	 * jewellery's charges. Broken and damaged items aren't, as they can't be worn.
	 */
	private static final Pattern VARIANT_SUFFIX = Pattern.compile("\\s*\\((uncharged|empty|full|inactive|c|or|t|g|i|u|e|f|l|r|au|max"
		+ "|dark|light|dusk|deadman|last man standing|p\\+{0,2}|h[1-5]|t?[1-8])\\)$");
	private static final Pattern DEGRADE_SUFFIX = Pattern.compile("\\s+(0|25|50|75|100|5|6)$");
	/** Ornament and upgrade prefixes, like a twisted ancestral hat or a volatile nightmare staff. */
	private static final Pattern VARIANT_PREFIX = Pattern.compile("^(twisted|sanguine|radiant|light|dark|blessed|imbued|corrupted"
		+ "|volatile|eldritch|hooded|oathplate|blood|ice|smoke|shadow|ghommal's) ");
	/** Versions that need fixing before they can be worn. */
	private static final Pattern UNUSABLE = Pattern.compile("\\((broken|mangled|damaged|tarnished)\\)", Pattern.CASE_INSENSITIVE);
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	/** How many parts to strip, for names like "Ghommal's avernic defender 5 (l)". */
	private static final int MAX_STRIPS = 4;

	private final Map<String, String> gearByName = new HashMap<>();
	private final Map<String, SlayerTaskRef> tasksByName = new HashMap<>();
	@Getter
	private final List<Boss> bosses;
	@Getter
	private final List<String> bossGroups;
	@Getter
	private final List<SlayerTaskRef> slayerTasks;

	GearCatalog(Catalog catalog)
	{
		catalog.getGear().forEach((id, names) -> names.forEach(name -> gearByName.put(normalize(name), id)));
		bosses = catalog.getBosses();
		bossGroups = catalog.getBossGroups();
		slayerTasks = catalog.getSlayerTasks();
		for (SlayerTaskRef task : slayerTasks)
		{
			tasksByName.put(taskKey(task.getName()), task);
			task.getGameNames().forEach(name -> tasksByName.put(taskKey(name), task));
		}
	}

	/** The copy bundled with the plugin, so sync works without network access. */
	static GearCatalog bundled(Gson gson)
	{
		try (InputStream in = GearCatalog.class.getResourceAsStream("gear-catalog.json");
			Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			return new GearCatalog(gson.fromJson(reader, Catalog.class));
		}
		catch (Exception e)
		{
			log.warn("Unable to read the bundled gear catalog", e);
			return new GearCatalog(new Catalog());
		}
	}

	static String normalize(String name)
	{
		String lower = name.trim().toLowerCase(Locale.ROOT).replace('\u2019', '\'').replace('`', '\'');
		return WHITESPACE.matcher(lower).replaceAll(" ");
	}

	/** The site's task for an in-game Slayer task name, or null. */
	SlayerTaskRef matchTask(String gameName)
	{
		return gameName == null ? null : tasksByName.get(taskKey(gameName));
	}

	/** Task names compare without case or a plural "s", so "Kalphite" matches "Kalphites". */
	private static String taskKey(String name)
	{
		String key = normalize(name);
		return key.endsWith("s") ? key.substring(0, key.length() - 1) : key;
	}

	/**
	 * The gear id for an owned item, or null if the planners don't use it.
	 *
	 * @param baseName the name of the item it's a version of, as RuneLite groups
	 *                 them, for versions named unlike it (a volcanic abyssal whip)
	 */
	String match(String itemName, String baseName)
	{
		String id = match(itemName);
		if (id == null && baseName != null && !UNUSABLE.matcher(itemName).find())
		{
			id = match(baseName);
		}
		return id;
	}

	/** The gear id for an in-game item name, or null if the planners don't use it. */
	String match(String itemName)
	{
		String name = normalize(itemName);
		for (int i = 0; i < MAX_STRIPS && !name.isEmpty(); i++)
		{
			String id = gearByName.get(name);
			if (id == null)
			{
				id = gearByName.get(VARIANT_PREFIX.matcher(name).replaceFirst(""));
			}
			if (id != null)
			{
				return id;
			}

			String stripped = VARIANT_SUFFIX.matcher(DEGRADE_SUFFIX.matcher(name).replaceFirst("")).replaceFirst("");
			if (stripped.equals(name))
			{
				stripped = VARIANT_PREFIX.matcher(name).replaceFirst("");
			}
			if (stripped.equals(name))
			{
				break;
			}
			name = stripped;
		}
		return null;
	}
}
