package com.jeiker.namespace;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Description: spring-boot-namespace
 * User: jeikerxiao
 * Date: 2019/7/17 10:20 AM
 */
public class MyNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        registerBeanDefinitionParser("dateformat", new SimpleDateFormatBeanDefinitionParser());
    }

}