/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.game.OpenScreenEvent;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.player.FakePlayer;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.entity.FakePlayerEntity;
import minegame159.meteorclient.utils.player.Chat;
import minegame159.meteorclient.utils.player.PlayerUtils;
import minegame159.meteorclient.utils.player.RotationUtils;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

// Created by Eureka

public class AutoAnvil extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("How far away the target can be to be affected.")
            .defaultValue(4)
            .min(0)
            .build()
    );

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
            .name("height")
            .description("How high to place the anvils.")
            .defaultValue(5)
            .min(0)
            .max(10)
            .sliderMin(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates to the position before placing anvils.")
            .defaultValue(true)
            .build()
    );


    private final Setting<Boolean> placeButton = sgGeneral.add(new BoolSetting.Builder()
            .name("place-at-feet")
            .description("Automatically places a button or pressure plate at the targets feet to break the anvils.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> toggleOnBreak = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-on-break")
            .description("Toggles when the target's helmet slot is empty.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The delay in between anvil placements.")
            .min(0)
            .defaultValue(0)
            .sliderMax(50)
            .build()
    );

    public AutoAnvil() {
        super(Category.Combat, "auto-anvil", "Automatically places anvils above players to destroy helmets.");
    }

    private PlayerEntity target;
    private int timer;

    @Override
    public void onActivate() {
        timer = 0;
        target = null;
    }

    @EventHandler
    private final Listener<OpenScreenEvent> onOpenScreen = new Listener<>(event -> {
        if (event.screen instanceof AnvilScreen) mc.player.closeScreen();
    });

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {

        if (isActive() && toggleOnBreak.get() && target != null && target.inventory.getArmorStack(3).isEmpty()) {
            Chat.info(this, "Target head slot is empty… Disabling.");
            toggle();
            return;
        }

        if (target != null && (mc.player.distanceTo(target) > range.get() || !target.isAlive())) target = null;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !FriendManager.INSTANCE.attack(player) || !player.isAlive() || mc.player.distanceTo(player) > range.get()) continue;

            if (target == null) target = player;
            else if (mc.player.distanceTo(target) > mc.player.distanceTo(player)) target = player;
        }

        if (target == null) {
            for (FakePlayerEntity player : FakePlayer.players.keySet()) {
                if (!FriendManager.INSTANCE.attack(player) || !player.isAlive() || mc.player.distanceTo(player) > range.get()) continue;

                if (target == null) target = player;
                else if (mc.player.distanceTo(target) > mc.player.distanceTo(player)) target = player;
            }
        }

        if (timer >= delay.get() && target != null) {

            timer = 0;

            int prevSlot = mc.player.inventory.selectedSlot;

            if (getAnvilSlot() == -1) return;

            if (placeButton.get()) {

                if (getFloorSlot() == -1) return;
                mc.player.inventory.selectedSlot = getFloorSlot();

                if (mc.world.getBlockState(target.getBlockPos()).isAir()) mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, target.getBlockPos(), true));
            }

            mc.player.inventory.selectedSlot = getAnvilSlot();

            BlockPos placePos = target.getBlockPos().up().add(0, height.get(), 0);

            if (rotate.get()) RotationUtils.packetRotate(placePos);

            PlayerUtils.placeBlock(placePos, Hand.MAIN_HAND);

            mc.player.inventory.selectedSlot = prevSlot;
        } else timer++;
    });

    public int getFloorSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();
            Block block = Block.getBlockFromItem(item);

            if (block instanceof AbstractPressurePlateBlock || block instanceof AbstractButtonBlock) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    private int getAnvilSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();
            Block block = Block.getBlockFromItem(item);

            if (block instanceof AnvilBlock) {
                slot = i;
                break;
            }
        }
        return slot;
    }
}
