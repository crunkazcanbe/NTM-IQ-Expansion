package com.crunkazcanbe.ntmiq.blood;

import com.crunkazcanbe.ntmiq.IqContent;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import static com.hbm.inventory.OreDictManager.ANY_RUBBER;
import static com.hbm.inventory.OreDictManager.IRON;

/**
 * F01 — Bloodstream + pharma from NTM: IQ Edition (1.7.10, GPL-3; irislgtm, tam69 and the IQ team), rebuilt on
 * HBM NTM CE 1.12.2 as an add-on: per-entity bloodstream capability, drug fluids, fluid syringes (single / combat /
 * medical / Notch), Auto Drip helmet mod, Blood Monitor HUD leggings mod, drug + symptom potions.
 * CE's own syringes and RadAway items are left untouched. API for later features: {@link BloodstreamAPI}.
 */
@Mod.EventBusSubscriber(modid = "ntmiq")
public final class IqBlood {
    private IqBlood() {}

    public static ItemFluidSyringe syringe, combatSyringe;
    public static ItemMedicalSyringe medicalSyringe, notchSyringe;
    public static ItemAutoDrip autoDrip;
    public static ItemBloodMonitor bloodMonitor;
    public static SoundEvent SND_VOMIT, SND_SNEEZE;

    /** call from the mod's preInit, after IqContent.create (creative tab) */
    public static void preInit(FMLPreInitializationEvent e) {
        CapabilityManager.INSTANCE.register(Bloodstream.class, new Bloodstream.Storage(), Bloodstream::new);
        IqPotions.create();
        BloodstreamAPI.defaultSymptoms();
        BloodFluids.create();

        syringe = new ItemFluidSyringe("syringe", 10);
        combatSyringe = new ItemFluidSyringe("combat_syringe", 100);
        medicalSyringe = new ItemMedicalSyringe("medical_syringe");
        notchSyringe = new ItemMedicalSyringe.Notch("medical_syringe_notch");
        autoDrip = new ItemAutoDrip("auto_drip");
        bloodMonitor = new ItemBloodMonitor("blood_monitor");
        for (Item i : items()) i.setCreativeTab(IqContent.TAB);

        SND_VOMIT = sound("player.vomit");
        SND_SNEEZE = sound("player.sneeze");
        SerializableRecipe.additionalListeners.add(name -> {
            if (MixerRecipes.class.getSimpleName().equals(name)) BloodFluids.mixerRecipes();
        });
    }

    static Item[] items() { return new Item[] { syringe, combatSyringe, medicalSyringe, notchSyringe, autoDrip, bloodMonitor }; }

    private static SoundEvent sound(String name) {
        ResourceLocation rl = new ResourceLocation("ntmiq", name);
        return new SoundEvent(rl).setRegistryName(rl);
    }

    // ---------------- registries ----------------
    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) { e.getRegistry().registerAll(items()); }

    @SubscribeEvent
    public static void potions(RegistryEvent.Register<Potion> e) { e.getRegistry().registerAll(IqPotions.ALL.toArray(new Potion[0])); }

    @SubscribeEvent
    public static void sounds(RegistryEvent.Register<SoundEvent> e) { e.getRegistry().registerAll(SND_VOMIT, SND_SNEEZE); }

    /** IQ's ConsumableRecipes for these items; also re-applies traits so F24 fluids made in other preInits get them */
    @SubscribeEvent
    public static void recipes(RegistryEvent.Register<IRecipe> e) {
        BloodFluids.traits();
        ItemStack basic = new ItemStack(ModItems.circuit, 1, EnumCircuitType.BASIC.ordinal());
        add(e, "syringe", new ShapedOreRecipe(null, new ItemStack(syringe), " P ", " C ", " B ", 'P', IRON.plate(), 'C', new ItemStack(ModItems.cell), 'B', Blocks.IRON_BARS));
        add(e, "combat_syringe", new ShapedOreRecipe(null, new ItemStack(combatSyringe), " P ", " C ", " B ", 'P', IRON.plate(), 'C', ModItems.rod_empty, 'B', Blocks.IRON_BARS));
        add(e, "medical_syringe", new ShapelessOreRecipe(null, new ItemStack(medicalSyringe), syringe, IRON.plate())); // IQ has no recipe (loot/cloner); ours: syringe + iron plate
        add(e, "auto_drip", new ShapedOreRecipe(null, new ItemStack(autoDrip), " R ", "TCT", " S ", 'R', ANY_RUBBER.ingot(), 'T', ModItems.iv_empty, 'C', basic, 'S', syringe));
        add(e, "blood_monitor", new ShapelessOreRecipe(null, new ItemStack(bloodMonitor), ModItems.injector_5htp, basic));
    }

    private static void add(RegistryEvent.Register<IRecipe> e, String name, IRecipe r) {
        e.getRegistry().register(r.setRegistryName(new ResourceLocation("ntmiq", name)));
    }

    // ---------------- capability + ticking ----------------
    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> e) {
        if (e.getObject() instanceof EntityLivingBase) e.addCapability(new ResourceLocation("ntmiq", "bloodstream"), new Bloodstream.Provider());
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingUpdateEvent e) {
        EntityLivingBase entity = e.getEntityLiving();
        Bloodstream blood = entity.getCapability(BloodstreamAPI.CAP, null);
        if (blood == null) return;
        if (!entity.world.isRemote) blood.tick(entity);
        BloodBehavior b = BloodstreamAPI.getBehavior(blood.getBloodType());
        if (b != null) b.onTick(entity);
    }

    /** IQ's ModEventHandler.onEntityDamaged drug/symptom rules + the blood-type behaviour */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        EntityLivingBase e = event.getEntityLiving();
        if (e.world.isRemote) return;
        Bloodstream blood = e.getCapability(BloodstreamAPI.CAP, null);
        if (blood != null) {
            BloodBehavior b = BloodstreamAPI.getBehavior(blood.getBloodType());
            if (b != null) b.onHurt(e, event);
            if (event.isCanceled()) return;
        }
        Entity src = event.getSource().getTrueSource();
        EntityLivingBase attacker = src instanceof EntityLivingBase ? (EntityLivingBase) src : null;

        if ((e.isPotionActive(IqPotions.turkishRage) || e.isPotionActive(IqPotions.ganja)) && "lightningBolt".equals(event.getSource().getDamageType()))
            event.setAmount(0);
        if (e.isPotionActive(IqPotions.symptomRash) || e.isPotionActive(IqPotions.symptomFever)) event.setAmount(event.getAmount() * 1.25F);
        if (attacker != null && attacker.isPotionActive(IqPotions.symptomParalysis)) event.setAmount(event.getAmount() * 0.1F);
        if (blood != null && e.isPotionActive(IqPotions.symptomSeptic) && event.getAmount() > 0 && event.getAmount() < 100) blood.drainBlood(event.getAmount() * 5F);
        if (e.isPotionActive(IqPotions.symptomCardiac) && event.getAmount() > 0) event.setAmount(Math.max(event.getAmount(), e.getMaxHealth()));

        if (e.isPotionActive(IqPotions.roidRage)) e.removePotionEffect(IqPotions.roidRage); // getting hit ends your rage
        if (attacker != null && attacker.isPotionActive(IqPotions.roidRage)) {           // each hit you land stacks it
            PotionEffect rage = attacker.getActivePotionEffect(IqPotions.roidRage);
            int next = Math.min(rage.getAmplifier() + 1, 9);
            event.setAmount((float) (event.getAmount() * Math.pow(1.25, next)));
            attacker.addPotionEffect(new PotionEffect(IqPotions.roidRage, rage.getDuration(), next));
        }
        if (attacker != null && (attacker.isPotionActive(IqPotions.turkishRage) || attacker.isPotionActive(IqPotions.ganja))) event.setAmount(event.getAmount() * 2.0F);

        // Med-X: damage reduction ramps from 95% down to 25% over its first 10 s
        if (e.isPotionActive(IqPotions.medx)) {
            int start = e.getEntityData().getInteger("medxStartTime");
            if (start == 0) {
                start = e.ticksExisted;
                e.getEntityData().setInteger("medxStartTime", start);
            }
            int elapsed = e.ticksExisted - start;
            float reduction = elapsed < 200 ? 0.95F - 0.70F * (elapsed / 200.0F) : 0.25F;
            event.setAmount(event.getAmount() * (1.0F - Math.max(0.25F, Math.min(0.95F, reduction))));
        } else {
            e.getEntityData().removeTag("medxStartTime");
        }
    }
}
