/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import baritone.api.BaritoneAPI;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.world.GoalDirection;

public class AutoWalk extends Module {
    public enum Mode {
        Simple,
        Smart
    }
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Walking mode.")
            .defaultValue(Mode.Smart)
            .onChanged(mode1 -> {
                if (isActive()) {
                    if (mode1 == Mode.Simple) {
                        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
                        goal = null;
                    } else {
                        timer = 0;
                        createGoal();
                    }
                }
            })
            .build()
    );

    private int timer = 0;
    private GoalDirection goal;

    public AutoWalk() {
        super(Category.Movement, "auto-walk", "Automatically walks forward.");
    }

    @Override
    public void onActivate() {
        if (mode.get() == Mode.Smart) createGoal();
    }

    @Override
    public void onDeactivate() {
        if (mode.get() == Mode.Simple) ((IKeyBinding) mc.options.keyForward).setPressed(false);
        else BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

        goal = null;
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (mode.get() == Mode.Simple) {
            ((IKeyBinding) mc.options.keyForward).setPressed(true);
        } else {
            if (timer > 20) {
                timer = 0;
                goal.recalculate(mc.player.getPos());
            }

            timer++;
        }
    });

    private void createGoal() {
        timer = 0;
        goal = new GoalDirection(mc.player.getPos(), mc.player.yaw);
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(goal);
    }
}
