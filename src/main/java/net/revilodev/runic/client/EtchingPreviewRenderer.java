package net.revilodev.runic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class EtchingPreviewRenderer {
    private EtchingPreviewRenderer() {}

    /**
     * Render a spinning 3D item inside a GUI rectangle (abs coords).
     */
    public static void render(GuiGraphics gg, ItemStack stack,
                              int absX, int absY, int w, int h, float partialTick) {
        if (stack == null || stack.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        Level level = mc.level;

        // clip to the preview box so nothing bleeds outside
        gg.enableScissor(absX, absY, absX + w, absY + h);

        PoseStack ps = gg.pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        ps.pushPose();

        // Draw above GUI
        ps.translate(absX + w / 2f, absY + (h * 0.55f), 200.0f);

        // Scale model to the box; item models are ~16 units
        float s = ((Math.min(w, h) * 0.9f) / 16f) * 10f;
        ps.scale(s, -s, s); // flip Y for GUI coords

        // gentle tilt + spin over time
        float t = level != null ? (level.getGameTime() + partialTick)
                : (float) (Util.getMillis() / 50.0);
        float yaw = (t * 2.0f) % 360.0f;  // slow spin
        ps.mulPose(Axis.XP.rotationDegrees(15.0f));
        ps.mulPose(Axis.YP.rotationDegrees(yaw));

        itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                ps,
                buffers,
                level,
                0 // seed
        );

        ps.popPose();

        // flush and unclip
        buffers.endBatch();
        gg.disableScissor();
    }
}
