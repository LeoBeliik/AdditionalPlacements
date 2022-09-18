package com.firemerald.dvsas.block;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface IVerticalBlock extends ItemLike
{
	public BlockState rotateImpl(BlockState blockState, Rotation rotation);

	public BlockState mirrorImpl(BlockState blockState, Mirror mirror);

	public BlockState getStateForPlacementImpl(BlockPlaceContext context);

	public default void appendHoverTextImpl(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag)
	{
		tooltip.add(MutableComponent.create(new TranslatableContents("tooltip.dvsas.verticalplacement")));
	}

	public boolean hasVertical();

	public BlockState getDefaultVerticalState(BlockState currentState, FluidState fluidState);

	public BlockState getDefaultHorizontalState(BlockState currentState, FluidState fluidState);

	public FluidState getFluidStateImpl(BlockState blockState);

	public boolean isThis(BlockState blockState);

	public static Quaternion[] DIRECTION_TRANSFORMS = new Quaternion[] {
		Quaternion.fromXYZDegrees(new Vector3f(90, 0, 0)), //DOWN
		Quaternion.fromXYZDegrees(new Vector3f(-90, 0, 0)), //UP
		Quaternion.fromXYZDegrees(new Vector3f(0, 180, 0)), //NORTH
		Quaternion.ONE, //SOUTH
		Quaternion.fromXYZDegrees(new Vector3f(0, -90, 0)), //WEST
		Quaternion.fromXYZDegrees(new Vector3f(0, 90, 0)), //EAST
	};

	@OnlyIn(Dist.CLIENT)
	public default void renderHighlight(PoseStack pose, VertexConsumer vertexConsumer, Player player, BlockHitResult result, Camera camera, float partial)
	{
		pose.pushPose();
		BlockPos hit = result.getBlockPos();
		double hitX = hit.getX();
		double hitY = hit.getY();
		double hitZ = hit.getZ();
		switch (result.getDirection())
		{
		case WEST:
			hitX = result.getLocation().x - 1.005;
			break;
		case EAST:
			hitX = result.getLocation().x + .005;
			break;
		case DOWN:
			hitY = result.getLocation().y - 1.005;
			break;
		case UP:
			hitY = result.getLocation().y + .005;
			break;
		case NORTH:
			hitZ = result.getLocation().z - 1.005;
			break;
		case SOUTH:
			hitZ = result.getLocation().z + .005;
			break;
		default:
		}
		Vec3 pos = camera.getPosition();
		pose.translate(hitX - pos.x + .5, hitY - pos.y + .5, hitZ - pos.z + .5);
		pose.mulPose(DIRECTION_TRANSFORMS[result.getDirection().ordinal()]);
		renderPlacementHighlight(pose, vertexConsumer, player, result, partial);
		pose.popPose();
	}

	@OnlyIn(Dist.CLIENT)
	public void renderPlacementHighlight(PoseStack pose, VertexConsumer vertexConsumer, Player player, BlockHitResult result, float partial);
}