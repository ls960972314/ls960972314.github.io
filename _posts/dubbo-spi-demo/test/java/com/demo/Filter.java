package com.demo;

import com.alibaba.dubbo.common.extension.SPI;
import com.epcc.dubbo.result.Result;

/**
 * filter
 *
 * @author lishun
 * @create 2019/3/10
 */
@SPI
public interface Filter {

    Result invoke(Invoker invoker, Context context);

    default Result onResponse(Result result, Context invocation) {
        return result;
    }

}
