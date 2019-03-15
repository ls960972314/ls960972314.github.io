package com.demo.disputeReasonCode;

import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.demo.Context;
import com.epcc.dubbo.result.Result;

/**
 * adaptiveTran
 *
 * @author lishun
 * @create 2019/3/9
 */
@Adaptive
public class AdaptiveDisputeReasonCode implements DisputeReasonCode {

    @Override
    public Result deal(Context context) {
        DisputeReasonCode disputeReasonCode;
        ExtensionLoader<DisputeReasonCode> loader = ExtensionLoader.getExtensionLoader(DisputeReasonCode.class);
        if (context.getDisputeReasonCode() != null && context.getDisputeReasonCode() != "") {
            disputeReasonCode = loader.getExtension(context.getDisputeReasonCode());
        } else {
            disputeReasonCode = loader.getDefaultExtension();
        }
        return disputeReasonCode.deal(context);
    }
}
