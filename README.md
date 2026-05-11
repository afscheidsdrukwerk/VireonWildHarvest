# Vireon Wild Harvest

First entry in the **Vireon** plugin series by ArtsyStudios.

This plugin will eventually turn Minecraft's mobs into a realistic wildlife
layer — hostile mobs become real-life animals (bears, wolves, snakes, etc.),
all mobs get realistic drops (bones, flesh, hide), and custom Blockbench
models load with a simple folder drop.

> **Current version: 0.1.0 — foundation only.**
> Plugin loads, base command works, config is in place. Mob conversion
> arrives in v0.2.0. Roadmap at the bottom of this file.

---

## Server requirements

| | Version |
|---|---|
| **Server** | Paper 1.21.x (Spigot also works for most features) |
| **Java (server)** | 21 |

You do **not** need Java or Maven installed locally — GitHub Actions
compiles everything for you in the cloud (see below).

---

## Building the .jar via GitHub Actions  *(recommended)*

This repo ships with two workflows under `.github/workflows/`:

| Workflow | When it runs | What you get |
|---|---|---|
| `build.yml` | Every push & PR + manual trigger | `.jar` uploaded as a workflow artifact |
| `release.yml` | When you push a `v*` git tag | `.jar` attached to a GitHub Release |

### One-time setup

1. Create a new GitHub repo (private or public, doesn't matter).
2. Push the contents of this folder to it:
   ```bash
   cd VireonWildHarvest
   git init
   git add .
   git commit -m "Initial commit: Vireon Wild Harvest 0.1.0"
   git branch -M main
   git remote add origin https://github.com/<your-user>/VireonWildHarvest.git
   git push -u origin main
   ```
3. Open the **Actions** tab on GitHub — the *Build plugin* workflow runs
   automatically on the first push.

### Getting your .jar from a build

1. Go to **Actions** → click the latest *Build plugin* run.
2. Scroll to the bottom — under **Artifacts** you'll see
   `VireonWildHarvest-0.1.0`.
3. Click it to download a zip containing your `.jar`.

### Cutting a versioned release

When you're ready to ship a version:

```bash
git tag v0.1.0
git push origin v0.1.0
```

The `release.yml` workflow builds and publishes a GitHub Release with the
`.jar` attached as a downloadable file — handy for sharing builds with
players or co-admins.

### Triggering a build manually

In the **Actions** tab → *Build plugin* → **Run workflow** button. Useful
for force-rebuilding without pushing a new commit.

---

## Installing on your server

1. Download `VireonWildHarvest-0.1.0.jar` from the GitHub Actions artifact
   (or Release).
2. Drop it into your server's `plugins/` folder.
3. Restart (`/reload` is unsupported and will be ignored — full restart).

You should see this in console on startup:

```
[Vireon] ───────────────────────────────
[Vireon]  Vireon Wild Harvest v0.1.0
[Vireon]  ArtsyStudios — vireon series
[Vireon]  Foundation ready.
[Vireon]  Mob conversion arrives in v0.2.0.
[Vireon] ───────────────────────────────
```

---

## In-game commands

| Command | Description |
|---|---|
| `/vireon version` | Show plugin version |
| `/vireon reload` | Reload `config.yml` without restart |
| `/vireon help` | Show all commands |

Aliases: `/vwh`, `/wildharvest`.

## Permissions

| Node | Default | Description |
|---|---|---|
| `vireon.admin` | op | Access to all admin commands |
| `vireon.use` | true | Player-facing features (none yet) |

---

## Optional: building locally

Only useful if you want to test changes without pushing to GitHub.
Requires Java 21 JDK + Maven 3.9+ on `PATH`.

```cmd
build.bat              :: Windows
./build.sh             # Linux / macOS
```

Jar lands at `target/VireonWildHarvest-0.1.0.jar`.

---

## Roadmap

| Version | Feature |
|---|---|
| ✅ **0.1.0** | Foundation — plugin loads, `/vireon` command, config system |
| **0.2.0** | Mob conversion engine — Zombie → Brown Bear, sun immunity, realistic drops |
| **0.3.0** | **Vireon Forge** — Blockbench model loader + auto-hosted resource pack |
| **0.4.0** | In-game GUI for managing mobs, models, drops |
| **0.5.0** | Hot-reload + more conversions (Skeleton → Wolf, Creeper → Boar, Spider → Rattlesnake, etc.) |

---

## Project layout

```
VireonWildHarvest/
├── .github/workflows/
│   ├── build.yml       ← CI: build .jar on every push
│   └── release.yml     ← Publish GitHub Release on version tag
├── pom.xml             ← Maven build configuration
├── build.bat           ← (optional) Windows local build
├── build.sh            ← (optional) Linux/macOS local build
├── README.md           ← this file
├── .gitignore
└── src/main/
    ├── java/nl/artsystudios/vireon/wildharvest/
    │   ├── VireonWildHarvest.java       ← plugin entry point
    │   ├── command/VireonCommand.java   ← /vireon command handler
    │   └── config/ConfigManager.java    ← config wrapper
    └── resources/
        ├── plugin.yml                   ← Bukkit plugin descriptor
        └── config.yml                   ← default user config
```

---

Made by **ArtsyStudios** · [artsystudios.nl](https://artsystudios.nl)
