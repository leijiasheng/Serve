package com.student.server.model;

import com.student.server.param.PageParam;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;

@Setter
@Getter
public class Result<T> extends PageParam implements Serializable{

    String message;

    String code;

    boolean isSuccess;

    T data;

}
