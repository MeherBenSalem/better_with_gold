package tn.nightbeam.better_with_gold.item;

import tn.nightbeam.better_with_gold.ally.Warbands;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Warbanner: use it on the ground to plant a rally point your warband guards in formation; use it in the air
 * to lift it and have the band follow you again. Reusable. Minecraft 1.21.1 edition.
 */
public class WarbannerItem extends Item {
	public WarbannerItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (context.getLevel().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		BlockPos clicked = context.getClickedPos();
		BlockPos place = context.getClickedFace() == Direction.UP ? clicked.above() : clicked.relative(context.getClickedFace());
		Warbands.plantBanner(serverPlayer, place, player.getYRot());
		return InteractionResult.CONSUME;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && !Warbands.liftBanner(serverPlayer)) {
			serverPlayer.displayClientMessage(Component.literal("Use the Warbanner on the ground to plant it.").withStyle(ChatFormatting.YELLOW), true);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.better_with_gold.warbanner.desc1").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.better_with_gold.warbanner.desc2").withStyle(ChatFormatting.GRAY));
	}
}
