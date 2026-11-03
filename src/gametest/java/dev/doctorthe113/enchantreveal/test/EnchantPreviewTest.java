package dev.doctorthe113.enchantreveal.test;

import java.util.ArrayList;
import java.util.List;

import dev.doctorthe113.enchantreveal.EnchantPreview;
import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;

public class EnchantPreviewTest {
	private static final Item[] ITEMS = {
			Items.DIAMOND_SWORD, Items.BOOK, Items.DIAMOND_PICKAXE, Items.BOW, Items.IRON_CHESTPLATE,
			Items.FISHING_ROD, Items.TRIDENT, Items.GOLDEN_HELMET, Items.CROSSBOW, Items.NETHERITE_BOOTS,
	};

	/** Enchants many items through the real menu and checks each result against the preview. */
	@GameTest
	public void previewMatchesRealEnchanting(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos table = helper.absolutePos(new BlockPos(3, 1, 3));
		level.setBlockAndUpdate(table, Blocks.ENCHANTING_TABLE.defaultBlockState());
		for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
			level.setBlockAndUpdate(table.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
		}

		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		EnchantmentMenu menu = new EnchantmentMenu(1, player.getInventory(), ContainerLevelAccess.create(level, table));
		var ids = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();

		int checked = 0;
		for (int round = 0; round < 600; round++) {
			player.experienceLevel = 1000;
			menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));
			menu.getSlot(0).set(new ItemStack(ITEMS[round % ITEMS.length]));
			ItemStack input = menu.getSlot(0).getItem().copy();
			int seed = player.getEnchantmentSeed();

			List<List<EnchantmentInstance>> previews = new ArrayList<>();
			for (int slot = 0; slot < 3; slot++) {
				List<EnchantmentInstance> preview = EnchantPreview.compute(level.registryAccess(), input, seed, slot, menu.costs[slot]);
				previews.add(preview);
				if (menu.costs[slot] > 0 && menu.enchantClue[slot] >= 0) {
					int s = slot;
					helper.assertTrue(preview.stream().anyMatch(e -> ids.getId(e.enchantment()) == menu.enchantClue[s]
							&& e.level() == menu.levelClue[s]), "clue missing from preview in round " + round);
				}
			}

			int button = round % 3;
			if (menu.costs[button] > 0) {
				helper.assertTrue(menu.clickMenuButton(player, button), "enchant click refused in round " + round);
				ItemEnchantments got = EnchantmentHelper.getEnchantmentsForCrafting(menu.getSlot(0).getItem());
				List<EnchantmentInstance> expected = previews.get(button);
				helper.assertTrue(got.size() == expected.size(),
						"round " + round + " (" + input.getItem() + ", slot " + button + "): expected " + describe(expected) + " got " + got);
				for (EnchantmentInstance e : expected) {
					helper.assertTrue(got.getLevel(e.enchantment()) == e.level(),
							"round " + round + " (" + input.getItem() + ", slot " + button + "): expected " + describe(expected) + " got " + got);
				}
				checked++;
			}
			menu.getSlot(0).set(ItemStack.EMPTY);
		}

		helper.assertTrue(checked > 500, "only " + checked + " enchants were checked");
		helper.succeed();
	}

	private static String describe(List<EnchantmentInstance> list) {
		List<String> parts = new ArrayList<>();
		for (EnchantmentInstance e : list) {
			Holder<Enchantment> holder = e.enchantment();
			parts.add(holder.getRegisteredName() + " " + e.level());
		}
		return parts.toString();
	}
}
