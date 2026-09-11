# Тоника

Нативный Android-клиент для [Navidrome](https://www.navidrome.org/): библиотека по исполнителям и альбомам, поиск, скачивание треков и менеджер офлайн-музыки.

## Возможности

- Вход на любой Navidrome-сервер (Subsonic / OpenSubsonic API). Сессия запоминается.
- Сортировка библиотеки по исполнителям и альбомам.
- Поиск по исполнителям, альбомам и трекам.
- Выбор папки для загрузок (системный проводник).
- Менеджер скачанного: удалить трек, альбом или всего исполнителя.
- Проигрывание с сервера и из офлайна.
- Material 3, тёмная тема, акцентный бирюзовый.

## Установка APK

После каждого пуша в `main` GitHub Actions собирает отладочный APK.

1. Откройте [Actions](https://github.com/Nasty1028339219182/tonica/actions).
2. Выберите последний успешный прогон **Android APK**.
3. Скачайте артефакт `tonica-debug`.
4. На телефоне разрешите установку из неизвестных источников и откройте `app-debug.apk`.

Готовый файл также публикуется в [Releases](https://github.com/Nasty1028339219182/tonica/releases), когда сборка проходит.

Демо-сервер для проверки: `https://demo.navidrome.org`, логин и пароль `demo`.

## Сборка у себя

Нужны JDK 17 и Android SDK (compileSdk 35).

```bash
cd android
./gradlew assembleDebug
```

APK появится в `android/app/build/outputs/apk/debug/app-debug.apk`.

## Структура

```
android/          Jetpack Compose-приложение (Kotlin)
  app/src/main/java/app/tonica/
    data/         Navidrome API, сессия, загрузки
    player/       ExoPlayer
    ui/           Material 3 экраны
```

Клиент ходит на `/auth/login`, затем в Subsonic REST (`u`, `t`, `s`, `v=1.16.1`, `c=Tonica`, `f=json`).
