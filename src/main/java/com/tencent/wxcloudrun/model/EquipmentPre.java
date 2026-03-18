package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentPre {

    private Integer id;           // APK装备ID (如22001)
    private String name;
    private String description;
    private Integer color;        // 品质: 2=绿, 3=蓝, 4=紫, 5=橙
    private Integer type;         // 部位: 1=武器, 2=戒指, 3=项链, 4=铠甲, 5=头盔, 6=靴子
    private String position;      // 部位中文名
    private Integer needLevel;    // 穿戴所需等级
    private Integer maxLevel;     // 最大强化等级
    private Integer suitId;       // 套装ID (0=散件)
    private String suitName;      // 套装名称
    private Integer basePrice;    // 基础售价(白银)
    private String iconUrl;       // 图片文件名
    private Integer genAtt;       // 武将攻击
    private Integer genDef;       // 武将防御
    private Integer genFor;       // 武勇
    private Integer genLeader;    // 统御
    private Integer armyLife;     // 军队生命
    private Integer armyAtt;      // 军队攻击
    private Integer armyDef;      // 军队防御
    private Integer armySp;       // 军队速度/机动
    private Integer armyHit;      // 命中
    private Integer armyMis;      // 闪避

    /**
     * 系统已对齐 APK 部位映射: 1=武器, 2=戒指, 3=项链, 4=铠甲, 5=头盔, 6=靴子
     */
    public Integer getSlotTypeId() {
        return type != null ? type : 1;
    }

    /**
     * 根据 color 推断品质ID
     */
    public Integer getDefaultQualityId() {
        if (color == null) return 2;
        return color;
    }

    // ---- 兼容旧代码的 getter ----

    public Integer getLevel() {
        return needLevel;
    }

    public Integer getAttack() {
        return genAtt;
    }

    public Integer getDefense() {
        return genDef;
    }

    public Integer getSoldierHp() {
        return armyLife;
    }

    public Integer getMobility() {
        return armySp;
    }

    public String getSetName() {
        return suitName;
    }

    public String getSetEffect3() {
        return null;
    }

    public String getSetEffect6() {
        return null;
    }
}
