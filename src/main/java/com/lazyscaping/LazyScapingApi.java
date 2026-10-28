package com.lazyscaping;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Calls lazyscaping.app's /api/plugin routes. Only used while the player has
 * turned on live recommendations. Callbacks run on OkHttp's threads.
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

	static class PlanStep
	{
		String method;
		int fromLevel;
		int toLevel;
		long xp;
		int xpHr;
		double hours;
		long cost;
		String notes;
	}

	static class Plan
	{
		boolean goalReached;
		double totalHours;
		long totalCost;
		List<PlanStep> steps;
	}

	static class MissingLevel
	{
		String skill;
		int have;
		int need;
	}

	static class BossSuggestion
	{
		String id;
		String name;
		/** "ready" or "close". */
		String readiness;
		String style;
		List<MissingLevel> missing;
		boolean gearShort;
	}

	static class Bosses
	{
		List<BossSuggestion> bosses;
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

	static class GearSlot
	{
		String slot;
		/** "owned", "none" or "twoHanded" (the weapon takes this slot). */
		String status;
		/** The best item you own for the slot, or null. */
		GearItem item;
		/** The next item to work towards. */
		GearItem next;
		/** The boss needs a specific item here, like an anti-dragon shield. */
		boolean missingRequired;
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
	}

	/** One of the site's item pictures, for items the game can't draw by id. */
	void image(String path, Consumer<BufferedImage> done)
	{
		HttpUrl url = SITE.resolve(path);
		if (url == null || !url.host().equals(SITE.host()))
		{
			return;
		}
		http.newCall(new Request.Builder().url(url).build()).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("LazyScaping image failed: {}", url, e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						return;
					}
					BufferedImage image;
					// ImageIO isn't thread-safe.
					synchronized (ImageIO.class)
					{
						image = ImageIO.read(body.byteStream());
					}
					if (image != null)
					{
						done.accept(image);
					}
				}
				catch (IOException e)
				{
					log.debug("Bad LazyScaping image: {}", url, e);
				}
			}
		});
	}

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

	void bosses(JsonObject body, Consumer<Bosses> done, Consumer<String> failed)
	{
		send(post("/api/plugin/bosses", body), Bosses.class, done, failed);
	}

	void gear(JsonObject body, Consumer<GearSetup> done, Consumer<String> failed)
	{
		send(post("/api/plugin/gear", body), GearSetup.class, done, failed);
	}

	private Request post(String path, JsonObject body)
	{
		return new Request.Builder()
			.url(SITE.resolve(path))
			.post(RequestBody.create(JSON, gson.toJson(body)))
			.build();
	}

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
						JsonObject error = text.startsWith("{") ? gson.fromJson(text, JsonObject.class) : null;
						failed.accept(error != null && error.has("error") ? error.get("error").getAsString() : "lazyscaping.app had a problem (" + response.code() + ").");
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
}
