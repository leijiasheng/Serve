package com.student.server.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class UserInfo implements Serializable {

    long id;

    String studentNum;

    String nickName;

    String email;

    String personSign;

    String avatar;

}
