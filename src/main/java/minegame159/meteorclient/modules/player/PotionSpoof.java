/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.mixininterface.IStatusEffectInstance;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.StatusEffectSetting;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

public class PotionSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Object2IntMap<StatusEffect>> potions = sgGeneral.add(new StatusEffectSetting.Builder()
            .name("potions")
            .description("Potions to add.")
            .defaultValue(Utils.createStatusEffectMap())
            .build()
    );

    public PotionSpoof() {
        super(Category.Player, "potion-spoof", "Spoofs specified potion effects for you. SOME effects DO NOT work.");
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        for (StatusEffect statusEffect : potions.get().keySet()) {
            int level = potions.get().getInt(statusEffect);
            if (level <= 0) continue;

            if (mc.player.hasStatusEffect(statusEffect)) {
                StatusEffectInstance instance = mc.player.getStatusEffect(statusEffect);
                ((IStatusEffectInstance) instance).setAmplifier(level - 1);
                if (instance.getDuration() < 20) ((IStatusEffectInstance) instance).setDuration(20);
            } else {
                mc.player.addStatusEffect(new StatusEffectInstance(statusEffect, 20, level - 1));
            }
        }
    });
}
