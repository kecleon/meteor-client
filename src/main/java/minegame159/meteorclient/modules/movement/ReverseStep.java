/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class ReverseStep extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> fallSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("fall-speed")
            .description("How fast to fall in blocks per second.")
            .defaultValue(3)
            .min(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Double> fallDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("fall-distance")
            .description("The maximum fall distance this setting will activate at.")
            .defaultValue(3)
            .min(0)
            .sliderMax(10)
            .build()
    );

    public ReverseStep() {
        super(Category.Movement, "reverse-step", "Allows you to fall down blocks at a greater speed.");
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (!mc.player.isOnGround() || mc.player.isHoldingOntoLadder() || mc.player.isSubmergedInWater() || mc.player.isInLava() ||mc.options.keyJump.isPressed() || mc.player.noClip || mc.player.forwardSpeed == 0 && mc.player.sidewaysSpeed == 0) return;

        if (!mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, (float) -(fallDistance.get() + 0.01), 0.0))) ((IVec3d) mc.player.getVelocity()).setY(-fallSpeed.get());
    });
}
