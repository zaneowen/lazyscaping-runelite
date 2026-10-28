package com.lazyscaping;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Provides;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.xptracker.XpTrackerPlugin;
import net.runelite.client.plugins.xptracker.XpTrackerService;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import okhttp3.HttpUrl;

@PluginDescriptor(
	name = "LazyScaping",
	description = "Your training goal, plan and methods, and boss gear setups from lazyscaping.app, with one-click sync of your gear and quests",
	tags = {"training", "goal", "planner", "xp", "boss", "gear", "bank"}
)
@PluginDependency(XpTrackerPlugin.class)
public class LazyScapingPlugin extends Plugin implements LazyScapingPanel.Actions
{
	/** The Training page's default budget. */
	private static final String DEFAULT_TRAINING_BUDGET = "1M";
	/** Gear setups start from what you own, until you give them a budget. */
	private static final String DEFAULT_GEAR_BUDGET = Budget.FREE;
	/** How often the goal's xp/hr and time left update, in game ticks (3 seconds). */
	private static final int PROGRESS_TICKS = 5;
	/** The gear setup is fetched again after a level-up or new gear, at most this often, in game ticks (a minute). */
	private static final int GEAR_REFRESH_TICKS = 100;
	/** The only site the guides' links may open, besides lazyscaping.app. */
	private static final String WIKI_HOST = "oldschool.runescape.wiki";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private XpTrackerService xpTrackerService;

	@Inject
	private LazyScapingConfig config;

	@Inject
	private LazyScapingApi api;

	@Inject
	private Account account;

	@Inject
	private ProfileSettings settings;

	@Inject
	private OwnedItems owned;

	@Inject
	private ItemIcons icons;

	@Inject
	private Gson gson;

	/** Read on the Swing thread, so `ui` sees it cleared when the plugin stops. */
	private volatile LazyScapingPanel panel;
	private NavigationButton navButton;
	/** Bundled with the plugin, then replaced by the site's current one when live recommendations are on. */
	private volatile GearCatalog catalog;

	// Everything below is only touched on the client thread.
	/** The logged-in player, once their stats have loaded. */
	private String player;
	private boolean loadPending;
	private int ticks;
	private final Map<Skill, Integer> knownLevels = new EnumMap<>(Skill.class);

	private Skill goalSkill;
	private int goalLevel;
	private long goalFromXp;
	/** A training method picked for the goal's plan, or null for the best plan. */
	private String goalMethod;
	/** The goal skill's level when the plan was fetched, so a level-up fetches it again. */
	private int planLevel = -1;

	/** The boss picked for the gear setup, and the style picked for it (null: the best for your gear). */
	private String gearBoss;
	private String gearStyle;
	/** The gear the setup was fetched with, so new gear fetches it again. */
	private Set<String> setupGear = Set.of();
	private boolean gearStale;
	private int lastGearFetch = -GEAR_REFRESH_TICKS;

	/** Budgets as picked or typed, like "10M", "Unlimited" or "My coins". */
	private String trainingBudget = DEFAULT_TRAINING_BUDGET;
	private String gearBudget = DEFAULT_GEAR_BUDGET;

	@Provides
	LazyScapingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LazyScapingConfig.class);
	}

	@Override
	protected void startUp()
	{
		catalog = GearCatalog.bundled(gson);
		panel = new LazyScapingPanel(this, icons);
		navButton = NavigationButton.builder()
			.tooltip("LazyScaping")
			.icon(ImageUtil.loadImageResource(getClass(), "panel_icon.png"))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		panel.setLiveEnabled(config.liveRecommendations());
		panel.setBossOptions(catalog.bosses());
		if (config.liveRecommendations())
		{
			fetchCatalog();
		}
		clientThread.invoke(() -> loadPending = client.getGameState() == GameState.LOGGED_IN);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
		icons.clear();
		clientThread.invoke(() ->
		{
			player = null;
			loadPending = false;
			knownLevels.clear();
		});
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			// Stats and the player's name arrive over the next tick or two.
			loadPending = true;
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			player = null;
			ui(p -> p.setLoggedIn(false));
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		ticks++;
		String name = loadPending ? account.name() : null;
		if (name != null)
		{
			loadPending = false;
			// World hops log in again; only a different account needs loading.
			if (!name.equals(player))
			{
				player = name;
				loadAccount();
			}
		}
		if (player == null)
		{
			return;
		}
		if (ticks % PROGRESS_TICKS == 0)
		{
			updateProgress();
		}
		if (gearStale && ticks - lastGearFetch >= GEAR_REFRESH_TICKS)
		{
			fetchGear();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (player == null)
		{
			return;
		}
		Integer known = knownLevels.put(event.getSkill(), event.getLevel());
		if (known != null && known != event.getLevel())
		{
			gearStale = true;
		}
		if (event.getSkill() == goalSkill)
		{
			updateProgress();
			if (event.getLevel() != planLevel)
			{
				fetchPlan();
			}
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int id = event.getContainerId();
		if (id != InventoryID.BANK && id != InventoryID.INV && id != InventoryID.WORN)
		{
			return;
		}
		if (id == InventoryID.BANK)
		{
			owned.bankChanged(event.getItemContainer());
		}
		if (player == null)
		{
			return;
		}
		if (id != InventoryID.WORN)
		{
			long coins = owned.coins();
			ui(p -> p.setCoins(coins));
		}
		if (gearBoss != null && config.liveRecommendations() && !owned.gear(catalog).equals(setupGear))
		{
			gearStale = true;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!LazyScapingConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		if ("liveRecommendations".equals(event.getKey()))
		{
			boolean live = config.liveRecommendations();
			ui(p -> p.setLiveEnabled(live));
			if (live)
			{
				fetchCatalog();
			}
			clientThread.invoke(() ->
			{
				planLevel = -1;
				fetchPlan();
				fetchGear();
			});
		}
		else if ("priority".equals(event.getKey()))
		{
			clientThread.invoke(this::fetchPlan);
		}
	}

	// Panel actions. These arrive on the Swing thread, and go to the client thread to read the game.

	@Override
	public void goalChanged(Skill skill, int targetLevel)
	{
		clientThread.invoke(() ->
		{
			if (player == null)
			{
				return;
			}
			int target = targetLevel;
			if (skill != null && skill != goalSkill)
			{
				// A new skill starts one level up, progress counts from now, and an
				// earlier picked method belonged to the old skill.
				target = Math.min(99, account.level(skill) + 1);
				goalFromXp = account.xp(skill);
				goalMethod = null;
				settings.set(ProfileSettings.GOAL_METHOD, null);
				int shown = target;
				ui(p -> p.setGoal(skill, shown));
			}
			goalSkill = skill;
			goalLevel = target;
			settings.set(ProfileSettings.GOAL_SKILL, skill == null ? null : skill.getName());
			if (skill != null)
			{
				settings.set(ProfileSettings.GOAL_LEVEL, goalLevel);
				settings.set(ProfileSettings.GOAL_FROM_XP, goalFromXp);
			}
			planLevel = -1;
			updateProgress();
			fetchPlan();
		});
	}

	@Override
	public void methodPicked(String methodId)
	{
		clientThread.invoke(() ->
		{
			goalMethod = methodId;
			if (player != null)
			{
				settings.set(ProfileSettings.GOAL_METHOD, methodId);
			}
			fetchPlan();
		});
	}

	@Override
	public void bossSelected(String bossId, String style)
	{
		clientThread.invoke(() ->
		{
			gearBoss = bossId;
			gearStyle = style;
			if (player != null)
			{
				settings.set(ProfileSettings.GEAR_BOSS, bossId);
			}
			fetchGear();
		});
	}

	@Override
	public void budgetChanged(boolean gear, String budget)
	{
		clientThread.invoke(() ->
		{
			// An editable picker reports the same choice more than once.
			if (budget.equals(gear ? gearBudget : trainingBudget))
			{
				return;
			}
			if (gear)
			{
				gearBudget = budget;
			}
			else
			{
				trainingBudget = budget;
			}
			if (player != null)
			{
				settings.set(gear ? ProfileSettings.GEAR_BUDGET : ProfileSettings.TRAINING_BUDGET, budget);
			}
			if (gear)
			{
				fetchGear();
			}
			else
			{
				fetchPlan();
			}
		});
	}

	@Override
	public void refresh()
	{
		clientThread.invoke(() ->
		{
			if (player == null)
			{
				return;
			}
			planLevel = -1;
			updateProgress();
			fetchPlan();
			fetchGear();
		});
	}

	@Override
	public void openPlanner()
	{
		clientThread.invoke(this::openSync);
	}

	@Override
	public void openBoss(String bossId)
	{
		clientThread.invoke(() ->
		{
			HttpUrl.Builder url = LazyScapingApi.SITE.newBuilder().addPathSegment("bosses").addQueryParameter("boss", bossId);
			if (player != null)
			{
				url.addQueryParameter("player", player);
				String type = account.hiscoresType();
				if (!"normal".equals(type))
				{
					url.addQueryParameter("type", type);
				}
			}
			LinkBrowser.browse(url.build().toString());
		});
	}

	@Override
	public void openUrl(String url)
	{
		// Only the OSRS Wiki links the site's guides carry.
		HttpUrl parsed = HttpUrl.parse(url);
		if (parsed != null && parsed.isHttps() && parsed.host().equals(WIKI_HOST))
		{
			LinkBrowser.browse(parsed.toString());
		}
	}

	// Everything below runs on the client thread.

	private void loadAccount()
	{
		knownLevels.clear();
		for (Skill skill : Account.SKILLS)
		{
			knownLevels.put(skill, account.level(skill));
		}
		owned.load();
		goalSkill = Account.skillNamed(settings.get(ProfileSettings.GOAL_SKILL));
		goalLevel = (int) settings.getLong(ProfileSettings.GOAL_LEVEL, 99);
		goalFromXp = settings.getLong(ProfileSettings.GOAL_FROM_XP, 0);
		goalMethod = settings.get(ProfileSettings.GOAL_METHOD);
		gearBoss = settings.get(ProfileSettings.GEAR_BOSS);
		gearStyle = null;
		trainingBudget = savedBudget(ProfileSettings.TRAINING_BUDGET, DEFAULT_TRAINING_BUDGET);
		gearBudget = savedBudget(ProfileSettings.GEAR_BUDGET, DEFAULT_GEAR_BUDGET);

		Skill skill = goalSkill;
		int level = goalLevel;
		String boss = gearBoss;
		String training = trainingBudget;
		String gear = gearBudget;
		long coins = owned.coins();
		ui(p ->
		{
			p.setLoggedIn(true);
			p.setChoices(skill, level, boss, training, gear);
			p.setCoins(coins);
		});
		planLevel = -1;
		setupGear = Set.of();
		updateProgress();
		fetchPlan();
		fetchGear();
	}

	private String savedBudget(String key, String fallback)
	{
		String saved = settings.get(key);
		return Budget.isValid(saved) ? saved : fallback;
	}

	private void updateProgress()
	{
		if (goalSkill == null)
		{
			ui(p -> p.showProgress(null));
			return;
		}
		LazyScapingPanel.Progress progress = new LazyScapingPanel.Progress();
		progress.level = account.level(goalSkill);
		progress.xp = account.xp(goalSkill);
		progress.targetXp = Experience.getXpForLevel(goalLevel);
		// Goals set before any xp was tracked count from the start of the current level.
		progress.fromXp = goalFromXp > 0 && goalFromXp <= progress.xp ? goalFromXp : Experience.getXpForLevel(Math.max(1, progress.level));
		progress.xpHr = xpTrackerService.getXpHr(goalSkill);
		ui(p -> p.showProgress(progress));
	}

	private void fetchPlan()
	{
		if (goalSkill == null)
		{
			ui(p -> p.showPlanMessage(null));
			return;
		}
		if (player == null || !config.liveRecommendations())
		{
			return;
		}
		planLevel = account.level(goalSkill);
		JsonObject body = new JsonObject();
		body.addProperty("skill", goalSkill.getName());
		body.addProperty("xp", account.xp(goalSkill));
		body.addProperty("target", goalLevel);
		body.add("levels", account.levels());
		body.addProperty("members", account.members());
		body.addProperty("ironman", account.ironman());
		JsonArray unlocks = new JsonArray();
		account.questUnlocks().forEach((key, done) ->
		{
			if (done)
			{
				unlocks.add(key);
			}
		});
		body.add("unlocks", unlocks);
		body.addProperty("priority", config.priority().apiValue);
		body.add("budget", budget(trainingBudget));
		if (goalMethod != null)
		{
			body.addProperty("method", goalMethod);
		}

		ui(p -> p.showPlanMessage("Loading your plan..."));
		api.plan(body, plan -> ui(p -> p.showPlan(plan)), error -> ui(p -> p.showPlanMessage(error)));
	}

	private void fetchGear()
	{
		gearStale = false;
		if (gearBoss == null)
		{
			ui(p -> p.showGearMessage(null));
			return;
		}
		if (player == null || !config.liveRecommendations())
		{
			return;
		}
		lastGearFetch = ticks;
		setupGear = owned.gear(catalog);
		JsonObject body = new JsonObject();
		body.addProperty("boss", gearBoss);
		if (gearStyle != null)
		{
			body.addProperty("style", gearStyle);
		}
		body.add("levels", account.levels());
		body.add("owned", gson.toJsonTree(setupGear));
		body.addProperty("members", account.members());
		body.add("budget", budget(gearBudget));
		body.addProperty("ironman", account.ironman());

		ui(p -> p.showGearMessage("Building your setup..."));
		api.gear(body, setup -> ui(p -> p.showGear(setup)), error -> ui(p -> p.showGearMessage(error)));
	}

	/** A budget for the API: gp, or null for unlimited. */
	private JsonElement budget(String budget)
	{
		Long gp = Budget.resolve(budget, owned.coins());
		return gp == null ? JsonNull.INSTANCE : new JsonPrimitive(gp);
	}

	/** The site's current gear list and bosses, in place of the bundled copy. */
	private void fetchCatalog()
	{
		api.catalog(c ->
		{
			catalog = c;
			// An older catalog has no bosses; keep the bundled ones then.
			if (!c.bosses().isEmpty())
			{
				ui(p -> p.setBossOptions(c.bosses()));
			}
		});
	}

	/**
	 * Opens lazyscaping.app with this account's name, type, gear and quest
	 * unlocks filled in. They go after the # in the link, which browsers never
	 * send to the server: the site saves them in the browser (src/app/sync).
	 */
	private void openSync()
	{
		JsonObject payload = new JsonObject();
		payload.addProperty("v", 1);
		payload.addProperty("to", "training");
		if (player != null)
		{
			payload.addProperty("rsn", player);
			payload.addProperty("type", account.hiscoresType());
			payload.addProperty("members", account.members());
			payload.add("gear", gson.toJsonTree(owned.gear(catalog)));
			payload.add("quests", gson.toJsonTree(account.questUnlocks()));
			if (goalSkill != null)
			{
				payload.addProperty("skill", goalSkill.getName());
				payload.addProperty("target", goalLevel);
			}
		}
		String data = Base64.getUrlEncoder().withoutPadding().encodeToString(gson.toJson(payload).getBytes(StandardCharsets.UTF_8));
		LinkBrowser.browse(LazyScapingApi.SITE.newBuilder().addPathSegment("sync").fragment("d=" + data).build().toString());
	}

	/** Updates the panel on the Swing thread, unless the plugin has stopped since. */
	private void ui(Consumer<LazyScapingPanel> update)
	{
		SwingUtilities.invokeLater(() ->
		{
			LazyScapingPanel p = panel;
			if (p != null)
			{
				update.accept(p);
			}
		});
	}
}
