# AGENTS.md — заметки по Berlit_ATRM

Краткая карта проекта для следующего агента/разработчика. Пользовательская документация —
`README.md`.

## Сборка

- Целевая платформа: Minecraft 1.7.10, Forge 10.13.4.1614, GTNH Gradle convention 2.0.33.
- Gradle 9.7.1 + JDK 25 для демона; JDK 8 — целевой тулчейн (RFG ставит/находит сам).
- `tools/dev-env.ps1`:
  - ищет JDK в `.tools`, иначе в `..\BerlitMod\.tools` (или `ATRM_TOOLS`);
  - `GRADLE_USER_HOME` = `.gradle-home` (в git не попадает);
  - генерирует `gradle.properties` с абсолютными путями тулчейнов и init-скрипт с локальным
    Maven-зеркалом (иначе GTNH Nexus на этой машине не отдаёт jar'ы).
- В репозитории нет git-тега, поэтому версия зафиксирована: `gtnh.modules.gitVersion=false`
  и `project.ext.modVersion = "1.0.0"` в `build.gradle.kts`.
- Запуск (ExecutionPolicy Restricted — отсюда `powershell -ExecutionPolicy Bypass`):
  ```powershell
  powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\dev-env.ps1 build
  powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\dev-env.ps1 runServer
  ```
- Перед `build`: `spotlessApply` (иначе spotlessJavaCheck валит сборку).
- Путь проекта содержит пробел, поэтому нельзя класть `-Djava.io.tmpdir=...` в
  `org.gradle.jvmargs` — Gradle разобьёт аргумент. Используются `TMP`/`TEMP`, которые
  выставляет `dev-env.ps1`.

## Структура

```
src/main/java/ru/beggar/berlimatrm/
  BerlitAtRm.java          @Mod, dependencies = required-after:MineTweaker3;required-after:Avaritia
  CommonProxy.java         config, реестр рецептов, ZenClass, сеть, /atrm, selftest
  ClientProxy.java         пустой (GUI-классы грузятся только на клиенте)
  Config.java              requireOp, maxRecipes, exportScript
  command/CommandAtRm.java /atrm, /atrm export, /atrm reload (permission 2)
  gui/AtRmLayout.java      геометрия; единственный источник координат для контейнера и GUI
  gui/ContainerAtRm.java   ghost-слоты 0..80 + выход 81 + инвентарь игрока
  gui/GuiAtRm.java         9x9 сетка, "*" кнопки, строка выбранной клетки, меню, опции
  gui/AtRmInventory.java   82 фейковых слота
  gui/GhostSlot.java       слот, из которого нельзя забрать предмет
  gui/AtRmCheckbox, ModeButton, ThemedButton, ThemedTextField, NineSlice, GuiKit, GuiTheme
  recipe/AtRmRecipe.java   модель + кроп + signature + NBT + scriptLines
  recipe/AtRmIngredient.java item/ore/anyItem/anyDamage/NBT-фильтр
  recipe/AtRmShapedRecipe, AtRmShapelessRecipe — IRecipe с NBT-мэтчингом
  recipe/AtRmRecipes.java  реестр, persist (config/berlimatrm/recipes.dat), .zs export, undo-хуки
  recipe/NbtUtil.java      containment-сравнение NBT, canonical-подпись, SNBT parse
  recipe/RecipeTokens.java item/ore токены, NBT -> ZenScript map literal
  network/                 PacketAtRmApply/Result/Clear, ModNetwork, ServerTaskQueue (FML tick)
  compat/AvaritiaTweaker.java @ZenClass("mods.berlimatrm.Avaritia")
  selftest/AtRmSelftest.java headless-проверка
docs/preview/gui_ingame.png  реальный скриншот экрана
```

## Как устроено

- **GUI открывается сервером**: `/atrm` → `player.openGui` → `AtRmGuiHandler` создаёт
  `ContainerAtRm` на сервере и `GuiAtRm` на клиенте. Это обязательно: ghost-предметы лежат в
  фейковом `IInventory`, который синхронизируется слотами контейнера.
- **Ghost-слоты**: `ContainerAtRm.slotClick` для slotId 0..81 не двигает предметы, а копирует
  `player.inventory.getItemStack()` в слот (1 шт. для ингредиента, весь стак для выхода);
  пустой курсор/правый клик очищают. `detectAndSendChanges()` отправляет изменение клиенту.
- **Звёздочки**: `GuiAtRm.mouseClicked` ловит клик в 9x9 угол клетки и делает `return` до
  `super`, поэтому клик не уходит в слот. `mouseClicked` в `GuiContainer` — единственное место
  обработки кликов по слотам в 1.7.10. Кнопка `*` рисуется только при наведении или у выбранной
  клетки (иначе 81 плашка выглядит как артефакт текстуры); при наведении есть тултип
  «Select slot N».
- **Масштаб**: `GuiAtRm.adaptGuiScale()` при открытии и на resize подбирает максимальный GUI
  scale, при котором 340×382 помещается в окно (`Display.getWidth/Height`), и восстанавливает
  настройку в `onGuiClosed` через `Minecraft.func_152344_a`. На 854×480 экран открывается в
  scale 1, на большом окне — в scale 2/3.
- **Поле NBT** — `ThemedTextField`, рисуется в foreground с отменой translate(guiLeft, guiTop),
  потому что координаты GuiTextField абсолютные.
- **OK** собирает `AtRmRecipe` на клиенте, сериализует в NBT и отправляет
  `PacketAtRmApply`. Сервер проверяет права (`canCommandSenderUseCommand(2, "atrm")`), валидирует
  рецепт и добавляет/удаляет его в `ExtremeCraftingManager`. Ответ — `PacketAtRmResult`.
- **Права**: в 1.7.10 `EntityPlayerMP.canCommandSenderUseCommand` игнорирует числовой уровень и
  проверяет только op-список, поэтому `requireOp=false` реализован переопределением
  `CommandAtRm.canCommandSenderUseCommand`, а не только `getRequiredPermissionLevel()`.
- **Потоки**: `IMessageHandler` в 1.7.10 вызывается в netty-потоке; изменения мира/рецептов
  выполняются через `ServerTaskQueue` (очередь, разбираемая в `TickEvent.ServerTickEvent` на
  FML-шине). Клиентские ответы — через `Minecraft.func_152344_a`.
- **Persist**: `config/berlimatrm/recipes.dat` (CompressedStreamTools) — основной источник;
  `applyPersisted()` вызывается в `FMLServerStartingEvent` (после init Avaritia и после
  загрузки скриптов MineTweaker). Удаления built-in рецептов тоже сохраняются списком `Removed`.
- **Экспорт**: `config/berlimatrm/BerlitATRM.zs` перегенерируется при каждом изменении, но
  **не** автозагружается. Строки используют `mods.berlimatrm.Avaritia`, а не
  `mods.avaritia.ExtremeCrafting`, чтобы работали NBT-фильтры.
- **ZenClass**: `addShaped(output, IIngredient[][], @Optional boolean mirrored)`,
  `addShapeless(...)`, `remove(...)`. Скриптовые рецепты не пишутся в `recipes.dat`; действия
  реализуют `IUndoableAction`, поэтому `/mt reload` их откатывает.
- **NBT-фильтр**: сравнение «содержит все ключи» (`NbtUtil.contains`), числа сравниваются по
  значению независимо от ширины типа. Это сознательно мягче, чем `.withTag` MineTweaker.

## Тесты

```powershell
$env:BERLIT_ATRM_SELFTEST='1'
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\dev-env.ps1 runServer
```

Ожидаемые строки:

```
BERLIT_ATRM_SELFTEST shaped/nbt/ore/shapeless/removal/nbt_text/persist_save/persist_reload/export/persist_remove: ok
BERLIT_ATRM_SELFTEST summary: passed=10 failed=0
```

Тест работает через `addRuntime` (не пишет в конфиг) и проверяет persist/export с бэкапом и
восстановлением `recipes.dat`/`BerlitATRM.zs`.

MineTweaker-интеграция проверялась скриптом в `run/server/scripts/` с
`Avaritia.addShaped(<diamond>, [[<iron_block>]], true)`,
`addShapeless`, `remove` и `.withTag({display: {Name: "ATRM tag test"}})` — все вызовы
проходят, реестр Avaritia растёт (17 → 20).

## Ограничения / что можно улучшить

- NEI: рецепты из GUI — собственные `IRecipe`, а NEI-хендлер Avaritia делает `instanceof` на
  свои классы, поэтому страницы NEI для них не появляются (крафт в столе работает). Если нужен
  NEI — добавить свой `IRecipeHandler`/`IUsageHandler` в `compat/nei`.
- На выделенном сервере NEI клиента не увидит серверные рецепты (это общая проблема
  MineTweaker-рецептов; крафт в столе работает). При желании — sync-пакет на login/изменение.
- Экспорт `.zs` не умеет NBT-массивы и ключи-не-идентификаторы; такие рецепты остаются только
  в `recipes.dat`.
- `remove` удаляет все рецепты с выходом (как MTRM), а не один.
- GUI не использует NEI drag; предметы берутся из инвентаря игрока в нижней панели.
