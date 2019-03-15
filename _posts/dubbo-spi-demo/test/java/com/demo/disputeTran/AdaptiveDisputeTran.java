package com.demo.disputeTran;

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
public class AdaptiveDisputeTran implements DisputeTran {

    @Override
    public Result process(Context context) {
        DisputeTran disputeTran;
        ExtensionLoader<DisputeTran> loader = ExtensionLoader.getExtensionLoader(DisputeTran.class);
        if (context.getDisputeType() != null && context.getDisputeType() != "") {
            disputeTran = loader.getExtension(context.getDisputeType());
        } else {
            disputeTran = loader.getDefaultExtension();
        }
        return disputeTran.process(context);
    }
}
