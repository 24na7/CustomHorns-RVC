# 🔉 CustomHorns SVC

Плагин позволяющий менять стандартные звуки на свои кастомные для предмета "Козьий рог"

Идея для плагина была с другого плагина **[CustomDiscs SVC](https://modrinth.com/plugin/customdiscs-svc)**
Можно сказать, что это является обычным **форком** который заменяет функционал на одном предмете в другой.

## ✅ Зависимости

* Скачайте зависимость **[SimpleVoiceChat-bukkit](https://modrinth.com/plugin/simple-voice-chat)**
* Скачайте зависимость **[ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)**
* Скачайте зависимость **[CommandAPI](https://modrinth.com/plugin/commandapi)**

## 🛠 Дорожная карта
- **Длительная поддержка плагина**
- **Поддержка других языков**
- **Детальная инструкция к использованию плагина**

## ⚙️ Настройки, команды и права

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

**1. Планируется ли поддержка других ядер, а также переход на новые версии?**

Да, я буду продолжать поддерживать плагин, пока занимаюсь моддингом, но в ближайшее время обновления до новых версий будут выходить очень редко из-за сложных жизненных обстоятельств.

**2. Плагин не работает, выдает много ошибок. Как с вами связаться?**

Вы можете создать тему (issue) на GitHub с приложенным логом ошибок. Я постараюсь исправить баг в плагине и решить вашу проблему.

**3. Как увеличить лимит аудиофайла с 8 секунд до большего?**

В файле config.yml есть параметр `max-duration: 8`. Измените это значение, но будьте осторожны с установкой слишком больших значений, так как это может вызвать ошибки.

---