package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.Dimension;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class VoidESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Colors");

    // General

    private final Setting<Boolean> airOnly = sgGeneral.add(new BoolSetting.Builder()
            .name("air-only")
            .description("Checks bedrock only for air blocks.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("horizontal-radius")
            .description("Horizontal radius in which to search for holes.")
            .defaultValue(64)
            .min(0)
            .sliderMax(256)
            .build()
    );

    private final Setting<Integer> holeHeight = sgGeneral.add(new IntSetting.Builder()
            .name("hole-height")
            .description("The minimum hole height to be rendered.")
            .defaultValue(1)  // If we already have one hole in the bedrock layer, there is already something interesting.
            .min(1)
            .sliderMax(5)     // There is no sense to check more than 5.
            .build()
    );

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("fill-color")
            .description("The color that fills holes in the void.")
            .defaultValue(new SettingColor(225, 25, 25))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color to draw lines of holes to the void.")
            .defaultValue(new SettingColor(225, 25, 255))
            .build()
    );

    public VoidESP() {
        super(Category.Render, "void-esp", "Renders holes in bedrock layers that lead to the void.");
    }

    private final List<BlockPos> voidHoles = new ArrayList<>();

    private void getHoles(int searchRange, int holeHeight) {
        voidHoles.clear();
        if (Utils.getDimension() == Dimension.End) return;

        BlockPos playerPos = mc.player.getBlockPos();
        int playerY = playerPos.getY();

        for (int x = -searchRange; x < searchRange; ++x) {
            for (int z = -searchRange; z < searchRange; ++z) {
                BlockPos bottomBlockPos = playerPos.add(x, -playerY, z);

                int blocksFromBottom = 0;
                for (int i = 0; i < holeHeight; ++i)
                    if (isBlockMatching(mc.world.getBlockState(bottomBlockPos.add(0, i, 0)).getBlock()))
                        ++blocksFromBottom;

                if (blocksFromBottom >= holeHeight) voidHoles.add(bottomBlockPos);

                // checking nether roof
                if (Utils.getDimension() == Dimension.Nether) {
                    BlockPos topBlockPos = playerPos.add(x, 127 - playerY, z);

                    int blocksFromTop = 0;
                    for (int i = 0; i < holeHeight; ++i)
                        if (isBlockMatching(mc.world.getBlockState(bottomBlockPos.add(0, 127 - i, 0)).getBlock()))
                            ++blocksFromTop;

                    if (blocksFromTop >= holeHeight) voidHoles.add(topBlockPos);
                }
            }
        }
    }

    private boolean isBlockMatching(Block block) {
        if (airOnly.get())
            return block == Blocks.AIR;
        return block != Blocks.BEDROCK;
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        getHoles(horizontalRadius.get(), holeHeight.get());
    });

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        for (BlockPos voidHole : voidHoles) {
            Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, voidHole, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    });
}
