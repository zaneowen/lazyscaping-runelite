package com.lazyscaping;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Provides;
import com.lazyscaping.api.LazyScapingClient;
import com.lazyscaping.ui.GoalProgress;
import com.lazyscaping.ui.LazyScapingPanel;
import com.lazyscaping.ui.PanelActions;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@PluginDescriptor(
	name = "LazyScaping",
	description = "Your training goal, plan and methods, and boss gear setups from lazyscaping.app, with one-click sync of your gear and quests",
	tags = {"training", "goal", "planner", "xp", "boss", "gear", "bank"}
)
@PluginDependency(XpTrackerPlugin.class)
public class LazyScapingPlugin extends Plugin implements PanelActions
{
	private static final String DEFAULT_TRAINING_BUDGET = "1M";
	private static final String DEFAULT_GEAR_BUDGET = Budget.FREE;
	private static final int MAX_LEVEL = 99;
	/** Game ticks between goal progress updates. */
	private static final int PROGRESS_UPDATE_TICKS = 5;
	/** Minimum game ticks between gear setup refreshes after the player's gear or levels change. */
	private static final int GEAR_REFRESH_TICKS = 100;
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
	private LazyScapingClient api;

	@Inject
	private Account account;

	@Inject
	private ProfileSettings settings;

	@Inject
	private OwnedItems ownedItems;

	@Inject
	private ItemIconLoader iconLoader;

	@Inject
	private Gson gson;

	private volatile LazyScapingPanel panel;
	private NavigationButton navButton;
	private volatile GearCatalog catalog;

	// Client thread state
	private String playerName;
	private boolean loadPending;
	private int tickCount;
	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);

	private Skill goalSkill;
	private int goalLevel;
	private long goalStartXp;
	private String goalMethod;
	/** The goal skill's level when the plan was last fetched. */
	private int planLevel = -1;

	private String gearBoss;
	private String gearStyle;
	/** The owned gear the current setup was fetched with. */
	private Set<String> setupGear = Collections.emptySet();
	private boolean setupStale;
	private int lastSetupFetchTick = -GEAR_REFRESH_TICKS;

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
		panel = new LazyScapingPanel(this, iconLoader);
		panel.setLiveEnabled(config.liveRecommendations());
		panel.setBossOptions(catalog.getBosses());

		navButton = NavigationButton.builder()
			.tooltip("LazyScaping")
			.icon(ImageUtil.loadImageResource(getClass(), "panel_icon.png"))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

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
		iconLoader.clear();
		clientThread.invoke(() ->
		{
			playerName = null;
			loadPending = false;
			levels.clear();
		});
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			// the player's name and stats arrive over the next tick or two
			loadPending = true;
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			playerName = null;
			updatePanel(p -> p.setLoggedIn(false));
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		tickCount++;
		if (loadPending)
		{
			String name = account.name();
			if (name != null)
			{
				loadPending = false;
				// world hops log in again, but only a different account needs loading
				if (!name.equals(playerName))
				{
					playerName = name;
					loadAccount();
				}
			}
		}

		if (playerName == null)
		{
			return;
		}
		if (tickCount % PROGRESS_UPDATE_TICKS == 0)
		{
			updateProgress();
		}
		if (setupStale && tickCount - lastSetupFetchTick >= GEAR_REFRESH_TICKS)
		{
			fetchGearSetup();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (playerName == null)
		{
			return;
		}

		Integer previous = levels.put(event.getSkill(), event.getLevel());
		if (previous != null && previous != event.getLevel())
		{
			setupStale = true;
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
		int containerId = event.getContainerId();
		if (containerId != InventoryID.BANK && containerId != InventoryID.INV && containerId != InventoryID.WORN)
		{
			return;
		}

		if (containerId == InventoryID.BANK)
		{
			ownedItems.onBankChanged(event.getItemContainer());
		}
		if (playerName == null)
		{
			return;
		}
		if (containerId != InventoryID.WORN)
		{
			long coins = ownedItems.coins();
			updatePanel(p -> p.setCoins(coins));
		}
		if (gearBoss != null && config.liveRecommendations() && !ownedItems.gear(catalog).equals(setupGear))
		{
			setupStale = true;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!LazyScapingConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		switch (event.getKey())
		{
			case "liveRecommendations":
				boolean live = config.liveRecommendations();
				updatePanel(p -> p.setLiveEnabled(live));
				if (live)
				{
					fetchCatalog();
				}
				clientThread.invoke(() ->
				{
					planLevel = -1;
					fetchPlan();
					fetchGearSetup();
				});
				break;
			case "priority":
				clientThread.invoke(this::fetchPlan);
				break;
			default:
				break;
		}
	}

	@Override
	public void goalChanged(Skill skill, int targetLevel)
	{
		clientThread.invoke(() ->
		{
			if (playerName == null)
			{
				return;
			}

			int target = targetLevel;
			if (skill != null && skill != goalSkill)
			{
				// a new goal starts one level up, counts progress from now, and drops the old skill's method
				target = Math.min(MAX_LEVEL, account.level(skill) + 1);
				goalStartXp = account.xp(skill);
				goalMethod = null;
				settings.set(ProfileSettings.GOAL_METHOD, null);
				int shownTarget = target;
				updatePanel(p -> p.setGoal(skill, shownTarget));
			}

			goalSkill = skill;
			goalLevel = target;
			settings.set(ProfileSettings.GOAL_SKILL, skill == null ? null : skill.getName());
			if (skill != null)
			{
				settings.set(ProfileSettings.GOAL_LEVEL, goalLevel);
				settings.set(ProfileSettings.GOAL_FROM_XP, goalStartXp);
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
			if (playerName != null)
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
			if (playerName != null)
			{
				settings.set(ProfileSettings.GEAR_BOSS, bossId);
			}
			fetchGearSetup();
		});
	}

	@Override
	public void budgetChanged(boolean gear, String budget)
	{
		clientThread.invoke(() ->
		{
			// editable combo boxes report the same value more than once
			if (budget.equals(gear ? gearBudget : trainingBudget))
			{
				return;
			}

			if (gear)
			{
				gearBudget = budget;
				saveIfLoggedIn(ProfileSettings.GEAR_BUDGET, budget);
				fetchGearSetup();
			}
			else
			{
				trainingBudget = budget;
				saveIfLoggedIn(ProfileSettings.TRAINING_BUDGET, budget);
				fetchPlan();
			}
		});
	}

	@Override
	public void refresh()
	{
		clientThread.invoke(() ->
		{
			if (playerName == null)
			{
				return;
			}
			planLevel = -1;
			updateProgress();
			fetchPlan();
			fetchGearSetup();
		});
	}

	@Override
	public void openPlanner()
	{
		clientThread.invoke(this::openSync);
	}

	@Override
	public void openBossPlan(String bossId)
	{
		clientThread.invoke(() ->
		{
			HttpUrl.Builder url = LazyScapingClient.SITE.newBuilder()
				.addPathSegment("bosses")
				.addQueryParameter("boss", bossId);
			if (playerName != null)
			{
				url.addQueryParameter("player", playerName);
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
	public void openWiki(String url)
	{
		HttpUrl parsed = HttpUrl.parse(url);
		if (parsed != null && parsed.isHttps() && parsed.host().equals(WIKI_HOST))
		{
			LinkBrowser.browse(parsed.toString());
		}
	}

	private void loadAccount()
	{
		levels.clear();
		Skills.ALL.forEach(skill -> levels.put(skill, account.level(skill)));
		ownedItems.load();

		goalSkill = Skills.byName(settings.get(ProfileSettings.GOAL_SKILL));
		goalLevel = (int) settings.getLong(ProfileSettings.GOAL_LEVEL, MAX_LEVEL);
		goalStartXp = settings.getLong(ProfileSettings.GOAL_FROM_XP, 0);
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
		long coins = ownedItems.coins();
		updatePanel(p ->
		{
			p.setLoggedIn(true);
			p.setChoices(skill, level, boss, training, gear);
			p.setCoins(coins);
		});

		planLevel = -1;
		setupGear = Collections.emptySet();
		updateProgress();
		fetchPlan();
		fetchGearSetup();
	}

	private String savedBudget(String key, String defaultValue)
	{
		String saved = settings.get(key);
		return Budget.isValid(saved) ? saved : defaultValue;
	}

	private void saveIfLoggedIn(String key, String value)
	{
		if (playerName != null)
		{
			settings.set(key, value);
		}
	}

	private void updateProgress()
	{
		if (goalSkill == null)
		{
			updatePanel(p -> p.showProgress(null));
			return;
		}

		int level = account.level(goalSkill);
		int xp = account.xp(goalSkill);
		// goals saved before progress was tracked count from the start of the current level
		long startXp = goalStartXp > 0 && goalStartXp <= xp ? goalStartXp : Experience.getXpForLevel(Math.max(1, level));
		GoalProgress progress = new GoalProgress(level, xp, startXp, Experience.getXpForLevel(goalLevel), xpTrackerService.getXpHr(goalSkill));
		updatePanel(p -> p.showProgress(progress));
	}

	private void fetchPlan()
	{
		if (goalSkill == null)
		{
			updatePanel(p -> p.showPlanMessage(null));
			return;
		}
		if (playerName == null || !config.liveRecommendations())
		{
			return;
		}

		planLevel = account.level(goalSkill);
		JsonArray unlocks = new JsonArray();
		account.questUnlocks().forEach((id, done) ->
		{
			if (done)
			{
				unlocks.add(id);
			}
		});

		JsonObject body = new JsonObject();
		body.addProperty("skill", goalSkill.getName());
		body.addProperty("xp", account.xp(goalSkill));
		body.addProperty("target", goalLevel);
		body.add("levels", account.levels());
		body.addProperty("members", account.isMember());
		body.addProperty("ironman", account.isIronman());
		body.add("unlocks", unlocks);
		body.addProperty("priority", config.priority().getApiName());
		body.add("budget", budgetJson(trainingBudget));
		if (goalMethod != null)
		{
			body.addProperty("method", goalMethod);
		}

		updatePanel(p -> p.showPlanMessage("Loading your plan..."));
		api.plan(body, plan -> updatePanel(p -> p.showPlan(plan)), error -> updatePanel(p -> p.showPlanMessage(error)));
	}

	private void fetchGearSetup()
	{
		setupStale = false;
		if (gearBoss == null)
		{
			updatePanel(p -> p.showGearMessage(null));
			return;
		}
		if (playerName == null || !config.liveRecommendations())
		{
			return;
		}

		lastSetupFetchTick = tickCount;
		setupGear = ownedItems.gear(catalog);

		JsonObject body = new JsonObject();
		body.addProperty("boss", gearBoss);
		if (gearStyle != null)
		{
			body.addProperty("style", gearStyle);
		}
		body.add("levels", account.levels());
		body.add("owned", gson.toJsonTree(setupGear));
		body.addProperty("members", account.isMember());
		body.addProperty("ironman", account.isIronman());
		body.add("budget", budgetJson(gearBudget));

		updatePanel(p -> p.showGearMessage("Building your setup..."));
		api.gearSetup(body, setup -> updatePanel(p -> p.showGearSetup(setup)), error -> updatePanel(p -> p.showGearMessage(error)));
	}

	/** A budget as the API expects it: gp, or null for unlimited. */
	private JsonElement budgetJson(String budget)
	{
		Long gp = Budget.resolve(budget, ownedItems.coins());
		return gp == null ? JsonNull.INSTANCE : new JsonPrimitive(gp);
	}

	private void fetchCatalog()
	{
		api.catalog(response ->
		{
			GearCatalog updated = new GearCatalog(response);
			catalog = updated;
			// keep the bundled bosses if the response predates the boss list
			if (!updated.getBosses().isEmpty())
			{
				updatePanel(p -> p.setBossOptions(updated.getBosses()));
			}
		});
	}

	/**
	 * Opens lazyscaping.app with this account's details filled in. They're sent
	 * after the # in the link, which browsers never send to the server.
	 */
	private void openSync()
	{
		JsonObject payload = new JsonObject();
		payload.addProperty("v", 1);
		payload.addProperty("to", "training");
		if (playerName != null)
		{
			payload.addProperty("rsn", playerName);
			payload.addProperty("type", account.hiscoresType());
			payload.addProperty("members", account.isMember());
			payload.add("gear", gson.toJsonTree(ownedItems.gear(catalog)));
			payload.add("quests", gson.toJsonTree(account.questUnlocks()));
			if (goalSkill != null)
			{
				payload.addProperty("skill", goalSkill.getName());
				payload.addProperty("target", goalLevel);
			}
		}

		String data = Base64.getUrlEncoder().withoutPadding().encodeToString(gson.toJson(payload).getBytes(StandardCharsets.UTF_8));
		LinkBrowser.browse(LazyScapingClient.SITE.newBuilder().addPathSegment("sync").fragment("d=" + data).build().toString());
	}

	/** Updates the panel on the Swing thread, if the plugin is still running. */
	private void updatePanel(Consumer<LazyScapingPanel> update)
	{
		SwingUtilities.invokeLater(() ->
		{
			LazyScapingPanel current = panel;
			if (current != null)
			{
				update.accept(current);
			}
		});
	}
}
