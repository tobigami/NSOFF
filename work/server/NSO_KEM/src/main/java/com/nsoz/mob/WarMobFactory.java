/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.mob;

import com.nsoz.constants.MobName;
import com.nsoz.map.War;
import com.nsoz.map.zones.Zone;

/**
 * @author kitakeyos - Hoàng Hữu Dũng
 */
public class WarMobFactory extends MobFactory {

    public WarMobFactory(Zone zone) {
        super(zone);
    }

    @Override
    public Mob createMonster(int id, MobPosition mob) {
        MobTemplate template = MobManager.getInstance().find(mob.getId());
        int hp = template.hp;
        short level = template.level;
        switch (zone.map.war.type) {
            case War.TYPE_LEVEL_50_TO_70:
                hp = 500000;
                level = 55;
                if (template.id == MobName.BACH_LONG_TRU || template.id == MobName.HAC_LONG_TRU) {
                    hp = 20000000;
                    level = 99;
                }
                break;

            case War.TYPE_LEVEL_71_TO_89:
                hp = 1000000;
                level = 75;
                if (template.id == MobName.BACH_LONG_TRU || template.id == MobName.HAC_LONG_TRU) {
                    hp = 40000000;
                    level = 99;
                }
                break;

            case War.TYPE_LEVEL_90_TO_110:
                hp = 1500000;
                level = 95;
                if (template.id == MobName.BACH_LONG_TRU || template.id == MobName.HAC_LONG_TRU) {
                    hp = 60000000;
                    level = 99;
                }
                break;
            case War.TYPE_LEVEL_111_TO_130:
                hp = 2000000;
                level = 110;
                if (template.id == MobName.BACH_LONG_TRU || template.id == MobName.HAC_LONG_TRU) {
                    hp = 100000000;
                    level = 99;
                }
                break;
        }
        Mob monster = new Mob(id, mob.getId(), hp, level, mob.getX(), mob.getY(), mob.isBeast() && zone.id % 5 == 0, template.isBoss(), zone);
        return monster;
    }

}
