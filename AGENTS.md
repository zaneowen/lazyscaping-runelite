# LazyScaping RuneLite plugin

A RuneLite plugin for [lazyscaping.app](https://lazyscaping.app): a side panel with Training, Bossing and Slayer tabs, bank-tag gear setups, and one-click sync to the site. It's on the RuneLite Plugin Hub. The README says what every feature does and exactly what's sent over the network. Read it first.

## Commands

- `./gradlew build --no-daemon`: compiles and runs the JUnit tests on Java 11. CI (`.github/workflows/build.yml`) runs the same thing, so run it before you open a PR.
- `./gradlew run`: a development RuneLite client with the plugin loaded. It needs a desktop and a game login, so you can't use it in a headless sandbox. Say in the PR what you couldn't test in the client.

## Where things live

- `src/main/java/com/lazyscaping/`: the plugin (`LazyScapingPlugin.java`), config (`LazyScapingConfig.java`), and on-computer logic like owned gear, house reading, kill tracking, budgets and bank tags.
- `api/`: `LazyScapingClient` and the response types of the site's plugin API.
- `ui/`: the side panel.
- `src/main/resources/com/lazyscaping/gear-catalog.json`: a bundled copy of https://lazyscaping.app/api/plugin/v1/catalog, so sync works with no requests. Refresh it when the site adds gear or bosses.
- `src/test/java/...`: JUnit tests.

## Rules

- **Privacy promises are part of the plugin.** With *Live recommendations* (`liveRecommendations` in the config) off, the plugin makes no network requests. The RuneScape name and the bank are never sent: owned gear is matched on the player's computer, and only LazyScaping gear ids leave it. Data goes in POST bodies, never URLs. If a change alters what's sent or when, update the README's "What's sent, and when" table in the same PR.
- **Never block the game.** Network calls go through `LazyScapingClient` (RuneLite's injected `OkHttpClient`, async). Game state is read on the client thread (`ClientThread`), and Swing UI is updated on the Swing thread (`SwingUtilities.invokeLater`).
- **Plugin Hub rules apply.** No reflection, no native code or JNI, no new third-party dependencies (only what RuneLite provides, plus Lombok), and no automating game input. Changes that break these get rejected at Plugin Hub review.
- **The site's API is versioned.** The plugin speaks `api/plugin/v1`, and the site ([zaneowen/OSRS-Tooling](https://github.com/zaneowen/OSRS-Tooling)) only adds to v1. Read new fields as optional, so older and newer sites both work. If a feature needs a site change, say so in the PR.
- **Plugin Hub releases are the owner's job.** Shipping an update means a PR to `runelite/plugin-hub` that bumps this repo's commit hash. Never open or edit that.

## Style

- Tabs for indentation and braces on their own lines, matching the existing Java.
- User-facing text and commit or PR titles are plain language that says what the player gets, like "Show whether to do, skip or block a Slayer task".
- Keep changes small and focused.

## Shipping

Work on a branch and open a PR. CI must pass, and the owner tests it in the client before merging. Never push to `main`.
