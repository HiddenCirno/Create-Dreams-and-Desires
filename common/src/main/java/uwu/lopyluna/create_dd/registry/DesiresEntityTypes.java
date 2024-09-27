package uwu.lopyluna.create_dd.registry;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.data.CreateEntityBuilder;
import com.tterrag.registrate.util.entry.EntityEntry;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootingEnchantFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import uwu.lopyluna.create_dd.content.blocks.contraptions.contraption_block.BoatContraptionEntity;
import uwu.lopyluna.create_dd.content.blocks.contraptions.contraption_block.BoatContraptionRenderer;
import uwu.lopyluna.create_dd.content.entities.inert_blazeling.InertBlaze;
import uwu.lopyluna.create_dd.content.entities.seething_ablaze.SeethingBlaze;
import uwu.lopyluna.create_dd.content.entities.seething_ablaze.SeethingBlazeRenderer;
import uwu.lopyluna.create_dd.registry.helper.Lang;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.EntityFactory;
import net.minecraft.world.entity.MobCategory;
import uwu.lopyluna.create_dd.Desires;

import static uwu.lopyluna.create_dd.Desires.REGISTRATE;

@SuppressWarnings({"unused", "SameParameterValue"})
public class DesiresEntityTypes {

	public static final EntityEntry<SeethingBlaze> SEETHING_ABLAZE = REGISTRATE.entity("seething_ablaze", SeethingBlaze::new, MobCategory.MONSTER)
			.loot((table, type) -> table.add(type, new LootTable.Builder().withPool(
					LootPool.lootPool()
							.setRolls(ConstantValue.exactly(1.0F))
							.add(LootItem.lootTableItem(DesiresItems.SEETHING_ABLAZE_ROD.get())
									.apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F)))
									.apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 2.0F))))
							.when(LootItemKilledByPlayerCondition.killedByPlayer())
			)))
			.lang("Seething Ablaze")
			.properties(b -> b
					.fireImmune()
					.dimensions(EntityDimensions.fixed(0.6F, 1.8F))
					.trackRangeBlocks(8))
			.renderer(() -> SeethingBlazeRenderer::new)
			.attributes(SeethingBlaze::createAttributes)
			.register();

	public static final EntityEntry<InertBlaze> INERT_BLAZELING = REGISTRATE.entity("inert_blazeling", InertBlaze::new, MobCategory.CREATURE)
			.loot((table, type) -> table.add(type, new LootTable.Builder().withPool(
					LootPool.lootPool()
							.setRolls(ConstantValue.exactly(1.0F))
							.add(LootItem.lootTableItem(Items.BLAZE_POWDER)
									.apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F)))
									.apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 2.0F))))
							.when(LootItemKilledByPlayerCondition.killedByPlayer())
			)))
			.lang("Inert Blazeling")
			.properties(b -> b
					.fireImmune()
					.dimensions(EntityDimensions.fixed(0.6F, 0.75F))
					.trackRangeBlocks(4))
			.attributes(InertBlaze::createAttributes)
			.register();

	public static final EntityEntry<BoatContraptionEntity> BOAT_CONTRAPTION = contraption("boat_contraption",
			BoatContraptionEntity::new, () -> BoatContraptionRenderer::new,10, 3, true)
			.register();


	// TODO: Move this to a platform-specific class with an Object type because FabricEntityTypeBuilder sucks
	private static <T extends Entity> CreateEntityBuilder<T, ?> contraption(String name,  EntityFactory<T> factory,
		NonNullSupplier<NonNullFunction<EntityRendererProvider.Context, EntityRenderer<? super T>>> renderer, int range,
		int updateFrequency, boolean sendVelocity) {
		return register(name, factory, renderer, MobCategory.MISC, range, updateFrequency, sendVelocity, true,
				AbstractContraptionEntity::build);
	}

	// TODO: Move this to a platform-specific class with an Object type because FabricEntityTypeBuilder sucks
	private static <T extends Entity> CreateEntityBuilder<T, ?> register(String name, EntityFactory<T> factory,
		NonNullSupplier<NonNullFunction<EntityRendererProvider.Context, EntityRenderer<? super T>>> renderer,
		MobCategory group, int clientTrackingRange, int updateFrequency, boolean sendVelocity, boolean immuneToFire,
		NonNullConsumer<EntityType.Builder<T>> propertyBuilder) {
		String id = Lang.asId(name);

		return (CreateEntityBuilder<T, ?>) Desires.REGISTRATE
				.entity(id, factory, group)
				.properties(b -> b.trackRangeBlocks(clientTrackingRange)
						.trackedUpdateRate(updateFrequency)
						.forceTrackedVelocityUpdates(sendVelocity))
				.properties(propertyBuilder)
				.properties(b -> {
					if (immuneToFire)
						b.fireImmune();
				})
				.renderer(renderer);
	}

	public static void register() {}
}
