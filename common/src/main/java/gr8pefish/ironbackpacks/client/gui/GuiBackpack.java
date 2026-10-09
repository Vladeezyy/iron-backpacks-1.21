package gr8pefish.ironbackpacks.client.gui;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

import gr8pefish.ironbackpacks.container.ContainerBackpack;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Class to generate the normal GUI of a backpack.
 *
 * All hail copygirl, code copied nearly verbatim (with permission) from @see <a = https://github.com/copygirl/WearableBackpacks/blob/feature/gui-rewrite/src/main/java/net/mcft/copy/backpacks/client/GuiBackpack.java>here</a>.
 */
public class GuiBackpack extends AbstractContainerScreen<ContainerBackpack> {

    /** 1.12 GuiTextureResource("backpack_gui", 512, 512). */
    private static final ResourceLocation CONTAINER_TEX = RegistrarIronBackpacks.id("textures/gui/backpack_gui.png");
    private static final int TEX_WIDTH = 512;
    private static final int TEX_HEIGHT = 512;

    @Nonnull
    private final ContainerBackpack container;

    /**
     * Creates the GUI of the backpack.
     *
     * @param containerBackpack - The container to initialize with.
     */
    public GuiBackpack(@Nonnull ContainerBackpack containerBackpack, @Nonnull Inventory inventory, @Nonnull Component title) {
        super(containerBackpack, inventory, title);

        Preconditions.checkNotNull(containerBackpack, "ContainerBackpack cannot be null");

        container = containerBackpack;
        imageWidth = container.getWidth();
        imageHeight = container.getHeight();
    }

    @Override
    public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, container.getName(), container.getBorderSide() + 1, 6, 0x404040, false);
        int invTitleX = container.getPlayerInvXOffset() + 1;
        int invTitleY = container.getBorderTop() + container.getContainerInvHeight() + 3;
        g.drawString(font, Component.translatable("container.inventory"), invTitleX, invTitleY, 0x404040, false);
    }

    /** 1.12 GuiTextureResource.drawQuad: part of the 512x512 texture, unscaled. */
    private static void quad(GuiGraphics g, int x, int y, int u, int v, int w, int h) {
        g.blit(CONTAINER_TEX, x, y, u, v, w, h, TEX_WIDTH, TEX_HEIGHT);
    }

    /*
     * This code is all copygirl's, minus one small change I made to allow it to be 7 rows.
     * TODO: Make bottom area sides "full" when >9 columns wide
     */
    @Override
    protected void renderBg(@Nonnull GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Miiight have gone *a little* overboard with the local variables...
        int b = container.getBorderSide();
        int bTop = container.getBorderTop();
        int bBot = container.getBorderBottom();
        int w = imageWidth - b * 2;
        int h = container.getContainerInvHeight();
        int pw = container.getPlayerInvWidth();
        int ph = container.getPlayerInvHeight();
        int maxw = container.getMaxColumns() * 18;
        int maxh = container.getMaxRows() * 18;
        int bufi = container.getBufferInventory();

        int x1 = x;
        int x2 = x + b;
        int x3 = x + imageWidth - b;
        int px = x + container.getPlayerInvXOffset();

        int tx1 = 4;
        int tx2 = tx1 + b + 2;
        int tx3 = tx2 + maxw + 2;
        int ty = 4;
        int tpx = 2 + b + 2 + (maxw - (pw + b * 2)) / 2 + 2;

        // Top
        quad(g, x1, y, tx1, ty, b, bTop);
        quad(g, x2, y, tx2, ty, w, bTop);
        quad(g, x3, y, tx3, ty, b, bTop);
        y += bTop;
        ty += bTop + 2;

        // Container background
        quad(g, x1, y, tx1, ty, b, h);
        quad(g, x2, y, tx2, ty, w, h);
        quad(g, x3, y, tx3, ty, b, h);
        // Container slots
        quad(g, x + container.getContainerInvXOffset(), y, tx2, 274, //Changed v to allow for 7 rows, as the texture location changed with my additions.
                container.getContainerInvWidth(), h);
        y += h;
        ty += maxh + 2;

        // Space between container and player inventory
        if (container.getBackpackSize().getColumns() > 9) {
            int sw = (w - (pw + b * 2)) / 2;
            quad(g, x1, y, tx1 - 2, ty, b, bBot);
            quad(g, x2, y, tx1 + b, ty, sw, bBot);
            quad(g, px - b, y, tpx, ty, pw + b * 2, bufi);
            quad(g, px + pw + b, y, tx3 - sw, ty, sw, bBot);
            quad(g, x3, y, tx3 + 2, ty, b, bBot);
        }
        ty += bufi + 2;
        if (container.getBackpackSize().getColumns() <= 9)
            quad(g, x, y, tpx, ty, pw + b * 2, bufi);
        y += bufi;
        ty += bufi + 2;

        // Player inventory
        quad(g, px - b, y, tpx, ty, pw + b * 2, ph + bBot);
    }
}
