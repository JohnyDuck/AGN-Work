# Magic RPG Resource Pack
## Custom Item Textures by Name for Minecraft 1.21.11

---

## 📥 How to Use (Client Test)

### 1. Copy to Minecraft Resource Packs Folder

**Windows:**
```
C:\Users\YOUR_NAME\AppData\Roaming\.minecraft\resourcepacks\
```

**Linux/Mac:**
```
~/.minecraft/resourcepacks/
```

### 2. Enable in Game
- Main Menu → Options → Resource Packs
- Move **MagicRPG-ResourcePack** to Active column
- Click **Done**

### 3. Test Commands

```mcfunction
# Custom menu items (by name)
/give @p minecraft:paper{display:{Name:'"custom_menu_#1"'}} 1
/give @p minecraft:paper{display:{Name:'"custom_menu_#2"'}} 1
/give @p minecraft:paper{display:{Name:'"custom_menu_#3"'}} 1

# Magic scrolls (by name)
/give @p minecraft:paper{display:{Name:'"Огненный свиток"'}} 1
/give @p minecraft:paper{display:{Name:'"Ледяной свиток"'}} 1
/give @p minecraft:paper{display:{Name:'"Свиток молнии"'}} 1
/give @p minecraft:paper{display:{Name:'"Свиток исцеления"'}} 1
/give @p minecraft:paper{display:{Name:'"Свиток телепортации"'}} 1
```

---

## 📁 Structure

```
MagicRPG-ResourcePack/
├── pack.mcmeta                    # Metadata (format: 26 for 1.21.11)
├── assets/
│   ├── minecraft/
│   │   └── models/
│   │       └── item/
│   │           └── paper.json     # Main model with name predicates
│   └── magicrpg/
│       ├── models/
│       │   └── item/              # Custom models
│       │       ├── custom_menu_1.json
│       │       ├── custom_menu_2.json
│       │       ├── custom_menu_3.json
│       │       ├── magic_scroll_fire.json
│       │       └── ...
│       └── textures/
│           └── item/              # Textures (replace these!)
│               ├── custom_menu_1.png
│               ├── custom_menu_2.png
│               ├── custom_menu_3.png
│               ├── magic_scroll_fire.png
│               └── ...
└── README.md
```

---

## 🎨 How to Add New Textures

### 1. Create Texture (16x16 or 32x32 PNG)
Save in: `assets/magicrpg/textures/item/your_texture.png`

### 2. Create Model
Create file: `assets/magicrpg/models/item/your_model.json`
```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "magicrpg:item/your_texture"
  }
}
```

### 3. Add Override to paper.json
```json
{
  "predicate": {
    "nbt": "{display:{Name:'\"Your Item Name\"'}}"
  },
  "model": "magicrpg:item/your_model"
}
```

### 4. Reload in Game
```mcfunction
/reload
```

---

## 🔧 Predicates by Name

The pack uses **NBT predicates** to check item display name:

```json
{
  "predicate": {
    "nbt": "{display:{Name:'\"custom_menu_#1\"'}}"
  },
  "model": "magicrpg:item/custom_menu_1"
}
```

**Important:** Name must be **exact** (case-sensitive) and **without JSON formatting**.

---

## 💡 Tips

### Hide Item Name (Use CAD Editor Plugin)
Install [CAD Editor](https://www.spigotmc.org/resources/cad-editor.105214/) on your server:
```yaml
# In config.yml
hide:
  display: true
  lore: true
```

### Check Current Item Name
```mcfunction
/data get entity @p SelectedItem.tag.display.Name
```

### Give Item with Simple Name
```mcfunction
/give @p minecraft:paper{display:{Name:'"Your Name"'}} 1
```

---

## 📦 Server Setup

1. Copy `MagicRPG-ResourcePack` to `/server/world/resource_packs/`
2. Edit `server.properties`:
```properties
resource-pack=MagicRPG-ResourcePack
require-resource-pack=true
```
3. Restart server

---

## 🎯 Features

✅ Custom textures by item **display name**  
✅ Works in **1.21.11**  
✅ No NBT tags required  
✅ Easy to add new textures  
✅ Works on **client and server**  
✅ Compatible with **CAD Editor** for hiding names  

---

## 📚 Full Documentation

See [INSTRUCTIONS_RU.md](INSTRUCTIONS_RU.md) for Russian guide.

---

**Created for Minecraft 1.21.11 Paper**
