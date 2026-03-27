# 🔉 CustomHorns SVC

A plugin that allows you to replace the standart sounds in goat horn with your own custom sounds.

I borrowed the idea for this plugin from the **[CustomDiscs SVC](https://modrinth.com/plugin/customdiscs-svc)** plugin.
You could say that this is a **fork** of that plugin, since it uses the same code.

## ✅ Dependencies

* Download dependency **[SimpleVoiceChat-bukkit](https://modrinth.com/plugin/simple-voice-chat)**
* Download dependency **[ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)**
* Download dependency **[CommandAPI](https://modrinth.com/plugin/commandapi)**

## 🛠 Roadmap
- **Long-term plugin support**
- **Support for other languages**
- **Detailed instructions for use within the plugin**

## ⚙️ Configuration, Commands & Permissions

```json5
info:
  # Don't change this value
  version: '1.0'
global:
  # Language of the plugin
  # Supported: ru_RU, en_US
  # Unknown languages will be replaced with en_US
  locale: en_US
  check-updates: true
  debug: false
command:
  create:
    local:
      custom-model: 0
    remote:
      # tabcomplete — Displaying hints when entering remote command
      # filter — Filter for applying custom-model-data to remote horns
      tabcomplete:
        - https://www.youtube.com/watch?v=
        - https://soundcloud.com/
      youtube:
        custom-model: 0
        filter:
          - https://www.youtube.com/watch?v=
          - https://youtu.be/
      soundcloud:
        custom-model: 0
        filter:
          - https://soundcloud.com/
  distance:
    max: 64
  download:
    # The maximum download size in megabytes
    max-size: 25
horn:
  # Maximum duration of horn sounds in seconds (7-8 recommended)
  max-duration: 8
  # The master volume of horns from 0-1
  volume: '1.0'
  # Default distance in blocks that horn sound can be heard
  default-distance: 32
  cooldown:
    # Enable cooldown between horn uses
    enabled: true
    # Cooldown time in seconds
    seconds: 5
  # Particle type when using horn: note, music, spell, or none
  particle-type: note
youtube:
  # This may help if the plugin is not working properly.
  # When you first play after the server starts, you will see an authorization request.
  use-oauth2: false
  # If you have oauth2 enabled, leave these fields blank.
  # This may help if the plugin is not working properly.
  po-token:
    token: ''
    visitor-data: ''
  # A method for obtaining streaming via a remote server.
  # Make sure Oauth2 was enabled!
  remote-server:
    url: ''
    password: ''
```

```json5
With the guys, we use the easiest method via Google Drive.
/ch download "URL" namefile.mp3 # download in google drive
/ch create local namefile.mp3 "name horn" # created mp3 in local host disk

Other command
/ch distance (radius) - change to your radius voice horn
/ch reload - reload to plugin
/ch help - showcase help
/ch create remote "url" namefile.mp3 - this download other site mp3
```

```json5
customhorns.create - Allow access create custom horns
customhorns.create.local/remote - Allow access create custom horns in local/remote disk host
customhorns.download - Allow acces download in mp3/wav/ogg in drivegoogle/youtube/soundcloud
customhorns.distance - Change your distance in voice horns
customhorns.help - Allow showcase in /ch help
customhorns.reload - Allow reload in plugin
```

## ❓ F.A.Q
**1. Will support for other kernels be planned, as well as migration to new versions?**

Yes, I will continue to support the plugin as long as I am a modder, but in the near future, updates to new versions will be very rare due to difficulties in my personal life.

**2. The plugin isn't working, I'm getting a lot of errors. How can I contact you?**

You can create an issues branch on GitHub with your crash log/errors. I will try to fix the bug in the plugin and solve your problem.

**3. How can I increase the limit from 8 seconds audiofile to more?**

In config.yml, there is a value max-duration: 8. Change it, but be careful when setting large values, as there may be bugs.

---