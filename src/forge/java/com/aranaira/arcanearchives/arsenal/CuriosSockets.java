package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;

final class CuriosSockets {
    private CuriosSockets() {}

    static List<GemSocketItem.SlotRef> equipped(Player player) {
        return CuriosApi.getCuriosInventory(player).map(inventory -> inventory.findCurios(ArcaneArchives.GEM_SOCKET.get())
                .stream().map(slot -> new GemSocketItem.SlotRef(-1, slot.slotContext().identifier(), slot.slotContext().index()))
                .toList()).orElseGet(List::of);
    }

    static ItemStack stack(Player player, GemSocketItem.SlotRef ref) {
        return CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(inventory -> inventory.findCurio(ref.curio(), ref.curioIndex()))
                .map(SlotResult::stack).orElse(ItemStack.EMPTY);
    }
}
