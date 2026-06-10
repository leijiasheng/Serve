package com.student.server.service.impl;

import com.student.server.dao.ProductDAO;
import com.student.server.dataobject.ProductDO;
import com.student.server.model.Product;
import com.student.server.model.Result;
import com.student.server.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductDAO productDAO;

    @Override
    public Result<List<Product>> getAll() {
        Result<List<Product>> result = new Result<>();
        result.setSuccess(true);

        List<ProductDO> productDOList = productDAO.getAll();
        List<Product> products = productDOList.stream().map(ProductDO::toModel).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(products)) {
            result.setSuccess(false);
            result.setMessage("暂无商品");
            return result;
        }

        result.setMessage("查询商品列表成功");
        result.setData(products);
        return result;
    }
}
