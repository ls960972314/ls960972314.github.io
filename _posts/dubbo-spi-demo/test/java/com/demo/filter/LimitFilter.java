package com.demo.filter;

import com.alibaba.dubbo.common.extension.Activate;
import com.demo.Context;
import com.demo.Filter;
import com.demo.Invoker;
import com.epcc.dubbo.result.Result;
import lombok.extern.slf4j.Slf4j;

/**
 * log filter
 *
 * @author lishun
 * @create 2019/3/10
 */
@Slf4j
@Activate(group = "dispute", order = 1000)
public class LimitFilter implements Filter {

    @Override
    public Result invoke(Invoker invoker, Context context) {
        log.info("i'm limit filter, order is 1000");
        return invoker.invoke(context);
    }
}
