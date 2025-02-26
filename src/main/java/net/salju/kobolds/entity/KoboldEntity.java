package net.salju.kobolds.entity;

import net.salju.kobolds.init.KoboldsModSounds;
import net.salju.kobolds.init.KoboldsModEntities;
import net.salju.kobolds.KoboldsMod;

import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.event.ForgeEventFactory;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

public class KoboldEntity extends AbstractKoboldEntity {
	public KoboldEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(KoboldsModEntities.KOBOLD.get(), world);
	}

	public KoboldEntity(EntityType<KoboldEntity> type, Level world) {
		super(type, world);
		this.setPersistenceRequired();
	}

	public static void init() {
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new KoboldEntity.KoboldTradeGoal(this));
		this.goalSelector.addGoal(1, new KoboldEntity.KoboldWarriorGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
	}

	class KoboldTradeGoal extends Goal {
		public final AbstractKoboldEntity kobold;

		public KoboldTradeGoal(AbstractKoboldEntity kobold) {
			this.kobold = kobold;
		}

		@Override
		public boolean canUse() {
			return (checkHand() && !(this.kobold.hasEffect(MobEffects.MOVEMENT_SPEED)));
		}

		@Override
		public void start() {
			this.kobold.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, -10, (false), (false)));
			KoboldsMod.queueServerWork(100, () -> {
				this.kobold.swing(InteractionHand.MAIN_HAND, true);
				this.kobold.playSound(KoboldsModSounds.KOBOLD_TRADE.get(), 1.0F, 1.0F);
				LevelAccessor world = this.kobold.level;
				double x = this.kobold.getX();
				double y = this.kobold.getY();
				double z = this.kobold.getZ();
				if (world instanceof ServerLevel lvl) {
					lvl.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, lvl, 4, "", Component.literal(""), lvl.getServer(), null).withSuppressedOutput(),
							"/loot spawn ~ ~ ~ loot kobolds:gameplay/trader_loot");
				}
				KoboldsMod.queueServerWork(20, () -> {
					this.kobold.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
				});
			});
		}

		protected boolean checkHand() {
			return (this.kobold.getOffhandItem().getItem() == (Items.EMERALD));
		}
	}

	class KoboldWarriorGoal extends Goal {
		public final AbstractKoboldEntity kobold;

		public KoboldWarriorGoal(AbstractKoboldEntity kobold) {
			this.kobold = kobold;
		}

		@Override
		public boolean canUse() {
			return (checkHand() && !(this.kobold.hasEffect(MobEffects.MOVEMENT_SPEED)));
		}

		@Override
		public void start() {
			this.kobold.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, -10, (false), (false)));
			KoboldsMod.queueServerWork(600, () -> {
				ItemStack weapon = this.kobold.getMainHandItem();
				ItemStack off = this.kobold.getOffhandItem();
				LevelAccessor world = this.kobold.level;
				double x = this.kobold.getX();
				double y = this.kobold.getY();
				double z = this.kobold.getZ();
				if (world instanceof ServerLevel lvl) {
					ItemEntity drop = new ItemEntity(lvl, x, y, z, weapon);
					drop.setPickUpDelay(10);
					world.addFreshEntity(drop);
					this.kobold.setItemSlot(EquipmentSlot.MAINHAND, off);
					this.kobold.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
					this.kobold.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
					KoboldWarriorEntity war = this.kobold.convertTo(KoboldsModEntities.KOBOLD_WARRIOR.get(), true);
					ForgeEventFactory.onLivingConvert(this.kobold, war);
				}
			});
		}

		protected boolean checkHand() {
			return (this.kobold.getOffhandItem().getItem() instanceof AxeItem);
		}
	}
}