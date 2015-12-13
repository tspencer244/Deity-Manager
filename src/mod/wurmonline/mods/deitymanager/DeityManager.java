package mod.wurmonline.mods.deitymanager;

import com.wurmonline.server.ServerDirInfo;
import com.wurmonline.server.deities.Deities;
import com.wurmonline.server.deities.Deity;
import com.wurmonline.server.spells.Spell;
import com.wurmonline.server.spells.Spells;

import javassist.*;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.io.InputStream;

public class DeityManager implements Configurable, Initable, PreInitable, ServerStartedListener, WurmMod {
    private static Logger logger = Logger.getLogger(DeityManager.class.getName());
    DeityData[] deities;
    int lastSelectedDeity;
    InputStream is;
    
    @Override
	public void configure(Properties properties) {
    	//
    }
    
    @Override
	public void init() {
    	//
    }
    
    @Override
    public void preInit() {
    	ClassPool pool = HookManager.getInstance().getClassPool();
    	
        try {
            CtClass deity = pool.get("com.wurmonline.server.deities.Deity");
            CtMethod method = CtNewMethod.make("public void removeSpell(com.wurmonline.server.spells.Spell spell) {" +
                    "this.spells.remove(spell);" +
                    "if(spell.targetCreature && this.creatureSpells.contains(spell)) { " +
                    "    this.creatureSpells.remove(spell);" +
                    "}" +
                    "if(spell.targetItem && this.itemSpells.contains(spell)) {" +
                    "    this.itemSpells.remove(spell);" +
                    "}" +
                    "if(spell.targetWound && this.woundSpells.contains(spell)) {" +
                    "    this.woundSpells.remove(spell);" +
                    "}" +
                    "if(spell.targetTile && this.tileSpells.contains(spell)) {" +
                    "    this.tileSpells.remove(spell);" +
                    "}" +
                    "}", deity);
            deity.addMethod(method);
        } catch (NotFoundException | CannotCompileException ex) {
            logger.warning("Error when creating removeSpell method.");
            ex.printStackTrace();
        }

        try {
            CtClass spellGenerator = pool.get("com.wurmonline.server.spells.SpellGenerator");
            CtMethod createSpells = spellGenerator.getDeclaredMethod("createSpells");
            createSpells.insertBefore("{ if (com.wurmonline.server.spells.Spells.getAllSpells().length != 0) { return; } }");
            spellGenerator.writeFile();
        } catch (NotFoundException | CannotCompileException | IOException ex) {
            logger.warning("Error when modifying SpellGenerator method.");
            ex.printStackTrace();
        }

        try {
            CtClass dbConnector = pool.get("com.wurmonline.server.DbConnector");

            pool.get("com.wurmonline.server.DbConnector$WurmDatabaseSchema").detach();
            is = DeityManager.class.getResourceAsStream("DbConnector$WurmDatabaseSchema.class"); 
            pool.makeClass(is);
            dbConnector.rebuildClassFile();

            dbConnector.getDeclaredMethod("initialize").insertAfter(
                    "final String dbUser = com.wurmonline.server.Constants.dbUser;" +
                    "final String dbPass = com.wurmonline.server.Constants.dbPass;" +
                    "String dbHost;" +
                    "String dbDriver;" +
                    "if(isSqlite()) {" +
                    "    dbHost = com.wurmonline.server.Constants.dbHost;" +
                    "    config.setJournalMode(org.sqlite.SQLiteConfig.JournalMode.WAL);" +
                    "    config.setSynchronous(org.sqlite.SQLiteConfig.SynchronousMode.NORMAL);" +
                    "    dbDriver = \"org.sqlite.JDBC\";" +
                    "    } else {" +
                    "    dbHost = com.wurmonline.server.Constants.dbHost + com.wurmonline.server.Constants.dbPort;" +
                    "    dbDriver = com.wurmonline.server.Constants.dbDriver;" +
                    "    }" +
                    "CONNECTORS.put(com.wurmonline.server.DbConnector.WurmDatabaseSchema.SPELLS, new com.wurmonline.server.DbConnector(" +
                    "dbDriver, dbHost, com.wurmonline.server.DbConnector.WurmDatabaseSchema.SPELLS.getDatabase(), dbUser, dbPass, \"spellsDbcon\"));");

            CtMethod method = CtNewMethod.make("public static final java.sql.Connection getSpellsDbCon() throws java.sql.SQLException {" +
                    "return refreshConnectionForSchema(com.wurmonline.server.DbConnector.WurmDatabaseSchema.SPELLS);}", dbConnector);
            dbConnector.addMethod(method);
            dbConnector.writeFile();

        } catch (NotFoundException | CannotCompileException | IOException ex) {
            logger.warning("Error when creating dbConnector method.");
            System.out.println(ex.getMessage());
            System.out.println(ex.getLocalizedMessage());
            System.out.println(ex.toString());
            ex.printStackTrace();
        }
        
        deities = DeityDBInterface.getAllData();
    }

    @Override
    public void onServerStarted () {
        ServerDirInfo.getFileDBPath();
        
        try {
            if (deities.length == 0) {
                DeityDBInterface.loadAllData();
                deities = DeityDBInterface.getAllData();
            }
            for (DeityData deityData : deities) {
                Deity deity = Deities.getDeity(deityData.getNumber());
                deity.alignment = deityData.getAlignment();
                deity.setPower((byte)deityData.getPower());
                ReflectionUtil.setPrivateField(deity, ReflectionUtil.getField(Deity.class, "faith"), deityData.getFaith());
                deity.setFavor(deityData.getFavor());
                ReflectionUtil.setPrivateField(deity, ReflectionUtil.getField(Deity.class, "attack"), deityData.getAttack());
                ReflectionUtil.setPrivateField(deity, ReflectionUtil.getField(Deity.class, "vitality"), deityData.getVitality());

                Method removeSpell = ReflectionUtil.getMethod(Deity.class, "removeSpell");
                Set<Spell> deitySpells = deity.getSpells();
                for (Spell spell : Spells.getAllSpells()) {
                    if (deitySpells.contains(spell)) {
                        if (!deityData.hasSpell(spell)) {
                            logger.info("Removing " + spell.getName() + " from " + deity.getName() + ".");
                            removeSpell.invoke(deity, spell);
                            assert !deity.hasSpell(spell);
                        }
                    } else {
                        if (deityData.hasSpell(spell)) {
                            logger.info("Adding " + spell.getName() + " to " + deity.getName() + ".");
                            deity.addSpell(spell);
                            assert deity.hasSpell(spell);
                        }
                    }
                }
            }
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | NoSuchFieldException ex) {
            logger.warning("An error occurred whilst trying to change the settings on the server:");
            ex.printStackTrace();
        }
    }
}
