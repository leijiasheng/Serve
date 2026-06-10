package com.student.server.service.impl;

import com.student.server.dao.UserDAO;
import com.student.server.dataobject.UserDO;
import com.student.server.model.Result;
import com.student.server.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.DigestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl.login() 纯单元测试
 *
 * <p>只加载 Service 和 Mock 依赖，不启动 Spring 容器，执行最快。</p>
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplLoginTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private RedisTemplate redisTemplate;

    @Mock
    private ValueOperations valueOperations;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // lenient() 避免参数为 null 的测试用例因未调用 Redis 而报 UnnecessaryStubbingException
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== 登录成功 ====================

    @Test
    @DisplayName("登录成功 — 密码正确应返回 code=200 和用户数据")
    void loginSuccess() {
        String studentNum = "2021001";
        String rawPassword = "123456";

        String saltPwd = rawPassword + "ljs_zwy";
        String expectedMd5 = DigestUtils.md5DigestAsHex(saltPwd.getBytes()).toUpperCase();

        UserDO mockUserDO = new UserDO();
        mockUserDO.setId(1);
        mockUserDO.setStudentNum(studentNum);
        mockUserDO.setPassword(expectedMd5);
        mockUserDO.setUserName("张三");

        when(valueOperations.get(studentNum)).thenReturn(mockUserDO);

        Result<User> result = userService.login(studentNum, rawPassword);

        assertTrue(result.isSuccess());
        assertEquals("200", result.getCode());
        assertEquals("登录成功", result.getMessage());
        assertNotNull(result.getData());
        assertEquals("张三", result.getData().getUserName());

        verify(valueOperations, times(1)).get(studentNum);
    }

    // ==================== 参数为空 ====================

    @Test
    @DisplayName("登录失败 — 学号为 null 应返回 code=600")
    void loginFailWhenStudentNumNull() {
        Result<User> result = userService.login(null, "123456");

        assertFalse(result.isSuccess());
        assertEquals("600", result.getCode());
        assertEquals("请正确输入内容", result.getMessage());

        // null 参数在方法开头就返回了，不会访问 Redis
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("登录失败 — 密码为 null 应返回 code=600")
    void loginFailWhenPasswordNull() {
        Result<User> result = userService.login("2021001", null);

        assertFalse(result.isSuccess());
        assertEquals("600", result.getCode());
    }

    // ==================== 用户不存在 ====================

    @Test
    @DisplayName("登录失败 — Redis 和 DB 都没有该用户应返回 code=601")
    void loginFailWhenUserNotFound() {
        String studentNum = "9999999";

        when(valueOperations.get(studentNum)).thenReturn(null);
        when(userDAO.findByStudentNum(studentNum)).thenReturn(null);

        Result<User> result = userService.login(studentNum, "123456");

        assertFalse(result.isSuccess());
        assertEquals("601", result.getCode());
        assertEquals("登录账户不存在", result.getMessage());

        // 验证：缓存了空对象防止缓存穿透（6 分钟短过期）
        verify(valueOperations, times(1))
                .set(eq(studentNum), any(UserDO.class), eq(6L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("登录失败 — 命中缓存的空对象应返回 code=601")
    void loginFailWhenHitEmptyObjectCache() {
        String studentNum = "9999999";

        UserDO emptyUserDO = new UserDO(); // id 默认 0，作为空对象标识
        when(valueOperations.get(studentNum)).thenReturn(emptyUserDO);

        Result<User> result = userService.login(studentNum, "123456");

        assertFalse(result.isSuccess());
        assertEquals("601", result.getCode());
    }

    // ==================== 密码错误 ====================

    @Test
    @DisplayName("登录失败 — 密码不匹配应返回 code=603")
    void loginFailWhenWrongPassword() {
        String studentNum = "2021001";

        UserDO mockUserDO = new UserDO();
        mockUserDO.setId(1);
        mockUserDO.setStudentNum(studentNum);
        mockUserDO.setPassword("CORRECT_MD5_HASH");

        when(valueOperations.get(studentNum)).thenReturn(mockUserDO);

        Result<User> result = userService.login(studentNum, "wrongPassword");

        assertFalse(result.isSuccess());
        assertEquals("603", result.getCode());
        assertEquals("登录密码错误", result.getMessage());
    }

    // ==================== 缓存穿透防护 ====================

    @Test
    @DisplayName("缓存穿透防护 — DB 为空时缓存空对象并设置短过期时间（6分钟）")
    void cacheEmptyObjectOnDbMiss() {
        String studentNum = "nonexistent";

        when(valueOperations.get(studentNum)).thenReturn(null);
        when(userDAO.findByStudentNum(studentNum)).thenReturn(null);

        userService.login(studentNum, "123456");

        // 空对象缓存过期时间是 6 分钟（非 6 小时）
        verify(valueOperations, times(1))
                .set(eq(studentNum), any(UserDO.class), eq(6L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("DB 命中后回写 Redis 缓存 6 小时")
    void cacheFullObjectOnDbHit() {
        String studentNum = "2021001";
        String rawPassword = "123456";
        String saltPwd = rawPassword + "ljs_zwy";
        String md5Pwd = DigestUtils.md5DigestAsHex(saltPwd.getBytes()).toUpperCase();

        UserDO dbUserDO = new UserDO();
        dbUserDO.setId(1);
        dbUserDO.setStudentNum(studentNum);
        dbUserDO.setPassword(md5Pwd);

        when(valueOperations.get(studentNum)).thenReturn(null);
        when(userDAO.findByStudentNum(studentNum)).thenReturn(dbUserDO);

        userService.login(studentNum, rawPassword);

        // DB 命中的缓存应该 6 小时
        verify(valueOperations, times(1))
                .set(eq(studentNum), eq(dbUserDO), eq(6L), eq(TimeUnit.HOURS));
    }
}
