# Аудит архитектуры и интерфейса

## Текущая архитектура

Поток выполнения линейный:

1. `src/main/kotlin/com/bananvovan/mcworldtopolar/Main.kt:7-16` загружает метаданные, настраивает FlatLaf и создаёт Swing-окно.
2. `src/main/kotlin/com/bananvovan/mcworldtopolar/module/gui/MainFrame.kt:34-63` выбирает папку и напрямую запускает singleton-конвертер.
3. `src/main/kotlin/com/bananvovan/mcworldtopolar/module/convert/WorldToPolar.kt:13` управляет Minestom, конвертацией Anvil → Polar и вызовом JSON extractor.
4. `src/main/kotlin/com/bananvovan/mcworldtopolar/module/extract/EntityJsonSaver.kt:17` обходит MCA-файлы, разбирает NBT, формирует нетипизированные структуры и сериализует их Gson.

Для небольшого прототипа поток понятен, но UI, orchestration, файловый ввод-вывод и parsing жёстко связаны. Это осложняет обработку ошибок, тестирование и развитие формата.

## P0 — исправить до расширения функциональности

### Гарантированно восстанавливать состояние UI после ошибки

`MainFrame.kt:43-63` запускает сырой `Thread`. Кнопка включается повторно только в success-ветке, а исключение печатается через `printStackTrace`. После ошибки интерфейс остаётся заблокированным и пользователь не получает объяснения.

**Улучшение:** заменить поток на `SwingWorker`/executor, возвращать `ConversionResult`, показывать error dialog и восстанавливать кнопки в едином `finally`/`done`.

### Сделать выходные пути явными и не менять исходный мир

`MainFrame.kt:12` обещает «Сохранить как Polar», но save-dialog отсутствует. `WorldToPolar.kt:19-20` сам формирует `$path.polar`, а `EntityJsonSaver.kt:55-56` записывает `entities.json` внутрь исходного мира.

**Улучшение:** UI выбирает входной мир, Polar-файл и JSON-файл; converter принимает `ConversionRequest(inputWorld, polarOutput, entitiesOutput)` и не создаёт скрытых файлов.

### Валидировать папку мира до запуска

`MainFrame.kt:35-39` принимает любую директорию и сразу разрешает конвертацию.

**Улучшение:** проверять директорию, `level.dat` и ожидаемые `region`/`entities`; сообщать, что именно отсутствует, до запуска тяжёлой операции.

### Исправить пограничные ошибки NBT

- `EntityJsonSaver.kt:152` записывает `base_plate = true`, когда NBT `NoBasePlate` равен `true`; имя поля и значение семантически противоположны.
- `EntityJsonSaver.kt:155-162` индексирует armor/hand lists без проверки размера.
- `EntityJsonSaver.kt:35-37` предполагает корректное имя каждого `.mca` и прерывает весь экспорт при malformed-файле.

**Улучшение:** нормализовать семантику boolean-полей, использовать безопасный доступ и изолировать ошибку одного region-файла от остальных.

## P1 — разделить ответственность и повысить тестируемость

### Ввести application service

`MainFrame.kt:50` напрямую вызывает `WorldToPolar.convert`, а `WorldToPolar.kt:13-34` одновременно управляет lifecycle Minestom, путями, Polar writer и entity export.

**Улучшение:** ввести `ConversionService`, `ConversionRequest`, `ConversionResult`; передавать сервис в UI. Отдельные компоненты должны отвечать за Polar conversion и entity export.

### Использовать типизированные DTO

`EntityJsonSaver.kt:17-214` строит вложенные `Map<String, Any>`. Ошибки ключей и типов обнаруживаются только в готовом JSON.

**Улучшение:** sealed/typed DTO для поддерживаемых entity types, отдельный NBT parser и JSON writer. Схема результата должна покрываться snapshot/fixture-тестами.

### Добавить progress и cancellation

Длительная конвертация не сообщает этап и не может быть отменена.

**Улучшение:** публиковать этапы `Validation`, `Polar conversion`, `Entity scan`, `Writing`; добавить progress bar, cancel и корректное завершение фоновой задачи при закрытии окна.

### Сделать результаты детерминированными

`EntityJsonSaver.kt:27-53` полагается на порядок `listFiles`, поэтому порядок JSON может различаться.

**Улучшение:** сортировать region-файлы и итоговые сущности по стабильному ключу, чтобы повторные сборки было удобно сравнивать.

## P2 — улучшить интерфейс и сопровождение

### Перестроить форму

`MainFrame.kt:16-31` использует `FlowLayout`, фиксированный размер и не показывает выбранный путь.

**Улучшение:** форма с отдельными строками input/output, `GridBagLayout` или вложенными панелями, `pack()`, минимальный размер, path labels, progress/status area.

### Доступность и предсказуемость

Добавить mnemonic, tooltips, accessible names, default button и подтверждение перед перезаписью существующего результата. На время конвертации блокировать обе кнопки выбора и запуска.

### Структурированное логирование

`WorldToPolar.kt:14,30` использует `println`, `MainFrame.kt:61` — `printStackTrace`.

**Улучшение:** единый logger с файлом диагностики и короткими локализованными сообщениями в UI.

## Рекомендуемая последовательность

1. Исправить P0 correctness и файловые side effects.
2. Ввести `ConversionService` и typed result без изменения формата Polar/JSON.
3. Добавить fixture-тесты NBT и интеграционный тест на маленькой копии мира.
4. После стабилизации service API обновить UI с progress/cancel и явными output paths.
