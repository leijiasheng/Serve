package com.student.server.service;

import com.student.server.model.Result;
import com.student.server.model.User;
import com.student.server.param.PageParam;

import java.util.List;

public interface UserService {

    public Result<User> register(String userName, String password,
                                 String email, String studentNum);

    public Result<User> login(String studentNum, String password);

    public Result<User> updatePerMsg(String nickName,String studentNum, String email, String personSign);

    public Result<User> updatePwd(String oldPwd ,String newPwd, String studentNum);

    public Result<User> resetPwd(String studentNum, String email, String code);

    public Result<List<User>> findAll(PageParam param);

    public int countAll();

    public Result<User> deleteByStudentNum(String studentNum);

    public Result<List<User>> findByKeyWord(String keyWord, PageParam param);

    public int countByKeyWord(String keyWord);

    public Result<User> insertUser(User user);

    public Result<String> buildCode(String studentNum, String email);


}
