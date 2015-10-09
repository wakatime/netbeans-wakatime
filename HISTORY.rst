
History
-------


4.0.0 (2015-10-09)
++++++++++++++++++

- fix location of resources directory
- get current wakatime cli version number from GitHub repository


3.0.10 (2015-08-03)
++++++++++++++++++

- obfuscate api key in debug log


3.0.9 (2015-08-03)
++++++++++++++++++

- fix NullPointerException in auto update handler


3.0.8 (2015-08-03)
++++++++++++++++++

- send heartbeat in background thread
- update wakatime cli to v4.1.0
- guess language using multiple methods, then use most accurate guess
- use entity and type for new heartbeats api resource schema
- fix offline logging
- limit language detection to known file extensions, unless file contents has a vim modeline
- correct priority for project detection



3.0.7 (2015-06-05)
++++++++++++++++++

- update wakatime cli to v4.0.14
- catch NullPointerException from UpdateHandler


3.0.6 (2015-05-04)
++++++++++++++++++

- update wakatime cli to v4.0.8
- catch NullPointerException when downloading new wakatime plugin update
- download and install python on Windows OS when not already available


3.0.5 (2015-03-28)
++++++++++++++++++

- only update wakatime plugin from autoupdate container


3.0.4 (2015-03-18)
++++++++++++++++++

- cancel update container progress upon update error


3.0.3 (2015-03-10)
++++++++++++++++++

- upgrade external wakatime-cli package to v4.0.4


3.0.2 (2015-03-06)
++++++++++++++++++

- upgrade external wakatime-cli package to v4.0.1
- use requests library instead of urllib2, so api SSL cert is verified
- new proxy config file item for https proxy support
- upgrade requests library to v2.5.3 to fix SSL problem on CentOS
- new options for excluding and including directories
- new --notfile argument to support logging time without a real file


3.0.1 (2014-12-25)
++++++++++++++++++

- upgrade external wakatime package to v3.0.3
- detect JavaScript frameworks from script tags in Html template files


3.0.0 (2014-12-23)
++++++++++++++++++

- upgrade external wakatime package to v3.0.1
- detect libraries and frameworks for C++, Java, .NET, PHP, and Python files


2.0.7 (2014-12-22)
++++++++++++++++++

- upgrade external wakatime package to v2.1.11
- fix bug in offline logging when no response from api


2.0.6 (2014-12-16)
++++++++++++++++++

- dedicated tab in options menu for WakaTime settings
- upgrade external wakatime package to v2.1.10


2.0.5 (2014-12-08)
++++++++++++++++++

- only check for updates when plugin first loaded


2.0.4 (2014-12-07)
++++++++++++++++++

- use NetBeans project as current project if available


2.0.3 (2014-12-05)
++++++++++++++++++

- upgrade external wakatime package to v2.1.9
- fix bug preventing offline heartbeats from being purged after uploaded


2.0.2 (2014-12-03)
++++++++++++++++++

- improve messages in autoupdate progress handlers
- upgrade external wakatime package to v2.1.7


2.0.1 (2014-11-25)
++++++++++++++++++

- detect current NetBeans project


2.0.0 (2014-11-25)
++++++++++++++++++

- auto-update plugin


1.0.1 (2014-11-25)
++++++++++++++++++

- handle case where fileObject is null


1.0.0 (2014-11-20)
++++++++++++++++++

- Birth

