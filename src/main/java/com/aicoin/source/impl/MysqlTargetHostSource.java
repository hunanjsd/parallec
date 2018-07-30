package com.aicoin.source.impl;

import com.aicoin.config.MysqlConfig;
import com.aicoin.source.TargetHostSource;
import com.aicoin.util.MysqlUtil;

import java.util.List;

/**
 * @Author: Simo
 * @Description:
 * @Date: Create in 上午10:40 2018/7/26
 * @Modified By:
 */
public class MysqlTargetHostSource implements TargetHostSource {

    public void init(){

    }

    @Override
    public List<String > getTargetHosts(){
        String sql = "SELECT url FROM coin_config  ";
        String colmunName = "url";
        return MysqlUtil.getInstance().query(sql,colmunName);
    }


}
