package com.aranaira.arcanearchives.network;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.GemCuttersTableBlockEntity;
import com.aranaira.arcanearchives.crafting.GemCuttersTableMenu;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class HiveLetterGameTests {
    private static final BlockPos TABLE = new BlockPos(1, 1, 1);

    @GameTest(template = "chest_fixture")
    public static void hiveInvitationAndResignationUseSignedRecipes(GameTestHelper test) {
        GemCuttersTableBlockEntity table = prepareTable(test);
        Player owner = player(test, "ArchiveOwner");
        Player member = player(test, "ArchiveMember");
        var networks = ArchiveNetworks.get(test.getLevel().getServer());

        test.assertTrue(craft(table, owner, "writ_expulsion").isEmpty()
                        && craft(table, member, "letter_resignation").isEmpty()
                        && table.drawer().getStackInSlot(0).getCount() == 32,
                "Personal networks should not craft resignation or expulsion letters or spend ingredients attempting to");
        ItemStack invitation = craft(table, owner, "letter_invitation");
        test.assertTrue(invitation.is(ArcaneArchives.LETTER_INVITATION.get())
                        && invitation.getTag().getUUID("creator").equals(owner.getUUID())
                        && invitation.getTag().getString("creator_name").equals("ArchiveOwner"),
                "An invitation should be stamped with its actual crafter's identity");
        read(invitation, owner);
        test.assertTrue(!invitation.isEmpty() && !networks.hasHive(owner.getUUID()),
                "The issuer cannot accept their own invitation");

        read(invitation, member);
        test.assertTrue(invitation.isEmpty() && networks.hiveOwner(member.getUUID()).equals(owner.getUUID())
                        && craft(table, member, "letter_invitation").isEmpty(),
                "A recipient should join the issuer's hive and lose invitation crafting rights");
        ItemStack letter = craft(table, member, "letter_resignation");
        read(letter, owner);
        test.assertTrue(letter.is(ArcaneArchives.LETTER_RESIGNATION.get()) && networks.sharesHive(owner.getUUID(), member.getUUID()),
                "Only the author can use their resignation");
        read(letter, member);
        test.assertTrue(letter.isEmpty() && !networks.hasHive(member.getUUID()) && !networks.hasHive(owner.getUUID()),
                "The author's resignation should consume the letter, leave the hive and disband it when empty");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void hiveWritResolvesAnvilNameAndChecksOwnership(GameTestHelper test) {
        GemCuttersTableBlockEntity table = prepareTable(test);
        Player owner = player(test, "ArchiveLeader");
        Player target = player(test, "OfflineMember");
        Player other = player(test, "OtherMember");
        read(craft(table, owner, "letter_invitation"), target);
        read(craft(table, owner, "letter_invitation"), other);
        var networks = ArchiveNetworks.get(test.getLevel().getServer());
        test.assertTrue(ArchiveNetworks.load(networks.save(new CompoundTag())).knownMemberName(target.getUUID()).equals("OfflineMember"),
                "A named member should stay identifiable after a save and reload");

        ItemStack writ = craft(table, owner, "writ_expulsion");
        test.assertTrue(writ.is(ArcaneArchives.WRIT_EXPULSION.get()), "A hive owner can craft a writ");
        read(writ, owner);
        test.assertTrue(!writ.isEmpty() && networks.hasHive(target.getUUID()), "The unnamed writ must not change membership");

        ItemStack named = rename(owner, writ, "offlinemember");
        test.assertTrue(named.getTag().hasUUID("expel") && target.getUUID().equals(named.getTag().getUUID("expel")),
                "The anvil should resolve the offline member by name and persist their UUID");
        ItemStack mistyped = rename(owner, named, "WrongName");
        test.assertTrue(!mistyped.getTag().contains("expel") && !mistyped.getTag().contains("expel_name"),
                "Renaming to an unknown name should clear the old target");
        read(named, other);
        test.assertTrue(!named.isEmpty() && networks.sharesHive(owner.getUUID(), target.getUUID()),
                "An unsigned reader cannot use another leader's writ");
        read(named, owner);
        test.assertTrue(named.isEmpty() && !networks.hasHive(target.getUUID()) && networks.sharesHive(owner.getUUID(), other.getUUID()),
                "The leader's writ must remove exactly the named member");
        test.succeed();
    }

    private static GemCuttersTableBlockEntity prepareTable(GameTestHelper test) {
        test.setBlock(TABLE, ArcaneArchives.GEMCUTTERS_TABLE.get());
        GemCuttersTableBlockEntity table = (GemCuttersTableBlockEntity) test.getBlockEntity(TABLE);
        table.drawer().setStackInSlot(0, new ItemStack(Items.PAPER, 32));
        table.drawer().setStackInSlot(1, new ItemStack(ArcaneArchives.RADIANT_DUST.get(), 16));
        table.drawer().setStackInSlot(2, new ItemStack(Items.LIGHT_BLUE_DYE, 12));
        table.drawer().setStackInSlot(3, new ItemStack(Items.PINK_DYE, 12));
        table.drawer().setStackInSlot(4, new ItemStack(Items.RED_DYE, 12));
        return table;
    }

    private static Player player(GameTestHelper test, String name) {
        Vec3 position = Vec3.atCenterOf(test.absolutePos(TABLE.south()));
        var profile = new GameProfile(UUID.randomUUID(), name);
        Player player = new Player(test.getLevel(), BlockPos.containing(position), 0, profile) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        player.setPos(position.x, position.y, position.z);
        return player;
    }

    private static ItemStack craft(GemCuttersTableBlockEntity table, Player player, String recipe) {
        GemCuttersTableMenu menu = (GemCuttersTableMenu) table.createMenu(1, player.getInventory(), player);
        if (!menu.selectRecipe(player, ArcaneArchives.id(recipe))) {
            throw new AssertionError("Missing Gem Cutter recipe " + recipe);
        }
        menu.clicked(0, 0, ClickType.PICKUP, player);
        return menu.getCarried();
    }

    private static void read(ItemStack letter, Player player) {
        letter.finishUsingItem(player.level(), player);
    }

    private static ItemStack rename(Player player, ItemStack letter, String name) {
        ItemStack named = letter.copy().setHoverName(Component.literal(name));
        MinecraftForge.EVENT_BUS.post(new AnvilRepairEvent(player, letter, ItemStack.EMPTY, named));
        return named;
    }
}
