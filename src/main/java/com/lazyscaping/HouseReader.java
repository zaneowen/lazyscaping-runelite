package com.lazyscaping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

/**
 * Reads what the player's own house has while they're in it, and ticks the
 * house settings to match: furniture from the objects in the house, and portal
 * nexus destinations from the nexus's teleport menu when it's opened.
 *
 * <p>The game doesn't say whose house is loaded, so a house only counts as the
 * player's own when they entered it a way that only leads there: the house
 * portal's Home or Build mode, a house tablet, the Teleport to House spell or a
 * Construction cape. Registered on the event bus while the plugin runs.
 */
@Slf4j
@Singleton
class HouseReader
{
	/** How long after choosing a way into their house the house may load, in ticks. Walking to the house portal takes a while. */
	private static final int ENTRY_TICKS = 30;
	private static final Pattern NOT_LETTERS = Pattern.compile("[^a-z ]+");
	private static final Pattern SPACES = Pattern.compile("\\s+");

	private static final Map<Integer, List<HouseTeleport>> FEATURES = new HashMap<>();
	private static final Set<Integer> NEXUSES = Set.of(ObjectID.POH_NEXUS_PORTAL_1, ObjectID.POH_NEXUS_PORTAL_2, ObjectID.POH_NEXUS_PORTAL_3);
	/** Names in the nexus's teleport menu, longest first so "West Ardougne" isn't read as "Ardougne". */
	private static final Map<String, HouseTeleport> NEXUS_NAMES = new LinkedHashMap<>();

	static
	{
		add(HouseTeleport.FAIRY_RING, ObjectID.POH_FAIRY_RING, ObjectID.POH_SPIRIT_RING, ObjectID.XMAS20_POH_SPIRIT_RING);
		add(HouseTeleport.SPIRIT_TREE, ObjectID.POH_SPIRIT_TREE, ObjectID.POH_SPIRIT_RING, ObjectID.XMAS20_POH_SPIRIT_TREE, ObjectID.XMAS20_POH_SPIRIT_RING);
		add(HouseTeleport.XERICS_TALISMAN, ObjectID.POH_AMULET_XERIC_LOOKOUT, ObjectID.POH_AMULET_XERIC_GLADE, ObjectID.POH_AMULET_XERIC_INFERNO,
			ObjectID.POH_AMULET_XERIC_HEART, ObjectID.POH_AMULET_XERIC_HONOUR, ObjectID.POH_AMULET_XERIC);
		add(HouseTeleport.DIGSITE_PENDANT, ObjectID.POH_AMULET_DIG_DIGSITE, ObjectID.POH_AMULET_DIG_FOSSIL, ObjectID.POH_AMULET_DIG_LITHKREN,
			ObjectID.POH_AMULET_DIGSITE);
		add(HouseTeleport.MOUNTED_GLORY, ObjectID.POH_TROPHY_AMULETOFGLORY_4);
		add(HouseTeleport.MYTHICAL_CAPE, ObjectID.POH_TROPHY_MYTHICAL_CAPE, ObjectID.POH_MOUNTED_MYTHICALCAPE);

		// basic, fancy and ornate jewellery boxes, each with the teleports of the ones before
		int[] boxes = {ObjectID.POH_JEWELLERY_BOX_1, ObjectID.POH_JEWELLERY_BOX_2, ObjectID.POH_JEWELLERY_BOX_3};
		add(HouseTeleport.RING_OF_DUELING, boxes);
		add(HouseTeleport.GAMES_NECKLACE, boxes);
		add(HouseTeleport.COMBAT_BRACELET, boxes[1], boxes[2]);
		add(HouseTeleport.SKILLS_NECKLACE, boxes[1], boxes[2]);
		add(HouseTeleport.RING_OF_WEALTH, boxes[2]);
		add(HouseTeleport.AMULET_OF_GLORY, boxes[2]);

		// portal chamber portals, in teak, mahogany and marble frames
		add(HouseTeleport.VARROCK_PORTAL, ObjectID.POH_PORTAL_TEAK_VARROCK, ObjectID.POH_PORTAL_MAG_VARROCK, ObjectID.POH_PORTAL_MARBLE_VARROCK);
		add(HouseTeleport.FALADOR_PORTAL, ObjectID.POH_PORTAL_TEAK_FALADOR, ObjectID.POH_PORTAL_MAG_FALADOR, ObjectID.POH_PORTAL_MARBLE_FALADOR);
		add(HouseTeleport.LUMBRIDGE_PORTAL, ObjectID.POH_PORTAL_TEAK_LUMBRIDGE, ObjectID.POH_PORTAL_MAG_LUMBRIDGE, ObjectID.POH_PORTAL_MARBLE_LUMBRIDGE);
		add(HouseTeleport.EAST_ARDOUGNE_PORTAL, ObjectID.POH_PORTAL_TEAK_ARDOUGNE, ObjectID.POH_PORTAL_MAG_ARDOUGNE, ObjectID.POH_PORTAL_MARBLE_ARDOUGNE);
		add(HouseTeleport.WATCHTOWER_PORTAL, ObjectID.POH_PORTAL_TEAK_YANILLE, ObjectID.POH_PORTAL_MAG_YANILLE, ObjectID.POH_PORTAL_MARBLE_YANILLE);
		add(HouseTeleport.CAMELOT_PORTAL, ObjectID.POH_PORTAL_TEAK_CAMELOT, ObjectID.POH_PORTAL_MAG_CAMELOT, ObjectID.POH_PORTAL_MARBLE_CAMELOT);
		add(HouseTeleport.LUNAR_ISLE_PORTAL, ObjectID.POH_PORTAL_TEAK_LUNARISLE, ObjectID.POH_PORTAL_MAG_LUNARISLE, ObjectID.POH_PORTAL_MARBLE_LUNARISLE);
		add(HouseTeleport.WATERBIRTH_PORTAL, ObjectID.POH_PORTAL_TEAK_WATERBIRTH, ObjectID.POH_PORTAL_MAG_WATERBIRTH, ObjectID.POH_PORTAL_MARBLE_WATERBIRTH);
		add(HouseTeleport.FISHING_GUILD_PORTAL, ObjectID.POH_PORTAL_TEAK_FISHINGGUILD, ObjectID.POH_PORTAL_MAG_FISHINGGUILD, ObjectID.POH_PORTAL_MARBLE_FISHINGGUILD);
		add(HouseTeleport.SENNTISTEN_PORTAL, ObjectID.POH_PORTAL_TEAK_SENNTISTEN, ObjectID.POH_PORTAL_MAG_SENNTISTEN, ObjectID.POH_PORTAL_MARBLE_SENNTISTEN);
		add(HouseTeleport.KHARYRLL_PORTAL, ObjectID.POH_PORTAL_TEAK_KHARYRLL, ObjectID.POH_PORTAL_MAG_KHARYRLL, ObjectID.POH_PORTAL_MARBLE_KHARYRLL);
		add(HouseTeleport.ANNAKARL_PORTAL, ObjectID.POH_PORTAL_TEAK_ANNAKARL, ObjectID.POH_PORTAL_MAG_ANNAKARL, ObjectID.POH_PORTAL_MARBLE_ANNAKARL);
		add(HouseTeleport.KOUREND_CASTLE_PORTAL, ObjectID.POH_PORTAL_TEAK_KOUREND, ObjectID.POH_PORTAL_MAG_KOUREND, ObjectID.POH_PORTAL_MARBLE_KOUREND);
		add(HouseTeleport.MARIM_PORTAL, ObjectID.POH_PORTAL_TEAK_MARIM, ObjectID.POH_PORTAL_MAG_MARIM, ObjectID.POH_PORTAL_MARBLE_MARIM);
		add(HouseTeleport.TROLL_STRONGHOLD_PORTAL, ObjectID.POH_PORTAL_TEAK_STRONGHOLD, ObjectID.POH_PORTAL_MAG_STRONGHOLD, ObjectID.POH_PORTAL_MARBLE_STRONGHOLD);
		add(HouseTeleport.GHORROCK_PORTAL, ObjectID.POH_PORTAL_TEAK_GHORROCK, ObjectID.POH_PORTAL_MAG_GHORROCK, ObjectID.POH_PORTAL_MARBLE_GHORROCK);
		add(HouseTeleport.CARRALLANGER_PORTAL, ObjectID.POH_PORTAL_TEAK_CARRALLANGAR, ObjectID.POH_PORTAL_MAG_CARRALLANGAR, ObjectID.POH_PORTAL_MARBLE_CARRALLANGAR);
		add(HouseTeleport.CATHERBY_PORTAL, ObjectID.POH_PORTAL_TEAK_CATHERBY, ObjectID.POH_PORTAL_MAG_CATHERBY, ObjectID.POH_PORTAL_MARBLE_CATHERBY);
		add(HouseTeleport.WEISS_PORTAL, ObjectID.POH_PORTAL_TEAK_WEISS, ObjectID.POH_PORTAL_MAG_WEISS, ObjectID.POH_PORTAL_MARBLE_WEISS);
		add(HouseTeleport.APE_ATOLL_DUNGEON_PORTAL, ObjectID.POH_PORTAL_TEAK_APE_ATOLL, ObjectID.POH_PORTAL_MAG_APE_ATOLL, ObjectID.POH_PORTAL_MARBLE_APE_ATOLL);
		add(HouseTeleport.BARROWS_PORTAL, ObjectID.POH_PORTAL_TEAK_BARROWS, ObjectID.POH_PORTAL_MAG_BARROWS, ObjectID.POH_PORTAL_MARBLE_BARROWS);
		add(HouseTeleport.BATTLEFRONT_PORTAL, ObjectID.POH_PORTAL_TEAK_BATTLEFRONT, ObjectID.POH_PORTAL_MAG_BATTLEFRONT, ObjectID.POH_PORTAL_MARBLE_BATTLEFRONT);
		add(HouseTeleport.FORGOTTEN_CEMETERY_PORTAL, ObjectID.POH_PORTAL_TEAK_CEMETERY, ObjectID.POH_PORTAL_MAG_CEMETERY, ObjectID.POH_PORTAL_MARBLE_CEMETERY);
		add(HouseTeleport.DRAYNOR_MANOR_PORTAL, ObjectID.POH_PORTAL_TEAK_DRAYNOR_MANOR, ObjectID.POH_PORTAL_MAG_DRAYNOR_MANOR, ObjectID.POH_PORTAL_MARBLE_DRAYNOR_MANOR);
		add(HouseTeleport.FENKENSTRAIN_PORTAL, ObjectID.POH_PORTAL_TEAK_FENKENSTRAIN, ObjectID.POH_PORTAL_MAG_FENKENSTRAIN, ObjectID.POH_PORTAL_MARBLE_FENKENSTRAIN);
		add(HouseTeleport.HARMONY_ISLAND_PORTAL, ObjectID.POH_PORTAL_TEAK_HARMONY_ISLAND, ObjectID.POH_PORTAL_MAG_HARMONY_ISLAND, ObjectID.POH_PORTAL_MARBLE_HARMONY_ISLAND);
		add(HouseTeleport.ARCEUUS_LIBRARY_PORTAL, ObjectID.POH_PORTAL_TEAK_ARCEUUS_LIBRARY, ObjectID.POH_PORTAL_MAG_ARCEUUS_LIBRARY, ObjectID.POH_PORTAL_MARBLE_ARCEUUS_LIBRARY);
		add(HouseTeleport.MIND_ALTAR_PORTAL, ObjectID.POH_PORTAL_TEAK_MIND_ALTAR, ObjectID.POH_PORTAL_MAG_MIND_ALTAR, ObjectID.POH_PORTAL_MARBLE_MIND_ALTAR);
		add(HouseTeleport.SALVE_GRAVEYARD_PORTAL, ObjectID.POH_PORTAL_TEAK_SALVE_GRAVEYARD, ObjectID.POH_PORTAL_MAG_SALVE_GRAVEYARD, ObjectID.POH_PORTAL_MARBLE_SALVE_GRAVEYARD);
		add(HouseTeleport.WEST_ARDOUGNE_PORTAL, ObjectID.POH_PORTAL_TEAK_WEST_ARDOUGNE, ObjectID.POH_PORTAL_MAG_WEST_ARDOUGNE, ObjectID.POH_PORTAL_MARBLE_WEST_ARDOUGNE);
		add(HouseTeleport.CIVITAS_PORTAL, ObjectID.POH_PORTAL_TEAK_FORTIS, ObjectID.POH_PORTAL_MAG_FORTIS, ObjectID.POH_PORTAL_MARBLE_FORTIS);
		add(HouseTeleport.TROLLHEIM_PORTAL, ObjectID.POH_PORTAL_TEAK_TROLLHEIM, ObjectID.POH_PORTAL_MAG_TROLLHEIM, ObjectID.POH_PORTAL_MARBLE_TROLLHEIM);
		add(HouseTeleport.PADDEWWA_PORTAL, ObjectID.POH_PORTAL_TEAK_PADDEWWA, ObjectID.POH_PORTAL_MAG_PADDEWWA, ObjectID.POH_PORTAL_MARBLE_PADDEWWA);
		add(HouseTeleport.LASSAR_PORTAL, ObjectID.POH_PORTAL_TEAK_LASSAR, ObjectID.POH_PORTAL_MAG_LASSAR, ObjectID.POH_PORTAL_MARBLE_LASSAR);
		add(HouseTeleport.DAREEYAK_PORTAL, ObjectID.POH_PORTAL_TEAK_DAREEYAK, ObjectID.POH_PORTAL_MAG_DAREEYAK, ObjectID.POH_PORTAL_MARBLE_DAREEYAK);
		add(HouseTeleport.OURANIA_CAVE_PORTAL, ObjectID.POH_PORTAL_TEAK_OURANIA, ObjectID.POH_PORTAL_MAG_OURANIA, ObjectID.POH_PORTAL_MARBLE_OURANIA);
		add(HouseTeleport.BARBARIAN_OUTPOST_PORTAL, ObjectID.POH_PORTAL_TEAK_BARBARIAN, ObjectID.POH_PORTAL_MAG_BARBARIAN, ObjectID.POH_PORTAL_MARBLE_BARBARIAN);
		add(HouseTeleport.PORT_KHAZARD_PORTAL, ObjectID.POH_PORTAL_TEAK_KHAZARD, ObjectID.POH_PORTAL_MAG_KHAZARD, ObjectID.POH_PORTAL_MARBLE_KHAZARD);
		add(HouseTeleport.ICE_PLATEAU_PORTAL, ObjectID.POH_PORTAL_TEAK_ICEPLATEAU, ObjectID.POH_PORTAL_MAG_ICEPLATEAU, ObjectID.POH_PORTAL_MARBLE_ICEPLATEAU);

		Map<String, HouseTeleport> names = new HashMap<>();
		nexusName(names, HouseTeleport.VARROCK_PORTAL, "varrock", "grand exchange");
		nexusName(names, HouseTeleport.FALADOR_PORTAL, "falador");
		nexusName(names, HouseTeleport.LUMBRIDGE_PORTAL, "lumbridge");
		nexusName(names, HouseTeleport.EAST_ARDOUGNE_PORTAL, "ardougne", "east ardougne");
		nexusName(names, HouseTeleport.WATCHTOWER_PORTAL, "watchtower", "yanille");
		nexusName(names, HouseTeleport.CAMELOT_PORTAL, "camelot", "seers village");
		nexusName(names, HouseTeleport.LUNAR_ISLE_PORTAL, "lunar isle");
		nexusName(names, HouseTeleport.WATERBIRTH_PORTAL, "waterbirth");
		nexusName(names, HouseTeleport.FISHING_GUILD_PORTAL, "fishing guild");
		nexusName(names, HouseTeleport.SENNTISTEN_PORTAL, "senntisten");
		nexusName(names, HouseTeleport.KHARYRLL_PORTAL, "kharyrll");
		nexusName(names, HouseTeleport.ANNAKARL_PORTAL, "annakarl");
		nexusName(names, HouseTeleport.KOUREND_CASTLE_PORTAL, "kourend castle");
		nexusName(names, HouseTeleport.MARIM_PORTAL, "marim", "ape atoll");
		nexusName(names, HouseTeleport.TROLL_STRONGHOLD_PORTAL, "troll stronghold");
		nexusName(names, HouseTeleport.GHORROCK_PORTAL, "ghorrock");
		nexusName(names, HouseTeleport.CARRALLANGER_PORTAL, "carrallanger", "carrallangar");
		nexusName(names, HouseTeleport.CATHERBY_PORTAL, "catherby");
		nexusName(names, HouseTeleport.WEISS_PORTAL, "weiss");
		nexusName(names, HouseTeleport.APE_ATOLL_DUNGEON_PORTAL, "ape atoll dungeon");
		nexusName(names, HouseTeleport.BARROWS_PORTAL, "barrows");
		nexusName(names, HouseTeleport.BATTLEFRONT_PORTAL, "battlefront");
		nexusName(names, HouseTeleport.FORGOTTEN_CEMETERY_PORTAL, "cemetery");
		nexusName(names, HouseTeleport.DRAYNOR_MANOR_PORTAL, "draynor manor");
		nexusName(names, HouseTeleport.FENKENSTRAIN_PORTAL, "fenkenstrains castle");
		nexusName(names, HouseTeleport.HARMONY_ISLAND_PORTAL, "harmony island");
		nexusName(names, HouseTeleport.ARCEUUS_LIBRARY_PORTAL, "arceuus library");
		nexusName(names, HouseTeleport.MIND_ALTAR_PORTAL, "mind altar");
		nexusName(names, HouseTeleport.SALVE_GRAVEYARD_PORTAL, "salve graveyard");
		nexusName(names, HouseTeleport.WEST_ARDOUGNE_PORTAL, "west ardougne");
		nexusName(names, HouseTeleport.CIVITAS_PORTAL, "civitas illa fortis");
		nexusName(names, HouseTeleport.TROLLHEIM_PORTAL, "trollheim");
		nexusName(names, HouseTeleport.PADDEWWA_PORTAL, "paddewwa");
		nexusName(names, HouseTeleport.LASSAR_PORTAL, "lassar");
		nexusName(names, HouseTeleport.DAREEYAK_PORTAL, "dareeyak");
		nexusName(names, HouseTeleport.OURANIA_CAVE_PORTAL, "ourania");
		nexusName(names, HouseTeleport.BARBARIAN_OUTPOST_PORTAL, "barbarian outpost");
		nexusName(names, HouseTeleport.PORT_KHAZARD_PORTAL, "port khazard");
		nexusName(names, HouseTeleport.ICE_PLATEAU_PORTAL, "ice plateau");
		// a Xeric's talisman or digsite pendant put in the nexus adds its teleports to the menu
		nexusName(names, HouseTeleport.XERICS_TALISMAN, "xerics");
		nexusName(names, HouseTeleport.DIGSITE_PENDANT, "digsite", "fossil island", "lithkren");
		names.keySet().stream()
			.sorted(Comparator.comparingInt(String::length).reversed())
			.forEach(name -> NEXUS_NAMES.put(name, names.get(name)));
	}

	private final Client client;
	private final ConfigManager configManager;
	private final LazyScapingConfig config;

	/** The tick the player last chose a way into their own house, or -1. */
	private int ownEntryTick = -1;
	private boolean inOwnHouse;
	/** The nexus's destinations, once its teleport menu has been read this visit. */
	private Set<HouseTeleport> nexus;
	private boolean scanPending;
	private boolean nexusPending;

	@Inject
	HouseReader(Client client, ConfigManager configManager, LazyScapingConfig config)
	{
		this.client = client;
		this.configManager = configManager;
		this.config = config;
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = Text.removeTags(event.getMenuOption()).toLowerCase(Locale.ROOT);
		String target = Text.removeTags(event.getMenuTarget()).toLowerCase(Locale.ROOT);
		boolean houseTeleport = (option.equals("break") || option.equals("inside") || option.equals("cast")) && target.contains("teleport to house");
		if (option.equals("home") || option.equals("build mode") || option.equals("tele to poh") || houseTeleport)
		{
			ownEntryTick = client.getTickCount();
		}
		else if (option.equals("friend's house"))
		{
			ownEntryTick = -1;
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.TELENEXUS_TELEPORT)
		{
			nexusPending = inOwnHouse;
		}
		else if (event.getGroupId() == InterfaceID.POH_BOARD)
		{
			// the house advertisement board leads to other players' houses
			ownEntryTick = -1;
		}
	}

	/** After every area loads, check on the next tick whether it's the player's house. */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			scanPending = true;
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		objectChanged();
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		objectChanged();
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		objectChanged();
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		objectChanged();
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		objectChanged();
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		objectChanged();
	}

	/** Furniture may have been built or removed. */
	private void objectChanged()
	{
		// also while the house may still be loading after the player chose to go there
		scanPending |= inOwnHouse || ownEntryTick != -1;
	}

	/** Reads anything pending, once a tick. */
	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (!config.readHouse())
		{
			return;
		}
		if (nexusPending)
		{
			nexusPending = false;
			nexus = readNexusMenu();
			scanPending = true;
		}
		if (scanPending)
		{
			scanPending = false;
			scan();
		}
		if (ownEntryTick != -1 && client.getTickCount() - ownEntryTick > ENTRY_TICKS)
		{
			ownEntryTick = -1;
		}
	}

	/** Reads the furniture if the player is in their own house, and updates the settings. */
	private void scan()
	{
		Set<Integer> objects = inOwnHouse || ownEntryTick != -1 ? sceneObjects() : Set.of();
		if (!objects.contains(ObjectID.POH_EXIT_PORTAL))
		{
			inOwnHouse = false;
			nexus = null;
			return;
		}
		inOwnHouse = true;
		ownEntryTick = -1;

		Set<HouseTeleport> found = EnumSet.noneOf(HouseTeleport.class);
		boolean hasNexus = false;
		for (int id : objects)
		{
			found.addAll(FEATURES.getOrDefault(id, List.of()));
			hasNexus |= NEXUSES.contains(id);
		}
		if (nexus != null)
		{
			found.addAll(nexus);
		}
		// until the nexus's menu has been read, a portal or amulet the house doesn't show may be in it
		boolean nexusUnknown = hasNexus && nexus == null;

		List<String> changes = new ArrayList<>();
		for (HouseTeleport teleport : HouseTeleport.values())
		{
			boolean has = found.contains(teleport);
			boolean canBeInNexus = teleport.isPortal() || teleport == HouseTeleport.XERICS_TALISMAN || teleport == HouseTeleport.DIGSITE_PENDANT;
			if (has != teleport.isTicked(configManager) && (has || !nexusUnknown || !canBeInNexus))
			{
				configManager.setConfiguration(LazyScapingConfig.GROUP, teleport.getConfigKey(), has);
				changes.add(teleport.getApiName() + (has ? " added" : " removed"));
			}
		}
		if (!changes.isEmpty())
		{
			log.debug("Read the house: {}", changes);
		}
	}

	private Set<Integer> sceneObjects()
	{
		Set<Integer> ids = new HashSet<>();
		for (Tile[][] plane : client.getTopLevelWorldView().getScene().getTiles())
		{
			for (Tile[] row : plane)
			{
				for (Tile tile : row)
				{
					if (tile == null)
					{
						continue;
					}
					for (GameObject object : tile.getGameObjects())
					{
						addId(ids, object);
					}
					addId(ids, tile.getWallObject());
					addId(ids, tile.getDecorativeObject());
					addId(ids, tile.getGroundObject());
				}
			}
		}
		return ids;
	}

	private static void addId(Set<Integer> ids, TileObject object)
	{
		if (object != null)
		{
			ids.add(object.getId());
		}
	}

	/** The destinations listed in the nexus's open teleport menu. */
	private Set<HouseTeleport> readNexusMenu()
	{
		List<String> texts = new ArrayList<>();
		collectText(client.getWidget(InterfaceID.TelenexusTeleport.UNIVERSE), texts);
		Set<HouseTeleport> destinations = EnumSet.noneOf(HouseTeleport.class);
		for (String text : texts)
		{
			HouseTeleport destination = nexusDestination(text);
			if (destination != null)
			{
				destinations.add(destination);
			}
		}
		log.debug("Read the portal nexus: {} from {}", destinations, texts);
		return destinations;
	}

	/** The destination a line of the nexus's menu names, like "1 : Varrock", or null. */
	static HouseTeleport nexusDestination(String text)
	{
		String line = " " + SPACES.matcher(NOT_LETTERS.matcher(Text.removeTags(text).toLowerCase(Locale.ROOT)).replaceAll("")).replaceAll(" ").trim() + " ";
		for (Map.Entry<String, HouseTeleport> entry : NEXUS_NAMES.entrySet())
		{
			if (line.contains(" " + entry.getKey() + " "))
			{
				return entry.getValue();
			}
		}
		return null;
	}

	private static void collectText(Widget widget, List<String> texts)
	{
		if (widget == null || widget.isHidden())
		{
			return;
		}
		if (widget.getText() != null && !widget.getText().isEmpty())
		{
			texts.add(widget.getText());
		}
		for (Widget[] children : new Widget[][]{widget.getStaticChildren(), widget.getDynamicChildren(), widget.getNestedChildren()})
		{
			if (children != null)
			{
				for (Widget child : children)
				{
					collectText(child, texts);
				}
			}
		}
	}

	private static void add(HouseTeleport feature, int... objectIds)
	{
		for (int id : objectIds)
		{
			FEATURES.computeIfAbsent(id, k -> new ArrayList<>()).add(feature);
		}
	}

	private static void nexusName(Map<String, HouseTeleport> names, HouseTeleport destination, String... aliases)
	{
		for (String alias : aliases)
		{
			names.put(alias, destination);
		}
	}
}
