package com.yupi.yupao.utils;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Random;

/**
 * 连接 腾讯云 cos 工具类
 *
 * @author Alonso
 */
public class TencentCOSUtils {

    // 存储桶名称
    private static final String bucketName = "friend-1314004726";

    private static final String secretId = "111";

    private static final String secretKey = "111";

    // 用户图片文件夹
    private static final String userImg = "image/friendUser";

    //队伍图片文件夹
    private static final String teamImg = "image/friendTeam";

    // 访问域名
    public static final String URL = "https://friend-1314004726.cos.ap-guangzhou.myqcloud.com/";

    // 创建 cos 凭证
    private static COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);

    // 配置成存储桶 bucket 的实际地域
    private static ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));


    /**
     * 图片上传并返回对象地址
     *
     * @param file
     * @param n
     * @return
     */
    public static String uploadFile(MultipartFile file, int n) {
        // 生成 cos 客户端
        COSClient cosClient = new COSClient(credentials, clientConfig);

        //生成文件原始名称
        String fileName = file.getOriginalFilename();

        try {
            String substring = fileName.substring(fileName.lastIndexOf("."));

            File localFile = File.createTempFile(String.valueOf(System.currentTimeMillis()), substring);

            file.transferTo(localFile);

            Random random = new Random();
            //如果 n 是 0，图片就放入用户图片的地址，是 1，就放入队伍图片的地址
            if (n == 0) {
                fileName = userImg + random.nextInt(10000) + System.currentTimeMillis() + substring;
            } else {
                fileName = teamImg + random.nextInt(10000) + System.currentTimeMillis() + substring;
            }
            // 将 图片上传至 cos
            PutObjectRequest objectRequest = new PutObjectRequest(bucketName, fileName, localFile);
            cosClient.putObject(objectRequest);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cosClient.shutdown();
        }
        return URL + fileName;
    }
}

