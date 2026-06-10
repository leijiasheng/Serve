package com.student.server.control;

import com.student.server.annotation.RateLimit;
import com.student.server.dao.ProductDAO;
import com.student.server.dataobject.ProductDO;
import com.student.server.model.Product;
import com.student.server.model.Result;
import com.student.server.model.UserInfo;
import com.student.server.redisKeys.RedisConstant;
import com.student.server.service.ProductService;
import com.student.server.service.SnappedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Tag(name = "商品抢购", description = "商品列表查询和秒杀抢购接口")
public class SnappedUpController {

    private final SnappedService snappedService;

    private final ProductService productService;

    /**
     * 获取所有商品列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取商品列表", description = "获取所有可抢购的商品列表")
    public Result<List<Product>> list() {

        return productService.getAll();
    }

    /**
     * 抢购
     * @param productId
     * @param request
     * @return
     */
    @GetMapping("/snappedUp")
    @Operation(summary = "抢购商品", description = "根据商品ID进行秒杀抢购（需登录）")
    public Result<Boolean> snappedUp(@Parameter(description = "商品ID") @RequestParam("productId") long productId,
                                     HttpServletRequest request) {

        UserInfo userInfo =(UserInfo) request.getAttribute("currentUser");
        long userId = userInfo.getId();

        return snappedService.snappedUp(productId, userId);
    }

}
