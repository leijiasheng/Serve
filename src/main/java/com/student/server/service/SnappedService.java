package com.student.server.service;

import com.student.server.model.Product;
import com.student.server.model.Result;

public interface SnappedService {

    public Result<Boolean> snappedUp(long productId, long userId);

    public void doSnapBusiness(long userId, long productId, Double price);

}
