# ᴀᴇᴛʜᴇʀɢʟɪᴛᴄʜ — TNT-Run 1.21.4 Paper

Готовый плагин `.jar` → **AetherGlitchTNTRun-1.0.0.jar**

### Установка
1. Закинь `AetherGlitchTNTRun-1.0.0.jar` в `plugins/` на Paper 1.21.4
2. Запусти сервер, появятся файлы:
   - `plugins/AetherGlitchTNTRun/config.yml` — все настройки геймплея, таймеров, блоков, наград, сообщений
   - `plugins/AetherGlitchTNTRun/arenas.yml` — **отдельный файл** только для миров/координат арены (по твоей просьбе)
   - `plugins/AetherGlitchTNTRun/stats.yml` — статистика (автосоздается)
3. Настрой арену в своем мире (например `world_tntrun`):
   - Телепортируйся в мир, встань в центр арены → `/tntrun setcenter`
   - Настрой радиус → `/tntrun setradius 20`
   - Встань в лобби → `/tntrun setlobby`
   - Высота пола → `/tntrun setfloor 80` (или авто от setcenter)
   - Добавь точки спавна (опционально) → `/tntrun addspawn` (несколько раз по кругу) иначе спавн рандом в круге
   - Проверь `/tntrun setworld world_tntrun`
4. Перезагрузи → `/tntrun reload`
5. Строй круглую арену: слой **SAND / RED_SAND / GRAVEL** на Y=floor-y, под ним **TNT** (красиво, но удаляется мгновенно)

### Как работает
- Мин. 2 / Макс. 25 игроков (настраивается в `config.yml` → `game.min-players`, `game.max-players`)
- Когда набирается 2+ → запускается таймер 60 сек с анонсами **60, 30, 20, 10, 5, 4, 3, 2, 1** в чате + Title + звуки
- Если игроков стало <2 — таймер отменяется
- До старта игроки заморожены в лобби, блоки под ними **НЕ падают** (`game.freeze-until-start`, `no-decay-in-lobby`)
- Во время игры наступание на песок → через `blocks.decay-delay-ticks: 10` (~0.5с) блок пропадает + `TNT` под ним удаляется `instant-remove` → можно перепрыгивать, но за тобой всё рушится. Частицы/звук включены
- Упал ниже `void-y` → выбываешь → становишься спектатором. Последний выживший — победитель
- При ливе/краше/рестарте — игрока возвращает на точку откуда он зашел (`returnLoc`), арена регенерируется (`blocks.regenerate`), таймер сбрасывается без багов. Не конфликтует с другими плагинами (Listener только на своих игроках, не ломает чужие блоки)
- Только бренд в чате пишется **ᴀᴇᴛʜᴇʀɢʟɪᴛᴄʜ** small caps, остальные сообщения обычным шрифтом — как просил

### Команды
- `/tntrun join` — войти
- `/tntrun leave` — выйти + тп назад
- `/tntrun stats [ник]` — победы/игры/винрейт
- `/tntrun top` — топ 10 по победам
- `/tntrun reload` — перезагрузка (admin)
- `/tntrun start` / `stop` — форсированный старт/стоп (admin)
- `/tntrun setlobby` / `setcenter` / `setradius <r>` / `setfloor <y>` / `setworld <мир>` / `addspawn` / `clearspawns` (admin)

### PlaceholderAPI
Требует PlaceholderAPI (softdepend). Плейсхолдеры:
```
%aetherglitch_wins% %aetherglitch_games% %aetherglitch_winrate%
%aetherglitch_players% %aetherglitch_max_players% %aetherglitch_state% %aetherglitch_countdown%
%aetherglitch_brand% → ᴀᴇᴛʜᴇʀɢʟɪᴛᴄʜ
%aetherglitch_top_1_name% %aetherglitch_top_1_wins% %aetherglitch_top_1_games% %aetherglitch_top_1_winrate%
... до 10 (настраивается placeholders.top-size)
```

### Важное в config.yml
- `decay-delay-ticks: 10` — увеличь до 14 если хочешь медленнее падение, уменьши до 6 если хардкор
- `under-material: TNT` — что под песком
- `arena` в `arenas.yml`, не в `config.yml` — все координаты отдельно!

Совместимость: Paper 1.21.4, не трогает чужие миры, работает на отдельном мире `world_tntrun` на любых координатах.

Удачной катки!
