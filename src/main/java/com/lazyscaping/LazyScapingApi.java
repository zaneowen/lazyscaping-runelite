package com.lazyscaping;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calls lazyscaping.app's /api/plugin routes (see src/app/api/plugin in the
 * site's repo). Only used while the player has turned on live recommendations.
 * Requests go through RuneLite's OkHttp client; callbacks run on its threads.
 */
@Singleton
class LazyScapingApi
{
	private static final Logger log = LoggerFactory.getLogger(LazyScapingApi.class);

	static final HttpUrl SITE = HttpUrl.parse("https://lazyscaping.app");
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	@Inject
	private OkHttpClient http;

	@Inject
	private Gson gson;

	/** The gear and bosses the planners know. */
	void catalog(Consumer<GearCatalog> done)
	{
		// Versioned, so a cached copy from before the catalog changed shape isn't reused.
		Request request = new Request.Builder().url(SITE.resolve("/api/plugin/catalog?v=2")).build();
		send(request, JsonObject.class, json -> done.accept(GearCatalog.fromJson(gson, json)), error -> {});
	}

	void plan(JsonObject body, Consumer<Plan> done, Consumer<String> failed)
	{
		send(post("/api/plugin/plan", body), Plan.class, done, failed);
	}

	void gear(JsonObject body, Consumer<GearSetup> done, Consumer<String> failed)
	{
		send(post("/api/plugin/gear", body), GearSetup.class, done, failed);
	}

	/** One of the site's item pictures (a path like /images/wiki/fire-cape.png), for items the game can't draw by id. */
	void image(String path, Consumer<BufferedImage> done, Runnable failed)
	{
		HttpUrl url = SITE.resolve(path);
		// Only ever the site's own images.
		if (url == null || !url.host().equals(SITE.host()))
		{
			failed.run();
			return;
		}
		http.newCall(new Request.Builder().url(url).build()).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("LazyScaping image failed: {}", url, e);
				failed.run();
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				BufferedImage image = null;
				try (ResponseBody body = response.body())
				{
					if (response.isSuccessful() && body != null)
					{
						// ImageIO isn't thread-safe.
						synchronized (ImageIO.class)
						{
							image = ImageIO.read(body.byteStream());
						}
					}
				}
				catch (IOException e)
				{
					log.debug("Bad LazyScaping image: {}", url, e);
				}
				if (image != null)
				{
					done.accept(image);
				}
				else
				{
					failed.run();
				}
			}
		});
	}

	private Request post(String path, JsonObject body)
	{
		return new Request.Builder()
			.url(SITE.resolve(path))
			.post(RequestBody.create(JSON, gson.toJson(body)))
			.build();
	}

	/** Sends a request and reads its JSON. `failed` gets a message to show the player. */
	private <T> void send(Request request, Class<T> type, Consumer<T> done, Consumer<String> failed)
	{
		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("LazyScaping request failed: {}", request.url(), e);
				failed.accept("Couldn't reach lazyscaping.app.");
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					String text = body == null ? "" : body.string();
					if (!response.isSuccessful())
					{
						failed.accept(errorMessage(text, response.code()));
						return;
					}
					done.accept(gson.fromJson(text, type));
				}
				catch (Exception e)
				{
					log.debug("Bad LazyScaping response: {}", request.url(), e);
					failed.accept("lazyscaping.app sent something unexpected.");
				}
			}
		});
	}

	/** The site's error message ({"error": "..."}), or a general one. */
	private String errorMessage(String text, int code)
	{
		try
		{
			JsonElement json = gson.fromJson(text, JsonElement.class);
			if (json != null && json.isJsonObject() && json.getAsJsonObject().has("error"))
			{
				return json.getAsJsonObject().get("error").getAsString();
			}
		}
		catch (RuntimeException ignored)
		{
		}
		return "lazyscaping.app had a problem (" + code + ").";
	}

	// The site's responses, read with Gson. Fields match the JSON.

	static class Plan
	{
		boolean goalReached;
		double totalHours;
		long totalCost;
		/** Even the cheapest methods cost more than the budget. */
		boolean overBudget;
		/** The method the player picked, when the plan could use it. */
		String method;
		List<PlanStep> steps;
		/** Every method for the skill, available ones first. */
		List<MethodOption> options;
	}

	static class PlanStep
	{
		/** The method's id, to find it in the plan's options. */
		String id;
		String method;
		int fromLevel;
		int toLevel;
		long xp;
		int xpHr;
		double hours;
		long cost;
		String notes;
	}

	/** A training method for the skill, as the Training page lists them. */
	static class MethodOption
	{
		String id;
		String name;
		int level;
		int xpHr;
		/** Positive costs gp per xp, negative makes gp. */
		double gpPerXp;
		/** 1 click-intensive, 2 moderate, 3 very AFK. */
		int afk;
		boolean members;
		boolean ironFriendly;
		String notes;
		MethodGuide guide;
		boolean available;
		/** Why it can't be used yet, e.g. "Needs level 70". */
		List<String> blockers;
		/** Can be picked: nothing but level stands in the way before the goal. */
		boolean pickable;
	}

	static class MethodGuide
	{
		String where;
		List<String> travel;
		List<String> steps;
		List<String> bring;
		List<String> needs;
		/** OSRS Wiki page with the full details. */
		String wiki;
	}

	static class GearSetup
	{
		GearCatalog.Boss boss;
		String style;
		List<String> styles;
		/** Set when you can't bring your own gear, saying why. */
		String noGear;
		List<GearSlot> picks;
		List<GearSwitch> switches;
		GearSlot spec;
		List<ItemLine> supplies;
		List<ItemLine> utility;
		/** GE cost of the supplies and utility items for one trip. */
		long tripCost;
		/** The budget the setup was built with, null for unlimited. */
		Long budget;
		boolean ironman;
		/** Gp spent on gear from the budget. */
		long gearCost;
		/** Supplies alone cost more than the budget, so no gear was bought. */
		boolean suppliesOverBudget;
	}

	static class GearSlot
	{
		String slot;
		/** "owned", "buy" (bought with the budget), "none" or "twoHanded" (the weapon takes this slot). */
		String status;
		/** Gp spent on this slot when it's bought. */
		long cost;
		/** The best item you own (or buy) for the slot, or null. */
		GearItem item;
		/** The next item to work towards. */
		GearItem next;
		/** The boss needs a specific item here, like an anti-dragon shield. */
		boolean missingRequired;
	}

	static class GearItem
	{
		/** The site's gear id. */
		String id;
		String name;
		/** The game's item id, for tradeable items. */
		Integer itemId;
		/** The site's picture of the item, a path like /images/wiki/fire-cape.png. */
		String icon;
		/** GE price, null when untradeable. Ammo is priced for one trip. */
		Long price;
	}

	static class GearSwitch
	{
		String style;
		List<GearSlot> picks;
	}

	static class ItemLine
	{
		String name;
		int qty;
		String why;
		/** GE cost for one trip, null when it can't be bought. */
		Long cost;
	}
}
