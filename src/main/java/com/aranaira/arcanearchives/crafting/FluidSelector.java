package com.aranaira.arcanearchives.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import java.util.List;

public record FluidSelector(List<Fluid> fluids, List<TagKey<Fluid>> tags) {
    public static final FluidSelector ANY = new FluidSelector(List.of(), List.of());
    public static final Codec<FluidSelector> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().listOf().optionalFieldOf("fluids", List.of()).forGetter(FluidSelector::fluids),
            TagKey.codec(Registries.FLUID).listOf().optionalFieldOf("tags", List.of()).forGetter(FluidSelector::tags)
    ).apply(instance, FluidSelector::new));

    public FluidSelector {
        fluids = List.copyOf(fluids);
        tags = List.copyOf(tags);
    }

    public boolean matches(FluidState state) {
        return (fluids.isEmpty() && tags.isEmpty()) || fluids.contains(state.getType()) || tags.stream().anyMatch(state::is);
    }

    public static FluidSelector anyOf(List<FluidSelector> options) {
        return new FluidSelector(options.stream().flatMap(option -> option.fluids().stream()).toList(),
                options.stream().flatMap(option -> option.tags().stream()).toList());
    }
}
