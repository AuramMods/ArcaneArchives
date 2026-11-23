package com.aranaira.arcanearchives.progression;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.client.ResonatorLoopSound;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;
import com.aranaira.arcanearchives.network.ArchiveDeviceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;

public final class RadiantResonatorBlockEntity extends ArchiveDeviceBlockEntity {
    private int growth;
    private int requiredTicks;
    private boolean active;
    @Nullable
    private ResourceLocation growingRule;
    @Nullable
    private BlockState growingOutput;
    @Nullable
    private BlockState grownOutput;
    @Nullable
    private ResonatorGrowth previousRule;

    public RadiantResonatorBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneArchives.RADIANT_RESONATOR_ENTITY.get(), pos, state);
    }

    static void tick(Level level, BlockPos pos, BlockState state, RadiantResonatorBlockEntity resonator) {
        if (level instanceof ServerLevel serverLevel) {
            Player owner = ArchiveStages.onlineOwner(serverLevel, resonator.owner());
            resonator.advance(serverLevel, owner, owner != null);
        } else if (level.isClientSide && resonator.isResonating()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ResonatorLoopSound.startIfNeeded(resonator));
        }
    }

    void advance(ServerLevel level, @Nullable Player owner, boolean ownerOnline) {
        BlockPos above = worldPosition.above();
        int oldSignal = comparatorSignal();
        var oldStatus = growthStatus();
        int oldDuration = requiredTicks;
        var rule = level.getRecipeManager().getAllRecipesFor(ArcaneArchives.RESONATOR_GROWTH_TYPE.get()).stream()
                .filter(candidate -> candidate.matches(level, worldPosition))
                .min(ResonatorGrowth.ORDER).orElse(null);
        active = rule != null && (!rule.settings().requiresOnlineOwner() || ownerOnline)
                && ArchiveStages.allowsRecipe(level, owner, ArchiveStages.RESONATOR_GROWTH, rule.id())
                && !level.isOutsideBuildHeight(above) && rule.settings().output().canSurvive(level, above);
        if (rule != null) {
            updateRule(rule);
            if (active && level.isEmptyBlock(above)) {
                advanceGrowth(level, above, rule.settings().output());
                setChanged();
            }
        } else if (growingRule != null && level.getRecipeManager().byKey(growingRule).isEmpty()) {
            growth = 0;
            requiredTicks = 0;
            growingRule = null;
            growingOutput = null;
            previousRule = null;
            setChanged();
        }
        if (oldSignal != comparatorSignal() || oldStatus != growthStatus() || oldDuration != requiredTicks) {
            BlockState state = getBlockState();
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void updateRule(ResonatorGrowth rule) {
        int duration = rule.duration();
        BlockState output = rule.settings().output();
        boolean legacyProgress = growingRule == null && rule.id().equals(ResonatorGrowth.DEFAULT);
        if (!legacyProgress && (!rule.id().equals(growingRule) || output != growingOutput
                || duration != requiredTicks || (previousRule != null && !previousRule.equals(rule)))) {
            growth = 0;
        }
        if (!rule.id().equals(growingRule) || output != growingOutput || duration != requiredTicks) {
            setChanged();
        }
        growingRule = rule.id();
        growingOutput = output;
        requiredTicks = duration;
        previousRule = rule;
    }

    private void advanceGrowth(ServerLevel level, BlockPos above, BlockState output) {
        if (growth < requiredTicks) {
            growth++;
        }
        if (growth >= requiredTicks && level.setBlock(above, output, Block.UPDATE_ALL)) {
            growth = 0;
            grownOutput = output;
            if (ArchiveConfig.MACHINE_SOUNDS.get() && ArchiveConfig.RESONATOR_COMPLETION_SOUND.get()) {
                level.playSound(null, worldPosition, ArcaneArchives.RESONATOR_COMPLETE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            for (Ocelot ocelot : level.getEntitiesOfClass(Ocelot.class, new AABB(above).inflate(0.4))) {
                ocelot.push((level.random.nextDouble() - 0.5) * 3.0, level.random.nextDouble() * 5.0,
                        (level.random.nextDouble() - 0.5) * 3.0);
            }
        }
    }

    public boolean isResonating() {
        return active && !isRemoved() && level != null && level.isEmptyBlock(worldPosition.above());
    }

    public int percentGrown() {
        return requiredTicks > 0 ? (int) Math.min(100, 100.0 * growth / requiredTicks) : 0;
    }

    public GrowthStatus growthStatus() {
        if (level != null) {
            BlockState above = level.getBlockState(worldPosition.above());
            if (isGrownOutput(above)) {
                return GrowthStatus.HARVESTABLE;
            }
            if (!above.isAir()) {
                return GrowthStatus.OBSTRUCTED;
            }
        }
        return active ? GrowthStatus.RESONATING : GrowthStatus.OFFLINE;
    }

    int comparatorSignal() {
        if (level == null) {
            return 0;
        }
        BlockState above = level.getBlockState(worldPosition.above());
        if (isGrownOutput(above)) {
            return 15;
        }
        if (!active || !above.isAir() || requiredTicks == 0) {
            return 0;
        }
        return 1 + Math.min(13, (int) (14.0 * growth / requiredTicks));
    }

    private boolean isGrownOutput(BlockState state) {
        return state.is(ArcaneArchives.RAW_QUARTZ_CLUSTER.get()) || state == grownOutput;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        growth = Math.max(0, tag.getInt("current_tick"));
        requiredTicks = Math.max(0, tag.getInt("growth_duration"));
        active = tag.getBoolean("can_tick");
        growingRule = tag.contains("growth_rule") ? ResourceLocation.tryParse(tag.getString("growth_rule")) : null;
        growingOutput = readState(tag, "growth_output");
        grownOutput = readState(tag, "grown_output");
        previousRule = null;
    }

    @Nullable
    private static BlockState readState(CompoundTag tag, String key) {
        return tag.contains(key) ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(key)) : null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveGrowth(tag);
        tag.putBoolean("can_tick", active);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveGrowth(tag);
    }

    private void saveGrowth(CompoundTag tag) {
        tag.putInt("current_tick", growth);
        tag.putInt("growth_duration", requiredTicks);
        if (growingRule != null) {
            tag.putString("growth_rule", growingRule.toString());
        }
        if (growingOutput != null) {
            tag.put("growth_output", NbtUtils.writeBlockState(growingOutput));
        }
        if (grownOutput != null) {
            tag.put("grown_output", NbtUtils.writeBlockState(grownOutput));
        }
    }

    public enum GrowthStatus {
        HARVESTABLE("harvestable", ChatFormatting.GOLD), OBSTRUCTED("obstruction", ChatFormatting.RED),
        RESONATING("resonating", ChatFormatting.GREEN), OFFLINE("offline", ChatFormatting.DARK_RED);

        private final String key;
        private final ChatFormatting color;

        GrowthStatus(String key, ChatFormatting color) {
            this.key = key;
            this.color = color;
        }

        public Component label() {
            return Component.translatable("arcanearchives.data.tooltip.resonator_status." + key).withStyle(color);
        }
    }
}
