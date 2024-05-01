package uwu.lopyluna.create_dd.content.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.world.level.block.Blocks;
import uwu.lopyluna.create_dd.content.recipes.SandingRecipe;

public class FanSandingCategory extends DProcessingViaFanCategory.MultiOutput<SandingRecipe> {

    public FanSandingCategory(Info<SandingRecipe> info) {
        super(info);
    }

    @Override
    protected AllGuiTextures getBlockShadow() {
        return AllGuiTextures.JEI_SHADOW;
    }

    @Override
    protected void renderAttachedBlock(IRecipeSlotsView iRecipeSlotsView, PoseStack matrixStack, double mouseX, double mouseY) {
        GuiGameElement.of(Blocks.SAND.defaultBlockState())
                .scale(SCALE)
                .atLocal(0, 0, 2)
                .lighting(AnimatedKinetics.DEFAULT_LIGHTING)
                .render(matrixStack);
    }

}
