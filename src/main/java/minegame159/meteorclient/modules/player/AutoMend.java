/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

//Updated by squidoodly 18/06/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.combat.AutoArmor;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.player.Chat;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.screen.slot.SlotActionType;

public class AutoMend extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> swords = sgGeneral.add(new BoolSetting.Builder()
            .name("swords")
            .description("Moves swords.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> armourSlots = sgGeneral.add(new BoolSetting.Builder()
            .name("use-armour-slots")
            .description("Whether or not to use armor slots to mend items quicker.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> removeFinished = sgGeneral.add(new BoolSetting.Builder()
            .name("remove-finished")
            .description("The items will be moved out of active slots if there are no items to replace, but space in your inventory.")
            .defaultValue(true)
            .build()
    );

    public AutoMend() {
        super(Category.Player, "auto-mend", "Automatically replaces equipped items and items in your offhand with Mending when fully repaired.");
    }

    private void replaceItem(boolean offhandEmpty) {
        for (int i = 0; i < mc.player.inventory.main.size(); i++) {
            ItemStack itemStack = mc.player.inventory.getStack(i);
            if (EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack) == 0 || !itemStack.isDamaged()) continue;
            if (!swords.get() && itemStack.getItem() instanceof SwordItem) continue;

            InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 0, SlotActionType.PICKUP);
            InvUtils.clickSlot(InvUtils.OFFHAND_SLOT, 0, SlotActionType.PICKUP);
            if (!offhandEmpty) InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 0, SlotActionType.PICKUP);

            break;
        }
        if(!mc.player.getOffHandStack().isDamaged() && removeFinished.get() && mc.player.inventory.getEmptySlot() != -1){
            InvUtils.clickSlot(InvUtils.OFFHAND_SLOT, 0, SlotActionType.PICKUP);
            InvUtils.clickSlot(InvUtils.invIndexToSlotId(mc.player.inventory.getEmptySlot()), 0, SlotActionType.PICKUP);
        }
    }

    private boolean checkSlot(ItemStack itemStack, int slot){
        boolean correct = false;
        if(slot == 5 && ((ArmorItem) itemStack.getItem()).getSlotType() == EquipmentSlot.HEAD) correct = true;
        else if(slot == 6 && ((ArmorItem) itemStack.getItem()).getSlotType() == EquipmentSlot.CHEST) correct = true;
        else if(slot == 7 && ((ArmorItem) itemStack.getItem()).getSlotType() == EquipmentSlot.LEGS) correct = true;
        else if(slot == 8 && ((ArmorItem) itemStack.getItem()).getSlotType() == EquipmentSlot.FEET) correct = true;
        return correct;
    }

    private void replaceArmour(int slot, boolean empty){
        for (int i = 0; i < mc.player.inventory.main.size(); i++) {
            ItemStack itemStack = mc.player.inventory.getStack(i);
            if(!(itemStack.getItem() instanceof ArmorItem)) continue;
            if(!checkSlot(mc.player.inventory.getStack(i), slot)) continue;
            if (EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack) == 0 || !itemStack.isDamaged()) continue;

            InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 0, SlotActionType.PICKUP);
            InvUtils.clickSlot(slot, 0, SlotActionType.PICKUP);
            if (!empty) InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 0, SlotActionType.PICKUP);

            break;
        }
        if(!mc.player.inventory.getStack(39 - (slot - 5)).isDamaged() && removeFinished.get() && mc.player.inventory.getEmptySlot() != -1){
            InvUtils.clickSlot(slot, 0, SlotActionType.PICKUP);
            InvUtils.clickSlot(InvUtils.invIndexToSlotId(mc.player.inventory.getEmptySlot()), 0, SlotActionType.PICKUP);
        }
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (mc.currentScreen instanceof HandledScreen<?>) return;

        if (mc.player.getOffHandStack().isEmpty()) replaceItem(true);
        else if (!mc.player.getOffHandStack().isDamaged()) replaceItem(false);
        else if (EnchantmentHelper.getLevel(Enchantments.MENDING, mc.player.getOffHandStack()) == 0) replaceItem(false);

        if(armourSlots.get()) {
            if(ModuleManager.INSTANCE.get(AutoArmor.class).isActive()) {
                Chat.warning(this, "Cannot use armor slots while AutoArmor is active. Please disable AutoArmor and try again. Disabling Use Armor Slots.");
                armourSlots.set(false);
            }
            for (int i = 5; i < 9; i++) {
                if (mc.player.inventory.getStack(39 - (i - 5)).isEmpty()) replaceArmour(i, true);
                else if (!mc.player.inventory.getStack(39 - (i - 5)).isDamaged()) replaceArmour(i, false);
                else if (EnchantmentHelper.getLevel(Enchantments.MENDING, mc.player.inventory.getStack(39 - (i - 5))) == 0) replaceArmour(i, false);
            }
        }
    });
}
