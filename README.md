# TimeSync

TimeSync is a Velocity plugin that synchronizes time across all Minecraft servers connected through a Velocity network. It sends time data (`ticks`) to Minecraft servers periodically, ensuring consistent time across all servers.

## Features
- Sends time data (`ticks`) to Minecraft servers.
- Saves the last known time to a configuration file (`time.toml`).
- Supports time synchronization across all connected servers.

## Installation
1. Download the `.jar` file from [releases](https://github.com/khisabkurniawn/TimeSync/releases).
2. Place the `.jar` file in the `plugins` folder of your Velocity server.
3. Restart the Velocity server.

## Configuration
The plugin will create a configuration file `time.toml` in the `plugins/TimeSync` folder. You can set the initial time or adjust the synchronization interval by modifying this file.

## Support
If you encounter any issues or have questions, please open an [issue](https://github.com/khisabkurniawn/TimeSync/issues) on GitHub.

## License
This project is licensed under the [Apache](https://github.com/khisabkurniawn/TimeSync?tab=Apache-2.0-1-ov-file).