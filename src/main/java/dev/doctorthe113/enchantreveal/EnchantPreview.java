package dev.doctorthe113.enchantreveal;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/** Re-runs the enchanting table's roll so every enchantment in a slot can be shown up front. */
public final class EnchantPreview {
	private EnchantPreview() {
	}

	/**
	 * Mirrors the private {@code EnchantmentMenu#getEnchantmentList}: the table reseeds its random with
	 * {@code seed + slot} before rolling, so the same seed always gives the same enchantments.
	 *
	 * @param seed the player's full enchantment seed. Clients only receive its low 16 bits, which is
	 *             why this needs the server-side player.
	 */
	public static List<EnchantmentInstance> compute(RegistryAccess registries, ItemStack stack, int seed, int slot, int cost) {
		Optional<HolderSet.Named<Enchantment>> table = registries.lookupOrThrow(Registries.ENCHANTMENT)
				.get(EnchantmentTags.IN_ENCHANTING_TABLE);
		if (table.isEmpty()) {
			return List.of();
		}

		RandomSource random = RandomSource.create(seed + slot);
		List<EnchantmentInstance> list = EnchantmentHelper.selectEnchantment(random, stack, cost, table.get().stream());
		if (stack.is(Items.BOOK) && list.size() > 1) {
			list.remove(random.nextInt(list.size()));
		}
		return list;
	}
}
