package com.gmail.virustotalop.pwt.nms;

import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.Items;

public class CustomRangedCrossbowAttackGoal<T extends Monster & RangedAttackMob & CrossbowAttackMob> {

    private final T mob;

    public CustomRangedCrossbowAttackGoal(T actor) {
        this.mob = actor;
    }

    private boolean isHoldingCrossbow() {
        return this.mob.isHolding(Items.CROSSBOW);
    }
}