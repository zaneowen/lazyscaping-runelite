package com.lazyscaping;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
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
	description = "Your training goal, plan and boss suggestions from lazyscaping.app, and one-click sync of your gear and quests",
	tags = {"training", "goal", "planner", "xp", "boss", "gear", "bank"}
)
@PluginDependency(XpTrackerPlugin.class)
public class LazyScapingPlugin extends Plugin implements LazyScapingPanel.Actions
{
	// Saved per account (RuneScape profile).
	private static final String BANK_KEY = "bankItems";
	private static final String GOAL_SKILL_KEY = "goalSkill";
	private static final String GOAL_LEVEL_KEY = "goalLevel";
	private static final String GOAL_FROM_KEY = "goalFromXp";
	private static final String GEAR_BOSS_KEY = "gearBoss";

	/** Skills with levels. Overall is deprecated in the API, so it's left out by name. */
	static final List<Skill> SKILLS = Arrays.stream(Skill.values())
		.filter(s -> !"Overall".equals(s.getName()))
		.collect(Collectors.toUnmodifiableList());

	/** How often the goal's xp/hr and time left update, in game ticks (3 seconds). */
	private static final int PROGRESS_TICKS = 5;
	/** Bosses are fetched again after a level-up or new gear, at most this often, in game ticks (a minute). */
	private static final int BOSS_REFRESH_TICKS = 100;

	/** The site's planner unlocks (see UNLOCKS in src/lib/methods.ts) that come from finishing a quest. */
	private static final Map<String, Quest> QUEST_UNLOCKS = new LinkedHashMap<>();

	static
	{
		QUEST_UNLOCKS.put("mm2", Quest.MONKEY_MADNESS_II);
		QUEST_UNLOCKS.put("dt1", Quest.DESERT_TREASURE_I);
		QUEST_UNLOCKS.put("lunar", Quest.LUNAR_DIPLOMACY);
		QUEST_UNLOCKS.put("sote", Quest.SONG_OF_THE_ELVES);
		QUEST_UNLOCKS.put("sotf", Quest.SINS_OF_THE_FATHER);
		QUEST_UNLOCKS.put("feud", Quest.THE_FEUD);
		QUEST_UNLOCKS.put("tote", Quest.TEMPLE_OF_THE_EYE);
		QUEST_UNLOCKS.put("boneVoyage", Quest.BONE_VOYAGE);
		QUEST_UNLOCKS.put("familyCrest", Quest.FAMILY_CREST);
		QUEST_UNLOCKS.put("touristTrap", Quest.THE_TOURIST_TRAP);
		QUEST_UNLOCKS.put("taiBwo", Quest.TAI_BWO_WANNAI_TRIO);
		QUEST_UNLOCKS.put("ghostsAhoy", Quest.GHOSTS_AHOY);
		QUEST_UNLOCKS.put("troubledTortugans", Quest.TROUBLED_TORTUGANS);
	}

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private XpTrackerService xpTrackerService;

	@Inject
	private LazyScapingConfig config;

	@Inject
	private LazyScapingApi api;

	@Inject
	private Gson gson;

	private LazyScapingPanel panel;
	private NavigationButton navButton;
	/** Replaced by the site's current catalog when live recommendations are on. */
	private volatile GearCatalog catalog;

	// Everything below is only touched on the client thread.
	/** The logged-in player, once their stats have loaded. */
	private String player;
	private boolean loadPending;
	private int ticks;
	private Skill goalSkill;
	private int goalLevel;
	private long goalFromXp;
	/** The goal skill's level when the plan was fetched, so a level-up fetches it again. */
	private int planLevel = -1;
	private final Map<Skill, Integer> knownLevels = new EnumMap<>(Skill.class);
	/** The boss picked for the gear setup, and the style picked for it (null: the best for your gear). */
	private String gearBoss;
	private String gearStyle;
	/** The gear the boss suggestions were fetched with. */
	private Set<String> bossGear = Set.of();
	private boolean bossesStale;
	private int lastBossFetch = -BOSS_REFRESH_TICKS;

	@Provides
	LazyScapingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LazyScapingConfig.class);
	}

	@Override
	protected void startUp()
	{
		catalog = GearCatalog.bundled(gson);
		panel = new LazyScapingPanel(this);
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
			ui(() -> panel.setLoggedIn(false));
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		ticks++;
		if (loadPending && client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null)
		{
			loadPending = false;
			String name = rsn();
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
		if (bossesStale && ticks - lastBossFetch >= BOSS_REFRESH_TICKS)
		{
			fetchBosses();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (player == null)
		{
			return;
		}
		Skill skill = event.getSkill();
		Integer known = knownLevels.put(skill, event.getLevel());
		boolean levelledUp = known != null && known != event.getLevel();
		if (levelledUp)
		{
			bossesStale = true;
		}
		if (skill == goalSkill)
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
		if (id == InventoryID.BANK)
		{
			saveBank(event.getItemContainer());
		}
		if (player != null && config.liveRecommendations() && (id == InventoryID.BANK || id == InventoryID.WORN || id == InventoryID.INV) && !ownedGear().equals(bossGear))
		{
			bossesStale = true;
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
			ui(() -> panel.setLiveEnabled(live));
			if (live)
			{
				fetchCatalog();
			}
			clientThread.invoke(() ->
			{
				planLevel = -1;
				fetchPlan();
				fetchBosses();
			});
		}
		else if ("priority".equals(event.getKey()))
		{
			clientThread.invoke(this::fetchPlan);
		}
	}

	// Panel actions. These arrive on the Swing thread.

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
			if (skill != goalSkill && skill != null)
			{
				// A new skill starts one level up, and progress counts from now.
				target = Math.min(99, client.getRealSkillLevel(skill) + 1);
				goalFromXp = client.getSkillExperience(skill);
				int shown = target;
				ui(() -> panel.setGoal(skill, shown));
			}
			goalSkill = skill;
			goalLevel = target;
			saveGoal();
			planLevel = -1;
			updateProgress();
			fetchPlan();
		});
	}

	@Override
	public void openTraining()
	{
		clientThread.invoke(() -> openSite("training"));
	}

	@Override
	public void openBosses()
	{
		clientThread.invoke(() -> openSite("bosses"));
	}

	@Override
	public void openBoss(String bossId)
	{
		HttpUrl.Builder url = LazyScapingApi.SITE.newBuilder().addPathSegment("bosses").addQueryParameter("boss", bossId);
		clientThread.invoke(() ->
		{
			if (player != null)
			{
				url.addQueryParameter("player", player);
				String type = accountType();
				if (!"normal".equals(type))
				{
					url.addQueryParameter("type", type);
				}
			}
			LinkBrowser.browse(url.build().toString());
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
				if (bossId == null)
				{
					configManager.unsetRSProfileConfiguration(LazyScapingConfig.GROUP, GEAR_BOSS_KEY);
				}
				else
				{
					configManager.setRSProfileConfiguration(LazyScapingConfig.GROUP, GEAR_BOSS_KEY, bossId);
				}
			}
			fetchGear();
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
			fetchBosses();
		});
	}

	// Everything below runs on the client thread.

	private void loadAccount()
	{
		knownLevels.clear();
		for (Skill skill : SKILLS)
		{
			knownLevels.put(skill, client.getRealSkillLevel(skill));
		}
		goalSkill = skillNamed(configManager.getRSProfileConfiguration(LazyScapingConfig.GROUP, GOAL_SKILL_KEY));
		goalLevel = parseInt(configManager.getRSProfileConfiguration(LazyScapingConfig.GROUP, GOAL_LEVEL_KEY), 99);
		goalFromXp = parseInt(configManager.getRSProfileConfiguration(LazyScapingConfig.GROUP, GOAL_FROM_KEY), 0);

		gearBoss = configManager.getRSProfileConfiguration(LazyScapingConfig.GROUP, GEAR_BOSS_KEY);
		gearStyle = null;

		Skill skill = goalSkill;
		int level = goalLevel;
		String boss = gearBoss;
		ui(() ->
		{
			panel.setLoggedIn(true);
			panel.setGoal(skill, level);
			panel.selectBoss(boss);
		});
		planLevel = -1;
		bossGear = Set.of();
		updateProgress();
		fetchPlan();
		fetchBosses();
	}

	private void saveGoal()
	{
		if (goalSkill == null)
		{
			configManager.unsetRSProfileConfiguration(LazyScapingConfig.GROUP, GOAL_SKILL_KEY);
			return;
		}
		configManager.setRSProfileConfiguration(LazyScapingConfig.GROUP, GOAL_SKILL_KEY, goalSkill.getName());
		configManager.setRSProfileConfiguration(LazyScapingConfig.GROUP, GOAL_LEVEL_KEY, goalLevel);
		configManager.setRSProfileConfiguration(LazyScapingConfig.GROUP, GOAL_FROM_KEY, goalFromXp);
	}

	private void updateProgress()
	{
		if (goalSkill == null)
		{
			ui(() -> panel.showProgress(null));
			return;
		}
		LazyScapingPanel.Progress p = new LazyScapingPanel.Progress();
		p.level = client.getRealSkillLevel(goalSkill);
		p.xp = client.getSkillExperience(goalSkill);
		p.targetXp = Experience.getXpForLevel(goalLevel);
		// Goals set before any xp was tracked count from the start of the current level.
		p.fromXp = goalFromXp > 0 && goalFromXp <= p.xp ? goalFromXp : Experience.getXpForLevel(Math.max(1, p.level));
		p.xpHr = xpTrackerService.getXpHr(goalSkill);
		ui(() -> panel.showProgress(p));
	}

	private void fetchPlan()
	{
		if (goalSkill == null)
		{
			ui(() -> panel.showPlanMessage(null));
			return;
		}
		if (player == null || !config.liveRecommendations())
		{
			return;
		}
		planLevel = client.getRealSkillLevel(goalSkill);
		JsonObject body = new JsonObject();
		body.addProperty("skill", goalSkill.getName());
		body.addProperty("xp", client.getSkillExperience(goalSkill));
		body.addProperty("target", goalLevel);
		body.add("levels", levels());
		body.addProperty("members", members());
		body.addProperty("ironman", client.getVarbitValue(VarbitID.IRONMAN) != 0);
		JsonArray unlocks = new JsonArray();
		questUnlocks().forEach((key, done) ->
		{
			if (done)
			{
				unlocks.add(key);
			}
		});
		body.add("unlocks", unlocks);
		body.addProperty("priority", config.priority().apiValue);

		ui(() -> panel.showPlanMessage("Loading your plan..."));
		api.plan(body, plan -> ui(() -> panel.showPlan(plan)), error -> ui(() -> panel.showPlanMessage(error)));
	}

	private void fetchBosses()
	{
		bossesStale = false;
		if (player == null || !config.liveRecommendations())
		{
			return;
		}
		lastBossFetch = ticks;
		bossGear = ownedGear();
		JsonObject body = new JsonObject();
		body.add("levels", levels());
		body.add("owned", gson.toJsonTree(bossGear));
		body.addProperty("members", members());

		ui(() -> panel.showBossesMessage("Finding bosses..."));
		api.bosses(body, bosses -> ui(() -> panel.showBosses(bosses)), error -> ui(() -> panel.showBossesMessage(error)));
		// The gear setup depends on the same levels and gear.
		fetchGear();
	}

	private void fetchGear()
	{
		if (gearBoss == null)
		{
			ui(() -> panel.showGearMessage(null));
			return;
		}
		if (player == null || !config.liveRecommendations())
		{
			return;
		}
		JsonObject body = new JsonObject();
		body.addProperty("boss", gearBoss);
		if (gearStyle != null)
		{
			body.addProperty("style", gearStyle);
		}
		body.add("levels", levels());
		body.add("owned", gson.toJsonTree(bossGear));
		body.addProperty("members", members());

		ui(() -> panel.showGearMessage("Building your setup..."));
		api.gear(body, setup -> ui(() -> panel.showGear(setup)), error -> ui(() -> panel.showGearMessage(error)));
	}

	/** The site's current gear list and bosses, in place of the bundled copy. */
	private void fetchCatalog()
	{
		api.catalog(c ->
		{
			catalog = c;
			ui(() -> panel.setBossOptions(c.bosses()));
		});
	}

	/**
	 * Opens lazyscaping.app with this account's name, type, gear and quest
	 * unlocks filled in. They go after the # in the link, which browsers never
	 * send to the server: the site saves them in the browser (src/app/sync).
	 */
	private void openSite(String to)
	{
		JsonObject payload = new JsonObject();
		payload.addProperty("v", 1);
		payload.addProperty("to", to);
		if (player != null)
		{
			payload.addProperty("rsn", player);
			payload.addProperty("type", accountType());
			payload.addProperty("members", members());
			payload.add("gear", gson.toJsonTree(ownedGear()));
			payload.add("quests", gson.toJsonTree(questUnlocks()));
			if (goalSkill != null)
			{
				payload.addProperty("skill", goalSkill.getName());
				payload.addProperty("target", goalLevel);
			}
		}
		String data = Base64.getUrlEncoder().withoutPadding().encodeToString(gson.toJson(payload).getBytes(StandardCharsets.UTF_8));
		LinkBrowser.browse(LazyScapingApi.SITE.newBuilder().addPathSegment("sync").fragment("d=" + data).build().toString());
	}

	/** Remembers what's in the bank, since the client only has it while the bank is open. */
	private void saveBank(ItemContainer bank)
	{
		if (bank == null)
		{
			return;
		}
		Set<Integer> ids = new TreeSet<>();
		for (Item item : bank.getItems())
		{
			if (item.getId() > 0)
			{
				ids.add(itemManager.canonicalize(item.getId()));
			}
		}
		String value = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
		if (!value.equals(configManager.getRSProfileConfiguration(LazyScapingConfig.GROUP, BANK_KEY)))
		{
			configManager.setRSProfileConfiguration(LazyScapingConfig.GROUP, BANK_KEY, value);
		}
	}

	/** Gear the planners know about in the bank (as last seen), worn, or in the inventory. */
	private Set<String> ownedGear()
	{
		Set<Integer> ids = new HashSet<>();
		String bank = configManager.getRSProfileConfiguration(LazyScapingConfig.GROUP, BANK_KEY);
		if (bank != null && !bank.isEmpty())
		{
			for (String id : bank.split(","))
			{
				ids.add(parseInt(id, -1));
			}
		}
		for (int containerId : new int[]{InventoryID.WORN, InventoryID.INV})
		{
			ItemContainer container = client.getItemContainer(containerId);
			if (container != null)
			{
				for (Item item : container.getItems())
				{
					ids.add(itemManager.canonicalize(item.getId()));
				}
			}
		}
		Set<String> names = new HashSet<>();
		for (int id : ids)
		{
			if (id > 0)
			{
				names.add(itemManager.getItemComposition(id).getName());
			}
		}
		return catalog.matchAll(names);
	}

	private JsonObject levels()
	{
		JsonObject levels = new JsonObject();
		for (Skill skill : SKILLS)
		{
			levels.addProperty(skill.getName(), client.getRealSkillLevel(skill));
		}
		return levels;
	}

	static Skill skillNamed(String name)
	{
		for (Skill skill : SKILLS)
		{
			if (skill.getName().equals(name))
			{
				return skill;
			}
		}
		return null;
	}

	private Map<String, Boolean> questUnlocks()
	{
		Map<String, Boolean> out = new LinkedHashMap<>();
		QUEST_UNLOCKS.forEach((key, quest) -> out.put(key, quest.getState(client) == QuestState.FINISHED));
		return out;
	}

	/** On a members world, or with membership days left. */
	private boolean members()
	{
		return client.getWorldType().contains(WorldType.MEMBERS) || client.getVarpValue(VarPlayerID.ACCOUNT_CREDIT) > 0;
	}

	/** The hiscores the account is on, as the site names them. Group ironmen are on the normal hiscores. */
	private String accountType()
	{
		switch (client.getVarbitValue(VarbitID.IRONMAN))
		{
			case 1:
				return "ironman";
			case 2:
				return "ultimate";
			case 3:
				return "hardcore";
			default:
				return "normal";
		}
	}

	private String rsn()
	{
		// Names with spaces come through with non-breaking spaces.
		return client.getLocalPlayer().getName().replace('\u00A0', ' ');
	}

	/** Runs on the Swing thread, unless the plugin has been turned off since. */
	private void ui(Runnable update)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				update.run();
			}
		});
	}

	private static int parseInt(String value, int fallback)
	{
		try
		{
			return value == null ? fallback : Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}
}
