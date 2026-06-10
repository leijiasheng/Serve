package com.student.server.control;

import com.student.server.cache.UserCache;
import com.student.server.dataobject.UserDO;
import com.student.server.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "在线统计", description = "实时在线人数统计接口")
public class OnlineController {

    private static final String ONLINE_USERS_KEY = "online:users";

    private final StringRedisTemplate stringRedisTemplate;
    private final UserCache userCache;

    @GetMapping("/online/count")
    @Operation(summary = "获取在线人数")
    public Result<Long> getOnlineCount() {
        Long count = stringRedisTemplate.opsForZSet().zCard(ONLINE_USERS_KEY);
        Result<Long> result = new Result<>();
        result.setSuccess(true);
        result.setData(count != null ? count : 0);
        result.setMessage("当前在线人数：" + (count != null ? count : 0));
        result.setCode("200");
        return result;
    }

    /**
     *  OnlineStats.vue  每 5 秒执行 GET /online/stats
     * @return
     */
    @GetMapping("/online/stats")
    @Operation(summary = "获取在线统计数据（在线人数 + 总注册用户数）")
    public Result<Map<String, Object>> getOnlineStats() {
        Long onlineCount = stringRedisTemplate.opsForZSet().zCard(ONLINE_USERS_KEY);
        int totalUsers = userCache.getTotalUsers();

        Map<String, Object> stats = new HashMap<>();
        stats.put("onlineCount", onlineCount != null ? onlineCount : 0);
        stats.put("totalUsers", totalUsers);

        Result<Map<String, Object>> result = new Result<>();
        result.setSuccess(true);
        result.setData(stats);
        result.setCode("200");
        return result;
    }

    /**
     * OnlineStats.vue  每 5 秒执行 GET /online/list
     * @return
     */
    @GetMapping("/online/list")
    @Operation(summary = "获取在线用户列表（含昵称，从数据库查询）")
    public Result<List<Map<String, Object>>> getOnlineUsers() {

        Set<String> userIds = stringRedisTemplate.opsForZSet().reverseRange(ONLINE_USERS_KEY, 0, 499);
        List<Map<String, Object>> list = new ArrayList<>();

        if (userIds != null && !userIds.isEmpty()) {
            List<Long> ids = userIds.stream().map(Long::parseLong).toList();
            List<UserDO> userDOS = userCache.getUsers(ids);
            for (UserDO userDO : userDOS) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", userDO.getId());
                item.put("nickName", userDO.getNickName());
                item.put("studentNum", userDO.getStudentNum());
                list.add(item);
            }
        }
        Result<List<Map<String, Object>>> result = new Result<>();
        result.setSuccess(true);
        result.setData(list);
        result.setCode("200");
        return result;
    }
}
