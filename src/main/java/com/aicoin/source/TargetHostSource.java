package com.aicoin.source;

import java.util.List;

/**
 * @Author: Simo
 * @Description: 定义一个target host,目前可以实现的是kafka source,mysql source, redis source
 * 可以实现从这三种数据源获取target host
 * @Date: Create in 上午10:36 2018/7/26
 * @Modified By:
 */
public interface TargetHostSource {
    /**
     * 返回target hosts
     * @return
     */
     List<String > getTargetHosts();



}
