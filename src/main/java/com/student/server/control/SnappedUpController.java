package com.student.server.control;

import com.student.server.model.Result;
import com.student.server.model.UserInfo;
import com.student.server.service.SnappedService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/product")
public class SnappedUpController {

    @Autowired
    private SnappedService snappedService;

    /**
     * 抢购
     * @param productId
     * @param request
     * @return
     */
    @GetMapping("/snappedUp")
    public Result<Boolean> snappedUp(@RequestParam("productId") long productId,
                                     HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        UserInfo userInfo =(UserInfo) session.getAttribute("user");
        long userId = userInfo.getId();

        return snappedService.snappedUp(productId, userId);
    }

}
