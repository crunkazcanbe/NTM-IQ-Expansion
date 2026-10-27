package com.crunkazcanbe.ntmiq.blood;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * IQ's drug + symptom potions (potion/HbmPotion additions, GPL-3). Drug icons come from IQ's potions sheet, symptom icons
 * from IQ's effect_icons atlas. The bodily effects of symptoms tick in {@link Bloodstream}; damage modifiers live in
 * {@link IqBlood#onHurt}.
 */
public class IqPotions extends Potion {
    static final ResourceLocation DRUG_ICONS = new ResourceLocation("ntmiq", "textures/gui/potions_iq.png");
    static final ResourceLocation SYMPTOM_ICONS = new ResourceLocation("ntmiq", "textures/gui/effect_icons.png");

    public static IqPotions stimulated, medx, roidRage, turkishRage, ganja;
    public static IqPotions symptomRash, symptomCough, symptomSneeze, symptomFever, symptomVomit, symptomSchizophrenia,
            symptomHemorrhage, symptomParalysis, symptomSeizure, symptomSeptic, symptomComa, symptomAortic, symptomNecrosis, symptomCardiac;
    static final java.util.List<IqPotions> ALL = new java.util.ArrayList<>();

    private final boolean symptom;
    private final int u, v, sheet; // icon cell on its sheet

    private IqPotions(String name, boolean bad, int color, boolean symptom, int u, int v) {
        super(bad, color);
        this.symptom = symptom;
        this.u = u;
        this.v = v;
        this.sheet = symptom ? 72 : 256;
        setPotionName("potion.hbm_" + name);
        setRegistryName("ntmiq", name);
        ALL.add(this);
    }

    private static IqPotions drug(String name, int color, int x, int y) { return new IqPotions(name, false, color, false, x * 18, 198 + y * 18); }
    private static IqPotions symptom(String name, int color, int u, int v) { return new IqPotions("symptom_" + name, true, color, true, u, v); }

    static void create() {
        stimulated = drug("stimulated", 0x44FF44, 2, 2);
        roidRage = drug("roid_rage", 0xFF4444, 0, 2);
        medx = drug("medx", 0x8888FF, 1, 2);
        turkishRage = drug("turkish_rage", 0xFF0000, 4, 2);
        ganja = drug("high", 0x3A5F0B, 5, 2);
        symptomCardiac = symptom("cardiac", 0x4444FF, 0, 0);
        symptomComa = symptom("coma", 0x222244, 18, 0);
        symptomCough = symptom("cough", 0xCCCCBB, 36, 0);
        symptomFever = symptom("fever", 0xFF4422, 54, 0);
        symptomNecrosis = symptom("necrosis", 0x222222, 0, 18);
        symptomParalysis = symptom("paralysis", 0x888888, 18, 18);
        symptomRash = symptom("rash", 0xFF8899, 36, 18);
        symptomSchizophrenia = symptom("schizophrenia", 0xAA44FF, 54, 18);
        symptomSeizure = symptom("seizure", 0xFF88FF, 0, 36);
        symptomSeptic = symptom("septic", 0x445522, 18, 36);
        symptomSneeze = symptom("sneeze", 0xFFFFFF, 36, 36);
        symptomVomit = symptom("vomit", 0xAA8844, 54, 36);
        symptomHemorrhage = symptom("hemorrhage", 0xCC2222, 0, 54);
        symptomAortic = symptom("aortic", 0xFF0000, 18, 54);
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        if (symptom) return duration % 10 == 0;
        return duration % 20 == 0;
    }

    @Override
    public void performEffect(EntityLivingBase e, int level) {
        if (e.world.isRemote) return;
        if (this == stimulated) e.heal((level + 1) * 2);
        // IQ spawns its own red/green lightning here; vanilla effect-only lightning stands in (harmless, as IQ's is to the user)
        if ((this == turkishRage || this == ganja) && e.ticksExisted % 200 == 0) {
            e.world.addWeatherEffect(new EntityLightningBolt(e.world, e.posX, e.posY, e.posZ, true));
            e.world.playSound(null, e.posX, e.posY, e.posZ, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.WEATHER, 100.0F, 0.8F + e.world.rand.nextFloat() * 0.2F);
        }
    }

    @Override
    public boolean hasStatusIcon() { return false; }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        mc.getTextureManager().bindTexture(symptom ? SYMPTOM_ICONS : DRUG_ICONS);
        Gui.drawModalRectWithCustomSizedTexture(x + 6, y + 7, u, v, 18, 18, sheet, sheet);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha) {
        mc.getTextureManager().bindTexture(symptom ? SYMPTOM_ICONS : DRUG_ICONS);
        Gui.drawModalRectWithCustomSizedTexture(x + 3, y + 3, u, v, 18, 18, sheet, sheet);
    }
}
