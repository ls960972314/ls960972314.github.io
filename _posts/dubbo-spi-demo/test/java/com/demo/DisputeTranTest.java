package com.demo;

import com.demo.disputeTran.DisputeTran;
import com.epcc.dubbo.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.List;

/**
 * dispute tran test
 *
 * @author lishun
 * @create 2019/3/10
 */
@Slf4j
public class DisputeTranTest {


    DisputeTran disputeTran = MyExtensionLoader.getExtensionLoader(DisputeTran.class).getAdaptiveExtension();

    @Test
    public void testSPI() {
        Context context = new Context();
        context.setDisputeType("submitTran");
        context.setDisputeReasonCode("reasonCode2201");
        disputeTran.process(context);
    }

    @Test
    public void testBuildInvokerChain() {
        Invoker last = new Invoker() {
            @Override
            public Result invoke(Context context) {
                log.info("i'm last invoker");

                log.info("package context info...");

                log.info("query ori tran info");

                log.info("do common param valid...");

                log.info("do proxy valid");

                disputeTran.process(context);

                return null;
            }
        };
        Context context = new Context();
        context.setDisputeType("submitTran");
        context.setDisputeReasonCode("reasonCode2301");

        last = buildInvokerChain(last);
        last.invoke(context);
    }


    public Invoker buildInvokerChain(Invoker invoker) {
        Invoker last = invoker;
        List<Filter> filters = MyExtensionLoader.getExtensionLoader(Filter.class).getActivateExtension("dispute");
        if (!filters.isEmpty()) {
            for (int i = filters.size() - 1; i >= 0; i--) {
                final Filter filter = filters.get(i);
                final Invoker next = last;

                last = new Invoker() {
                    @Override
                    public Result invoke(Context context) {
                        Result result = filter.invoke(next, context);
                        return filter.onResponse(result, context);
                    }

                };
            }
        }
        return last;
    }
}
