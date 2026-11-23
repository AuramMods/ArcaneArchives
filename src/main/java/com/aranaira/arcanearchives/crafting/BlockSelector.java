package com.aranaira.arcanearchives.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Matches any of the supplied blocks, block tags, or exact states. An empty selector matches any state. */
public record BlockSelector(List<Block> blocks, List<TagKey<Block>> tags, List<BlockState> states) {
    public static final BlockSelector ANY = new BlockSelector(List.of(), List.of(), List.of());
    public static final Codec<BlockSelector> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().listOf().optionalFieldOf("blocks", List.of()).forGetter(BlockSelector::blocks),
            TagKey.codec(Registries.BLOCK).listOf().optionalFieldOf("tags", List.of()).forGetter(BlockSelector::tags),
            BlockState.CODEC.listOf().optionalFieldOf("states", List.of()).forGetter(BlockSelector::states)
    ).apply(instance, BlockSelector::new));

    public BlockSelector {
        blocks = List.copyOf(blocks);
        tags = List.copyOf(tags);
        states = List.copyOf(states);
    }

    public boolean matches(BlockState state) {
        return (blocks.isEmpty() && tags.isEmpty() && states.isEmpty())
                || blocks.contains(state.getBlock()) || states.contains(state) || tags.stream().anyMatch(state::is);
    }

    public static BlockSelector anyOf(List<BlockSelector> options) {
        return new BlockSelector(options.stream().flatMap(option -> option.blocks().stream()).toList(),
                options.stream().flatMap(option -> option.tags().stream()).toList(),
                options.stream().flatMap(option -> option.states().stream()).toList());
    }
}
