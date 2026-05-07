package com.student.server;

import com.student.server.service.SnappedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;

@SpringBootTest
class ServerApplicationTests {

    @Autowired
    private SnappedService snappedService;

    @Test
    public void testSnappedUp() throws InterruptedException {
        long productId = 1;
        // 模拟200个用户并发抢购
        int userCount = 2000;

        // 门闩：所有线程等待，一起开始
        CountDownLatch startLatch = new CountDownLatch(1);
        // 结束闩：等待所有线程执行完，测试才结束
        CountDownLatch endLatch = new CountDownLatch(userCount);

        for (int i = 1; i <= userCount; i++) {
            long userId = i;
            new Thread(() -> {
                try {
                    // 等待统一开始信号
                    startLatch.await();
                    snappedService.snappedUp(productId, userId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // 每个线程执行完，计数器-1
                    endLatch.countDown();
                }
            }).start();
        }

        Thread.sleep(1000);
        System.out.println("===== 开始并发抢购 =====");
        // 释放所有线程，并发执行
        startLatch.countDown();

        // 【关键】等待所有200个线程全部执行完再结束测试
        endLatch.await();
        System.out.println("===== 所有线程执行完毕 =====");
    }
}