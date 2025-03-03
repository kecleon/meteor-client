/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PreTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.player.PlayerUtils;
import minegame159.meteorclient.utils.world.BlockIterator;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class HoleFiller extends Module {

    public enum PlaceMode {
        Obsidian,
        Cobweb,
        Any
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("horizontal-radius")
            .description("Horizontal radius in which to search for holes.")
            .defaultValue(4)
            .min(0)
            .sliderMax(6)
            .build()
    );

    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("vertical-radius")
            .description("Vertical radius in which to search for holes.")
            .defaultValue(4)
            .min(0)
            .sliderMax(6)
            .build()
    );

    private final Setting<HoleFiller.PlaceMode> mode = sgGeneral.add(new EnumSetting.Builder<HoleFiller.PlaceMode>()
            .name("block")
            .description("What kind of blocks you use to fill holes with.")
            .defaultValue(PlaceMode.Obsidian)
            .build()
    );

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    public HoleFiller() {
        super(Category.Combat, "hole-filler", "Fills holes with specified blocks.");
    }

    @EventHandler
    private final Listener<PreTickEvent> onTick = new Listener<>(event -> {
        BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos1, blockState) -> {
            if (!blockState.getMaterial().isReplaceable()) return;

            blockPos.set(blockPos1);

            Block bottom = mc.world.getBlockState(add(0, -1, 0)).getBlock();
            if (bottom != Blocks.BEDROCK && bottom != Blocks.OBSIDIAN) return;
            Block forward = mc.world.getBlockState(add(0, 1, 1)).getBlock();
            if (forward != Blocks.BEDROCK && forward != Blocks.OBSIDIAN) return;
            Block back = mc.world.getBlockState(add(0, 0, -2)).getBlock();
            if (back != Blocks.BEDROCK && back != Blocks.OBSIDIAN) return;
            Block right = mc.world.getBlockState(add(1, 0, 1)).getBlock();
            if (right != Blocks.BEDROCK && right != Blocks.OBSIDIAN) return;
            Block left = mc.world.getBlockState(add(-2, 0, 0)).getBlock();
            if (left != Blocks.BEDROCK && left != Blocks.OBSIDIAN) return;
            add(1, 0, 0);

            if (PlayerUtils.placeBlock(blockPos, findSlot(), Hand.MAIN_HAND)) BlockIterator.disableCurrent();
        });
    });

    private int findSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.player.inventory.getStack(i);

            switch (mode.get()) {
                case Obsidian:
                    if (itemStack.getItem() == Items.OBSIDIAN || itemStack.getItem() == Items.CRYING_OBSIDIAN) return i;
                    break;
                case Cobweb:
                    if (itemStack.getItem() == Items.COBWEB) return i;
                    break;
                case Any:
                    if (itemStack.getItem() instanceof BlockItem && ((BlockItem) itemStack.getItem()).getBlock().getDefaultState().isFullCube(mc.world, blockPos)) return i;
                    break;
            }
        }

        return -1;
    }

    private BlockPos.Mutable add(int x, int y, int z) {
        blockPos.setX(blockPos.getX() + x);
        blockPos.setY(blockPos.getY() + y);
        blockPos.setZ(blockPos.getZ() + z);
        return blockPos;
    }
}
