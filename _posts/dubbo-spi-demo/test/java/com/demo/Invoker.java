package com.demo;

import com.alibaba.dubbo.rpc.RpcException;
import com.epcc.dubbo.result.Result;

/**
 * invoker
 *
 * @author lishun
 * @create 2019/3/10
 */
public interface Invoker {

    Result invoke(Context context);

}
