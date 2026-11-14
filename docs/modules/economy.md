# Economy
Siddy's Simple Plugin comes with a rather common feature available in other plugins too. Its the most simple system.

This module is known as the `economy` module. Its a kind of `standalone` module. You can set up this module through `economy: true`. It has its main command as `/siddyssimpleplugin:economy` or `/economy` simply. Its command **MUST** have arguments attached to it. These arguments can be `permission.level.default` or `permission.level.op`. The **OP** includes `give`, `take` and `set`. The **DEFAULT** includes `balance` and `pay` only. I plan on adding a bank system too, but that will be for later.

## Economy Configuration
As this is a `standalone` module, it includes its own `config.yml` present in `siddyssimpleplugin/economy/config.yml`. It can also be turned *ON* or *OFF*. Its config looks something like:
:::dropdown `economy/config.yml`
```
economy:
  starting-balance: 1000.0
  allow-negative-balances: false
  minimum-transaction: 0.01
  maximum-transaction: 1.0E9
display:
  decimal-places: 2
  use-comma-separator: true
actionbar:
  enabled: true
  balance: '&aBalance: &f$%balance%'
  balance-set: '&aBalance set to &f$%balance%'
  money-received: '&a+$%amount% &7→ &f$%balance%'
  money-removed: '&c-$%amount% &7→ &f$%balance%'
  payment-sent: '&c-$%amount% &7→ &f$%balance%'
  payment-received: '&a+$%amount% &7→ &f$%balance%'
  insufficient-funds: '&cInsufficient funds!'
  invalid-amount: '&cInvalid amount.'
  transaction-too-small: '&cAmount must be at least $%amount%.'
  transaction-too-large: '&cAmount cannot exceed $%amount%.'
  negative-balance: '&cThis transaction would create a negative balance.'
sounds:
  enabled: true
  transaction: message
  error: error
  balance: message
messages:
  balance: '&a%player% has &f$%balance%'
  payment-sent: '&aYou paid &f$%amount% &ato &f%player%&a.'
  payment-received: '&aYou received &f$%amount% &afrom &f%player%&a.'
  give: '&aGave &f$%amount% &ato &f%player%&a.'
  take: '&aTook &f$%amount% &afrom &f%player%&a.'
  set: '&aSet &f%player%&a''s balance to &f$%amount%&a.'
  usage: '&eUsage: /%label% <balance|pay|give|take|set>'
  usage-pay: '&eUsage: /%label% pay <player> <amount>'
  usage-give: '&eUsage: /%label% give <player> <amount>'
  usage-take: '&eUsage: /%label% take <player> <amount>'
  usage-set: '&eUsage: /%label% set <player> <amount>'
errors:
  unknown-command: '&cUnknown economy command.'
  player-only: '&cOnly players can use this command.'
  console-player-required: '&cConsole must specify a player.'
  player-not-found: '&cThat player is not online.'
  self-payment: '&cYou cannot pay yourself.'
  invalid-amount: '&cInvalid amount.'
  negative-amount: '&cAmount must be greater than 0.'
  insufficient-funds: '&cYou do not have enough money.'
  no-permission: '&cYou do not have permission to do this.'
  negative-balance: '&cThis transaction would create a negative balance.'
```
:::

Not explaining everything in brief, I will only explain the sections. There are many sections, and henceforth `economy` is **very** customizable:
- `economy`: The main Economy settings. 
 - `starting-balance`: The balance at which every new player starts. It does **NOT** affect players already registered within `economy/economy.yml`.
 - `allow-negative-balance`: Allows negative balances, works only with Auction House. 
 - `minimum-transaction` and `maximum-transaction`: Decide the Minimum and Maximum amount a person can transfer.

- `display`: Configures how the money is displayed, includes if `comma-seperation` and `formatted-money` is used or not.
- `action-bar`: Controls weather to display economy information in the action bar *(The text visible above the hotbar that is used to display jukebox status)* and what messages to display on the action bar.
- `sounds`: Controls weather to play sounds using `soundsystem` or not and also what sounds to play on different events.
- `messages`: Controls what messages to send to player through `sendMessage()` to the player on certain events.
 - `errors`: Controls what messages to send to player when an error occurs.

## Economy Support
**Siddy's Simple Plugin** supports *Vault*, which if you did not know is a kind of plugin that acts as a link between an economy plugin and another plugin. Lets take an example. You are using a seperate Economy plugin maybe because you aren't satisfied with the current economy of **SSMP**. Now, for SSMP to actually use that economy, *Vault* acts as a bridge between both plugins.

If you intend on using *Vault* and any other economy plugin, take into consideration that turning off `economy` from `config.yml` will cause issues. **SSMP** is designed such that you can turn off modules to your choice, but some modules are dependent on others. If you plan on disabling `economy`, there might be some plugins you have to disable too. But, I **do not reccomend** turning economy off, because you can obviously use vault. Turning off `economy` is advised in conditions where you already have substitutes or don't want the modules in [Teleport: #economy-dependent-modules](Economy Dependent Modules)

## Economy Dependent Modules
Here is a list of modules that a directly / indirectly dependent on `economy`. These modules are specifically designed to work with `economy`, but some of them can be configured to not be. The modules which can work without `economy` **BUT** with some configurations are called `soft-depend modules`. The modules that **EXPLICITLY** require `economy` are called `hard-depend modules`.
|Module Name|Type|
|------|------|
|Shop|Hard Depend|
|Auction House| Hard Depend|
|Levels| Soft Depend|
|Contracts| Soft Depend|
|Bounties| Hard Depend|
|Kits| Soft Depend|
|Player Info| Soft Depend|