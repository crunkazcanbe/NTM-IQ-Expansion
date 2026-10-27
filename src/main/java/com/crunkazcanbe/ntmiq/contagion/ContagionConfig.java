package com.crunkazcanbe.ntmiq.contagion;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * ntmiq-contagion.cfg. IQ's ServerConfig switches plus the ones a shared server wants: a global kill switch, one for
 * all natural spread (leaving injections working) and a cap on how many infected bodies may stand around one carrier,
 * so an outbreak cannot snowball into a world-wide plague.
 */
public final class ContagionConfig {
    private ContagionConfig() {}

    public static boolean enabled = true;
    public static boolean naturalSpread = true;
    public static boolean mobToPlayer = true;
    public static boolean aerosol = true;
    public static boolean touch = true;
    public static boolean hit = true;
    public static boolean proximity = true;
    public static boolean injected = true;
    public static boolean fomites = true;
    public static int maxInfectedNearby = 8;
    public static boolean wildSyringeLoot = true;

    public static void load(FMLPreInitializationEvent e) {
        Configuration cfg = new Configuration(new File(e.getModConfigurationDirectory(), "ntmiq-contagion.cfg"));
        String c = "contagion";
        cfg.setCategoryComment(c, "NTM IQ Edition diseases (F02). Turning a switch off never cures anything that is already infected.");
        enabled = cfg.getBoolean("enableDiseases", c, true, "Master switch: false stops all disease ticking, symptoms and spread");
        naturalSpread = cfg.getBoolean("naturalSpread", c, true, "Diseases spread on their own (aerosol/touch/hit/proximity/fomites). False leaves only injections");
        mobToPlayer = cfg.getBoolean("mobToPlayerSpread", c, true, "Infected mobs can infect players");
        aerosol = cfg.getBoolean("spreadAerosol", c, true, "Airborne spread (boosted while the carrier is wet, and by coughing)");
        touch = cfg.getBoolean("spreadTouch", c, true, "Spread by standing right next to a carrier");
        hit = cfg.getBoolean("spreadHit", c, true, "Spread by melee hits");
        proximity = cfg.getBoolean("spreadProximity", c, true, "Spread at range");
        injected = cfg.getBoolean("spreadInjected", c, true, "Syringes and pathogen fluids can infect (false = diseases cannot be given at all)");
        fomites = cfg.getBoolean("contaminatedItems", c, true, "Carriers contaminate single-stack items, which infect whoever carries them without gloves");
        maxInfectedNearby = cfg.getInt("maxInfectedNearby", c, 8, 0, 256,
                "A carrier stops infecting once this many already-infected living entities are within 32 blocks (0 = no cap). Keeps outbreaks local");
        wildSyringeLoot = cfg.getBoolean("wildDiseaseLoot", c, true, "Wild disease syringes in dungeon/mineshaft chests and rarely from zombies");
        if (cfg.hasChanged()) cfg.save();
    }
}
