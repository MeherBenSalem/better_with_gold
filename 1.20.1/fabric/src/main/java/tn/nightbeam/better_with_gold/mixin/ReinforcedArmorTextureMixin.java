package tn.nightbeam.better_with_gold.mixin;

import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tn.nightbeam.better_with_gold.item.ReinforcedGoldenArmorItem;

@Mixin(HumanoidArmorLayer.class)
public abstract class ReinforcedArmorTextureMixin {
    @Inject(method = "getArmorLocation", at = @At("HEAD"), cancellable = true)
    private void betterWithGoldTexture(ArmorItem item, boolean leggings, String overlay,
                                      CallbackInfoReturnable<ResourceLocation> result) {
        if (item instanceof ReinforcedGoldenArmorItem) {
            result.setReturnValue(new ResourceLocation("better_with_gold",
                    "textures/models/armor/gold_amyth_layer_" + (leggings ? 2 : 1) + ".png"));
        }
    }
}
