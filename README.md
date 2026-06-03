<!-- Variables -->

[resourceId]: 100382

[buildImage]: https://github.com/M0diis/M0-CoreCord/actions/workflows/gradle.yml/badge.svg
[releaseImage]: https://img.shields.io/github/v/release/M0diis/SRV-Cron.svg?label=github%20release
[licenseImage]: https://img.shields.io/github/license/M0diis/SRV-Cron.svg
[ratingImage]: https://img.shields.io/badge/dynamic/json.svg?color=brightgreen&label=rating&query=%24.rating.average&suffix=%20%2F%205&url=https%3A%2F%2Fapi.spiget.org%2Fv2%2Fresources%2F100382
[downloadsImage]: https://img.shields.io/badge/dynamic/json.svg?color=brightgreen&label=downloads%20%28spigotmc.org%29&query=%24.downloads&url=https%3A%2F%2Fapi.spiget.org%2Fv2%2Fresources%2F100382
[spigotVersionImage]: https://img.shields.io/badge/dynamic/json.svg?color=brightgreen&label=latest%20version%20%28spigotmc.org%29&query=%24.name&url=https%3A%2F%2Fapi.spiget.org%2Fv2%2Fresources%2F100382%2Fversions%2Flatest

<!-- End of variables block -->

![build][buildImage] ![release][releaseImage] ![license][licenseImage]
 ![rating][ratingImage] ![downloads][downloadsImage] ![spigotVersion][spigotVersionImage]

## SRV-Cron
Scheduler, jobs, events for your server. Schedule anything that your server needs.

### Commands & Permissions

| Command             | Permission               | Usage & Description           
|---------------------|--------------------------|--------------------
| /srvcron reload     | srvcron.command.reload   | Reload jobs & configuration.        
| /srvcron run        | srvcron.command.run      | `/srvcron run <job-name> [event <event-name]` - run a job manually.
| /srvcron suspend    | srvcron.command.suspend  | `/srvcron suspend <job-name>` - suspend job execution. The commands will not be executed on job dispatch.
| /srvcron resume     | srvcron.command.resume   | `/srvcron resume <job-name>` - resume a job. Allow the job to execute commands on the next dispatch.
| /srvcron jobinfo    | srvcron.command.jobinfo  | `/srvcron jobinfo <job-name>` - information about a Cron-Job.
| /srvcron list       | srvcron.command.list     | `/srvcron list [events]` - this will list all Cron or, if provided required argument, Event jobs.
| /srvcron schedule   | srvcron.command.schedule | `/srvcron schedule <job-name> [N]` - preview the next scheduled runs for a job.
| /srvcron validate   | srvcron.command.schedule | `/srvcron validate <time-expression>` - parse/validate an expression and show the next 5 runs.
| /timer              | srvcron.command.timer    | `/timer <seconds> <command>` - this will execute a command after specified delay. (Max time is 30 min.)

`<>` neccessary; `[]` optional.

### Time Expression Syntax

SRV-Cron supports the original DSL and a broader syntax for readability and advanced use-cases.

Examples:

```yaml
# named weekdays
time: every wednesday at 00:00
time: every monday,friday at 18:30

# list/range support
time: every day of week in 1,3,5 at 12:00
time: every day of month in 1..5 at 09:00

# multiple times per day
time: every day at 08:00,12:00,18:00

# time windows
time: every 15 minutes from 09:00 to 17:00

# nth / last weekday in month
time: every 2nd monday of month at 10:00
time: every last friday of month at 22:00

# relative calendar keywords
time: every weekday at 09:00
time: every weekend at 11:00
time: every month on last-day at 23:55

# start/end constraints
time: every 1 hour between 2026-06-01 and 2026-09-01

# per-job timezone and jitter
time: every day at 09:00 timezone Europe/Berlin
time: every 5 minutes jitter 30s

# one-shot execution
time: at 2026-06-10 14:30

# classic cron expression (opt-in)
time: cron: 0 0 * * 3
```

Notes:
- Cron format is `minute hour day-of-month month day-of-week`.
- Day-of-week DSL numbers stay compatible with existing configs (`1=Sunday ... 7=Saturday`).
- `schedule` and `checkschedule` are aliases.

### Development
Building is really simple.

To build SRV-Cron, you need JDK 21 and Gradle installed on your system.

The Bukkit implementation targets Paper API `1.21.11-R0.1-SNAPSHOT`.

```
git clone https://github.com/M0diis/SRV-Cron.git
cd SRV-Cron
gradlew shadowjar
```

The jar will be generated in `/build/libs/` folder. 

### Dev-builds

All the development builds can be found on actions page. These builds will have the latest features but may include some bugs and other unwanted issues. Use at your own risk.

Open the latest workflow and get the artifact from there:  
https://github.com/M0diis/SRV-Cron/actions

### API

SRV-Cron comes with an API that allows developers to interact with the plugin. If you think there is something missing in the API - feel free to create a feature or pull request.

Before you can actually make use of SRV-Cron API, you first have to import it into your project.
You can get the access to the API simply by depending on the plugin itself.  
Replace `{VERSION}` with the latest build release.

#### Import with Gradle
```groovy
compileOnly 'com.github.m0diis:srv-cron:{VERSION}'
```
#### Import with Maven
```xml
<dependency>
 <groupId>com.github.m0diis</groupId>
  <artifactId>srv-cron</artifactId>
  <version>{VERSION}</version>
 <scope>provided</scope>
</dependency>
```
Get the API:
```java
public static void myMethod()
{
    SRVCronAPI api = SRVCron.getInstance().getAPI();
}
```

Read more about the API usage on the wiki page:  
https://github.com/M0diis/SRV-Cron/wiki/API

#### Links

- [Spigot Page](https://www.spigotmc.org/resources/100382/)
- [Issues](https://github.com/M0diis/SRV-Cron/issues)
  - [Bug report](https://github.com/M0diis/SRV-Cron/issues/new?assignees=&labels=bug&template=bug_report.md&title=)
  - [Feature request](https://github.com/M0diis/SRV-Cron/issues/new?assignees=&labels=enhancement&template=feature.md)
- [Pull requests](https://github.com/M0diis/SRV-Cron/pulls)

## FAQ

#### I have a feature suggestion | I found a bug
Please open a Github [issue](https://github.com/M0diis/SRV-Cron/issues) or reach out to me via SpigotMC or, preferably, [discord](https://discord.gg/ZSzJTSWxmv).

#### Where can I download the plugin from?
You can get it from:
- [Spigot Page](https://www.spigotmc.org/resources/100382/)
- [Releases](https://github.com/M0diis/SRV-Cron/releases) here on Github.
- Compile it yourself.

#### Can I modify your plugin?
Yes, you can, as long as you comply with the license. 

#### How can I collaborate on the project?
- You can find bugs or issues and notify me about them;
- You can add features or fix issues by forking the project, editing it and creating a Pull Request;
- You can translate language files and open a Pull Request or send them directly to me.
