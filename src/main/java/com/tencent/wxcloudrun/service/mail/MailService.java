package com.tencent.wxcloudrun.service.mail;

import com.tencent.wxcloudrun.dao.MailMapper;
import com.tencent.wxcloudrun.exception.BusinessException;
import com.tencent.wxcloudrun.model.Warehouse;
import com.tencent.wxcloudrun.service.UserResourceService;
import com.tencent.wxcloudrun.service.warehouse.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MailService {

    @Autowired private MailMapper mailMapper;
    @Autowired private WarehouseService warehouseService;
    @Autowired private UserResourceService userResourceService;

    /**
     * 获取收件箱
     */
    public Map<String, Object> getInbox(String userId) {
        List<Map<String, Object>> mails = mailMapper.findByReceiver(userId);
        int unread = mailMapper.countUnread(userId);

        // 为每封有附件的邮件加载附件信息
        for (Map<String, Object> mail : mails) {
            Object hasAtt = mail.get("has_attachment");
            if (hasAtt != null && (Integer.parseInt(String.valueOf(hasAtt)) == 1)) {
                long mailId = Long.parseLong(String.valueOf(mail.get("id")));
                mail.put("attachments", mailMapper.findAttachments(mailId));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unread", unread);
        result.put("mails", mails);
        return result;
    }

    /**
     * 读取邮件
     */
    public Map<String, Object> readMail(String userId, long mailId) {
        Map<String, Object> mail = mailMapper.findById(mailId);
        if (mail == null) throw new BusinessException("邮件不存在");
        if (!userId.equals(String.valueOf(mail.get("receiver_id")))) {
            throw new BusinessException("无权查看此邮件");
        }
        mailMapper.markRead(mailId);
        mail.put("is_read", 1);

        Object hasAtt = mail.get("has_attachment");
        if (hasAtt != null && Integer.parseInt(String.valueOf(hasAtt)) == 1) {
            mail.put("attachments", mailMapper.findAttachments(mailId));
        }
        return mail;
    }

    /**
     * 领取附件
     */
    public Map<String, Object> claimAttachment(String userId, long mailId) {
        Map<String, Object> mail = mailMapper.findById(mailId);
        if (mail == null) throw new BusinessException("邮件不存在");
        if (!userId.equals(String.valueOf(mail.get("receiver_id")))) {
            throw new BusinessException("无权操作此邮件");
        }
        if (Integer.parseInt(String.valueOf(mail.get("has_attachment"))) != 1) {
            throw new BusinessException("此邮件没有附件");
        }
        if (Integer.parseInt(String.valueOf(mail.get("attachment_claimed"))) == 1) {
            throw new BusinessException("附件已领取");
        }

        List<Map<String, Object>> attachments = mailMapper.findAttachments(mailId);
        List<String> rewards = new ArrayList<>();

        for (Map<String, Object> att : attachments) {
            String itemType = String.valueOf(att.get("item_type"));
            String itemName = String.valueOf(att.get("item_name"));
            int count = Integer.parseInt(String.valueOf(att.get("count")));

            switch (itemType) {
                case "silver":
                    userResourceService.addSilver(userId, count);
                    break;
                case "gold":
                    userResourceService.addGold(userId, count);
                    break;
                case "food":
                    userResourceService.addFood(userId, count);
                    break;
                case "wood":
                    userResourceService.addWood(userId, count);
                    break;
                case "paper":
                    userResourceService.addPaper(userId, count);
                    break;
                default:
                    // item / equipment -> 放入仓库
                    String itemId = String.valueOf(att.get("item_id"));
                    String quality = String.valueOf(att.get("item_quality"));
                    Warehouse.WarehouseItem item = Warehouse.WarehouseItem.builder()
                            .itemId(itemId).itemType(itemType).name(itemName)
                            .quality(quality).count(count).maxStack(9999)
                            .usable(!"equipment".equals(itemType)).build();
                    warehouseService.addItem(userId, item);
                    break;
            }
            rewards.add(itemName + " x" + count);
        }

        mailMapper.markAttachmentClaimed(mailId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mailId", mailId);
        result.put("rewards", rewards);
        return result;
    }

    /**
     * 发送玩家邮件
     */
    public void sendPlayerMail(String senderId, String senderName,
                                String receiverId, String title, String content) {
        if (senderId.equals(receiverId)) throw new BusinessException("不能给自己发邮件");
        if (title == null || title.trim().isEmpty()) throw new BusinessException("标题不能为空");
        if (title.length() > 50) throw new BusinessException("标题最多50字");
        if (content != null && content.length() > 500) throw new BusinessException("内容最多500字");

        Map<String, Object> mail = new LinkedHashMap<>();
        mail.put("senderId", senderId);
        mail.put("senderName", senderName);
        mail.put("receiverId", receiverId);
        mail.put("mailType", "player");
        mail.put("title", title);
        mail.put("content", content != null ? content : "");
        mail.put("hasAttachment", 0);
        mail.put("createTime", System.currentTimeMillis());
        mail.put("expireTime", 0L);
        mailMapper.insertMail(mail);
    }

    /**
     * 系统发送带附件的邮件（供后端内部调用）
     */
    public void sendSystemMail(String receiverId, String title, String content,
                                List<Map<String, Object>> attachments) {
        Map<String, Object> mail = new LinkedHashMap<>();
        mail.put("senderId", "system");
        mail.put("senderName", "系统");
        mail.put("receiverId", receiverId);
        mail.put("mailType", "system");
        mail.put("title", title);
        mail.put("content", content != null ? content : "");
        mail.put("hasAttachment", (attachments != null && !attachments.isEmpty()) ? 1 : 0);
        mail.put("createTime", System.currentTimeMillis());
        mail.put("expireTime", 0L);
        mailMapper.insertMail(mail);

        long mailId = Long.parseLong(String.valueOf(mail.get("id")));

        if (attachments != null) {
            for (Map<String, Object> att : attachments) {
                att.put("mailId", mailId);
                mailMapper.insertAttachment(att);
            }
        }
    }

    /**
     * 删除邮件
     */
    public void deleteMail(String userId, long mailId) {
        int rows = mailMapper.deleteMail(mailId, userId);
        if (rows == 0) throw new BusinessException("邮件不存在或无权删除");
    }

    /**
     * 未读数量
     */
    public int getUnreadCount(String userId) {
        return mailMapper.countUnread(userId);
    }
}
