# Configuration
Learn how to configure and personalize your server through SSMP.
So, basically, SSMP plugin generates a folder inside your `plugins` folder, which contains all of the config files and data that the plugin uses. The main file is the `config.yml`. Its the core file that is accessed by the main class of the plugin, so its really important. That file looks something like this:
:::spoiler config.yml : 2.0.0
```
modules:
  ranks:
    enabled: true
  permissions:
    enabled: true
  economy:
    enabled: true
  shop:
    enabled: true
  auction:
    enabled: true
    max-auction-price: 1000000
    negate-buy-price: 0
    max-pages: 10
    non-sellable-items:
    - COMPASS
  tablist:
    enabled: true
    header:
      enabled: true
      lines:
      - ''
      - <#55FFFF>&cSiddy's Tablist
      - '&7Welcome, %player_nickname%'
      - ''
    footer:
      enabled: true
      lines:
      - ''
      - '&7Online: &f%online%&7/&f%max_players%'
      - '&7This is siddy''s default tablist. Change in SiddysSimplePlugin/config.yml'
      - '&7Or completely disable this module from there.'
      - ''
    player-list:
      enabled: true
      format: '%player_prefix%%player_nickname%%player_suffix% &8• &7%ping%ms'
  scoreboard:
    enabled: true
  chat:
    enabled: true
  placeholders:
    enabled: true
  menu:
    enabled: true
  enderchest:
    enabled: true
    doubled: false
  homes:
    enabled: true
    max-homes: 3
  warps:
    enabled: true
  kits:
    enabled: true
  rtp:
    enabled: true
```
:::

And I know, most of you, might have not a slight idea what it is. So allow me to explain it to you in simple steps. This is the configuration of a modular plugin. You can enable and disable plugins here, and customize the *tablist* and *auction house* from here. This document piece will guide you through all about configuring the specific files, not in deep details and all, but in a short and breif manner. This guide will consist of:
- Module toggle
- Module editing
- Extra features
- Sound system
- Permissions

## Module toggles
All the modules present in this plugin can be toggled, that's what makes it a modular design. The modules present in this plugin are wide. And there might be some that may be of no use of you. Well, to fix that problem, this plugin is designed such that you can toggle the modules on and off. This can be achieved through changing the `enabled` tag of a specific module `true` or `false`. Simple as that. For example, the below code turns the `warps` module off:
```
warps:
  enabled: false
```

and same for turning it on:
```
warps:
  enabled: true
```

Thats simple as that.

## Module editing
I've decided to name the modules based on their kinds. There are currently 3 types of modules. The `core` module, which has its configuration present right in the core config.yml. The `standalone` module, which has its own folder for its configuration. And the last, the `internal` module, this is the one that has its hardcoded code and **CANNOT** be configured, as in basic terms not.

==== NOT COMPLETED ====
Its is for the day. This doc is not completed... sorry.