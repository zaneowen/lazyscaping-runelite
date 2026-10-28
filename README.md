# LazyScaping

A RuneLite plugin for [LazyScaping](https://lazyscaping.app), the easy, cheap OSRS training and boss gear planner.

## What it does

**One-click sync.** The *Open my planner on lazyscaping.app* button opens the site with your name, account type, membership, gear and quest unlocks already filled in, so you don't have to type them or paste a bank export. Your gear comes from your equipment, your inventory and your bank (open the bank once so the plugin can see it). The data travels after the `#` in the link, which your browser never sends to the server, and the site saves it in your browser like anything you set there yourself.

**Training goal.** Pick a skill and a target level in the side panel. The panel shows your progress since you set the goal, xp left, and, while you're training, your xp/hr and how long the goal will take (from RuneLite's XP Tracker). Goals are saved per account.

**Live recommendations** (opt-in). Turn on *Live recommendations* in the plugin's settings to see:

- the LazyScaping training plan for your goal: which method to use at each level, with xp/hr and hours, following your *Plan priority* setting (fastest, cheapest or AFK),
- bosses your levels and gear are ready or almost ready for. Click one to open its gear plan on the site.

## Privacy

With *Live recommendations* off, the plugin makes no network requests. Sync only opens a link in your browser.

With it on, the plugin sends your skill levels, the LazyScaping gear ids you own (matched on your computer against the site's gear list, never your full bank), your quest unlocks, your goal and your account type to lazyscaping.app. The site uses them to build the plan and suggestions and doesn't store them. Your RuneScape name isn't sent.

## Development

Run a development client with the plugin loaded:

```
./gradlew run
```

To log in with a Jagex account, follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

`src/main/resources/com/lazyscaping/gear-catalog.json` is a copy of https://lazyscaping.app/api/plugin/catalog, bundled so sync works without any requests. Refresh it when the site adds gear.
