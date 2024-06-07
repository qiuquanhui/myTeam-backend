package com.hui.myteam.service;

import com.hui.myteam.model.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.*;

@SpringBootTest
public class InsertUserTest {

    @Resource
    private UserService userService;

    private ExecutorService executorService = new ThreadPoolExecutor(40, 1000, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));

    /**
     * 批量查询
     */
    @Test
    public void insertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 100000;
        ArrayList<User> userList = new ArrayList<>();
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
            user.setUsername("假数据");
            user.setUserAccount("xxAccount");
            user.setProfile("xxxxx");
            user.setAvatarUrl("https://imgsrc.baidu.com/forum/pic/item/9c82d158ccbf6c81a7c49b78be3eb13532fa40aa.jpg");
            user.setGender(0);
            user.setUserPassword("123456");
            user.setPhone("123456");
            user.setEmail("123@qq.com");
            user.setUserRole(0);
            user.setTags("[]");
            userList.add(user);
        }
        userService.saveBatch(userList, 10000);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis()); // 17763
    }

    /**
     * 并发执行（使用的是 idea默认的线程 ）
     */
    @Test
    public void doDefaultConcurrencyInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        //分 20 组，每组 5000 条
        int batchSize = 5000;
        int j = 0;
        ArrayList<CompletableFuture<Void>> futureList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ArrayList<User> userList = new ArrayList<>();
            while (true){
                j++;
                User user = new User();
                user.setUsername("假数据");
                user.setUserAccount("xxAccount");
                user.setProfile("xxxxx");
                user.setAvatarUrl("https://imgsrc.baidu.com/forum/pic/item/9c82d158ccbf6c81a7c49b78be3eb13532fa40aa.jpg");
                user.setGender(0);
                user.setUserPassword("123456");
                user.setPhone("123456");
                user.setEmail("123@qq.com");
                user.setUserRole(0);
                user.setTags("[]");
                userList.add(user);
                if (j % batchSize == 0){
                    break;
                }
            }
            //异步执行
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                userService.saveBatch(userList, batchSize);
            });
            futureList.add(future);
        }

        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis()); //8748
    }

    /**
     * 并发执行（使用的是自定义的线程 executorService ）
     */
    @Test
    public void doConcurrencyInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        //分 20 组，每组 5000 条
        int batchSize = 5000;
        int j = 0;
        ArrayList<CompletableFuture<Void>> futureList = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            ArrayList<User> userList = new ArrayList<>();
            while (true){
                j++;
                User user = new User();
                user.setUsername("假数据");
                user.setUserAccount("xxAccount");
                user.setProfile("xxxxx");
                user.setAvatarUrl("https://imgsrc.baidu.com/forum/pic/item/9c82d158ccbf6c81a7c49b78be3eb13532fa40aa.jpg");
                user.setGender(0);
                user.setUserPassword("123456");
                user.setPhone("123456");
                user.setEmail("123@qq.com");
                user.setUserRole(0);
                user.setTags("[]");
                userList.add(user);
                if (j % batchSize == 0){
                    break;
                }
            }
            //异步执行
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                userService.saveBatch(userList, batchSize);
            }, executorService);

            futureList.add(future);
        }

        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();

        System.out.println(stopWatch.getTotalTimeMillis()); // 8354
    }

    @Test
    public void doConcurrentInsertUsers(){

        StopWatch stopWatch = new StopWatch(); //记录时间
        stopWatch.start(); //开始记录

        ArrayList<CompletableFuture> futures = new ArrayList<>(); //定义futures

        int batchSize = 5000; //每次加入的数量
        int j = 0;

        for (int i = 0; i < 20; i++) {
            ArrayList<User> users = new ArrayList<>();
            while (true){
                j++;
                User user = new User();
                user.setUsername("11");
                user.setUserAccount("1");
                user.setAvatarUrl("11");
                user.setGender(0);
                user.setUserPassword("1");
                user.setProfile("1");
                user.setPhone("1");
                user.setEmail("1");
                user.setTags("1");
                user.setUserStatus(0);
                user.setCreateTime(new Date());
                user.setUpdateTime(new Date());
                user.setIsDelete(0);
                user.setUserRole(0);
                users.add(user);
                if (j % batchSize == 0){
                    break;
                }
            }
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                userService.saveBatch(users, batchSize);
            },executorService);

            futures.add(future);
        }

        //异步执行
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{})).join();

        stopWatch.stop();

        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
