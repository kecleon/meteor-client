package minegame159.meteorclient.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class RenderUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    //Items
    public static void drawItem(ItemStack itemStack, int x, int y, double scale, boolean count) {
        DiffuseLighting.enable();
        RenderSystem.pushMatrix();
        RenderSystem.scaled(scale, scale, 1);
        mc.getItemRenderer().renderGuiItemIcon(itemStack, x, y);
        if (count) mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, x, y, null);
        RenderSystem.popMatrix();
        DiffuseLighting.disable();
    }

    //Tracers
    public enum TracerTarget {
        Head,
        Body,
        Feet
    }

    public static Vec3d getCameraVector() {
        return new Vec3d(0, 0, 1)
                .rotateX(-(float) Math.toRadians(mc.gameRenderer.getCamera().getPitch()))
                .rotateY(-(float) Math.toRadians(mc.gameRenderer.getCamera().getYaw()))
                .add(mc.gameRenderer.getCamera().getPos());
    }


    public static void drawTracerToEntity(RenderEvent event, Entity entity, Color color, TracerTarget target, boolean stem) {
        double x = entity.prevX + (entity.getX() - entity.prevX) * event.tickDelta;
        double y = entity.prevY + (entity.getY() - entity.prevY) * event.tickDelta;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * event.tickDelta;


        double height = entity.getBoundingBox().maxY - entity.getBoundingBox().minY;
        if (target == TracerTarget.Head) y += height;
        else if (target == TracerTarget.Body) y += height / 2;

        drawLine(getCameraVector(), x, y, z, color, event);
        if (stem) Renderer.LINES.line(x, entity.getY(), z, x, entity.getY() + height, z, color);
    }

    public static void drawTracerToPos(BlockPos pos, Color color, RenderEvent event) {
        drawLine(getCameraVector(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5f, color, event);
    }

    public static void drawLine(Vec3d vec, double x2, double y2, double z2, Color color, RenderEvent event) {
        Renderer.LINES.line(
                vec.x - (mc.gameRenderer.getCamera().getPos().x - event.offsetX),
                vec.y - (mc.gameRenderer.getCamera().getPos().y - event.offsetY),
                vec.z - (mc.gameRenderer.getCamera().getPos().z - event.offsetZ),
                x2, y2, z2, color);
    }

    public static void drawTracerToBlockEntity(BlockEntity blockEntity, Color color, RenderEvent event) {
        drawTracerToPos(blockEntity.getPos(), color, event);
    }
}

