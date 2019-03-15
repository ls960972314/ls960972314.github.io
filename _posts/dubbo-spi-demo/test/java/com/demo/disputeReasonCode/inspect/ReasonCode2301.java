package com.demo.disputeReasonCode.inspect;

import com.demo.Context;
import com.demo.disputeReasonCode.DisputeReasonCode;
import com.epcc.dubbo.result.Result;
import lombok.extern.slf4j.Slf4j;

/**
 * type2201
 *
 * @author lishun
 * @create 2019/3/9
 */
@Slf4j
public class ReasonCode2301 implements DisputeReasonCode {

    @Override
    public Result deal(Context context) {
        log.info("i'm in 2301");
        return null;
    }
}
