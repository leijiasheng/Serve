package com.student.server.service.impl;

import com.alibaba.fastjson2.JSON;
import com.student.server.dao.UserDAO;
import com.student.server.dataobject.UserDO;
import com.student.server.email.EmailClient;
import com.student.server.kafkaTopics.Topics;
import com.student.server.model.Result;
import com.student.server.model.User;
import com.student.server.param.PageParam;
import com.student.server.redisKeys.RedisConstant;
import com.student.server.service.UserService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private EmailClient emailClient;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    /**
     * 用户注册
     * @param userName
     * @param password
     * @param email
     * @param studentNum
     * @return
     */
    @Override
    public Result<User> register(String userName, String password, String email, String studentNum) {
        Result<User> result = new Result<>();
        result.setSuccess(true);

        if (userName == null || password == null || email == null || studentNum == null) {
            result.setMessage("请正确输入内容，注册失败");
            result.setSuccess(false);
            result.setCode("600");
            return result;
        }

        UserDO userDO1 =(UserDO) redisTemplate.opsForValue().get(studentNum);

        if (userDO1 == null) {
            userDO1 = userDAO.findByStudentNum(studentNum);
        }

        if (userDO1 != null && userDO1.getId() > 0) {
            result.setMessage("该学号已注册，注册失败");
            result.setSuccess(false);
            result.setCode("602");
            return result;
        }

        UserDO userDO2 = new UserDO();
        userDO2.setUserName(userName);
        userDO2.setNickName(userName);
        userDO2.setEmail(email);
        userDO2.setStudentNum(studentNum);

        String saltPwd = password + "ljs_zwy";
        String md5Pwd = DigestUtils.md5DigestAsHex(saltPwd.getBytes()).toUpperCase();
        userDO2.setPassword(md5Pwd);

        int insertResult = userDAO.insert(userDO2);

        if (insertResult > 0) {
            result.setMessage("注册成功");
            result.setCode("200");
            result.setData(userDO2.toModel());
            redisTemplate.opsForValue().set(studentNum, userDO2, 6, TimeUnit.HOURS);
            return result;
        } else {
            result.setSuccess(false);
            result.setMessage("注册失败");
            return result;
        }

    }

    /**
     * 用户登录
     * @param studentNum
     * @param password
     * @return
     */
    @Override
    public Result<User> login(String studentNum, String password) {

        Result<User> result = new Result<>();
        result.setSuccess(true);

        // 1. 非空校验
        if (studentNum == null || password == null) {
            result.setMessage("请正确输入内容");
            result.setSuccess(false);
            result.setCode("600");
            return result;
        }

        // 2. 先从Redis拿
        UserDO userDO = (UserDO) redisTemplate.opsForValue().get(studentNum);

        // 3. Redis没有 → 查数据库
        if (userDO == null) {
            userDO = userDAO.findByStudentNum(studentNum); // 先查库！！！

            // 4. 只有数据库真没有，才缓存空对象
            if (userDO == null) {
                redisTemplate.opsForValue().set(studentNum, new UserDO(), 6, TimeUnit.MINUTES);
                result.setMessage("登录账户不存在");
                result.setSuccess(false);
                result.setCode("601");
                return result;
            }

            // 5. 查到了 → 再把完整数据存Redis（这里会带studentNum！！！）
            redisTemplate.opsForValue().set(studentNum, userDO, 6, TimeUnit.HOURS);
        }

        // 6. 判断空对象（防止null指针）
        if (userDO.getId() <= 0) {
            result.setMessage("登录账户不存在");
            result.setSuccess(false);
            result.setCode("601");
            return result;
        }

        // 7. 密码校验
        String saltPwd = password + "ljs_zwy";
        String md5Pwd = DigestUtils.md5DigestAsHex(saltPwd.getBytes()).toUpperCase();

        if (md5Pwd.equals(userDO.getPassword())) {
            result.setCode("200");
            result.setMessage("登录成功");
            result.setData(userDO.toModel());
            return result;
        } else {
            result.setMessage("登录密码错误");
            result.setSuccess(false);
            result.setCode("603");
            return result;
        }
    }

    /**
     * 更新个人信息
     * @param nickName
     * @param studentNum
     * @param email
     * @param personSign
     * @return
     */
    @Override
    public Result<User> updatePerMsg(String nickName,String studentNum, String email, String personSign) {

        Result<User> result = new Result<>();
        result.setSuccess(true);

        UserDO userDO =(UserDO) redisTemplate.opsForValue().get(studentNum);

        if (userDO == null) {
            userDO = userDAO.findByStudentNum(studentNum);
        }

        userDO.setNickName(nickName);
        userDO.setEmail(email);
        userDO.setPersonSign(personSign);

      int result1 = userDAO.updateByStudentNum(userDO);

        if (result1 > 0) {
            //修改个人信息后更新redis缓存
            redisTemplate.opsForValue().set(studentNum, userDO, 6, TimeUnit.HOURS);
            result.setCode("200");
            result.setMessage("更新个人信息成功");
            result.setData(userDO.toModel());
            return result;
        } else {
            result.setSuccess(false);
            result.setMessage("更新个人信息失败");
            return result;
        }
    }

    /**
     * 修改密码
     * @param oldPwd
     * @param newPwd
     * @param studentNum
     * @return
     */
    @Override
    public Result<User> updatePwd(String oldPwd, String newPwd, String studentNum) {

        Result<User> result = new Result<>();
        result.setSuccess(true);

        UserDO userDO =(UserDO) redisTemplate.opsForValue().get(studentNum);

        if (userDO == null) {
            userDO = userDAO.findByStudentNum(studentNum);
        }

        //旧密码校验
        String saltPwd = oldPwd + "ljs_zwy";
        String oldMd5Pwd = DigestUtils.md5DigestAsHex(saltPwd.getBytes()).toUpperCase();

        if (!oldMd5Pwd.equals(userDO.getPassword())) {
            result.setMessage("旧密码错误，修改密码失败");
            result.setSuccess(false);
            return result;
        }

        String newSaltPwd = newPwd + "ljs_zwy";
        String newMd5Pwd = DigestUtils.md5DigestAsHex(newSaltPwd.getBytes()).toUpperCase();

        int result1 = userDAO.updatePwd(newMd5Pwd, studentNum);
        userDO.setPassword(newMd5Pwd);

        if (result1 > 0) {
            result.setMessage("修改密码成功");
            result.setCode("200");
            result.setData(userDAO.findByStudentNum(studentNum).toModel());
            //跟新redis密码
            redisTemplate.opsForValue().set(studentNum, userDO, 6, TimeUnit.HOURS);
            return result;
        } else {
            result.setSuccess(false);
            result.setMessage("修改密码失败");
            return result;
        }
    }

    /**
     * 忘记/重置密码
     * @param studentNum
     * @param email
     * @return
     */
    @Override
    public Result<User> resetPwd(String studentNum, String email, String code) {

        Result<User> result = new Result<>();
        result.setSuccess(true);

        UserDO userDO = userDAO.findByStudentNum(studentNum);

        //校验验证码是否正确
        String emailCodeKey = RedisConstant.EMAIL_CODE_KEY_PREFIX + studentNum;
        String recCode =(String) redisTemplate.opsForValue().get(emailCodeKey);
        if (!code.equals(recCode)) {
            result.setSuccess(false);
            result.setMessage("输入验证码不正确，请稍后重试");
            return result;
        }

        String resetPwd = "123456";
        String saltPwd = resetPwd + "ljs_zwy";
        String resetMd5Pwd = DigestUtils.md5DigestAsHex(saltPwd.getBytes()).toUpperCase();

        if (resetMd5Pwd.equals(userDO.getPassword())) {
            result.setSuccess(false);
            result.setMessage("重置密码失败，密码已经为123456");
            return result;
        }

        int setResult = userDAO.updatePwd(resetMd5Pwd, studentNum);

        if (setResult > 0) {
            userDO.setPassword(resetMd5Pwd);
            result.setCode("200");
            result.setMessage("重置密码成功");
            redisTemplate.opsForValue().set(studentNum, userDO, 6, TimeUnit.HOURS);
            return result;
        } else {
            result.setSuccess(false);
            result.setMessage("重置密码失败");
            return result;
        }
    }

    /**
     * 查询所有用户
     * @param param
     * @return
     */
    @Override
    public Result<List<User>> findAll(PageParam param) {
        Result<List<User>> result = new Result<>();
        result.setSuccess(true);

        List<UserDO> userDOS = userDAO.findAll(param);

        if (!CollectionUtils.isEmpty(userDOS)) {
            List<User> users = userDOS.stream().map(UserDO::toModel).collect(Collectors.toList());
            result.setMessage("查找用户列表成功");
            result.setData(users);
            result.setCode("200");
        } else {
            result.setSuccess(false);
            result.setMessage("查找用户列表失败");
        }
        return result;
    }

    /**
     * 查询个数
     * @return
     */
    @Override
    public int countAll() {
        return userDAO.countAll();
    }

    /**
     * 删除用户
     * @param studentNum
     * @return
     */
    @Override
    public Result<User> deleteByStudentNum(String studentNum) {
        Result<User> result = new Result<>();
        result.setSuccess(true);

        UserDO userDO = userDAO.findByStudentNum(studentNum);

        if (userDO == null) {
            result.setMessage("该用户不存在，删除用户失败");
            result.setSuccess(false);
            return result;
        }

        int delResult = userDAO.deleteByStudentNum(studentNum);

        if (delResult > 0) {
            redisTemplate.delete(studentNum);
            result.setMessage("删除用户成功");
            result.setCode("200");
            result.setData(userDO.toModel());


            return result;
        } else {
            result.setMessage("删除用户失败");
            result.setSuccess(false);
            return result;
        }
    }

    /**
     * 搜索关键字查询
     * @param keyWord
     * @param param
     * @return
     */
    @Override
    public Result<List<User>> findByKeyWord(String keyWord, PageParam param) {
        Result<List<User>> result = new Result<>();
        result.setSuccess(true);

        if (keyWord == null) {
            result.setSuccess(false);
            result.setMessage("请输入关键词");
            return result;
        }

        List<UserDO> userDOs = userDAO.findByKey(keyWord, param);

        if (CollectionUtils.isEmpty(userDOs)) {
            result.setSuccess(false);
            result.setMessage("没有关键词用户");
            return result;
        }

        result.setMessage("查找关键词用户成功");
        result.setCode("200");

        List<User> users = userDOs.stream().map(UserDO::toModel).collect(Collectors.toList());
        result.setData(users);
        return result;
    }

    /**
     * 查询搜索结果个数
     * @param keyWord
     * @return
     */
    @Override
    public int countByKeyWord(String keyWord) {
        if (keyWord == null) {
            return -1;
        }

        int result = userDAO.countByKeyWord(keyWord);

        if (!(result <0)) {
            return result;
        }

        return -1;
    }

    @Override
    public Result<User> insertUser(User user) {
        Result<User> result = new Result<>();
        result.setSuccess(true);

        if (user == null) {
            result.setSuccess(false);
            result.setMessage("请输入用户");
            return result;
        }

        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(user, userDO);

        int insertRes = userDAO.insert(userDO);
        if (insertRes <= 0) {
            result.setSuccess(false);
            result.setMessage("插入用户失败");
            return result;
        }

        result.setMessage("插入用户成功");
        result.setData(user);
        return result;
    }

    /**
     * 发送验证码到redis和邮箱
     * @param studentNum
     * @param email
     * @return
     */
    @Override
    public Result<String> buildCode(String studentNum, String email) {
        Result<String> result1 = new Result<>();
        result1.setSuccess(true);

        Result<User> result = new Result<>();
        result.setSuccess(true);

        UserDO userDO = userDAO.findByStudentNum(studentNum);

        if (userDO == null) {
            result1.setSuccess(false);
            result1.setMessage("该用户不存在，重置密码失败");
            return result1;
        }

        if (!email.equals(userDO.getEmail())) {
            result1.setSuccess(false);
            result1.setMessage("邮箱错误，重置密码失败");
            return result1;
        }

        Random random = new Random();
        int code = random.nextInt(10000);
        String codeNum = String.format("%04d", code);
        result1.setData(codeNum);
        result1.setMessage("生成验证码成功");

        //验证码发送到redis，过期时间五分钟
        String emailCodeKey = RedisConstant.EMAIL_CODE_KEY_PREFIX + studentNum;
        redisTemplate.opsForValue().set(emailCodeKey, codeNum, RedisConstant.EMAIL_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);


        kafkaTemplate.send(Topics.KAFKA_CODE_TOPIC, email, codeNum);

        return result1;
    }
}
