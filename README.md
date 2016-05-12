netbeans-wakatime
=================

Metrics, insights, and time tracking automatically generated from your programming activity.


Installation
------------

1. Download the `.nbm` plugin from [GitHub releases](https://github.com/wakatime/netbeans-wakatime/releases/latest).

2. Inside your IDE, select `Tools` -> `Plugins` -> `Downloaded` -> `Add Plugins...`

3. Select the downloaded nbm file.

4. Check `WakaTime` and click the `Install` button.
   
5. Follow the wizard instructions to complete the installation.

6. Enter your [api key](https://wakatime.com/settings#apikey), then click `OK`.

7. Use your IDE like you normally do and your time will be tracked for you automatically.

8. Visit https://wakatime.com to see your logged time.

Screen Shots
------------

![Project Overview](https://wakatime.com/static/img/ScreenShots/Screen-Shot-2016-03-21.png)


Troubleshooting
---------------

Netbeans logs to it's own log file (`View` -> `IDE Log`).

After the plugin passes control to [wakatime-cli][wakatime cli], logs go to the common `~/.wakatime.log` file.

For more general troubleshooting information, see [wakatime cli][wakatime cli].

[wakatime cli]: https://github.com/wakatime/wakatime#troubleshooting
