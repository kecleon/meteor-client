package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.mixininterface.IStatusEffectInstance;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.entity.effect.StatusEffectInstance;

import static net.minecraft.entity.effect.StatusEffects.HASTE;

public class SpeedMine extends Module {

    public enum Mode {
        Normal,
        Haste1,
        Haste2
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .defaultValue(Mode.Normal)
            .build()
    );
    public final Setting<Double> modifier = sgGeneral.add(new DoubleSetting.Builder()
            .name("modifier")
            .description("Mining speed modifier. An additional value of 0.2 is equivalent to one haste level (1.2 = haste 1).")
            .defaultValue(1.4D)
            .min(0D)
            .sliderMin(1D)
            .sliderMax(10D)
            .build()
    );

    public SpeedMine() {
        super(Category.Player, "speed-mine", "Allows you to quickly mine blocks.");
    }

    @EventHandler
    public final Listener<PostTickEvent> onTick = new Listener<>(e -> {
        Mode mode = this.mode.get();

        if (mode == Mode.Haste1 || mode == Mode.Haste2) {
            int amplifier = mode == Mode.Haste2 ? 1 : 0;
            if (mc.player.hasStatusEffect(HASTE)) {
                StatusEffectInstance effect = mc.player.getStatusEffect(HASTE);
                ((IStatusEffectInstance) effect).setAmplifier(amplifier);
                if (effect.getDuration() < 20) {
                    ((IStatusEffectInstance) effect).setDuration(20);
                }
            } else {
                mc.player.addStatusEffect(new StatusEffectInstance(HASTE, 20, amplifier, false, false, false));
            }
        }
    });
}
