# SRV-Cron

Continuation of the well-known Cron plugin — [MC-Cron](https://www.spigotmc.org/resources/mc-cron-scheduler-jobs-bungeecord-support.37632/).

All credits go to the original author — **TheTadeSK**.

SRV-Cron is an all-in-one Minecraft server scheduler that has many features to schedule jobs for certain times or in-game events.

It is a continuation of the well-known Cron plugin MC-Cron, with many additional and expanded features that the original MC-Cron plugin does not provide, such as PlaceholderAPI support, sending commands as players, variable actions, and condition checking.

**Current release:** 2.14.0 · **Target:** Paper 26.2 · **Java:** 25

---

# Features

These are some of the features that this plugin provides.

- Customizable jobs
- Console, player, and operator command execution
- Execute commands by every online player
- Send messages, titles, and action bars
- Play sounds and spawn particle effects
- Broadcast messages and write scheduled logs
- Filter actions by permissions, PlaceholderAPI values, or scoreboard scores
- Event jobs & commands
- Start-up commands
- Delayed command execution with `/timer`
- **BungeeCord** support
- **PlaceholderAPI** support

## Command Actions & Conditions

Scheduled commands run as the console by default. Prefix a command with an action to send a message, show a title or action bar, play a sound, spawn particles, chat or run a command as a player, run as an operator, broadcast, or write a log message. Supported prefixes are `[MESSAGE]`/`[TEXT]`, `[TITLE]`, `[ACTIONBAR]`, `[SOUND]`, `[PARTICLE]`, `[CHAT]`, `[PLAYER]`, `[OP]`, `[CONSOLE]`, `[BROADCAST]`, `[LOG]`, and `[LOG <file>]`.

Messages, titles, action bars, and broadcasts support MiniMessage and legacy `&`/`§` color codes, including RGB formats. Prefix conditions support permissions, PlaceholderAPI comparisons (when PlaceholderAPI is installed), scoreboard scores, and combinations using `&&` and `||`:

```yaml
- '{IF:hasPermission:vip} [MESSAGE] <gold>Welcome, %player_name%!'
- '{IF:placeholder:%vault_eco_balance%>=1000} [CONSOLE] give %player_name% diamond 1'
- '{IF:score:points>=10} [ACTIONBAR] &aYou have enough points.'
```

Conditions require a player context, such as an event job or a command dispatched for `<ALL>`/`<ALL+>`. Existing action-header filters such as `[TEXT (PERMISSION:staff)] ...` remain supported.

## Available Events

Use these values in the configuration:

- `join-event`
- `quit-event`
- `weather-change-event`
- `world-load-event`
- `player-bed-enter-event`
- `player-bed-leave-event`
- `player-change-world-event`
- `player-gamemode-change-event`
- `player-kick-event`
- `chat-event`
- `command-event`
- `item-pickup-event`
- `player-advancement-done-event`

## Generic Paper/Bukkit Event Jobs

`generic-event-jobs` can register any Bukkit/Paper `Event` class that provides
the standard `HandlerList`, including events from enabled plugins. Configure its
fully qualified class name, event priority, cancelled-event handling, and
optional player/world getter paths. Event properties are read through public
zero-argument `getX()`/`isX()` getters; list and array values accept numeric
`[index]` access. Use `{event.<path>}` directly or define shorter aliases under
`placeholders`. Event values are captured when the event fires, including for
delayed jobs.

```yaml
generic-event-jobs:
  item-pickup:
    event: org.bukkit.event.player.PlayerAttemptPickupItemEvent
    priority: NORMAL
    ignore-cancelled: false
    context:
      player: player
      world: player.world
    placeholders:
      item_type: item.itemStack.type
      item_amount: item.itemStack.amount
    jobs:
      announce:
        time: 0
        commands:
          - '[MESSAGE] Picked up {event.item.itemStack.amount} {item_type}'
```

Player and world context are inferred when an event exposes one unambiguous
value; `context.player` and `context.world` select a specific getter path when
needed. Existing `event-jobs` names and BungeeCord join/quit events remain
available.

## Built-in Placeholders

- `{player_name}` — for events that relate to a player
- `{world_name}` — for events that relate to a world

And many more event-specific placeholders.

Read more here:  
https://github.com/M0diis/SRV-Cron/wiki/Configuration#events

You can use both placeholders if the event relates to both a player and a world, for example `player-change-world-event`.

> **BungeeCord has only two events available: join and quit.**

---

# Commands & Permissions

## Run command after specified seconds

Command:

```text
/timer <seconds> <command>
```

Permission:

```text
srvcron.command.timer
```

## Reload configuration & jobs

Command:

```text
/srvcron reload
```

Permission:

```text
srvcron.command.reload
```

## Run cron or event job manually

Command:

```text
/srvcron run <job-name>
/srvcron run event <event-name>
```

Permission:

```text
srvcron.command.run
```

## Suspend job execution

Command:

```text
/srvcron suspend <job-name>
```

Permission:

```text
srvcron.command.suspend
```

## Resume job execution

Command:

```text
/srvcron resume <job-name>
```

Permission:

```text
srvcron.command.resume
```

## Job information

Command:

```text
/srvcron jobinfo <job-name>
```

Permission:

```text
srvcron.command.jobinfo
```

## Job & event list

Command:

```text
/srvcron list [events]
```

Permission:

```text
srvcron.command.list
```

---

# Configuration

Configuration is pretty simple and easy to understand.

For a more detailed explanation, visit the GitHub Wiki:

**[Configuration · M0diis/SRV-Cron Wiki](https://github.com/M0diis/SRV-Cron/wiki/Configuration)**

## Default Configuration

```yaml
jobs:
  save:
    time: every 1 hour
    commands:
      - say Saving world!
      - save-all
      - say Save Complete!
  restart:
    time: every 1 day of week at 6:00
    commands:
      - say Server restart in 10 seconds!
      - timer 10 stop
  tps:
    time: every 30 minutes
    commands:
      - tps

# If you do not want to use any event jobs, use:
# event-jobs: { }
event-jobs:
  join-event:
    welcome:
      time: 1
      commands:
        - tell {player_name} Hello!
        - <ALL>[CHAT] Hello {player_name}!
  quit-event:
    bye:
      time: 5
      commands:
        - say {player_name} left the game few seconds ago.
  player-gamemode-change-event:
    notify:
      time: 0
      commands:
        - '<ALL> [TEXT (PERM:staff.notify.gamemode)] Player {player_name} changed his gamemode from {from_gamemode} to {to_gamemode}.'

# Generic Bukkit/Paper events can also be configured with their class name:
# generic-event-jobs:
#   item-pickup:
#     event: org.bukkit.event.player.PlayerAttemptPickupItemEvent
#     priority: NORMAL
#     ignore-cancelled: false
#     context:
#       player: player
#       world: player.world
#     placeholders:
#       item_type: item.itemStack.type
#     jobs:
#       announce:
#         time: 0
#         commands:
#           - '[MESSAGE] Picked up {event.item.itemStack.amount} {item_type}'

startup:
  commands:
    - say Server was started!
    - timer 60 say Server is online for 1 minute!
    - save-all

schedule:
  # Controls numeric weekdays in expressions like: every day of week in 1,5 at 12:00
  # Supported: monday-first (default, ISO), sunday-first (legacy), iso, legacy
  weekday-numbering: monday-first

debug: false
silent-start: false
notify-update: true
log-to-file: true
locale: 'en'
```

## Syntax


```yaml
# named weekdays
time: every wednesday at 00:00
time: every monday,friday at 18:30

# list/range support
time: every day of week in 1,3,5 at 12:00
time: every day of week in 2..6 at 07:45
time: every day of month in 1..5 at 09:00

# plain intervals
time: every 30 seconds
time: every 5 minutes
time: every 1 hour
time: every 2 days

# multiple times per day
time: every day at 08:00,12:00,18:00

# time windows
time: every 15 minutes from 09:00 to 17:00
time: every 20 minutes from 23:00 to 03:00

# nth / last weekday in month
time: every 2nd monday of month at 10:00
time: every last friday of month at 22:00

# relative calendar keywords
time: every weekday at 09:00
time: every weekend at 11:00
time: every month on last-day at 23:55

# month-name schedules
time: every january,march day 1 at 08:00

# start/end constraints
time: every 1 hour between 2026-06-01 and 2026-09-01

# per-job timezone and jitter
# options order is: ... [between ...] [jitter ...] [timezone ...]
time: every day at 09:00 timezone Europe/Berlin
time: every 5 minutes jitter 30s
time: every 10 minutes jitter 2m timezone Europe/Berlin

# one-shot execution
time: at 2026-06-10 14:30

# classic cron expression (opt-in)
time: "cron: 0 0 * * 3"
time: "cron: */15 9-17 * * mon-fri"
```

Notes:
- Cron format is `minute hour day-of-month month day-of-week`.
- Cron supports `*`, lists (`,`), ranges (`-`), and steps (`/`) in each field.
- Cron month/day-of-week names are supported (e.g. `jan`, `mon-fri`).
- Day-of-week DSL numbers stay compatible with existing configs (`1=Sunday ... 7=Saturday`).
- Legacy expressions are still accepted for backward compatibility:
  - `every 4 day of week at 00:00` (or without `at`, defaults to `00:00`)
  - `every 1 day of month at 09:00` (or without `at`, defaults to `00:00`)

---

# API

For a more detailed explanation, visit the GitHub Wiki:

**[API · M0diis/SRV-Cron Wiki](https://github.com/M0diis/SRV-Cron/wiki/API)**

There are currently 3 events that are fired upon certain actions:

- [CronJobDispatchEvent](https://github.com/M0diis/SRV-Cron/blob/main/src/main/java/me/m0dii/srvcron/managers/CronJobDispatchEvent.java)
- [EventJobDispatchEvent](https://github.com/M0diis/SRV-Cron/blob/main/src/main/java/me/m0dii/srvcron/managers/EventJobDispatchEvent.java)
- [StartupCommandDispatchEvent](https://github.com/M0diis/SRV-Cron/blob/main/src/main/java/me/m0dii/srvcron/managers/StartupCommandDispatchEvent.java)

You can also interact with the [API](https://github.com/M0diis/SRV-Cron/blob/main/src/main/java/me/m0dii/srvcron/SRVCronAPI.java).
