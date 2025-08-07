<div align="center">
<img src="https://github.com/teamld/teamld.github.io/blob/main/manhuntplus/banners/mh+.png?raw=true" alt="ManhuntPlus Banner">
A complete Manhunt game implementation for Minecraft servers.
</div>

## 🚀 Features
- **Hunter and Speedrunner Roles**: Assign roles to players for an exciting chase experience.
- **Game Management**: Start, stop, and manage the game with simple commands.
- **Wide Compatibility**: Works on Minecraft versions from 1.12.2 to the latest.
- **Permission System**: Robust permissions to manage who can use and administer the game.
- **Customizable Effects**: Configure effects for hunters and speedrunners.
- **Scoreboard Integration**: Real-time game status updates via scoreboard.

---

## 📦 Installation
1. Download ManhuntPlus from [Modrinth](https://modrinth.com/plugin/manhunt-plus).
2. Drop it into your server’s `plugins/` folder.
3. Restart the server.

```bash
# For admins who love commands:
/reload confirm
```

---

## ⚙️ Configuration
Edit `plugins/ManhuntPlus/config.yml` to tweak behavior:

```yaml
# Manhunt Configuration File
game:
  freeze-duration: 5
  hunter-spawn-radius: 20
  end-on-dragon-kill: true
  give-hunters-resistance: false
  give-speedrunners-speed: false

compass:
  name: "&a&lHunter Compass"
  lore:
    - "&7Tracks the nearest speedrunner"
    - "&7Works across dimensions"
  update-interval: 20

scoreboard:
  enabled: true
  title: "&6&lManhunt"
  update-interval: 20

messages:
  prefix: "&8[&aManhunt&8]"
  countdown: "&eGame starting in &6{time}&e..."
  game-start: "&aManhunt has begun! Good luck!"
  game-end-hunters-win: "&cHunters have won the Manhunt!"
  game-end-speedrunners-win: "&aSpeedrunners have won the Manhunt!"

world:
  disabled-worlds:
    - "world_nether"
    - "world_the_end"
  enable-world-border: false
  border-start-size: 1000
  border-shrink: false
  border-shrink-speed: 10
  border-final-size: 50

effects:
  hunters:
    resistance:
      level: 1
      duration: 30
    speed:
      level: 0
      duration: 0
  speedrunners:
    speed:
      level: 1
      duration: 30
    resistance:
      level: 0
      duration: 0
```

---

## 💻 Commands & Permissions

| Command | Description | Permission |
|---------|-------------|------------|
| `/manhunt hunter <player>` | Add a player as a hunter. | `manhunt.use` |
| `/manhunt speedrunner <player>` | Add a player as a speedrunner. | `manhunt.use` |
| `/manhunt start` | Start the Manhunt game. | `manhunt.admin` |
| `/manhunt stop` | Stop the Manhunt game. | `manhunt.admin` |
| `/manhunt status` | Show the current game status. | `manhunt.use` |
| `/manhunt reload` | Reload the plugin configuration. | `manhunt.admin` |

---

## 📜 Compatibility

| **Minecraft** | **Server Type** | **Java** |
|--------------|----------------|---------|
| 1.12.x - Latest | Spigot, Paper | JDK 17+ |

---

## ❓ Support & Links

- **Bug Reports**: [GitHub Issues](https://github.com/lifedestroyed/manhuntplus/issues)
- **Source Code**: [GitHub](https://github.com/lifedestroyed/manhuntplus)
- **License**: life destroyed license
