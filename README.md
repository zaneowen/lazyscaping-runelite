# LazyScaping

A RuneLite plugin for [LazyScaping](https://lazyscaping.app), the easy, cheap OSRS training and boss gear planner.

## What it does

**One-click sync.** The *Open my planner on lazyscaping.app* button opens the site with your name, account type, membership, gear and quest unlocks already filled in, so you don't have to type them or paste a bank export. Your gear comes from your equipment, your inventory and your bank (open the bank once so the plugin can see it). The data travels after the `#` in the link, which your browser never sends to the server, and the site saves it in your browser like anything you set there yourself.

The side panel has three tabs.

**Training**
- **Training goal.** Pick a skill and a target level. The panel shows your progress since you set the goal, xp left, and, while you're training, your xp/hr and how long the goal will take (from RuneLite's XP Tracker).
- **Your plan.** The LazyScaping plan for your goal: which method to use at each level, with xp/hr, hours and cost, following your *Plan priority* setting and your budget. Click a step for the method's guide and materials.
- **Training methods.** Every method for the skill, with what it needs. Click one for its details and guide, and *Use this method* to plan around it.

**Bossing**
- **Gear setup.** Pick a boss to see the best setup from the gear you own: the worn gear laid out like the equipment screen (hover a slot for what to get next and its GE price), the packed inventory with switches, the special attack weapon and supplies, and how to get there. With a budget, it also buys the best upgrades you can afford (not for ironmen).

**Slayer**
- **Your task.** The task from RuneLite's Slayer plugin, with kills left, is picked automatically; you can also pick any task. It shows the requirements, where to kill them and tips.
- **Gear for the task.** The same gear setup as the Bossing tab, planned for the task. *Plan with my Slayer helmet (i)* is ticked when you own one (any colour), and you can tick it yourself if yours is somewhere the plugin can't see.

**Show setup in bank.** Every gear setup can open a temporary *lazyscaping* bank tag with its items, laid out with the worn gear on top and the inventory as it's packed. If your bank is closed, it opens next time you open it.

**Budgets.** Each tab has a budget: a preset, an amount like `25m`, *Unlimited*, or *My coins* (the coins in your bank and inventory).

Goals, picked methods, bosses, Slayer tasks and budgets are saved per account.

**Live recommendations** (opt-in). The plan, methods and gear setups come from lazyscaping.app, so they need *Live recommendations* turned on in the plugin's settings.

## Privacy

With *Live recommendations* off, the plugin makes no network requests. Sync only opens a link in your browser.

With it on, the plugin sends your skill levels, the LazyScaping gear ids you own (matched on your computer against the site's gear list, never your full bank), your quest unlocks, your account type, your goal and picked method, your budgets and the boss or Slayer task you pick to lazyscaping.app. The site uses them to build the plan and gear setup and doesn't store them. Your RuneScape name isn't sent. Item pictures for untradeable items are loaded from lazyscaping.app.

## Development

Run a development client with the plugin loaded:

```
./gradlew run
```

To log in with a Jagex account, follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

`src/main/resources/com/lazyscaping/gear-catalog.json` is a copy of https://lazyscaping.app/api/plugin/catalog, bundled so sync works without any requests. Refresh it when the site adds gear or bosses.
