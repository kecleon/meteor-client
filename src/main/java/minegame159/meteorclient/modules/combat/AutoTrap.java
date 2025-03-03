/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.player.FakePlayer;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.entity.FakePlayerEntity;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.PlayerUtils;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class AutoTrap extends Module {
    public enum TopMode {
        Full,
        Top,
        None
    }

    public enum BottomMode {
        Single,
        Platform,
        None
    }


    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<TopMode> topPlacement = sgGeneral.add(new EnumSetting.Builder<TopMode>()
            .name("top-mode")
            .description("Which blocks to place on the top half of the target.")
            .defaultValue(TopMode.Full)
            .build()
    );

    private final Setting<BottomMode> bottomPlacement = sgGeneral.add(new EnumSetting.Builder<BottomMode>()
            .name("bottom-mode")
            .description("Which blocks to place on the bottom half of the target.")
            .defaultValue(BottomMode.Platform)
            .build()
    );

    private final Setting<Integer> delaySetting = sgGeneral.add(new IntSetting.Builder()
            .name("place-delay")
            .description("How many ticks between block placements.")
            .defaultValue(1)
            .sliderMin(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .description("How far away you can target players.")
            .defaultValue(5)
            .sliderMin(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
            .name("turn-off")
            .description("Turns off after placing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders a block overlay where obsidian will be placed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 10))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .build()
    );

    private PlayerEntity target;
    private List<BlockPos> placePositions = new ArrayList<>();
    private boolean placed;
    private int delay;

    public AutoTrap(){
        super(Category.Combat, "auto-trap", "Traps people in an obsidian box to prevent them from moving.");
    }

    @Override
    public void onActivate() {
        target = null;
        if (!placePositions.isEmpty()) placePositions.clear();
        delay = 0;
        placed = false;
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {

        int slot = InvUtils.findItemInHotbar(Blocks.OBSIDIAN.asItem(), itemStack -> true);

        if (turnOff.get() && ((placed && placePositions.isEmpty()) || slot == -1)) {
            sendToggledMsg();
            toggle();
            return;
        }

        if (slot == -1) {
            placePositions.clear();
            return;
        }

        if (target == null || mc.player.distanceTo(target) > range.get()) {
            placePositions.clear();
            target = findTarget();
            placed = false;
            return;
        }

        placePositions = getPlacePos(target);

        if (delay >= delaySetting.get() && placePositions.size() > 0) {
            int prevSlot = mc.player.inventory.selectedSlot;
            mc.player.inventory.selectedSlot = slot;

            if (PlayerUtils.placeBlockRotate(placePositions.get(placePositions.size()-1))) {
                placePositions.remove(placePositions.get(placePositions.size() - 1));
                placed = true;
            }

            mc.player.inventory.selectedSlot = prevSlot;
            delay = 0;
        } else delay++;
    });

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        if (!render.get() || placePositions.isEmpty()) return;
        for (BlockPos pos : placePositions) Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    });


    private List<BlockPos> getPlacePos(PlayerEntity target) {
        placePositions.clear();
        BlockPos targetPos = target.getBlockPos();

        switch (topPlacement.get()) {
            case Full:
                add(targetPos.add(0, 2, 0));
                add(targetPos.add(1, 1, 0));
                add(targetPos.add(-1, 1, 0));
                add(targetPos.add(0, 1, 1));
                add(targetPos.add(0, 1, -1));
                break;
            case Top:
                add(targetPos.add(0, 2, 0));
        }

        switch (bottomPlacement.get()) {
            case Platform:
                add(targetPos.add(0, -1, 0));
                add(targetPos.add(1, -1, 0));
                add(targetPos.add(0, -1, 0));
                add(targetPos.add(0, -1, 1));
                add(targetPos.add(0, -1, -1));
                break;
            case Single:
                add(targetPos.add(0, -1, 0));
        }
        return placePositions;
    }


    private void add(BlockPos blockPos) {
        if (!placePositions.contains(blockPos) && mc.world.getBlockState(blockPos).getMaterial().isReplaceable() && mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), blockPos, ShapeContext.absent())) placePositions.add(blockPos);
    }

    private PlayerEntity findTarget() {
        for(PlayerEntity player : mc.world.getPlayers()){
            if (player == mc.player || !FriendManager.INSTANCE.attack(player) || !player.isAlive()) continue;
            if (target == null) target = player;
            else if (mc.player.distanceTo(player) < mc.player.distanceTo(target)) target = player;
        }

        for (FakePlayerEntity fakeTarget : FakePlayer.players.keySet()) {
            if (!FriendManager.INSTANCE.attack(fakeTarget) || !fakeTarget.isAlive()) continue;
            if (target == null) target = fakeTarget;
            else if (mc.player.distanceTo(fakeTarget) < mc.player.distanceTo(target)) target = fakeTarget;
        }

        return target;
    }
}