package com.lazyscaping;

import com.google.gson.Gson;
import com.lazyscaping.api.Boss;
import com.lazyscaping.api.Catalog;
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
 * Matches in-game item names to the gear the site's planners know, the same way
 * as the site's bank import: names are normalized, then charged, degraded and
 * decorated suffixes are stripped.
 */
@Slf4j
class GearCatalog
{
	private static final Pattern VARIANT_SUFFIX = Pattern.compile("\\s*\\((uncharged|empty|full|inactive|c|or|t|g|i|u|deadman|last man standing)\\)$");
	private static final Pattern DEGRADE_SUFFIX = Pattern.compile("\\s+(0|25|50|75|100)$");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	/** How many suffixes to strip, for names like "Ahrim's hood (or) 100". */
	private static final int MAX_STRIPS = 3;

	private final Map<String, String> gearByName = new HashMap<>();
	@Getter
	private final List<Boss> bosses;

	GearCatalog(Catalog catalog)
	{
		catalog.getGear().forEach((id, names) -> names.forEach(name -> gearByName.put(normalize(name), id)));
		bosses = catalog.getBosses();
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

	/** The gear id for an in-game item name, or null if the planners don't use it. */
	String match(String itemName)
	{
		String name = normalize(itemName);
		for (int i = 0; i < MAX_STRIPS && !name.isEmpty(); i++)
		{
			String id = gearByName.get(name);
			if (id != null)
			{
				return id;
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
}
