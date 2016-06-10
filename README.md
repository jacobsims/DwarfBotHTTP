# DEPRECATED, development continues at [Choco31415/DwarfBot](https://github.com/Choco31415/DwarfBot)

---

Old readme continues below.

---

A web interface for [DwarfBot](https://github.com/Choco31415/DwarfBot)

### Setup

Requires Java 8

```sh
$ git clone 'https://github.com/jacobsims/DwarfBotHTTP.git'
$ cd DwarfBotHTTP
$ ./gradlew run # Now go to http://localhost:4567
```

```sh
$ ./gradlew shadowJar # build a single jar with all dependencies contained
$ java -jar build/libs/DwarfBotHTTP-all.jar # run it
```

### Slack

To submit failure reports with Slack, you will need to make a config file at `~/.config/dwarfbothttp/config.json`, with the format:

```json
{
    "slackToken": "xxxx-xxxxxxxxx-xxxx",
    "slackChannel": "channel-name"
}
```
