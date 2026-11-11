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
- Sound system

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

Editing modules is kind of easy. For this example, we are going to edit the `plugins/siddyssimpleplugin/scoreboard/scoreboard.yml`. Scoreboard is kind of the simplest module to edit because it doesn't require that much of knowledge. The code looks something like this:

```
enabled: true
title: '&6&lSiddy''s SB'
lines:
  '1': '&7Welcome, &f%player%'
  '2': ''
  '3': '&fBalance: &a$%balance%'
  '4': '&fOnline: &e%online%&7/&e%max_players%'
  '5': ''
  '6': '&7World: &f%world%'
  '7': '&7XYZ: &f%x% %y% %z%'
```

Right off, it looks kind of confusing, but its simple as it gets. There are a few things present in this configuration. I will be explaining it in kind of breif:
- `enabled: true`: Just like before, this line of code is used to enable to disable the module from its config file itself, just in case you forgot to from the `config.yml`. I dunno why I did that, but seems like it.
- `title: ...`: As it says, this is the title. You may have seen the ampersand and number / letter. That is the color, to be set beforehand.
- `lines: ...`: These are the main lines shown. The text between the percentage symbol is a placeholder, which is rather unique for every player. **SSMP** handles placeholders like these through `ssmpplaceholders`, in which many of these percentage placeholders have been defined. You can learn more about them [here](#modules-placeholders)

## Sound System
The sound system is also pretty straightforward too. Its very simple. As in many other plugins, certain events have certain sounds playing. However, those are not configurable, not that easily. **SSMP** makes it far more easy, as you can configure each event's `volume`, `pitch` and `sound_played`. All these sounds can be controlled through `plugins/siddyssimpleplugin/sounds/config.yml`. Putting the full default file here is of no use. So, to just explain configuration, I will briefly describe this sound system in a short manner. This is an example sound event:

```
menu-open:
    enabled: true
    sound: minecraft:block.chest.open
    volume: 1.0
    pitch: 1.0
```

This code snipper contains the following things:
- `event-name`: This is the name of the event, i.e. the event at which the sound plays. This event cannot be simply any event. This event must be one matching the `soundsystem` table. That soundsystem table can be found either on the page you downloaded **SSMP** from, which are currently in the bottom of the sidebar, or [here](#resources-sounds).
- `enabled: ...`: This decides weather the sound is enabled or not. Another way of disabling is *deleting the entire snippet itself*, but this feature has been provided so that you don't have to go around searching for documentation after you've deleted something important.
- `sound: ...`: This is the sound that plays on the event. It must be matching the resource-pack / datapack or be a valid in-game sound. 
- `volume` & `pitch`: Do what they are, 1.0 is the default value of both.

Well then, now you know how to set up configs. This same format can also be reflected upon other plugins. Very simple.