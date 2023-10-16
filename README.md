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
| /timer              | srvcron.command.timer    | `/timer <seconds> <command>` - this will execute a command after specified delay. (Max time is 30 min.)

`<>` neccessary; `[]` optional.

### Development
Building is really simple.

To build SRV-Cron, you need JDK 8 or higher and Gradle installed on your system.

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
