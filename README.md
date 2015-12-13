# Deity-Manager
Manages deities and their spells for Wurm Unlimited.

## What the fork?
Hoping to convert this mod to work with https://github.com/ago1024/WurmServerModLauncher

## If you are not using ANT to build
The compiled com.wurmonline.server.DbConnector$WurmDatabaseSchema.class file must be moved inside the mod.wurmonline.mods.deitymanager package of the final JAR file. Compile it as being part of the com.wurmonline.server package, but it is actually needed inside of the mod.wurmonline.mods.deitymanager package.

## What does this mod do?
This mod maintains a new sqlite database called "wurmspells.db". This database contains a matrix of all spells in the game that are available to deities. By editing this database you can fine-tune which spells a deity can use, and thus by association, the spells that priests of the deity can use.
