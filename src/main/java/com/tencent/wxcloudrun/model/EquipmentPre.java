package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 装备模板（equipment_pre 表映射）
 * 定义所有可获取装备的基础属性模板
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentPre {

    private Integer id;
    private String name;
    private Integer level;
    private String source;       // 副本掉落/副本产出/手工制作/秘境产出/天地宝箱
    private String position;     // 武器/戒指/铠甲/项链/头盔/鞋子
    private String setName;      // 套装名
    private String setEffect3;   // 3件套效果
    private String setEffect6;   // 6件套效果
    private Integer attack;
    private Integer defense;
    private Integer soldierHp;
    private Integer mobility;

    /**
     * position → slotTypeId 映射
     */
    public Integer getSlotTypeId() {
        if (position == null) return 1;
        switch (position) {
            case "武器": return 1;
            case "头盔": return 2;
            case "铠甲": return 3;
            case "戒指": return 4;
            case "鞋子": return 5;
            case "项链": return 6;
            default: return 1;
        }
    }

    /**
     * 根据等级推断默认品质ID
     */
    public Integer getDefaultQualityId() {
        if (level == null) return 1;
        if (level <= 1) return 1;
        if (level <= 20) return 2;
        if (level <= 40) return 3;
        if (level <= 60) return 4;
        if (level <= 80) return 5;
        return 5;
    }
}
