package com.demo.disputeReasonCode;

import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;
import com.demo.Context;
import com.epcc.dubbo.result.Result;

/**
 * dispute reasonCode
 *
 * @author lishun
 * @create 2019/3/10
 */
@SPI("reasonCode2201")
public interface DisputeReasonCode {

    @Adaptive
    Result deal(Context context);
}