package uwu.lopyluna.create_dd.registry;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingTypeRegistry;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.trains.CubeParticleData;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.VecHelper;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import uwu.lopyluna.create_dd.DesiresCreate;
import uwu.lopyluna.create_dd.content.entities.inert_blazeling.InertBlaze;
import uwu.lopyluna.create_dd.content.entities.seething_ablaze.SeethingBlaze;
import uwu.lopyluna.create_dd.content.recipes.FreezingRecipe;
import uwu.lopyluna.create_dd.content.recipes.SandingRecipe;
import uwu.lopyluna.create_dd.content.recipes.SeethingRecipe;

import java.util.List;
import java.util.Optional;

public class DesireFanProcessingTypes {
    public static final SandingType SANDING = register("sanding", new SandingType());
    public static final FreezingType FREEZING = register("freezing", new FreezingType());
    public static final SeethingType SEETHING = register("seething", new SeethingType());

    static {
        Object2ReferenceOpenHashMap<String, FanProcessingType> map = new Object2ReferenceOpenHashMap<>();
        map.put("SANDING", SANDING);
        map.put("FREEZING", FREEZING);
        map.put("SEETHING", SEETHING);
        map.trim();
    }

    private static <T extends FanProcessingType> T register(String id, T type) {
        FanProcessingTypeRegistry.register(DesiresCreate.asResource(id), type);
        return type;
    }

    public static void register() {
    }

    public static class SandingType implements FanProcessingType {
        private static final SandingRecipe.SandingWrapper SANDING_WRAPPER = new SandingRecipe.SandingWrapper();

        @Override
        public boolean isValidAt(Level level, BlockPos pos) {
            FluidState fluidState = level.getFluidState(pos);
            if (DesiresTags.AllFluidTags.FAN_PROCESSING_CATALYSTS_SANDING.matches(fluidState)) {
                return true;
            }
            BlockState blockState = level.getBlockState(pos);
            return DesiresTags.AllBlockTags.FAN_PROCESSING_CATALYSTS_SANDING.matches(blockState);
        }

        @Override
        public int getPriority() {
            return 691000;
        }

        @Override
        public boolean canProcess(ItemStack stack, Level level) {

            SANDING_WRAPPER.setItem(0, stack);
            Optional<SandingRecipe> recipe = DesiresRecipeTypes.SANDING.find(SANDING_WRAPPER, level);
            return recipe.isPresent();
        }

        @Override
        @Nullable
        public List<ItemStack> process(ItemStack stack, Level level) {
            SANDING_WRAPPER.setItem(0, stack);
            Optional<SandingRecipe> recipe = DesiresRecipeTypes.SANDING.find(SANDING_WRAPPER, level);
            return recipe.map(sandingRecipe -> RecipeApplier.applyRecipeOn(level, stack, sandingRecipe)).orElse(null);
        }

        @Override
        public void spawnProcessingParticles(Level level, Vec3 pos) {
            if (level.random.nextInt(8) != 0)
                return;
            Vector3f color1 = new Color(0xEDEBCB).asVectorF();
            Vector3f color2 = new Color(0xE7E4BB).asVectorF();
            level.addParticle(new DustParticleOptions(color1, 1), pos.x + (level.random.nextFloat() - .5f) * .5f,
                    pos.y + .5f, pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
            level.addParticle(new DustParticleOptions(color2, 1), pos.x + (level.random.nextFloat() - .5f) * .5f,
                    pos.y + .5f, pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
            level.addParticle(new DustParticleOptions(color1, 1), pos.x + (level.random.nextFloat() - .5f) * .5f,
                    pos.y + .5f, pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
            level.addParticle(ParticleTypes.CRIT, pos.x + (level.random.nextFloat() - .5f) * .5f, pos.y + .5f,
                    pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
        }

        @Override
        public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
            particleAccess.setColor(Color.mixColors(0xE7E4BB, 0xEDEBCB, random.nextFloat()));
            particleAccess.setAlpha(1f);
            if (random.nextFloat() < 1 / 128f)
                particleAccess.spawnExtraParticle(ParticleTypes.CRIT, .125f);
            if (random.nextFloat() < 1 / 32f)
                particleAccess.spawnExtraParticle(ParticleTypes.WHITE_ASH, .125f);
        }

        @Override
        public void affectEntity(Entity entity, Level level) {

            if (entity instanceof Blaze blaze && !(entity instanceof SeethingBlaze)) {
                int progress = blaze.getPersistentData()
                        .getInt("CreateSanding");
                if (progress < 50) {
                    if (progress % 10 == 0) {
                        level.playSound(null, entity.blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.NEUTRAL,
                                0.5f, 1.5f * progress / 50f);
                    }
                    blaze.getPersistentData()
                            .putInt("CreateSanding", progress + 1);
                    return;
                }

                level.playSound(null, entity.blockPosition(), SoundEvents.BLAZE_BURN,
                        SoundSource.NEUTRAL, 1.25f, 0.65f);

                InertBlaze inertBlaze = DesiresEntityTypes.INERT_BLAZELING.create(level);
                CompoundTag serializeNBT = blaze.saveWithoutId(new CompoundTag());
                serializeNBT.remove("UUID");

                assert inertBlaze != null;
                inertBlaze.deserializeNBT(serializeNBT);
                inertBlaze.setPos(blaze.getPosition(0));
                level.addFreshEntity(inertBlaze);
                blaze.discard();
            }

            if (entity instanceof Zombie zombie && !(entity instanceof Husk)) {
                int progress = zombie.getPersistentData()
                        .getInt("CreateSanding");
                if (progress < 50) {
                    if (progress % 10 == 0) {
                        level.playSound(null, entity.blockPosition(), SoundEvents.HUSK_AMBIENT, SoundSource.NEUTRAL,
                                1f, 1.5f * progress / 50f);
                    }
                    zombie.getPersistentData()
                            .putInt("CreateSanding", progress + 1);
                    return;
                }

                level.playSound(null, entity.blockPosition(), SoundEvents.HUSK_CONVERTED_TO_ZOMBIE,
                        SoundSource.NEUTRAL, 1.25f, 0.65f);

                Husk husk = EntityType.HUSK.create(level);
                CompoundTag serializeNBT = zombie.saveWithoutId(new CompoundTag());
                serializeNBT.remove("UUID");

                assert husk != null;
                husk.deserializeNBT(serializeNBT);
                husk.setPos(zombie.getPosition(0));
                level.addFreshEntity(husk);
                zombie.discard();
            }
        }
    }

    public static class SeethingType implements FanProcessingType {
        private static final SeethingRecipe.SeethingWrapper SEETHING_WRAPPER = new SeethingRecipe.SeethingWrapper();

        @Override
        public boolean isValidAt(Level level, BlockPos pos) {
            FluidState fluidState = level.getFluidState(pos);
            if (DesiresTags.AllFluidTags.FAN_PROCESSING_CATALYSTS_SEETHING.matches(fluidState)) {
                return true;
            }
            BlockState blockState = level.getBlockState(pos);
            if (DesiresTags.AllBlockTags.FAN_PROCESSING_CATALYSTS_SEETHING.matches(blockState)) {
                return !blockState.hasProperty(BlazeBurnerBlock.HEAT_LEVEL) || blockState.getValue(BlazeBurnerBlock.HEAT_LEVEL).isAtLeast(BlazeBurnerBlock.HeatLevel.SEETHING);
            }
            return false;
        }

        @Override
        public int getPriority() {
            return 691200;
        }

        @Override
        public boolean canProcess(ItemStack stack, Level level) {
            SEETHING_WRAPPER.setItem(0, stack);
            Optional<SeethingRecipe> recipe = DesiresRecipeTypes.SEETHING.find(SEETHING_WRAPPER, level);
            return recipe.isPresent();
        }

        @Override
        @Nullable
        public List<ItemStack> process(ItemStack stack, Level level) {
            SEETHING_WRAPPER.setItem(0, stack);
            Optional<SeethingRecipe> recipe = DesiresRecipeTypes.SEETHING.find(SEETHING_WRAPPER, level);
            return recipe.map(seethingRecipe -> RecipeApplier.applyRecipeOn(level, stack, seethingRecipe)).orElse(null);
        }

        @Override
        public void spawnProcessingParticles(Level level, Vec3 pos) {
            if (level.random.nextInt(8) != 0)
                return;
            Vector3f color = new Color(0x1e0f3d).asVectorF();
            level.addParticle(new DustParticleOptions(color, 1), pos.x + (level.random.nextFloat() - .5f) * .5f,
                    pos.y + .5f, pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + .45f, pos.z, 0, 0, 0);
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x + (level.random.nextFloat() - .5f) * .5f, pos.y + .5f,
                    pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
        }

        @Override
        public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
            particleAccess.setColor(Color.mixColors(0x64C9FD, 0x3f74e8, random.nextFloat()));
            particleAccess.setAlpha(1f);
            if (random.nextFloat() < 1 / 32f)
                particleAccess.spawnExtraParticle(ParticleTypes.SOUL_FIRE_FLAME,  .125f);
            Vector3f colorBright = new Color(0x64C9FD).asVectorF();
            Vector3f colorDark = new Color(0x3f74e8).asVectorF();
            if (random.nextFloat() < 1 / 32f)
                particleAccess.spawnExtraParticle((new DustParticleOptions(colorBright, 1)), .125f);
            if (random.nextFloat() < 1 / 32f)
                particleAccess.spawnExtraParticle((new DustParticleOptions(colorDark, 1)), .125f);
            if (random.nextFloat() < 1 / 48f)
                particleAccess.spawnExtraParticle(ParticleTypes.SMOKE, .125f);
            if (random.nextFloat() < 1 / 32f)
                particleAccess.spawnExtraParticle((new CubeParticleData(192, 122, 85, 0.075f, 10, true)), .125f);
            if (random.nextFloat() < 1 / 32f)
                particleAccess.spawnExtraParticle((new CubeParticleData(191, 82, 91, 0.1f, 10, true)), .125f);
        }

        @Override
        public void affectEntity(Entity entity, Level level) {
            if (level.isClientSide)
                return;

            if (entity instanceof Blaze blaze) {
                blaze.heal(4);
            }

            if (!entity.fireImmune()) {
                entity.setSecondsOnFire(10);
                entity.hurt(level.damageSources().lava(), 10);
            }

            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
                livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 1, false, false));
                livingEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false));
            }


            if (entity instanceof Blaze blaze && !(entity instanceof SeethingBlaze)) {
                int progress = blaze.getPersistentData()
                        .getInt("CreateSanding");
                if (progress < 100) {
                    if (progress % 10 == 0) {
                        level.playSound(null, entity.blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.NEUTRAL,
                                1.5f, 0.5f * progress / 100f);
                    }
                    blaze.getPersistentData()
                            .putInt("CreateSanding", progress + 1);
                    return;
                }

                level.playSound(null, entity.blockPosition(), SoundEvents.BLAZE_BURN,
                        SoundSource.NEUTRAL, 1.25f, 0.65f);

                SeethingBlaze seethingBlaze = DesiresEntityTypes.SEETHING_ABLAZE.create(level);
                CompoundTag serializeNBT = blaze.saveWithoutId(new CompoundTag());
                serializeNBT.remove("UUID");

                assert seethingBlaze != null;
                seethingBlaze.deserializeNBT(serializeNBT);
                seethingBlaze.setPos(blaze.getPosition(0));
                level.addFreshEntity(seethingBlaze);
                blaze.discard();
            }
        }
    }

    public static class FreezingType implements FanProcessingType {
        private static final FreezingRecipe.FreezingWrapper FREEZING_WRAPPER = new FreezingRecipe.FreezingWrapper();

        @Override
        public boolean isValidAt(Level level, BlockPos pos) {
            FluidState fluidState = level.getFluidState(pos);
            if (DesiresTags.AllFluidTags.FAN_PROCESSING_CATALYSTS_FREEZING.matches(fluidState)) {
                return true;
            }
            BlockState blockState = level.getBlockState(pos);
            return DesiresTags.AllBlockTags.FAN_PROCESSING_CATALYSTS_FREEZING.matches(blockState);
        }

        @Override
        public int getPriority() {
            return 691100;
        }

        @Override
        public boolean canProcess(ItemStack stack, Level level) {
            FREEZING_WRAPPER.setItem(0, stack);
            Optional<FreezingRecipe> recipe = DesiresRecipeTypes.FREEZING.find(FREEZING_WRAPPER, level);
            return recipe.isPresent();
        }

        @Override
        @Nullable
        public List<ItemStack> process(ItemStack stack, Level level) {
            FREEZING_WRAPPER.setItem(0, stack);
            Optional<FreezingRecipe> recipe = DesiresRecipeTypes.FREEZING.find(FREEZING_WRAPPER, level);
            return recipe.map(freezingRecipe -> RecipeApplier.applyRecipeOn(level, stack, freezingRecipe)).orElse(null);
        }

        @Override
        public void spawnProcessingParticles(Level level, Vec3 pos) {
            if (level.random.nextInt(8) != 0)
                return;
            Vector3f color = new Color(0xDDE8FF).asVectorF();
            level.addParticle(new DustParticleOptions(color, 1), pos.x + (level.random.nextFloat() - .5f) * .5f,
                    pos.y + .5f, pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
            level.addParticle(ParticleTypes.SNOWFLAKE, pos.x + (level.random.nextFloat() - .5f) * .5f, pos.y + .5f,
                    pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
        }

        @Override
        public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
            particleAccess.setColor(Color.mixColors(0xEEEEFF, 0xDDE8FF, random.nextFloat()));
            particleAccess.setAlpha(1f);
            if (random.nextFloat() < 1 / 128f)
                particleAccess.spawnExtraParticle(ParticleTypes.SNOWFLAKE, .125f);
            if (random.nextFloat() < 1 / 32f)
                particleAccess.spawnExtraParticle(ParticleTypes.POOF, .125f);
        }

        @Override
        public void affectEntity(Entity entity, Level level) {
            if (level.isClientSide) {

                if (entity instanceof Skeleton) {
                    Vec3 p = entity.getPosition(0);
                    Vec3 v = p.add(0, 0.5f, 0)
                            .add(VecHelper.offsetRandomly(Vec3.ZERO, level.random, 1)
                                    .multiply(1, 0.2f, 1)
                                    .normalize()
                                    .scale(1f));
                    level.addParticle(ParticleTypes.SNOWFLAKE, v.x, v.y, v.z, 0, 0.1f, 0);
                    if (level.random.nextInt(3) == 0)
                        level.addParticle(ParticleTypes.SNOWFLAKE, p.x, p.y + .5f, p.z,
                                (level.random.nextFloat() - .5f) * .5f, 0.1f, (level.random.nextFloat() - .5f) * .5f);
                }
                return;
            }

            if (entity instanceof EnderMan || entity.getType() == EntityType.BLAZE) {
                entity.hurt(level.damageSources().freeze(), 8);
            }

            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 7, false, false));
            }

            if (entity instanceof SnowGolem snowgolem) {
                snowgolem.heal(4);
            }

            if (entity instanceof Stray stray) {
                stray.heal(2);
            }

            if (entity.isOnFire()) {
                entity.clearFire();
                level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE,
                        SoundSource.NEUTRAL, 0.7F, 1.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.4F);
            }

            if (entity instanceof Skeleton skeleton) {
                int progress = skeleton.getPersistentData()
                        .getInt("CreateFreezing");
                if (progress < 50) {
                    if (progress % 10 == 0) {
                        level.playSound(null, entity.blockPosition(), SoundEvents.STRAY_AMBIENT, SoundSource.NEUTRAL,
                                1f, 1.5f * progress / 50f);
                    }
                    skeleton.getPersistentData()
                            .putInt("CreateFreezing", progress + 1);
                    return;
                }

                level.playSound(null, entity.blockPosition(), SoundEvents.SKELETON_CONVERTED_TO_STRAY,
                        SoundSource.NEUTRAL, 1.25f, 0.65f);

                Stray stray = EntityType.STRAY.create(level);
                CompoundTag serializeNBT = skeleton.saveWithoutId(new CompoundTag());
                serializeNBT.remove("UUID");

                assert stray != null;
                stray.deserializeNBT(serializeNBT);
                stray.setPos(skeleton.getPosition(0));
                level.addFreshEntity(stray);
                skeleton.discard();
            }
        }
    }

}
