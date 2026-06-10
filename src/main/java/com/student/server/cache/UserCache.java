package com.student.server.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.student.server.dao.UserDAO;
import com.student.server.dataobject.UserDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class UserCache {

    private final Cache<Long, UserDO> userCache;
    private final Cache<String, Integer> countCache;
    private final UserDAO userDAO;

    public UserCache(UserDAO userDAO) {
        this.userDAO = userDAO;
        this.userCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
        this.countCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    }

    public UserDO getUser(Long userId) {
        return userCache.get(userId, id -> {
            log.debug("UserCache 未命中，查库 userId={}", id);
            return userDAO.selectByUserId(id);
        });
    }

    /**
     * 批量获取用户，未命中时一次 IN 查询批量加载，避免循环逐条查库
     */
    public List<UserDO> getUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return new ArrayList<>();

        Map<Long, UserDO> all = userCache.getAll(userIds, ids -> {
            log.debug("UserCache 批量未命中 {} 个用户，一次查库", ids.size());
            List<Long> idList = new ArrayList<>(ids);
            List<UserDO> userDOSs = userDAO.selectByUserIds(idList);
            return userDOSs.stream().collect(Collectors.toMap(UserDO::getId, Function.identity()));
        });

        // 按传入顺序返回（Caffeine 的 getAll 不保证顺序）
        List<UserDO> result = new ArrayList<>(userIds.size());
        for (Long id : userIds) {
            UserDO userDO = all.get(id);
            if (userDO != null) {
                result.add(userDO);
            }
        }
        return result;
    }

    public int getTotalUsers() {
        return countCache.get("total", key -> {
            log.debug("总用户数缓存未命中，查库");
            return userDAO.countAll();
        });
    }

    //修改用户信息，头像，删除用户时 该用户缓存失效，保证在线列表展示最新的用户信息
    public void evict(Long userId) {
        userCache.invalidate(userId);
    }

    //现在在注册和删除时失效总人数缓存
    public void evictTotalUsers() {
        countCache.invalidate("total");
    }
}
