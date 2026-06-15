# Гибкие левая/правая среды через описание слоя

**Дата:** 2026-06-15
**Статус:** Draft → на ревью

## 1. Проблема

Левая и правая среды (`Left Medium` / `Right Medium`) сейчас задаются жёстко:
выпадающий список из предопределённых дисперсий (`Air`, `GaAs: Adachi`, `GaAs: Gauss`,
`GaN`) либо режим `Custom` с вводом константной диэлектрической проницаемости
(вещественная + мнимая часть).

Этой гибкости недостаточно. Пользователь не может точно согласовать диэлектрическую
функцию среды с той дисперсией, что применяется внутри структуры (большое текстовое
поле `Structure Description`). Например, если крайний слой структуры —
`AlGaAs, eps: adachi_mod_gauss, cAl: 0.35`, то среду рядом нельзя описать такой же
моделью: доступна только константа. Возникает **диэлектрический контраст** на границе
«среда / крайний слой», а из-за него — паразитные интерференционные осцилляции,
которые портят расчётный спектр.

## 2. Цель

Дать возможность задавать левую и правую среды **тем же синтаксисом описания слоя**,
что и в `Structure Description`. Тогда среду можно сделать физически идентичной
крайнему слою структуры (тот же материал, та же eps-модель, та же концентрация),
контраст исчезает, паразитные осцилляции уходят.

## 3. Ключевая идея

Среда физически — это **полубесконечная среда**, от которой в методе матриц переноса
используется **только показатель преломления** `n(wl, T)`; толщина не участвует в
расчёте никогда (`core/Mirror.kt:47-51` — берутся только `n1`, `n2`).

Поэтому среда естественно описывается как **одно описание слоя** существующего формата,
у которого параметр толщины `d` не имеет физического смысла и игнорируется.

Парсер описания структуры (`String.buildStructure()` в
`core/structure/StructureBuilder.kt:10`) уже умеет превращать текст в `ILayer`.
Значит `Medium` достаточно сделать обёрткой над строкой-описанием, а `Medium.toLayer()`
— парсить эту строку и возвращать единственный слой. **`Mirror` не меняется вообще** —
он и так вызывает только `opticalParams.leftMedium.toLayer()` /
`rightMedium.toLayer()` (`core/Mirror.kt:30-31, 35-36`).

Прецедент в коде есть: внутренние узлы `medium: { ... }` композитных слоёв
(excitonic / eff_medium / spheres_lattice / mie) задаются без `d`, и парсер сам
подставляет туда `d:0` через `addThicknessNodeToMedium()`
(`core/structure/description/StructureDescriptionUtil.kt:97`).

**ВАЖНО (по итогам арх-ревью):** этот `addThicknessNodeToMedium()` срабатывает
**только** на текстовый шаблон `medium:{...}`. Описание среды — это **top-level**
слой (`material: GaN`), он НЕ обёрнут в `medium:{}`, поэтому regex его не затронет, и
`layer()` упадёт на `requireNonNegativeDouble(DescriptionParameters.d)`
(`LayerPresetsUtil.kt:28`). То есть «переиспользовать `addThicknessNodeToMedium`
напрямую» нельзя. Вместо этого подставляем `d` **на уровне JSON-дерева** в отдельной
функции-парсере среды (см. раздел 6.1) — это надёжнее, чем regex по сырой строке
(regex легко поймает `df:`, `drude`, `Adachi`, `lattice_factor`).

## 4. Объём (что входит / не входит)

### Входит
- Замена UI левой/правой среды на два текстовых поля (`TextArea`).
- Перенос селектора поляризации и поля угла в одну строку (освобождение вертикали).
- Перевод модели `Medium` на строку-описание + парсинг в `toLayer()`.
- Миграция существующих сохранённых конфигов (старый формат → новый).
- Валидация: ровно один слой, понятные ошибки.

### Не входит (YAGNI)
- Кнопки/меню «вставить шаблон» (Air/GaAs/…). Решено: **только текст**, пользователь
  печатает описание сам или копирует из `Structure Description`.
- Среда **не** становится per-mode (в отличие от `textDescriptions`, которые хранятся
  по режимам). Остаётся одна пара левая/правая на состояние, как сейчас.
- Никаких изменений в физике расчёта `Mirror`.

## 5. Изменения UI (FXML + контроллеры)

Принят **вариант A — окошки сред рядом** (side-by-side), не во всю ширину.

### 5.1 `src/main/resources/fxml/state/MediumParams.fxml`
- **Удалить** оба `ChoiceBox` (`leftMediumChoiceBox`, `rightMediumChoiceBox`).
- **Удалить** четыре поля Permittivity: `epsRealLeftMediumTextField`,
  `epsImaginaryLeftMediumTextField`, `epsRealRightMediumTextField`,
  `epsImaginaryRightMediumTextField`, и подписи `Permittivity`
  (`leftMediumEpsLabel`, `rightMediumEpsLabel`).
- **Добавить** два `TextArea` рядом: `leftMediumTextArea`, `rightMediumTextArea`,
  небольшой высоты (порядка 2-3 строк), с переносом по словам выключенным
  (моноширинное описание читается как в Structure Description).
- Сохранить подписи `Left Medium` / `Right Medium` над окошками.

### 5.2 `src/main/resources/fxml/state/LightParams.fxml`
- Поляризацию и угол вывести в одну строку: подпись + контрол рядом
  (`Pol [P▾]`, `Angle [0.0]`). Если по ширине не влезает — короткие подписи
  (`Pol`, `Angle`).

### 5.3 `src/main/resources/fxml/state/OpticalParams.fxml`
- Перебалансировать `percentHeight` строк `GridPane` под новый состав
  (медиа-строка с двумя `TextArea` + укороченная строка light-параметров).
- `Main.fxml` (деление колонки 50/50 между OpticalParams и StructureDescription)
  и `Root.fxml` **не трогаем** — высоты панелей фиксированы относительно окна,
  места внутри половины OpticalParams хватает за счёт удаления строки eps-полей
  и строки подписей light-параметров.

### 5.4 `ui/controllers/state/MediumParamsController.kt`
- Убрать всю логику choiceBox + enable/disable eps-полей.
- Новый интерфейс контроллера:
  - `leftMediumText(): String` / `rightMediumText(): String` — читают `TextArea`.
  - `setLeftMedium(medium: Medium)` / `setRightMedium(medium: Medium)` — пишут
    `medium.description` в `TextArea`.
  - `disableAll()` / `enableAll()` — как раньше, но только для двух `TextArea`
    и подписей.

## 6. Изменения модели данных

### 6.1 `core/state/Medium.kt`
Было:
```kotlin
data class Medium(
  val type: ExternalMediumType,
  val epsReal: Double,
  val epsImaginary: Double
)
```
Станет:
```kotlin
data class Medium(val description: String)
```

**Новая функция-парсер среды** (решение по открытому вопросу №1): вынести логику в
отдельную функцию рядом с `buildStructure()`, например `String.buildMediumLayer(): ILayer`
в `core/structure/StructureBuilder.kt`. `Medium.toLayer()` лишь вызывает её. Причины:
подстановка `d` идёт на уровне JSON-дерева (рядом с пайплайном `json()`/`asArray()`/
`layer()`), а не в data-class; проще юнит-тестировать без `Medium`; обработка пустой
строки и проверка «один слой» логически принадлежат парсеру.

Алгоритм `buildMediumLayer()`:
1. Если строка пустая/из пробелов → подставить дефолт `material: custom, eps: 1.0`
   (воздух) **до** парсинга (см. раздел 8, R2).
2. Прогнать через `json()` → `asArray()` (тот же пайплайн, что у структуры).
3. **Подставить `d` на уровне дерева:** если у узла единственного слоя нет поля `d`,
   добавить `d:0` в `ObjectNode` (аналог `addThicknessNodeToMedium`, но по дереву, а не
   по тексту). Если `d` уже задан пользователем (актуально для композитов) — не трогать.
4. Построить блоки/структуру (`buildBlocks { layersBlockBuilder() }`).
5. Проверить: `blocks.size == 1`, `blocks[0].repeat == 1`, `blocks[0].layers.size == 1`.
   Пустой список блоков (например ввод `x0 ...`) и любое нарушение → ошибка
   валидации с понятным текстом. **Не** использовать `blocks.first()` без проверки
   на пустоту (иначе `NoSuchElementException`).
6. Вернуть `blocks[0].layers[0]` как `ILayer`.

`Medium.toLayer()` = `description.buildMediumLayer()`.
`Mirror` не меняется (потребляет результат `toLayer()` как и прежде).

### 6.2 `core/state/OpticalParams.kt`
- `updateLeftMediumFromUI()` / `updateRightMediumFromUI()`: вместо чтения
  `(тип, epsReal, epsImaginary)` и `MediumParamValidator.validatePermittivity(...)`
  — читать строку из `mediumParamsController().leftMediumText()` и собирать
  `Medium(description)`.
- **Явная валидация среды здесь же (по итогам арх-ревью, C2):** сразу попытаться
  распарсить описание (`Medium(description).toLayer()` или `description.buildMediumLayer()`),
  поймать `StructureDescriptionException` / `JsonParseException` от парсера и
  **пере-обернуть** в `StateException`/валидационное исключение с заголовком
  **«Left medium error» / «Right medium error»**. Без этого ошибка среды всплывёт
  позже, на этапе `mirror.updateVia(...)` (`ComputationState.updateFromUI`), и
  `ControlsController.handle` покажет её под вводящим в заблуждение заголовком
  «Structure description error».
- `updateUILeftMedium()` / `updateUIRightMedium()`: пишут `medium.description`.
- Убрать хелпер `String.toMediumType()` и зависимость от
  `ExternalMediumType` / `ExternalMediumTypes` в этом файле.
- `MediumParamValidator.validatePermittivity` становится мёртвым — удалить либо
  перепрофилировать под валидацию описания среды.

### 6.3 `ExternalMediumType` / `ExternalMediumTypes`
- Из активного потока данных удаляются. Оставить (как deprecated/util) **только**
  если требуется логике миграции для разбора старых значений `type`. Иначе удалить.

## 7. Миграция сохранённых конфигов

`data/internal/state/config.json` хранит среду в старом формате, например:
```json
"leftMedium":  {"type":"AIR","epsReal":1.0,"epsImaginary":0.0},
"rightMedium": {"type":"CUSTOM","epsReal":13.2225,"epsImaginary":0.0}
```

Новый формат:
```json
"leftMedium":  {"description":"material: custom, eps: 1.0"},
"rightMedium": {"description":"material: custom, eps: (13.2225, 0.0)"}
```

### Правила перевода старого `type` → строка
| Старый `type`  | Новое описание                              |
|----------------|---------------------------------------------|
| `AIR`          | `material: custom, eps: 1.0`                 |
| `GAAS_ADACHI`  | `material: GaAs, eps: adachi_simple`         |
| `GAAS_GAUSS`   | `material: GaAs, eps: adachi_gauss`          |
| `GAN`          | `material: GaN`                              |
| `CUSTOM`       | `material: custom, eps: (epsReal, epsImaginary)` |

`AIR` со значениями eps, отличными от (1,0), и любой неизвестный `type` трактуем
как `CUSTOM` (берём фактические `epsReal`/`epsImaginary`), чтобы ничего не потерять.

### Механизм
Кастомный `JsonDeserializer<Medium>` **обязателен** (по итогам арх-ревью, C3): без
него `data class Medium(val description: String)` на старом JSON
`{"type":"AIR",...}` бросит `MissingKotlinParameterException` — старые конфиги вообще
не откроются. `@JsonCreator` не подходит, т.к. нужно ветвление **по форме узла**
(наличие `description` vs `type`+`eps*`), а не конструирование из одного значения.

Логика десериализатора (с доступом к `JsonNode`):
- видит поле `description` → читает напрямую (новый формат);
- иначе (есть `type`/`epsReal`/`epsImaginary`) → конвертирует по таблице выше
  (старый формат). Правило «AIR с eps ≠ (1,0) → CUSTOM» требует читать
  `epsReal`/`epsImaginary` независимо от `type` — `JsonNode` это позволяет.

Таблицу перевода зашиваем **прямо в десериализатор** строковым маппингом (решение по
открытому вопросу №3) — `ExternalMediumType` для этого тащить не нужно.

**Регистрация:** `SimpleModule` с этим десериализатором регистрируется на `mapper`
**там, где `mapper` объявлен** (`core/state/Config.kt`), до первого
`readTree`/`parse`, иначе порядок статической инициализации может укусить.
`mapper` — `jacksonObjectMapper()` (`Config.kt`), используется и для чтения, и для
записи (`saveConfig`).

Сериализация (запись) — дефолтная, всегда новый формат (`{"description": "..."}`).
Старые сохранёнки открываются без потери данных и при первом сохранении
переписываются в новый формат.

## 8. Валидация и крайние случаи

| Случай | Поведение |
|--------|-----------|
| Пустое окошко / пробелы | Дефолт `material: custom, eps: 1.0` (воздух), подставляется **до** парсинга. Не ошибка. (R2: пустую строку `asArray()` отфильтрует в пустой список — на парсер полагаться нельзя.) |
| Корректный одиночный `material:` слой | OK. `d` необязателен (подставляется на уровне дерева). |
| Корректный одиночный `type:` композит | OK (выбран вариант «любой одиночный слой»). `d` пользователь задаёт сам, т.к. для композитов он влияет на эффективный `n()`. |
| `x0 ...` (repeat==0) | Блок отфильтровывается (`StructureBuilder.kt:58`) → пустой список блоков. Ловим явно как ошибку «среда должна быть одним слоем», без `blocks.first()`. |
| Несколько слоёв / `xN > 1` / несколько блоков | Ошибка: «Среда должна быть описана одним слоем». |
| Синтаксический мусор / неизвестный материал | Ошибка парсинга, пере-обёрнутая в «Left/Right medium error» (см. 6.2). |

- Валидация выполняется при обновлении состояния из UI (момент `Compute`), как и
  сейчас для остальных оптических параметров — в
  `updateLeftMediumFromUI/updateRightMediumFromUI` (раздел 6.2).
- Сообщения об ошибках — через тот же канал `ControlsController.handle`, что и ошибки
  описания структуры, но с **корректным заголовком** про среду (а не «Structure
  description error»).

### Известное ограничение (R6)
`extractDefinitions()` очищает `userDefinitions` на каждый `buildStructure()`
(`LayerPresetsUtil.kt`). Описание среды парсится **отдельно** от Structure Description,
поэтому в среде **нельзя сослаться** на `def:`-материал, объявленный в Structure
Description. Среда поддерживает предопределённые материалы, `custom`, выражения и
импортированные внешние дисперсии (общий синглтон `ExternalDispersionsContainer`,
доступен и среде, и структуре), но не локальные `def:`-определения структуры.
Зафиксировать в `help.md`.

## 9. Тестирование

- **Эквивалентность пресетов:** для каждого старого пресета (`Air`, `GaAs: Adachi`,
  `GaAs: Gauss`, `GaN`, `Custom` с заданной eps) новая строка-описание даёт через
  `toLayer().n(wl, T)` тот же показатель преломления, что и старый `Medium.toLayer()`
  на нескольких длинах волн.
- **Миграция:** JSON старого формата (для всех пяти `type`) десериализуется в
  корректный `Medium` с ожидаемой строкой; повторная сериализация даёт новый формат.
- **Флагман-сценарий:** среда = крайний слой структуры (`AlGaAs, eps: adachi_mod_gauss,
  cAl: 0.35`) → на границе нет скачка `n` (контраст ≈ 0).
- **Ошибки:** пустая строка → воздух; два слоя → ошибка; `xN` → ошибка; мусор →
  ошибка парсинга.
- **`d` игнорируется:** одно и то же описание материала с разными `d` (или без `d`)
  даёт идентичный `n(wl, T)`.

## 10. Затронутые файлы (ориентир)

- `src/main/resources/fxml/state/MediumParams.fxml` — переверстать.
- `src/main/resources/fxml/state/LightParams.fxml` — инлайн pol/angle.
- `src/main/resources/fxml/state/OpticalParams.fxml` — перебалансировать высоты строк.
- `src/main/kotlin/ui/controllers/state/MediumParamsController.kt` — новый интерфейс.
- `src/main/kotlin/core/state/Medium.kt` — новая модель + `toLayer()` парсинг.
- `src/main/kotlin/core/state/OpticalParams.kt` — чтение/запись описания + валидация.
- Точка десериализации `Medium` (кастомный deserializer; регистрация на `mapper`).
- `data/help.md` — обновить раздел про среды (опционально, в рамках задачи).
- Тесты в `src/test/...` (расположение — по существующей структуре тестов).

## 11. Решения по итогам арх-ревью (бывшие открытые вопросы)

1. **Где логика парсинга среды** → отдельная функция `String.buildMediumLayer(): ILayer`
   рядом с `buildStructure()`; `Medium.toLayer()` её вызывает (раздел 6.1).
2. **Миграция** → кастомный `JsonDeserializer<Medium>` через `SimpleModule`,
   зарегистрированный на `mapper` в `Config.kt`. Не `@JsonCreator` (раздел 7).
3. **`ExternalMediumType`** → не сохранять; таблицу перевода зашить прямо в
   десериализатор. Enum удалить (потребитель только `OpticsUtil.kt`).

## 12. Замечания арх-ревью, учтённые в плане
- C1 — подстановка `d` на уровне JSON-дерева, не через `addThicknessNodeToMedium` (3, 6.1).
- C2 — явная валидация + пере-оборачивание ошибок среды в «Left/Right medium error» (6.2, 8).
- C3 — кастомный десериализатор обязателен, регистрация в `Config.kt` (7).
- R1/R2 — обработка пустой строки и `repeat==0` без `blocks.first()` (6.1, 8).
- R5 — `MediumParamValidator` мёртв; `ConfigParserTest.kt` содержит закомментированные
  ссылки на старый `Medium(...)` — переписать в рамках задачи (раздел 9).
- R6 — ограничение про `def:` задокументировано (8).
