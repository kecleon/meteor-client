/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

//Created by squidoodly 24/12/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.game.GetTooltipEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.input.KeyBinds;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShulkerPeek extends Module {
    public enum Mode {
        Tooltip,
        Always
    }

    public enum BackgroundMode {
        Light,
        Dark
    }

    public ShulkerPeek(){
        super(Category.Render, "shulker-peek", "Allows you to see what is inside Shulker Boxes without placing them.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The way to display the shulker info.")
            .defaultValue(Mode.Always)
            .build()
    );

    public final Setting<BackgroundMode> bgMode = sgGeneral.add(new EnumSetting.Builder<BackgroundMode>()
            .name("background-mode")
            .description("The background of the tooltip.")
            .defaultValue(BackgroundMode.Light)
            .build()
    );

    private final Setting<Integer> lines = sgGeneral.add(new IntSetting.Builder()
            .name("lines")
            .description("The number of lines to show in the tooltip mode.")
            .defaultValue(8)
            .min(0)
            .build()
    );

    @EventHandler
    private final Listener<GetTooltipEvent> onGetTooltip = new Listener<>(event -> {
        if (mode.get() != Mode.Tooltip) return;
        if (!Utils.isShulker(event.itemStack.getItem())) return;

        CompoundTag compoundTag = event.itemStack.getSubTag("BlockEntityTag");
        if (compoundTag != null) {
            if (compoundTag.contains("Items", 9)) {
                DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(27, ItemStack.EMPTY);
                Inventories.fromTag(compoundTag, itemStacks);
                int totalItemStacks = 0;
                int displaysItemStacks = 0;

                if (ModuleManager.INSTANCE.get(this.getClass()).isActive()) {
                    Map<Text, Integer> itemCounts = new HashMap<>();
                    for (ItemStack itemStack : itemStacks) {
                        if (!itemStack.isEmpty()) {
                            Text name = itemStack.getName();
                            int itemCount = itemCounts.computeIfAbsent(name, item -> 0);
                            itemCount += itemStack.getCount();
                            itemCounts.put(name, itemCount);
                        }
                    }

                    totalItemStacks = itemCounts.size();

                    List<Pair<Text, Integer>> items = new ArrayList<>(5);
                    for (int i = 0; i < lines.get(); i++) {
                        if (itemCounts.size() == 0) break;

                        Text bestItem = null;
                        int mostItem = 0;

                        for (Text a : itemCounts.keySet()) {
                            int b = itemCounts.get(a);
                            if (b > mostItem) {
                                mostItem = b;
                                bestItem = a;
                            }
                        }

                        items.add(new Pair<>(bestItem, mostItem));
                        itemCounts.remove(bestItem);
                    }

                    for (Pair<Text, Integer> itemCount : items) {
                        displaysItemStacks++;
                        MutableText text = itemCount.getLeft().copy();
                        text.append(" x").append(String.valueOf(itemCount.getRight()));
                        event.list.add(text);
                    }
                } else {
                    for (ItemStack itemStack : itemStacks) {
                        if (!itemStack.isEmpty()) {
                            totalItemStacks++;

                            if (displaysItemStacks <= 4) {
                                displaysItemStacks++;
                                MutableText text = itemStack.getName().copy();
                                text.append(" x").append(String.valueOf(itemStack.getCount()));
                                event.list.add(text);
                            }
                        }
                    }
                }

                if (totalItemStacks - displaysItemStacks > 0) {
                    event.list.add((new TranslatableText("container.shulkerBox.more", totalItemStacks - displaysItemStacks)).formatted(Formatting.ITALIC));
                }

                event.list.add(new LiteralText(""));
                event.list.add(new LiteralText("Press " + Formatting.YELLOW + Utils.getKeyName(KeyBindingHelper.getBoundKeyOf(KeyBinds.SHULKER_PEEK).getCode()) + Formatting.RESET + " to peek"));
            }
        }
    });
}
