# SIDDY'S IMPROVED PLUGIN
## Quick Start Guide

**Version:** 5.x  
**Documentation type:** Server owners and third-party documentation

---

# Introduction

Thank you for choosing **Siddy's Simple Plugin: Improved**! This quick start guide will walk you through the basic configuration, debugging features and utilities available in SSMP.
This guide covers:

1. Debugging features
2. Module management
3. Control Panel
4. Warnings and errors

# Debugging Features
SSMP includes various debugging and automatic debugging features designed to identify module failures.
The plugin is designed with modularity in mind and includes multiple failsafe mechanisms.
If one or more modules fail to load, SSMP will:

- Print the relevant **exception message** in the server console.
- Disable the respective module.
- Prevent dependent modules from loading when their required dependencies are unavailable.

Debugging is straightforward. You only need access to the **server console**.
During startup, SSMP prints debugging information **only when something goes wrong**.
# Modules

SSMP ships with multiple modules, each serving a specific purpose.
Modules can be individually enabled or disabled. Disabling a module may also prevent other modules that depend on it from loading.
Most aspects of the plugin are configurable, although certain core features are not.
## Economy-Based Modules

These modules depend on the **Economy** module and explicitly require it to be enabled.
Although they rely on the Economy module, their configuration is not necessarily limited to the Economy configuration file. Most of these modules have their own `config.yml`.
The economy-dependent modules are:
- Auction House
- Shop
- Bounties
- Player Information
- Contracts
**IMPORTANT:** These modules will refuse to load if the Economy module itself is disabled or unavailable.
## Moderation, Chat and Support-Based Modules
These modules provide moderation, chat filtering, chat formatting and support for external dependencies.
They include:
- Chat Formatting
- Placeholders
- Moderation Filtering
- Moderation Database
- Moderation System
- External Support (PlaceholderAPI only)
- Ranks (partially)
These modules contribute to different aspects of the server's chat and moderation systems.
Configuration is relatively straightforward. The **Ranks** module is the primary one in this group that requires additional setup.
## Core Modules

SSMP also ships with core modules that cannot be individually disabled.
These include:
- Update checking
- Support features
- Menu system
- Vault integration
- PlaceholderAPI support
These features are considered part of SSMP's core functionality.
# Control Panel
SSMP includes a web-based Control Panel that allows server owners to manage their server and configure supported plugin settings through a browser.
This is particularly useful if you do not have direct access to the plugin's configuration files.

## Accessing the Control Panel
The default port is: **8765**
To access the panel, open your browser and navigate to: `http://your-server-address:8765`
For example: `http://exampleserver.domain:8765`
If you are using a third-party hosting provider, you may need to request an additional port from your host.
Once the port is assigned, configure the `port` value inside:
`SiddysSimplePlugin/cpanel/panel.ssmpManifest`

## Authentication
Accessing the Control Panel requires authentication.
During the first startup with SSMP, the generated password will be printed in the server console.
**IMPORTANT:** The password will not be printed again after the initial generation. Make sure you save it somewhere safe.
# Warnings and Errors
SSMP uses different warning and error categories to communicate problems and provide useful information.
Understanding these categories makes troubleshooting easier.

## Exception / RuntimeException
These errors are printed in the server console when a module fails to load because of an exception.
They generally indicate an unexpected issue during initialization or execution.
Check the exception message in the console to identify the source of the problem.

## Invalid Usage
These messages can appear in both the server console and in-game, depending on who executed the command.
They indicate that a command was used incorrectly.
The message generally provides the correct command usage.

## Fatal Failure
Fatal failures are handled by SSMP's protective mechanisms.
These mechanisms are intended to protect plugin files from damage in situations where a critical failure occurs.
If you encounter a fatal failure, inspect the server console and preserve the relevant logs before attempting further changes.

---

**Thank you for choosing Siddy's Simple Plugin: Improved!**
Developed by **Siddharth C**.
Licensed under the **MIT License** on **Github**.