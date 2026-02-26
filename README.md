# TrueBan

TrueBan is a production-ready Minecraft moderation plugin for PaperMC servers, designed for server owners who want powerful ban management without complex configuration files.

## Features

TrueBan provides comprehensive moderation tools with a focus on simplicity and reliability:

- **Player Management**: Ban, kick, and mute players with detailed reasons and tracking
- **IP Banning**: Automatic IP tracking and IP-based bans
- **Temporary Punishments**: Time-based bans and mutes with flexible duration parsing
- **Punishment History**: Complete audit trail of all moderation actions
- **Database Support**: SQLite and MySQL support with connection pooling
- **Permission System**: Granular permissions for different moderation roles
- **Multi-language Support**: Configurable messages with placeholder support

## Requirements

TrueBan requires PaperMC to run. Other server software may work, but are not officially tested or supported.

### Supported Versions
- **Minecraft**: 1.21.4 and newer
- **Java**: 21 or higher (recommended: latest Java version)

### Dependencies
- **PaperMC**: Required for core functionality
- **Database**: SQLite (included) or MySQL (optional)
- **Vault**: Optional, for enhanced permissions integration

## Installation

1. Download the latest TrueBan.jar from the [releases page](https://github.com/insanmiy/TrueBan/releases)
2. Place the jar file in your server's `plugins/` directory
3. Restart your server
4. Configure permissions as needed

## Commands

TrueBan provides a comprehensive set of moderation commands:

| Command | Usage | Permission | Description |
|---------|-------|------------|-------------|
| `/ban <player> <reason>` | Ban a player permanently | `trueban.ban` | Permanently bans a player with a reason |
| `/tempban <player> <duration> <reason>` | Temporarily ban a player | `trueban.tempban` | Bans a player for a specified duration |
| `/unban <player|ip>` | Unban a player or IP | `trueban.unban` | Removes active bans for players or IPs |
| `/ipban <player|ip>` | Ban an IP address | `trueban.ipban` | Bans a player's IP address |
| `/kick <player> <reason>` | Kick a player | `trueban.kick` | Kicks a player with a reason |
| `/mute <player> <reason>` | Permanently mute a player | `trueban.mute` | Mutes a player permanently |
| `/tempmute <player> <duration> <reason>` | Temporarily mute a player | `trueban.tempmute` | Mutes a player for a specified duration |
| `/unmute <player>` | Unmute a player | `trueban.unmute` | Removes active mutes for a player |
| `/history <player>` | View punishment history | `trueban.history` | Shows complete punishment history for a player |

### Duration Format
Duration strings support the following formats:
- `30m` - 30 minutes
- `2h` - 2 hours
- `1d` - 1 day
- `1w` - 1 week
- `1mo` - 1 month
- Combinations like `1h30m` - 1 hour and 30 minutes

## Permissions

TrueBan uses a comprehensive permission system:

### Basic Permissions
- `trueban.ban` - Ban players
- `trueban.tempban` - Temporarily ban players
- `trueban.unban` - Unban players
- `trueban.ipban` - IP ban players
- `trueban.kick` - Kick players
- `trueban.mute` - Permanently mute players
- `trueban.tempmute` - Temporarily mute players
- `trueban.unmute` - Unmute players
- `trueban.history` - View punishment history

### Administrative Permissions
- `trueban.bypass` - Bypass all punishments
- `trueban.notify` - Receive moderation notifications
- `trueban.mod` - Moderator commands
- `trueban.admin` - Administrative commands
- `trueban.*` - All TrueBan permissions

## Configuration

TrueBan is designed to work out-of-the-box with minimal configuration. The plugin automatically:

- Creates an SQLite database for punishment storage
- Sets up default message configurations
- Configures permissions for operators

### Database Configuration
For MySQL support, you can configure the database connection in `config.yml`:

```yaml
database:
  type: mysql
  host: localhost
  port: 3306
  database: trueban
  username: your_username
  password: your_password
```

## Building from Source

To build TrueBan from source, you need JDK 21 or higher installed.

### Prerequisites
- JDK 21+
- Maven 3.6+

### Build Instructions

1. Clone this repository:
   ```bash
   git clone https://github.com/insanmiy/TrueBan.git
   cd TrueBan
   ```

2. Build the project:
   ```bash
   mvn clean package
   ```

3. The built jar file will be located in `target/TrueBan-{version}.jar`

## Using TrueBan in Your Plugin

Want to integrate with TrueBan in your plugin? You can build against the TrueBan API.

### Maven Dependency
```xml
<dependency>
    <groupId>dev.insanmiy</groupId>
    <artifactId>TrueBan</artifactId>
    <version>2.1.5</version>
    <scope>provided</scope>
</dependency>
```

### API Usage
```java
// Get the TrueBan plugin instance
TrueBan trueBan = (TrueBan) Bukkit.getPluginManager().getPlugin("TrueBan");

// Access punishment manager
PunishmentManager punishmentManager = trueBan.getPunishmentManager();

// Check if player is banned
punishmentManager.getActivePunishments(playerUUID).thenAccept(punishments -> {
    boolean isBanned = punishments.stream()
        .anyMatch(p -> p.getType().isBan() && p.isActive());
});
```

## Support

Need help with TrueBan? Here are several ways to get assistance:

### Getting Help
- **Issues**: Report bugs or request features on [GitHub Issues](https://github.com/insanmiy/TrueBan/issues)
- **Discussions**: Join discussions and ask questions on [GitHub Discussions](https://github.com/insanmiy/TrueBan/discussions)

### Contributing
We welcome contributions from the community! See our [Contributing Guidelines](CONTRIBUTING.md) for more information.

#### Ways to Contribute
- **Bug Reports**: Help us identify and fix issues
- **Feature Requests**: Suggest new functionality
- **Code Contributions**: Submit pull requests for bug fixes and features
- **Documentation**: Help improve our documentation
- **Translations**: Contribute to multi-language support

## License

TrueBan is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **PaperMC Team**: For the excellent server platform
- **Bukkit/Spigot Community**: For inspiration and feedback
- **Contributors**: Everyone who has helped improve TrueBan

---

**TrueBan** - Simple, powerful moderation for your Minecraft server.
