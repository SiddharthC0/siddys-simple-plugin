# commands

## general

SSMP commands let you access the plugin's features directly.

`/ssmp` opens the main management command. You can also use `/ssp` or `/siddysimpleplugin`.

`/ssmp reload` reloads SSMP. `/ssmp permission` accesses permission management.

`/rtp` opens the Random Teleport menu. Aliases: `/randomtp`, `/wild`, `/wildtp`, `/randomteleport`.

## economy

`/economy balance` checks your balance. You can also use `/bal` or `/balance`.

`/economy pay <player> <amount>` sends money to another player.

`/economy give <player> <amount>` gives money to a player.

`/economy take <player> <amount>` removes money from a player's balance.

`/economy set <player> <amount>` sets a player's balance.

Alias: `/eco`.

## shop

`/shop` opens the server shop. Alias: `/store`.

## auction

`/ah` opens the Auction House.

`/ah sell <price>` lists the item you are holding for sale.

`/ah remove <id>` removes one of your listings.

Aliases: `/auction`, `/auctionhouse`.

## ranks

`/rank list` displays available ranks.

`/rank create <name>` creates a rank.

`/rank delete <name>` deletes a rank.

`/rank give <player> <rank>` assigns a rank to a player.

`/rank remove <player>` removes a player's rank.

`/rank setprefix <rank> <prefix>` changes a rank's prefix.

`/rank setsuffix <rank> <suffix>` changes a rank's suffix.

`/rank setname <rank> <name>` changes a rank's display name.

`/rank info <rank>` displays information about a rank.

Alias: `/ranks`.

## kits

`/kit` opens the kits menu.

`/kit <name>` uses a kit.

`/kit create <name>` creates a kit.

`/kit delete <name>` deletes a kit.

`/kit edit <name>` edits a kit.

`/kit give <player> <kit>` gives a kit to a player.

`/kit list` displays available kits.

`/kit reload` reloads kit data.

Aliases: `/kits`, `/kitgui`, `/kitsgui`, `/kitsshop`, `/kitshop`.

## homes

`/sethome <name>` saves your current location as a home.

`/home <name>` teleports you to a saved home. Using `/home` without a name opens the homes menu.

`/homes` opens the homes menu.

`/delhome <name>` deletes a saved home.

## warps

`/setwarp <name>` creates a server warp at your current location.

`/warp <name>` teleports you to a warp. Using `/warp` without a name opens the warps menu.

`/warps` opens the warps menu.

`/delwarp <name>` deletes a server warp.

Alias for `/warp`: `/w`.

## nickname

`/nick <nickname>` changes your nickname.

`/nick off` removes your nickname.

`/nick color` opens or accesses nickname colour options.

Aliases: `/nickname`, `/nicky`.

## playerinfo

`/playerinfo` opens your player profile.

Aliases: `/pinfo`, `/info`, `/playerinfogui`.

## enderchest

`/enderchest` opens your Ender Chest.

Aliases: `/ec`, `/echest`.

## contracts

`/contracts` opens the Contracts menu.

Aliases: `/contract`, `/contractgui`, `/contractsgui`.

Requires `ssmp.contracts.use`.

## bounties

`/bounty` opens the Bounties menu.

Aliases: `/bounties`, `/bountygui`.

Requires `ssmp.bounties.use`.

## moderation

`/mute <player> <duration|permanent> <reason>` mutes a player.

`/unmute <player>` removes a player's mute.

`/warn <player> <reason>` warns a player.

`/ban <player> <duration|permanent> <reason>` bans a player.

`/unban <player>` removes a player's ban.

`/filter <add|remove|list|reload>` manages blocked chat terms.

`/history <player>` displays a player's moderation history.

`/resetviolations <player>` resets a player's chat-filter violation count.

## support

`/support` opens the support guide.

`/support <name>` opens documentation for a specific topic.

`/support <name> <subname>` opens a specific topic section.

`/help` is an alias for `/support`.


# permissions

SSMP uses permission nodes to control access to its features. Most administrative permissions default to operators, while common player features are available to everyone by default.

## moderation

`ssmp.moderation.admin` grants the full moderation permission group.

`ssmp.moderation.mute` allows muting players.

`ssmp.moderation.unmute` allows removing mutes.

`ssmp.moderation.warn` allows warning players.
`ssmp.moderation.ban` allows banning players.
`ssmp.moderation.unban` allows removing bans.
`ssmp.moderation.filter` allows managing blocked chat terms.
`ssmp.moderation.history` allows viewing moderation history.
`ssmp.moderation.reset` allows resetting chat-filter violations.
`ssmp.moderation.notify` allows receiving moderation notifications.
`ssmp.moderation.bypass` allows bypassing chat moderation.
All moderation permissions default to operators.

## ranks
`ssmp.rank.use` allows using rank commands.
`ssmp.rank.create` allows creating ranks.
`ssmp.rank.delete` allows deleting ranks.
`ssmp.rank.give` allows assigning ranks.
`ssmp.rank.remove` allows removing ranks.
`ssmp.rank.setprefix` allows changing rank prefixes.
`ssmp.rank.setsuffix` allows changing rank suffixes.
`ssmp.rank.setname` allows changing rank display names.
These permissions default to operators.

## permissions
`ssmp.permission.use` allows using SSMP permission management.
`ssmp.permission.player` allows managing player permissions.
`ssmp.permission.rank` allows managing rank permissions.
These permissions default to operators.

## economy
`ssmp.economy.use` allows using economy management commands.
`ssmp.economy.balance` allows viewing balances.
`ssmp.economy.pay` allows sending money to other players.
`ssmp.economy.give` allows giving money to players.
`ssmp.economy.take` allows taking money from players.
`ssmp.economy.set` allows setting player balances.
Balance and pay permissions default to everyone. The management permissions default to operators.

## shop
`ssmp.shop.use` allows using the server shop.
Defaults to everyone.

## auction
`ssmp.auction.use` allows accessing the Auction House.
`ssmp.auction.sell` allows listing items.
`ssmp.auction.buy` allows buying listings.
`ssmp.auction.remove` allows removing your own listings.
All four permissions default to everyone.

## kits
`ssmp.kits.use` allows using kits.
`ssmp.kits.create` allows creating and editing kits.
`ssmp.kits.delete` allows deleting kits.
`ssmp.kits.give` allows giving kits to players.
Using kits defaults to everyone. The management permissions default to operators.

## crates
`ssmp.crates.use` allows using crates.
`ssmp.crates.create` allows creating crates.
`ssmp.crates.delete` allows deleting crates.
`ssmp.crates.give` allows giving crate keys.
The crate permissions are included in the administrator permission group. Only `ssmp.crates.give` has an individual default explicitly declared, which is operator.

## nickname
`ssmp.nickname.use` allows using nicknames.
`ssmp.nickname.color` allows changing nickname colours.
Both permissions default to operators.

## homes
`ssmp.homes.use` allows viewing and using homes.
`ssmp.homes.sethome` allows setting homes.
`ssmp.homes.delete` allows deleting homes.
All three permissions default to everyone.

## warps
`ssmp.warps.use` allows using server warps.
`ssmp.warps.set` allows creating server warps.
`ssmp.warps.delete` allows deleting server warps.
Using warps defaults to everyone. Creating and deleting warps default to operators.

## other
`ssmp.rtp.use` allows using Random Teleport. Defaults to everyone.
`ssmp.enderchest.use` allows using the Ender Chest. Defaults to everyone.
`ssmp.contracts.use` allows opening the Contracts menu. Defaults to everyone.
`ssmp.bounties.use` allows opening the Bounties menu. Defaults to everyone.
`ssmp.playerinfo.use` allows opening the Player Information menu. Defaults to everyone.
## administrator
`ssmp.moderation.admin` is the main administrator permission group. It includes moderation, rank management, permission management, economy administration, shop access, Ender Chest access, Auction House access, orders, kits, crates, nicknames, homes, warps, and Random Teleport permissions.
You can assign individual permissions or use the administrator group through your permissions manager.

# Placeholders
Note: Works only with placeholder API, but only some of them are default in the plugin...

## Default
%ssmp_player_name% - Name of player
%ssmp_player_displayname% - Display name of player
%ssmp_player_uuid% - Player UUID
%ssmp_player_health% - Player health (Formatted)
%ssmp_player_max_health% - Player MAX health (Formatted)
%ssmp_player_food% - Player food bar
%ssmp_player_level% - Player level
%ssmp_player_exp% - Player experience
%ssmp_player_world% - Player current standing world
%ssmp_player_x% - Player X
%ssmp_player_y% - Player Y
%ssmp_player_z% - Player Z
%ssmp_player_gamemode% - Player gamemode
%ssmp_player_ping% - Player ping
%ssmp_player_online% - Total players online

## Ranks
%ssmp_rank% - Rank ID
%ssmp_rank_name% - Rank Displayname
%ssmp_rank_prefix% - Rank prefix
%ssmp_rank_suffix% - Rank suffix
%ssmp_rank_inherits% - Rank inherits perms from
%ssmp_player_rank_color% - Rank Color
%ssmp_player_has_permission_<permission>% - Boolean permission check for player
%ssmp_player_is_op% - Boolean OP or NOT

## Economy
%ssmp_balance% - Player balance ($ sign)
%ssmp_balance_formatted% - Player formatted balance (Commans + $ sign)
%ssmp_balance_raw% - Player raw balance (just no.s)
%ssmp_money_symbol% - Money symbol
%ssmp_baltop_position% - Rank of player
%ssmp_baltop_balance% - Balance of baltop player

## Contracts
%ssmp_contracts_active% - Active contracts
%ssmp_contracts_completed% - Total Completed Contracts
%ssmp_contracts_total% - Total contracts
%ssmp_contract_<id>_name% - Contract displayname
%ssmp_contract_<id>_description% - Contract description
%ssmp_contract_<id>_accepted% - Contract accepted (boolean)
%ssmp_contract_<id>_completed% - Contract completed (boolean)
%ssmp_contract_<id>_tasks_total% - Total tasks
%ssmp_contract_<id>_tasks_completed% - Tasks completed
%ssmp_contract_<id>_progress% - Progress (%)
%ssmp_contract_<id>_reward_money% - Contract reward Money
%ssmp_contract_<id>_reward_xp% - Contract reward XP
%ssmp_contract_<id>_reward_levels% - Contract reward LVLS

## Bounties
%ssmp_bounty% - Total bounty of player
%ssmp_bounty_claimed_total% - Bounty claimed (TOTAL) by player in money
%ssmp_bounty_claimed_count% - Bounty claimed COUNT
%ssmp_bounty_<player>_amount% -

## Levels
%ssmp_level% - Player level
%ssmp_level_xp% - XP of current level of player
%ssmp_level_required% - Required XP for level up
%ssmp_level_remaining% - Remaining XP for level up
%ssmp_level_progress% - Progress of level up
%ssmp_level_progress_bar% - Progress bar
%ssmp_level_next% - Next level
%ssmp_level_total_xp% - Total XP of level

## Homes
%ssmp_homes_count% - Count of total homes
%ssmp_homes_max% - Count of maximum homes
%ssmp_homes_remaining% - Count of remaining homes
%ssmp_home_<name>_world% - World of home
%ssmp_home_<name>_x% - home X
%ssmp_home_<name>_y% - home Y
%ssmp_home_<name>_z% - home Z

## RTP
%ssmp_rtp_cooldown% - RTP cooldown
%ssmp_rtp_available% - RTP availability boolean
%ssmp_rtp_cooldown_formatted% - RTP cooldown formatted


## Nickname
%ssmp_nickname% - Nickname of player
%ssmp_nickname_color% - Nickname color of player

## Auction
%ssmp_auction_listings% - Total listings of player
%ssmp_auction_sales% - Total sales of player
%ssmp_auction_active% - Currently ative listings
%ssmp_auction_top_price% - Top price