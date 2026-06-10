package com.student.server.service;

import com.student.server.model.Product;
import com.student.server.model.Result;

import java.util.List;

public interface ProductService {

    Result<List<Product>> getAll();

}
