# 📜 Magic RPG Resource Pack - Полное Руководство
## Кастомные текстуры предметов по НАЗВАНИЮ для Minecraft 1.21.11

---

## 🎮 КРАТКО: Как протестировать на клиенте

### Шаг 1: Скопируйте ресурспак в клиент

**Где находится ресурспак:**
```
/home/user/AGN-Work/MagicRPG-ResourcePack/
```

**Куда скопировать:**
- **Windows:** `C:\Users\ТВОЙ_НИК\AppData\Roaming\.minecraft\resourcepacks\`
- **Linux:** `~/.minecraft/resourcepacks/`
- **Mac:** `~/Library/Application Support/minecraft/resourcepacks/`

### Шаг 2: Включите ресурспак в игре
1. Запустите Minecraft 1.21.11
2. Главное меню → **Настройки** → **Ресурспаки**
3. Найдите **MagicRPG-ResourcePack** в списке
4. Перетащите его в **правую колонку** (активные паки)
5. Нажмите **Готово**

### Шаг 3: Тестируйте команды

```mcfunction
# Кастомные меню (по названию)
/give @p minecraft:paper{display:{Name:'"custom_menu_#1"'}} 1
/give @p minecraft:paper{display:{Name:'"custom_menu_#2"'}} 1
/give @p minecraft:paper{display:{Name:'"custom_menu_#3"'}} 1

# Магические свитки (по названию)
/give @p minecraft:paper{display:{Name:'"Огненный свиток"'}} 1
/give @p minecraft:paper{display:{Name:'"Ледяной свиток"'}} 1
/give @p minecraft:paper{display:{Name:'"Свиток молнии"'}} 1
/give @p minecraft:paper{display:{Name:'"Свиток исцеления"'}} 1
/give @p minecraft:paper{display:{Name:'"Свиток телепортации"'}} 1
```

**✅ Результат:** В инвентаре появятся бумаги с **кастомными текстурами**!

---

## 📁 Структура ресурспака

```
MagicRPG-ResourcePack/
├── pack.mcmeta                    # 📄 Метаданные (format: 26 для 1.21.11)
├── assets/
│   ├── minecraft/
│   │   └── models/
│   │       └── item/
│   │           └── paper.json     # ✨ ГЛАВНАЯ МОДЕЛЬ (здесь predicates!)
│   └── magicrpg/
│       ├── models/
│       │   └── item/              # 📄 Модели для каждой текстуры
│       │       ├── custom_menu_1.json
│       │       ├── custom_menu_2.json
│       │       ├── custom_menu_3.json
│       │       ├── magic_scroll_fire.json
│       │       └── ...
│       └── textures/
│           └── item/              # 🎨 Текстуры (ЗАМЕНИТЕ ИХ!)
│               ├── custom_menu_1.png
│               ├── custom_menu_2.png
│               ├── custom_menu_3.png
│               ├── magic_scroll_fire.png
│               └── ...
└── README.md
└── INSTRUCTIONS_RU.md
```

---

## 🎯 Как это работает

### Технология: NBT Predicates по display.Name

В файле `paper.json` используются **predicates** (условия) для проверки **названия предмета**:

```json
{
  "overrides": [
    {
      "predicate": {
        "nbt": "{display:{Name:'\"custom_menu_#1\"'}}"
      },
      "model": "magicrpg:item/custom_menu_1"
    }
  ]
}
```

**Как это работает:**
1. Игра проверяет NBT тэг `display.Name` у предмета
2. Если имя **совпадает** с указанным в predicate → применяется новая модель
3. Модель указывает на кастомную текстуру

**⚠️ Важно:**
- Имя должно быть **точным** (регистр важен!)
- Имя **не должно содержать JSON форматирование** (просто текст)

---

## 🎨 Как добавить свои текстуры

### Шаг 1: Создайте текстуру

1. Нарисуйте текстуру **16x16** или **32x32** пикселей
2. Сохраните в формате **PNG с прозрачностью**
3. Поместите в папку:
   ```
   MagicRPG-ResourcePack/assets/magicrpg/textures/item/your_texture.png
   ```

**Рекомендации:**
- Используйте **Photoshop, GIMP, Krita, Paint.NET**
- Цвета: яркие, контрастные
- Стиль: Magic RPG (руны, символы, магия)

---

### Шаг 2: Создайте модель

Создайте файл:
```
MagicRPG-ResourcePack/assets/magicrpg/models/item/your_model.json
```

Содержимое:
```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "magicrpg:item/your_texture"
  }
}
```

---

### Шаг 3: Добавьте override в paper.json

Откройте `MagicRPG-ResourcePack/assets/minecraft/models/item/paper.json`

Добавьте новый override:
```json
{
  "predicate": {
    "nbt": "{display:{Name:'\"Ваше Название\"'}}"
  },
  "model": "magicrpg:item/your_model"
}
```

**Пример:**
```json
{
  "predicate": {
    "nbt": "{display:{Name:'\"Мой супер предмет\"'}}"
  },
  "model": "magicrpg:item/my_super_item"
}
```

---

### Шаг 4: Перезагрузите ресурспак

В игре:
```mcfunction
/reload
```

Или перезайдите в мир.

---

## 🔧 Как выдавать предметы

### Базовый синтаксис
```mcfunction
/give @p minecraft:paper{display:{Name:'"Название"'}} 1
```

**Важно:**
- Используйте **одинарные кавычки** снаружи
- Используйте **экранированные двойные кавычки** внутри: `\"`
- Регистр **важен**!

---

### Примеры

```mcfunction
# Простое название
/give @p minecraft:paper{display:{Name:'"Меню"'}} 1

# Название с пробелами
/give @p minecraft:paper{display:{Name:'"Магический свиток"'}} 1

# Название с цифрами
/give @p minecraft:paper{display:{Name:'"Предмет #42"'}} 1

# Название с символами
/give @p minecraft:paper{display:{Name:'"✨Магия✨"'}} 1
```

---

## 💡 Как скрыть название предмета

Чтобы оставить **только картинку** без названия, используйте плагин **CAD Editor**:

### Шаг 1: Установите плагин
- Скачайте: [CAD Editor на SpigotMC](https://www.spigotmc.org/resources/cad-editor.105214/)
- Поместите в `/plugins/` на сервере

### Шаг 2: Настройте config.yml
```yaml
hide:
  display: true      # Скрыть название
  lore: true         # Скрыть описание
  attributes: true   # Скрыть атрибуты
  enchants: true     # Скрыть зачарования
```

### Шаг 3: Перезапустите сервер

**Результат:** Название и описание будут скрыты, останется только **кастомная текстура**!

---

## 📊 Примеры для разных случаев

### Пример 1: Кастомное меню
```mcfunction
/give @p minecraft:paper{display:{Name:'"Главное меню"'}} 1
/give @p minecraft:paper{display:{Name:'"Магазин"'}} 1
/give @p minecraft:paper{display:{Name:'"Квесты"'}} 1
```

В `paper.json`:
```json
{
  "predicate": {"nbt": "{display:{Name:'\"Главное меню\"'}}"},
  "model": "magicrpg:item/menu_main"
},
{
  "predicate": {"nbt": "{display:{Name:'\"Магазин\"'}}"},
  "model": "magicrpg:item/menu_shop"
}
```

---

### Пример 2: Уровневые предметы
```mcfunction
/give @p minecraft:paper{display:{Name:'"Свиток 1 уровня"'}} 1
/give @p minecraft:paper{display:{Name:'"Свиток 2 уровня"'}} 1
/give @p minecraft:paper{display:{Name:'"Свиток 3 уровня"'}} 1
```

---

### Пример 3: Уникальные предметы
```mcfunction
/give @p minecraft:paper{display:{Name:'"Легендарный свиток"'}} 1
/give @p minecraft:paper{display:{Name:'"Эпический меч"'}} 1
/give @p minecraft:paper{display:{Name:'"Редкий артефакт"'}} 1
```

---

## ⚡ Как проверить текущее имя предмета

```mcfunction
/data get entity @p SelectedItem.tag.display.Name
```

**Пример вывода:**
```
[minecraft:string, "Огненный свиток"]
```

Если выводит что-то вроде:
```
[minecraft:string, '{"text":"Огненный свиток"}']
```
Значит имя сохранено в **JSON формате** и predicate **не сработает**.

---

## 🔄 Как исправить JSON форматирование

Если имя сохранено в JSON формате (через наковальню), есть **3 способа**:

### Способ 1: Использовать команды с простым текстом
```mcfunction
# ❌ Плохо (JSON формат)
/give @p minecraft:paper{display:{Name:'{"text":"Огонь"}'}} 1

# ✅ Хорошо (простой текст)
/give @p minecraft:paper{display:{Name:'"Огонь"'}} 1
```

### Способ 2: Использовать плагин для очистки
Плагины типа **ItemsAdder** или **CustomItems** могут автоматически удалять JSON форматирование.

### Способ 3: Использовать datapack для очистки
Создайте функцию, которая удаляет JSON из имени:
```mcfunction
# В datapack
/data modify entity @p SelectedItem.tag.display.Name set value '"Огонь"'
```

---

## 📥 Перенос на сервер

### Шаг 1: Загрузите ресурспак на сервер
```bash
# По SSH
scp -r /home/user/AGN-Work/MagicRPG-ResourcePack user@server-ip:/server/world/resource_packs/

# Или через FTP
# Загрузите папку MagicRPG-ResourcePack в /server/world/resource_packs/
```

### Шаг 2: Настройте server.properties
```properties
# Включите ресурспак
resource-pack=MagicRPG-ResourcePack
require-resource-pack=true
```

### Шаг 3: Перезапустите сервер
```bash
cd /server
./restart.sh
```

### Шаг 4: Проверьте на сервере
1. Зайдите на сервер
2. Примите ресурспак
3. Выполните тестовую команду

---

## ❓ Частые вопросы

### Q: Текстуры не отображаются. Что делать?
**A:**
1. Проверьте, что ресурспак **включен** в игре (`/resource pack list`)
2. Проверьте **точное имя** предмета (`/data get entity @p SelectedItem`)
3. Убедитесь, что имя **совпадает** с predicate в `paper.json`
4. Перезагрузите ресурспак (`/reload`)

---

### Q: Как проверить, что predicate работает?
**A:**
```mcfunction
# Выдайте предмет с точным именем
/give @p minecraft:paper{display:{Name:'"custom_menu_#1"'}} 1

# Проверьте имя
/data get entity @p SelectedItem.tag.display.Name
```

---

### Q: Можно ли использовать для других предметов?
**A:** Да! Создайте аналогичные модели для:
- `book.json` - для книг
- `stick.json` - для палок
- `sword.json` - для мечей
- `shield.json` - для щитов

---

### Q: Сколько override можно добавить?
**A:** 
- Максимум **~256 override** в одной модели
- Если нужно больше — создайте **несколько моделей** для одного предмета

---

### Q: Как скрыть название без плагинов?
**A:** Без плагинов **нельзя** скрыть название. Используйте:
- **CAD Editor** (рекомендуется)
- **ItemsAdder**
- **CustomItems**

---

## 🎨 Цветовые схемы для текстур

| Тип | Основной цвет | Вторичный цвет | Пример |
|-----|--------------|----------------|--------|
| Огонь | `#FF4500` | `#FF8C00` | 🔥 |
| Лёд | `#00BFFF` | `#ADD8E6` | ❄️ |
| Молния | `#FFD700` | `#FFA500` | ⚡ |
| Исцеление | `#32CD32` | `#90EE90` | ✚ |
| Телепортация | `#9370DB` | `#DDA0DD` | 🔮 |
| Взрыв | `#DC143C` | `#B22222` | 💥 |
| Тьма | `#4B0082` | `#800080` | 🌑 |
| Свет | `#FFD700` | `#FFFFFF` | 💡 |

---

## 📚 Полезные ссылки

- [Minecraft Wiki - Resource Packs](https://minecraft.wiki/w/Resource_Pack)
- [Minecraft Wiki - Model Overrides](https://minecraft.wiki/w/Model#Item_model_overrides)
- [CAD Editor Plugin](https://www.spigotmc.org/resources/cad-editor.105214/)
- [ItemsAdder Plugin](https://www.spigotmc.org/resources/itemsadder.37610/)

---

## 🎉 Итог

✅ **Ресурспак работает по названию предмета**  
✅ **Легко добавлять новые текстуры**  
✅ **Работает на клиенте и сервере**  
✅ **Совместим с CAD Editor для скрытия названия**  
✅ **Поддерживает 1.21.11**  

**Удачи в создании Magic RPG сервера! 🎮✨**

---

**Вопросы?** Спрашивайте!
