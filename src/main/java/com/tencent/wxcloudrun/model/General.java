package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import java.util.List;
import java.util.Map;

/**
 * 武将实体类（打平存储）
 * 基础六维属性：攻击/防御/武勇/统御/闪避/机动
 * 装备槽位：武器/铠甲/项链/戒指/鞋子/头盔
 * 兵法：单槽，只能装备一个
 * 兵种：步/骑/弓
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class General {
    
    private String id;              // 武将实例ID
    private String userId;          // 所属用户ID
    private String templateId;      // 关联武将模板ID
    private String name;            // 武将名称
    private Integer level;          // 当前等级
    private Long exp;               // 当前经验值
    private Long maxExp;            // 升级所需经验
    private String avatar;          // 头像资源路径
    private String faction;         // 阵营：魏/蜀/吴/群/虚构
    
    // ====== 品质（前缀 quality_） ======
    private Integer qualityId;              // 品质ID：1白2绿3蓝4紫5橙
    private String qualityName;             // 品质名称
    private String qualityColor;            // 品质颜色代码
    @Builder.Default
    private Double qualityBaseMultiplier = 1.0;  // 品质属性倍率
    @Builder.Default
    private Integer qualityStar = 1;        // 星级1-5
    private String qualityIcon;             // 品质图标
    
    // ====== 兵种（步/骑/弓） ======
    private String troopType;               // 兵种：步/骑/弓
    
    // ====== 六维属性（前缀 attr_） ======
    @Builder.Default
    private Integer attrAttack = 0;         // 攻击
    @Builder.Default
    private Integer attrDefense = 0;        // 防御
    @Builder.Default
    private Integer attrValor = 0;          // 武勇
    @Builder.Default
    private Integer attrCommand = 0;        // 统御
    @Builder.Default
    private Double attrDodge = 0.0;         // 闪避
    @Builder.Default
    private Integer attrMobility = 0;       // 机动
    
    // ====== 士兵 ======
    @Builder.Default
    private Integer soldierRank = 1;        // 兵种等级
    @Builder.Default
    private Integer soldierCount = 100;     // 当前士兵数
    @Builder.Default
    private Integer soldierMaxCount = 100;  // 士兵上限
    
    // ====== 装备槽（6个） ======
    private String equipWeaponId;           // 武器装备ID
    private String equipArmorId;            // 铠甲装备ID
    private String equipNecklaceId;         // 项链装备ID
    private String equipRingId;             // 戒指装备ID
    private String equipShoesId;            // 鞋子装备ID
    private String equipHelmetId;           // 头盔装备ID
    
    // ====== 兵法（单槽） ======
    private String tacticsId;               // 已装备的兵法ID
    
    // ====== 状态（前缀 status_） ======
    @Builder.Default
    private Boolean statusLocked = false;
    @Builder.Default
    private Boolean statusInBattle = false;
    @Builder.Default
    private Boolean statusInjured = false;
    @Builder.Default
    private Integer statusMorale = 100;
    
    // ====== 战斗统计（前缀 stat_） ======
    @Builder.Default
    private Integer statTotalBattles = 0;
    @Builder.Default
    private Integer statVictories = 0;
    @Builder.Default
    private Integer statDefeats = 0;
    @Builder.Default
    private Integer statKills = 0;
    @Builder.Default
    private Integer statMvpCount = 0;
    
    // ====== 特征列表（JSON存储） ======
    private List<String> traits;
    
    // ====== 装备加成缓存（JSON存储） ======
    private Map<String, Integer> equipmentBonus;
    
    private Long createTime;
    private Long updateTime;
    
    // ====== JSON 辅助（MyBatis列映射） ======
    
    public String getTraitsJson() {
        return traits != null ? JSON.toJSONString(traits) : null;
    }
    
    public void setTraitsJson(String json) {
        if (json != null && !json.isEmpty()) {
            this.traits = JSON.parseArray(json, String.class);
        }
    }
    
    public String getEquipmentBonusJson() {
        return equipmentBonus != null ? JSON.toJSONString(equipmentBonus) : null;
    }
    
    public void setEquipmentBonusJson(String json) {
        if (json != null && !json.isEmpty()) {
            this.equipmentBonus = JSON.parseObject(json, new TypeReference<Map<String, Integer>>(){});
        }
    }
}
