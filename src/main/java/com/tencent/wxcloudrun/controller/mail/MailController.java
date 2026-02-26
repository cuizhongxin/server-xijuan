package com.tencent.wxcloudrun.controller.mail;

import com.tencent.wxcloudrun.dao.GameServerMapper;
import com.tencent.wxcloudrun.dto.ApiResponse;
import com.tencent.wxcloudrun.service.mail.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mail")
public class MailController {

    @Autowired
    private MailService mailService;

    @Autowired
    private GameServerMapper gameServerMapper;

    /** 收件箱 */
    @GetMapping("/inbox")
    public ApiResponse<Map<String, Object>> inbox(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        return ApiResponse.success(mailService.getInbox(userId));
    }

    /** 读取邮件 */
    @GetMapping("/read")
    public ApiResponse<Map<String, Object>> read(@RequestParam long mailId, HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        return ApiResponse.success(mailService.readMail(userId, mailId));
    }

    /** 领取附件 */
    @PostMapping("/claim")
    public ApiResponse<Map<String, Object>> claim(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        long mailId = Long.parseLong(String.valueOf(body.get("mailId")));
        return ApiResponse.success(mailService.claimAttachment(userId, mailId));
    }

    /** 发送玩家邮件 */
    @PostMapping("/send")
    public ApiResponse<String> send(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String senderName = getLordName(request, userId);
        String receiverId = body.get("receiverId");
        String title = body.get("title");
        String content = body.get("content");
        mailService.sendPlayerMail(userId, senderName, receiverId, title, content);
        return ApiResponse.success("发送成功");
    }

    /** 删除邮件 */
    @PostMapping("/delete")
    public ApiResponse<String> delete(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        long mailId = Long.parseLong(String.valueOf(body.get("mailId")));
        mailService.deleteMail(userId, mailId);
        return ApiResponse.success("删除成功");
    }

    /** 未读数量 */
    @GetMapping("/unread")
    public ApiResponse<Integer> unread(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        return ApiResponse.success(mailService.getUnreadCount(userId));
    }

    private String getLordName(HttpServletRequest request, String compositeUserId) {
        Object rawId = request.getAttribute("rawUserId");
        String userId = rawId != null ? String.valueOf(rawId) : compositeUserId;
        
        Object sidObj = request.getAttribute("serverId");
        if (sidObj != null) {
            try {
                int serverId = Integer.parseInt(String.valueOf(sidObj));
                Map<String, Object> ps = gameServerMapper.findPlayerServer(userId, serverId);
                if (ps != null && ps.get("lordName") != null) {
                    return (String) ps.get("lordName");
                }
            } catch (Exception ignored) {}
        }
        List<Map<String, Object>> servers = gameServerMapper.findPlayerServers(userId);
        if (servers != null && !servers.isEmpty()) {
            Object name = servers.get(0).get("lordName");
            if (name != null) return (String) name;
        }
        return "玩家";
    }
}
