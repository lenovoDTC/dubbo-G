package com.alibaba.dubbo.container;

/**
 * Created by lzg on 2017/3/27.
 */
import java.util.ArrayList;
import java.util.List;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

public class Http {
    private static List<String> providers = new ArrayList<String>();
    private static final Logger logger = LoggerFactory.getLogger(Http.class);
    public static void main(String[] args) {
        Container container = ExtensionLoader.getExtensionLoader(Container.class).getExtension("spring");
        container.start();
        synchronized (Http.class) {
            while (true) {
                try {
                    Http.class.wait();
                } catch (Throwable e) {
                }
            }
        }
    }
}