package com.jeiker.namespace;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Description: spring-boot-namespace
 * User: jeikerxiao
 * Date: 2019/7/17 10:19 AM
 */
public class SchemaBeanDefinitionTest {


    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("schema-beans.xml");
        SimpleDateFormat dateFormat = context.getBean("dateFormat", SimpleDateFormat.class);
        System.out.println("-------------------gain object--------------------");
        System.out.println(dateFormat);
        String dateStr = dateFormat.format(new Date());
        System.out.println(dateStr);
    }
}
