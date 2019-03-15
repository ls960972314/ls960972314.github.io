package com.demo.disputeTran;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.demo.Context;
import com.demo.disputeReasonCode.DisputeReasonCode;
import com.epcc.dubbo.result.Result;
import lombok.extern.slf4j.Slf4j;

/**
 * submit tran
 *
 * @author lishun
 * @create 2019/3/9
 */
@Slf4j
public class SubmitTran implements DisputeTran {

    DisputeReasonCode disputeReasonCode = ExtensionLoader.getExtensionLoader(DisputeReasonCode.class).getAdaptiveExtension();

    @Override
    public Result process(Context context) {
        log.info("deal submit");
        disputeReasonCode.deal(context);
        log.info("insert db");
        return null;
    }
}
