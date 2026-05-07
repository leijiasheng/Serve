package com.student.server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class Order {

    String id;

    String orderNumber;

    Long userId;

    User user;

    Product product;

    String productId;

    Double totalPrice;

    OrderStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    Date gmtCreated;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    Date gmtModified;
}
