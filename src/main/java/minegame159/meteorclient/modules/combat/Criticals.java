/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

//Updated by squidoodly 18/07/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.events.world.PreTickEvent;
import minegame159.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.movement.NoFall;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class Criticals extends Module {
    public enum Mode {
        Packet,
        Jump,
        MiniJump
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The type of way Criticals will work.")
            .defaultValue(Mode.Packet)
            .build()
    );

    public Criticals() {
        super(Category.Combat, "criticals", "Performs critical attacks when you hit your target.");
    }

    private boolean wasNoFallActive;

    private PlayerInteractEntityC2SPacket attackPacket;
    private HandSwingC2SPacket swingPacket;
    private boolean sendPackets;
    private int sendTimer;

    @Override
    public void onActivate() {
        wasNoFallActive = false;
        attackPacket = null;
        swingPacket = null;
        sendPackets = false;
        sendTimer = 0;
    }

    @EventHandler
    private final Listener<SendPacketEvent> onSendPacket = new Listener<>(event -> {

        if (event.packet instanceof PlayerInteractEntityC2SPacket && ((PlayerInteractEntityC2SPacket) event.packet).getType() == PlayerInteractEntityC2SPacket.InteractionType.ATTACK) {
            if (!shouldDoCriticals()) return;
            if (mode.get() == Mode.Packet) doPacketMode();
            else doJumpMode(event);
        } else if (event.packet instanceof HandSwingC2SPacket && mode.get() != Mode.Packet) {
            if (!shouldDoCriticals()) return;
            doJumpModeSwing(event);
        }
    });

    @EventHandler
    private final Listener<PreTickEvent> onTick = new Listener<>(event -> {
        if (sendPackets) {
            if (sendTimer <= 0) {
                sendPackets = false;

                if (attackPacket == null || swingPacket == null) return;
                mc.getNetworkHandler().sendPacket(attackPacket);
                mc.getNetworkHandler().sendPacket(swingPacket);

                attackPacket = null;
                swingPacket = null;

                onEnd();
            } else {
                sendTimer--;
            }
        }
    });

    private void doPacketMode() {
        onStart();

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        PlayerMoveC2SPacket p1 = new PlayerMoveC2SPacket.PositionOnly(x, y + 0.0625, z, false);
        PlayerMoveC2SPacket p2 = new PlayerMoveC2SPacket.PositionOnly(x, y, z, false);

        //Ignore IntelliJ, it's fucking stupid. This is valid.
        ((IPlayerMoveC2SPacket) p1).setTag(1337);
        ((IPlayerMoveC2SPacket) p2).setTag(1337);

        mc.player.networkHandler.sendPacket(p1);
        mc.player.networkHandler.sendPacket(p2);

        onEnd();
    }

    private void doJumpMode(SendPacketEvent event) {
        if (!sendPackets) {
            onStart();

            sendPackets = true;
            sendTimer = mode.get() == Mode.Jump ? 6 : 4;
            attackPacket = (PlayerInteractEntityC2SPacket) event.packet;

            if (mode.get() == Mode.Jump) mc.player.jump();
            else ((IVec3d) mc.player.getVelocity()).setY(0.25);
            event.cancel();
        }
    }

    private void doJumpModeSwing(SendPacketEvent event) {
        if (sendPackets && swingPacket == null) {
            swingPacket = (HandSwingC2SPacket) event.packet;

            event.cancel();
        }
    }

    private void onStart() {
        wasNoFallActive = ModuleManager.INSTANCE.get(NoFall.class).isActive();

        if (wasNoFallActive) {
            ModuleManager.INSTANCE.get(NoFall.class).toggle();
        }
    }

    private void onEnd() {
        if (wasNoFallActive) {
            ModuleManager.INSTANCE.get(NoFall.class).toggle();
        }
    }

    private boolean shouldDoCriticals() {
        boolean a = !mc.player.isSubmergedInWater() && !mc.player.isInLava() && !mc.player.isClimbing();
        if (!mc.player.isOnGround()) return false;
        return a;
    }
}
