**HawkEye Reloaded Continue**
===================

**HawkEye Reloaded Continue** is a continuation of the beloved **HawkEye Reloaded** plugin.

I previously helped with the original HawkEye Reloaded project, and this version aims to keep it alive by providing ongoing updates, maintenance, improvements, and future evolution.

----------

**Features**:
---------
* Logging of over 45 different actions
* Worldedit logging
* Smart logging
* Smart rollback/block restoral
* Block filter to avoid logging unwanted material
* Rollback commands with simple-to-use parameters
* Advanced interactive web interface for viewing logs
* Rollback previews - have the rollback only appear to you at first
* WorldEdit selection rollbacks - rollback everything in your WE selection
* Configurable search tool to quickly see edits on single blocks
* Simple, and easy to learn parameters
* Fast efficient logging
* API so other plugins can interact with the HawkEye database

The player identity model is now **UUID-only** internally. Legacy databases that still use numeric `player_id` values are migrated to `player_uuid`, and entries keep the actor name directly on each log row for display and non-player events.

----------

**Internal command help (`moreHelp`)**
---------

| Help block | Commands (`/hawk help <command>`) | `moreHelp` text |
|---|---|---|
| Basic help | `help` | Shows all HawkEye commands. Type `/hawk help <command>` for help on that command. |
| Tool usage | `tool` | Gives you the HawkEye tool. You can use this to see changes at specific places. Left click a block or place the tool to get information. |
| Tool custom bind | `tool bind` | Allows you to bind custom search parameters onto the tool. See `/hawk search help` for info on parameters. |
| Tool reset | `tool reset` | Reset HawkEye tool to default properties. See `/hawk tool bind help`. |
| Parameter help (8 params) | `search`, `writelog` | There are 8 parameters you can use - `a: p: w: r: b: f: t: l:`. Action `a:` - list of actions separated by commas. Player `p:` - list of players. World `w:` - list of worlds. Block `b:` - list of material ids (e.g. `minecraft:oak_log`). Filter `f:` - list of keywords. Location `l:` - `x,y,z` location. Radius `r:` - radius to search around given location. Time `t:` formats: `yyyy-MM-dd`, `t:10h45m10s`, `t:2011-06-02,10:45:10`, `t:2011-06-02,10:45:10,2011-07-04,18:15:00`. |
| Page navigation | `page` | Shows the specified page of results from your latest search. |
| Teleport to entry | `tpto` | Takes you to the location of the data entry with the specified ID. The ID can be found in either the DataLog interface or when you do a search command. |
| Radius lookup | `here` | Shows all changes in a radius around you. Radius should be an integer. |
| Preview rollback | `preview` | Previews a rollback to only you. This type of rollback does not affect the actual world in any way. The effects can be applied with `/hawk preview apply` or cancelled with `/hawk preview cancel`. The parameters are the same as `/hawk rollback`. |
| Apply preview | `preview apply` | Applies the results of a `/hawk preview` globally. Until this command is called, the preview is only visible to you. |
| Cancel preview | `preview cancel` | Cancels results of a `/hawk preview`. Only affects you - no changes are seen by anyone else. |
| Parameter help (7 params) | `rollback`, `rebuild` | There are 7 parameters you can use - `a: p: w: r: b: f: t:`. Action `a:` - list of actions separated by commas. Player `p:` - list of players. World `w:` - list of worlds. Block `b:` - list of material ids (e.g. `minecraft:oak_log`). Filter `f:` - list of keywords. Radius `r:` - radius to search around given location. Time `t:` formats: `t:10h45m10s`, `t:2011-06-02,10:45:10`, `t:2011-06-02,10:45:10,2011-07-04,18:15:00`. |
| Undo rollback | `undo` | Reverses your previous rollback if you made a mistake with it. |
| Delete data | `delete` | Deletes specified entries from the database permanently. Uses the same parameters and format as `/hawk search`. |
| Plugin info | `info` | Displays HawkEye's details. |
| Reload config | `reload` | Reloads Hawkeye's configuration. |
