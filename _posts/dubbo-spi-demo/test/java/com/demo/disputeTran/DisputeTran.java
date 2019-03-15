package com.demo.disputeTran;

import com.alibaba.dubbo.common.extension.SPI;
import com.demo.Context;
import com.epcc.dubbo.result.Result;

/**
 * disputeTran
 *
 * @author lishun
 * @create 2019/3/9
 */
@SPI
public interface DisputeTran {

    /**
     *
     * @param context
     * @return
     */
    public Result process(Context context);

}
