# MC World to Polar

Desktop-приложение на Kotlin/Swing для конвертации Minecraft Anvil-мира в формат Polar и выгрузки сущностей мира в JSON.

## Готовые сборки

Windows release содержит:

- `MC-World-to-Polar-<version>-windows-x64.exe` — установщик для текущего пользователя;
- `MC-World-to-Polar-<version>-windows-x64-portable.zip` — portable-приложение;
- `MC-World-to-Polar-<version>.jar` — executable fat JAR;
- `SHA256SUMS.txt` — контрольные суммы артефактов.

Установщик и portable ZIP включают Java Runtime. Пользователю не требуется отдельно устанавливать Java.

## Использование

1. Запустите приложение.
2. Нажмите «Выбрать директорию мира» и укажите папку Minecraft-мира.
3. Нажмите «Сохранить как Polar».

Текущая версия создаёт `<путь-к-миру>.polar` рядом с выбранной папкой и `entities.json` внутри выбранной папки мира. Используйте копию мира до устранения документированного side effect.

## Локальная сборка

Требуется JDK 24. `JAVA_HOME` должен указывать на полный JDK. Более новый JDK может собрать bytecode через `--release 24`, но Windows package-скрипт намеренно требует именно JDK 24, чтобы bundled Runtime совпадал с release-конфигурацией.

```powershell
.\mvnw.cmd clean verify
java -jar target\mcWorldToPolar.jar --version
java -jar target\mcWorldToPolar.jar
```

Для Unix-подобной среды используйте `./mvnw`.

## Windows package

Дополнительно требуется WiX Toolset 3.14.1, доступный в `PATH`.

```powershell
.\scripts\package-windows.ps1 -Version 1.0.0
```

Скрипт собирает JAR, app-image с включённым Runtime, portable ZIP, EXE installer и SHA-256 в `target/release`.

Иконки можно воспроизвести командой:

```powershell
.\scripts\generate-icons.ps1
```

## Releases

GitHub Actions workflow `.github/workflows/release.yml` поддерживает:

- ручной `workflow_dispatch` — собирает проверяемые CI artifacts без публикации релиза;
- tag `vX.Y.Z` — собирает и публикует GitHub Release.

Пример:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Архитектура и известные ограничения

Приоритетный список архитектурных и UI-улучшений находится в [docs/architecture-and-ui-review.md](docs/architecture-and-ui-review.md).
