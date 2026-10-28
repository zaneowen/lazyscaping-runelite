package com.lazyscaping.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
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
 * Client for lazyscaping.app's plugin API. Requests are made asynchronously with
 * RuneLite's {@link OkHttpClient}; callbacks run on OkHttp's threads.
 */
@Slf4j
@Singleton
public class LazyScapingClient
{
	public static final HttpUrl SITE = HttpUrl.get("https://lazyscaping.app");

	/** The API version this plugin speaks. The site keeps old versions working for plugins not yet updated. */
	private static final String API = "api/plugin/v1/";
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	/** Bumped when the catalog's shape changes, so cached copies of the old one aren't used. */
	private static final String CATALOG_VERSION = "3";

	private final OkHttpClient http;
	private final Gson gson;

	@Inject
	LazyScapingClient(OkHttpClient http, Gson gson)
	{
		this.http = http;
		this.gson = gson;
	}

	public void catalog(Consumer<Catalog> onSuccess)
	{
		HttpUrl url = SITE.newBuilder()
			.addPathSegments(API + "catalog")
			.addQueryParameter("v", CATALOG_VERSION)
			.build();
		send(new Request.Builder().url(url).build(), Catalog.class, onSuccess, error -> {});
	}

	public void plan(JsonObject body, Consumer<Plan> onSuccess, Consumer<String> onError)
	{
		send(post(API + "plan", body), Plan.class, onSuccess, onError);
	}

	public void gearSetup(JsonObject body, Consumer<GearSetup> onSuccess, Consumer<String> onError)
	{
		send(post(API + "gear", body), GearSetup.class, onSuccess, onError);
	}

	/** Downloads one of the site's item pictures, given its path. */
	public void image(String path, Consumer<BufferedImage> onSuccess, Runnable onError)
	{
		HttpUrl url = SITE.resolve(path);
		if (url == null || !url.host().equals(SITE.host()))
		{
			onError.run();
			return;
		}

		http.newCall(new Request.Builder().url(url).build()).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Failed to fetch {}", url, e);
				onError.run();
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				BufferedImage image = null;
				try (ResponseBody body = response.body())
				{
					if (response.isSuccessful() && body != null)
					{
						synchronized (ImageIO.class)
						{
							image = ImageIO.read(body.byteStream());
						}
					}
				}
				catch (IOException e)
				{
					log.debug("Failed to read {}", url, e);
				}

				if (image != null)
				{
					onSuccess.accept(image);
				}
				else
				{
					onError.run();
				}
			}
		});
	}

	private Request post(String path, JsonObject body)
	{
		return new Request.Builder()
			.url(SITE.newBuilder().addPathSegments(path).build())
			.post(RequestBody.create(JSON, gson.toJson(body)))
			.build();
	}

	/** Sends a request and parses the response. {@code onError} receives a message to show the player. */
	private <T> void send(Request request, Class<T> type, Consumer<T> onSuccess, Consumer<String> onError)
	{
		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Request to {} failed", request.url(), e);
				onError.accept("Couldn't reach lazyscaping.app. Check your connection and try again.");
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				T result;
				try (ResponseBody body = response.body())
				{
					String text = body == null ? "" : body.string();
					if (!response.isSuccessful())
					{
						onError.accept(errorMessage(text, response.code()));
						return;
					}
					result = gson.fromJson(text, type);
				}
				catch (IOException | RuntimeException e)
				{
					log.debug("Bad response from {}", request.url(), e);
					result = null;
				}

				if (result == null)
				{
					onError.accept("lazyscaping.app sent something unexpected. Try again, or restart RuneLite to update the plugin.");
					return;
				}
				onSuccess.accept(result);
			}
		});
	}

	/** A message for a failed request: the site's {@code {"error": "..."}} message, or one for the status code. */
	private String errorMessage(String body, int code)
	{
		if (code == 404 || code == 410)
		{
			// the API version this plugin speaks has been retired
			return "This version of the plugin is out of date. Restart RuneLite to update it.";
		}
		if (code >= 500)
		{
			return "lazyscaping.app is having trouble. Try again in a minute.";
		}
		try
		{
			JsonElement json = gson.fromJson(body, JsonElement.class);
			if (json != null && json.isJsonObject() && json.getAsJsonObject().has("error"))
			{
				return json.getAsJsonObject().get("error").getAsString();
			}
		}
		catch (RuntimeException e)
		{
			log.debug("Unreadable error response", e);
		}
		return "lazyscaping.app couldn't handle the request (" + code + "). Try again, or restart RuneLite to update the plugin.";
	}
}
