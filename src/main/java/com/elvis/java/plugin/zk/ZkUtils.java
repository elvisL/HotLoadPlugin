package com.elvis.java.plugin.zk;

import com.alibaba.fastjson.JSON;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.ArrayList;
import java.util.List;


public class ZkUtils {

    public static CuratorFramework getClient() {
        return CuratorFrameworkFactory.builder()
                .connectString("???")  //修改成自己公司对应的//xx-soazk1.workxx.cn:888
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .connectionTimeoutMs(10 * 1000) //连接超时时间，默认15秒
                .sessionTimeoutMs(60 * 1000) //会话超时时间，默认60秒
                .build();
    }

    public static List<ServiceNode> getServiceNodes(String appName) {
        try {
            CuratorFramework client = getClient();
            client.start();
            List<ServiceNode> serviceNodes = new ArrayList<>();
            //需要修改成公司对应的  如:/soav1/" + appName + "/pro/providerList
            String path = "???";
            List<String> providerList = client.getChildren().forPath(path);
            for (String provider : providerList) {
                String data = new String(client.getData().forPath(path + provider));
                serviceNodes.add(JSON.parseObject(data, ServiceNode.class));
            }
            client.close();
            return serviceNodes;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }


}
