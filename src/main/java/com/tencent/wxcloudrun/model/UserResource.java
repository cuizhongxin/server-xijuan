package com.tencent.wxcloudrun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户资源模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResource {
    
    private String id;
    private String odUserId;
    
    // 货币
    private Long gold;          // 黄金（充值获得）
    private Long silver;        // 白银（游戏内获得）
    private Long diamond;       // 钻石（充值获得）
    
    // 等级
    private Integer level;             // 君主等级
    
    // 体力和令牌
    private Integer stamina;           // 体力
    private Integer maxStamina;        // 最大体力
    private Integer generalOrder;      // 将令
    private Integer maxGeneralOrder;   // 最大将令
    private Integer tigerTally;        // 虎符（扫荡用）
    
    // 材料资源
    private Long wood;          // 木材
    private Long metal;         // 金属
    private Long food;          // 粮食
    private Long paper;         // 纸张（学习兵法用）
    
    // 强化石（1-6级）
    private Integer enhanceStone1;  // 1级强化石
    private Integer enhanceStone2;  // 2级强化石
    private Integer enhanceStone3;  // 3级强化石
    private Integer enhanceStone4;  // 4级强化石
    private Integer enhanceStone5;  // 5级强化石
    private Integer enhanceStone6;  // 6级强化石
    
    // 强化符（用于转移强化）
    private Integer enhanceScrollBasic;     // 初级强化符
    private Integer enhanceScrollMedium;    // 中级强化符
    private Integer enhanceScrollAdvanced;  // 高级强化符
    
    // 合成符（用于合成高级强化石）
    private Integer mergeScroll;            // 合成符
    
    // 品质石（用于提升装备品质）
    private Integer qualityStone;           // 品质石
    
    // 训练用粮食
    private Integer basicFood;              // 初级粮食
    private Integer advancedFood;           // 中级粮食
    private Integer premiumFood;            // 高级粮食
    
    // 招贤令
    private Integer normalRecruitToken;    // 普通招贤令
    private Integer advancedRecruitToken;  // 高级招贤令
    private Integer juniorToken;           // 初级招贤令
    private Integer intermediateToken;     // 中级招贤令
    private Integer seniorToken;           // 高级招贤令
    
    // 每日领取
    private String lastClaimDate;          // 上次领取日期
    private Integer dailyTokenClaimed;     // 今日已领取次数
    
    // 爵位和声望
    private String rank;        // 爵位
    private Long fame;          // 声望
    
    // 武将容量
    private Integer generalCount;       // 当前武将数
    private Integer baseGeneralSlots;   // 基础武将位（10个）
    private Integer purchasedSlots;     // 购买的武将位
    private Integer vipBonusSlots;      // VIP奖励武将位
    private Integer maxGeneral;         // 最大武将数（计算得出：基础+购买+VIP，上限50）
    
    // VIP
    private Integer vipLevel;       // VIP等级
    private Long vipExp;            // VIP经验
    
    // 充值相关
    private Long totalRecharge;     // 累计充值（分）
    
    // 时间戳
    private Long createTime;
    private Long updateTime;
    private Long lastStaminaRecoverTime;  // 上次体力恢复时间
    
    /**
     * 创建默认资源
     */
    public static UserResource createDefault(String odUserId) {
        return UserResource.builder()
                .id("res_" + System.currentTimeMillis())
                .odUserId(odUserId)
                .gold(0L)              // 初始1000黄金
                .silver(10000L)           // 初始10000白银
                .diamond(100L)            // 初始100钻石
                .level(1)                 // 初始1级
                .stamina(100)             // 初始100体力
                .maxStamina(100)
                .generalOrder(10)         // 初始10将令
                .maxGeneralOrder(10)
                .tigerTally(10)           // 初始10虎符
                .wood(500L)
                .metal(500L)
                .food(1000L)
                .paper(500L)
                .enhanceStone1(0)        // 初始20个1级强化石
                .enhanceStone2(0)        // 初始10个2级强化石
                .enhanceStone3(0)         // 初始5个3级强化石
                .enhanceStone4(0)
                .enhanceStone5(0)
                .enhanceStone6(0)
                .enhanceScrollBasic(5)    // 初始5个初级强化符
                .enhanceScrollMedium(2)   // 初始2个中级强化符
                .enhanceScrollAdvanced(0) // 初始0个高级强化符
                .mergeScroll(10)          // 初始10个合成符
                .qualityStone(0)          // 初始0个品质石
                .basicFood(50)            // 初始50个初级粮食
                .advancedFood(20)         // 初始20个中级粮食
                .premiumFood(5)           // 初始5个高级粮食
                .normalRecruitToken(10)   // 初始10普通招贤令
                .advancedRecruitToken(1)  // 初始1高级招贤令
                .juniorToken(10)          // 初始10初级招贤令
                .intermediateToken(0)     // 初始0中级招贤令
                .seniorToken(0)           // 初始0高级招贤令
                .lastClaimDate("")
                .dailyTokenClaimed(0)
                .rank("白身")
                .fame(0L)
                .generalCount(0)
                .baseGeneralSlots(10)     // 基础10个武将位
                .purchasedSlots(0)        // 初始没购买
                .vipBonusSlots(0)         // 初始没有VIP奖励
                .maxGeneral(10)           // 初始最大10个
                .vipLevel(0)
                .vipExp(0L)
                .totalRecharge(0L)
                .createTime(System.currentTimeMillis())
                .updateTime(System.currentTimeMillis())
                .lastStaminaRecoverTime(System.currentTimeMillis())
                .build();
    }
}
