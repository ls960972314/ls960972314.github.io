package com.demo.filter;

import com.alibaba.dubbo.common.extension.Activate;
import com.epcc.dubbo.result.Result;
import com.epcc.risk.api.biz.base.spi.Context;
import com.epcc.risk.api.biz.base.spi.Filter;
import com.epcc.risk.api.biz.base.spi.Invoker;
import lombok.extern.slf4j.Slf4j;

/**
 * log filter
 *
 * @author lishun
 * @create 2019/3/10
 */
@Slf4j
@Activate(group = "dispute", order = 1001)
public class TokenFilter implements Filter {

    @Override
    public Result invoke(Invoker invoker, Context context) {
        log.info("i'm token filter, order is 1001");
        return invoker.invoke(context);
    }
}
