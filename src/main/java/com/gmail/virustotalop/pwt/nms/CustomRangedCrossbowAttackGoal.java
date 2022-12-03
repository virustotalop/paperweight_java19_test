package com.gmail.virustotalop.pwt.nms;

import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

/*
Copy of RangedCrossbowAttackGoal with a configurable delay,
we don't extend the class since there is so much code that
we need to override
*/

public class CustomRangedCrossbowAttackGoal<T extends Monster & RangedAttackMob & CrossbowAttackMob> extends Goal {

    public static final UniformInt PATHFINDING_DELAY_RANGE = TimeUtil.rangeOfSeconds(1, 2);
    private final T mob;
    private CrossbowState crossbowState = CrossbowState.UNCHARGED;
    private final double speedModifier;
    private final float attackRadiusSqr;
    private final int initialAttackDelay;
    private int seeTime;
    private int attackDelay;
    private int updatePathDelay;

    public CustomRangedCrossbowAttackGoal(T actor, double speed, float range, int initialAttackDelay) {
        this.mob = actor;
        this.speedModifier = speed;
        this.attackRadiusSqr = range * range;
        this.initialAttackDelay = initialAttackDelay;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.isValidTarget() && this.isHoldingCrossbow();
    }

    private boolean isHoldingCrossbow() {
        return this.mob.isHolding(Items.CROSSBOW);
    }

    @Override
    public boolean canContinueToUse() {
        return this.isValidTarget() && (this.canUse() || !this.mob.getNavigation().isDone()) && this.isHoldingCrossbow();
    }

    private boolean isValidTarget() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.mob.setTarget(null);
        this.seeTime = 0;
        if (this.mob.isUsingItem()) {
            this.mob.stopUsingItem();
            this.mob.setChargingCrossbow(false);
            CrossbowItem.setCharged(this.mob.getUseItem(), false);
        }

    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity != null) {
            boolean bl = this.mob.getSensing().hasLineOfSight(livingEntity);
            boolean bl2 = this.seeTime > 0;
            if (bl != bl2) {
                this.seeTime = 0;
            }

            if (bl) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }

            double d = this.mob.distanceToSqr(livingEntity);
            boolean bl3 = (d > (double) this.attackRadiusSqr || this.seeTime < 5) && this.attackDelay == 0;
            if (bl3) {
                --this.updatePathDelay;
                if (this.updatePathDelay <= 0) {
                    this.mob.getNavigation().moveTo(livingEntity, this.canRun() ? this.speedModifier : this.speedModifier * 0.5D);
                    this.updatePathDelay = PATHFINDING_DELAY_RANGE.sample(this.mob.getRandom());
                }
            } else {
                this.updatePathDelay = 0;
                this.mob.getNavigation().stop();
            }

            this.mob.getLookControl().setLookAt(livingEntity, 30.0F, 30.0F);
            if (this.crossbowState == CrossbowState.UNCHARGED) {
                if (!bl3) {
                    this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, Items.CROSSBOW));
                    this.crossbowState = CrossbowState.CHARGING;
                    this.mob.setChargingCrossbow(true);
                }
            } else if (this.crossbowState == CrossbowState.CHARGING) {
                if (!this.mob.isUsingItem()) {
                    this.crossbowState = CrossbowState.UNCHARGED;
                }

                int i = this.mob.getTicksUsingItem();
                ItemStack itemStack = this.mob.getUseItem();
                if (i >= CrossbowItem.getChargeDuration(itemStack)) {
                    this.mob.releaseUsingItem();
                    this.crossbowState = CrossbowState.CHARGED;
                    this.attackDelay = this.initialAttackDelay;
                    this.mob.setChargingCrossbow(false);
                }
            } else if (this.crossbowState == CrossbowState.CHARGED) {
                --this.attackDelay;
                if (this.attackDelay == 0) {
                    this.crossbowState = CrossbowState.READY_TO_ATTACK;
                }
            } else if (this.crossbowState == CrossbowState.READY_TO_ATTACK && bl) {
                this.mob.performRangedAttack(livingEntity, 1.0F);
                ItemStack itemStack2 = this.mob.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this.mob, Items.CROSSBOW));
                CrossbowItem.setCharged(itemStack2, false);
                this.crossbowState = CrossbowState.UNCHARGED;
            }

        }
    }

    private boolean canRun() {
        return this.crossbowState == CrossbowState.UNCHARGED;
    }

    enum CrossbowState {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK
    }
}