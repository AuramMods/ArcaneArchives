package com.aranaira.arcanearchives.arsenal;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public final class ChromaticPowderItem extends Item {
    public ChromaticPowderItem(Properties properties) {
        super(properties);
    }

    public static GemType.Color color(ItemStack stack) {
        return stack.getTag() == null ? GemType.Color.NONE : GemType.Color.fromOrdinal(stack.getTag().getInt("color"));
    }

    public ItemStack of(GemType.Color color) {
        var stack = new ItemStack(this);
        stack.getOrCreateTag().putInt("color", color.ordinal());
        return stack;
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return color(stack) == GemType.Color.NONE ? super.getDescriptionId(stack) : super.getDescriptionId(stack) + "." + color(stack).id();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("arcanearchives.tooltip.item.chromatic_powder").withStyle(ChatFormatting.GOLD));
    }
}
