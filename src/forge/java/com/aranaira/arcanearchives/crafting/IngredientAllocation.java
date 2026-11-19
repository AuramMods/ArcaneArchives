package com.aranaira.arcanearchives.crafting;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;

record IngredientAllocation(int slots, int[][] remainingCapacity) {
    @Nullable
    static IngredientAllocation solve(List<GemCuttingRecipe.Requirement> requirements, int slots,
                                      IntFunction<ItemStack> getStack) {
        int firstRequirement = slots + 1;
        int sink = firstRequirement + requirements.size();
        int[][] capacity = new int[sink + 1][sink + 1];
        int demand = 0;
        for (int index = 0; index < requirements.size(); index++) {
            int count = requirements.get(index).count();
            capacity[firstRequirement + index][sink] = count;
            demand += count;
        }

        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = getStack.apply(slot);
            if (!stack.isEmpty()) {
                capacity[0][slot + 1] = stack.getCount();
                for (int index = 0; index < requirements.size(); index++) {
                    if (requirements.get(index).ingredient().test(stack)) {
                        capacity[slot + 1][firstRequirement + index] = stack.getCount();
                    }
                }
            }
        }

        // Residual paths can reassign a shared match to a recipe's more specific ingredient.
        int[] parents = new int[sink + 1];
        int[] queue = new int[sink + 1];
        while (demand > 0) {
            Arrays.fill(parents, -1);
            parents[0] = 0;
            queue[0] = 0;
            int tail = 1;
            for (int head = 0; head < tail && parents[sink] == -1; head++) {
                int from = queue[head];
                for (int to = 1; to <= sink; to++) {
                    if (parents[to] == -1 && capacity[from][to] > 0) {
                        parents[to] = from;
                        queue[tail++] = to;
                    }
                }
            }
            if (parents[sink] == -1) {
                return null;
            }

            int moved = demand;
            for (int node = sink; node != 0; node = parents[node]) {
                moved = Math.min(moved, capacity[parents[node]][node]);
            }
            for (int node = sink; node != 0; node = parents[node]) {
                int from = parents[node];
                capacity[from][node] -= moved;
                capacity[node][from] += moved;
            }
            demand -= moved;
        }


        return new IngredientAllocation(slots, capacity);
    }

    int[] counts() {
        int[] counts = new int[slots];
        for (int slot = 0; slot < slots; slot++) {
            counts[slot] = remainingCapacity[slot + 1][0];
        }
        return counts;
    }

    int sourceFor(int requirement) {
        int[] assigned = remainingCapacity[slots + 1 + requirement];
        for (int slot = 0; slot < slots; slot++) {
            if (assigned[slot + 1] > 0) {
                return slot;
            }
        }
        throw new IllegalArgumentException("No assigned source for ingredient " + requirement);
    }
}
