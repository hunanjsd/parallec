package com.aicoin.demo;

import com.aicoin.source.TargetHostSource;
import com.aicoin.source.impl.MysqlTargetHostSource;
import io.parallec.core.ParallecResponseHandler;
import io.parallec.core.ParallelClient;
import io.parallec.core.ResponseOnSingleTask;
import java.util.List;
import java.util.Map;

/**
 * @Author: Simo
 * @Description:
 * @Date: Create in 上午11:39 2018/7/26
 * @Modified By:
 */
public class mysqlDemo {
    public static void main(String []args){
        TargetHostSource hostSource = new MysqlTargetHostSource();
        List<String > host = hostSource.getTargetHosts();
        ParallelClient pc = new ParallelClient();
        long startExecuteTime = System.currentTimeMillis();
        pc.prepareHttpGet("")
                .setConcurrency(1000)
                .setTargetHostsFromList(host)
                .execute(new ParallecResponseHandler() {

                    @Override
                    public void onCompleted(ResponseOnSingleTask res,
                                            Map<String, Object> responseContext) {
                        System.out.println(res);
                    }
                });
        System.out.println("execute all url cast time:"+(System.currentTimeMillis()-startExecuteTime));
        pc.releaseExternalResources();
    }
}
