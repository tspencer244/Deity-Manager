# Deity-Manager
Manages deities and their spells for Wurm Unlimited.

## What the fork?
Hoping to convert this mod to work with https://github.com/ago1024/WurmServerModLauncher

## If you are not using ANT to build
The compiled com.wurmonline.server.DbConnector$WurmDatabaseSchema.class file must be moved inside the mod.wurmonline.mods.deitymanager package of the final JAR file. Compile it as being part of the com.wurmonline.server package, but it is actually needed inside of the mod.wurmonline.mods.deitymanager package.