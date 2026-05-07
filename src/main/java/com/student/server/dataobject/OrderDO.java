package com.student.server.dataobject;

import com.student.server.model.Order;
import com.student.server.model.OrderStatus;
import com.student.server.model.Product;
import com.student.server.model.User;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.Date;

@Data
public class OrderDO {

    String id;

    String orderNumber;

    Long userId;

    User user;

    Product product;

    String productId;

    Double totalPrice;

    OrderStatus status;

    Date gmtCreated;

    Date gmtModified;

    public Order toModel() {
        Order order = new Order();
        BeanUtils.copyProperties(this, order);
        return order;
    }

}
