# Spring 自定义命名空间


## 说明

Spring在解析xml文件中的标签的时候会区分当前的标签是四种基本标签（import、alias、bean和beans）还是自定义标签，如果是自定义标签，则会按照自定义标签的逻辑解析当前的标签。

Spring框架从2.0版本开始,提供了基于Schema风格的Spring XML格式用来定义bean的扩展机制。引入Schema-based XML是为了对Traditional的XML配置形式进行简化。

通过Schema的定义，把一些原本需要通过几个bean的定义或者复杂的bean的组合定义的配置形式，用另外一种简单而可读的配置形式呈现出来。

Schema-based XML由三部分构成：

1. namespace —— 拥有很明确的逻辑分类
2. element —— 拥有很明确的过程语义
3. attributes —— 拥有很简明的配置选项

例如:

```xml
<mvc:annotation-driven />
```

这段配置想要表达的意思，就是在mvc的空间内实现Annotation驱动的配置方式。

其中，
* `mvc`表示配置的有效范围，
* `annotation-driven`则表达了一个动态的过程，实际的逻辑含义是：整个SpringMVC的实现是基于Annotation模式，请为我注册相关的行为模式。

## 自定义

下面将阐述一下怎么写自定义XML的bean definition解析和集成这个解析到Spring IOC容器中。

在后面的内容中我们将会提到一个重要的概念那就是bean definition.其实Spring中有一个重要的概念那就是bean.而BeanDefinition这个对象就是对应的标签解析后的对象。

利用下面几个简答的步骤可以创建新的xml配置扩展: 

* Authoring一个XML schema用来描述你的自定义element(s) 
* Coding一个自定义的NamespaceHandler实现(这是一个很简答的步骤,don’t worry) 
* Coding一个或者多个BeanDefinitionParse实现(这是最主要的) 
* Registeringr把注册上面的到Spring(这也是一个简答的步骤)


## 示例

我们需要创建一个XML扩展(自定义xml element)允许我们可以用一种简单的方式来配置SimpleDateFormat对象(在java.text包中)。最后我们可以定义一个SimpleDateFormat类型的bean definition如下:

```xml
    <myns:dateformat id="dateFormat"
                     pattern="yyyy-MM-dd HH:mm"
                     lenient="true"/>
```

### 1. 创建一个配置

在工程的resources中创建文件:

myns.xsd

```xml
<?xml version="1.0" encoding="UTF-8"?>
<xsd:schema xmlns="http://www.mycompany.com/schema/myns"
            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            xmlns:beans="http://www.springframework.org/schema/beans"
            targetNamespace="http://www.mycompany.com/schema/myns"
            elementFormDefault="qualified"
            attributeFormDefault="unqualified">

    <xsd:import namespace="http://www.springframework.org/schema/beans"/>

    <xsd:element name="dateformat">
        <xsd:complexType>
            <xsd:complexContent>
                <xsd:extension base="beans:identifiedType">
                    <xsd:attribute name="lenient" type="xsd:boolean"/>
                    <xsd:attribute name="pattern" type="xsd:string" use="required"/>
                </xsd:extension>
            </xsd:complexContent>
        </xsd:complexType>
    </xsd:element>
</xsd:schema>
```

上面的schema将会用来配置SimpleDateFormat对象。

直接在一个xml应用context文件使用 `<myns:dateformat />`。

```xml
<myns:dateformat id="dateFormat"
    pattern="yyyy-MM-dd HH:mm"
    lenient="true"/>
```
    
注意：上面的XML片段本质上和下面的XML片段意义一样。

```xml
<bean id="dateFormat" class="java.text.SimpleDateFormat">
    <constructor-arg value="yyyy-HH-dd HH:mm"/>
    <property name="lenient" value="true"/>
</bean>
```


### 2. 编写命名空间处理器

针对于上面的的schema，我们需要一个`NamespaceHandler`用来解析Spring遇到的所有这个特定的`namespace`配置文件中的所有`elements`.这个`NamespaceHandler`将会关心解析`myns:dateformat`元素。

这个NamespaceHandler接口相对简单，它包括三个重要的方法。

1. `init()` - 会在spring使用handler之前实例化NamespaceHandler
2. `BeanDefinition parse(Element, ParseContext)` - 当Spring遇到上面定义的top-level元素(也就是myms)将会被调用,这个方法能够注册bean definitions并且可以返回一个bean definition.
3. `BeanDefinitionHolder decorate(Node, BeanDefinitionHolder, ParserContext)` - 当spring遇到一个attribute或者嵌入到namespace中的元素中将会被调用。

MyNamespaceHandler.java

```java
package com.jeiker.namespace;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class MyNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        registerBeanDefinitionParser("dateformat", new SimpleDateFormatBeanDefinitionParser());
    }

}
```

### 3. 解析自定义的xml元素

BeanDefinitionParser的责任是解析定义schema在top-level的XML元素.在解析过程中，我们必须访问XML元素,因此我们可以解析我们自定义的XML内容.

SimpleDateFormatBeanDefinitionParser.java

```java
package com.jeiker.namespace;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

import java.text.SimpleDateFormat;

public class SimpleDateFormatBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected Class getBeanClass(Element element) {
        return SimpleDateFormat.class;
    }

    @Override
    protected void doParse(Element element, BeanDefinitionBuilder bean) {
        // this will never be null since the schema explicitly requires that a value be supplied
        String pattern = element.getAttribute("pattern");
        bean.addConstructorArgValue(pattern);

        // this however is an optional property
        String lenient = element.getAttribute("lenient");
        if (StringUtils.hasText(lenient)) {
            bean.addPropertyValue("lenient", Boolean.valueOf(lenient));
        }
    }

}
```


注意：

1. 我们使用Spring提供的AbstractSingleBeanDefinitionParser 来处理创建一个single的BeanDefinition的一些基本的工作。
2. 我们重写了AbstractSingleBeanDefinitionParser父类的doParse方法来实现我们自己创建single类型的BeanDefinition的逻辑。

### 4. 在Spring 中注册schema和handler

### 4.1 注册schema

在resources中创建

META-INF/spring.schemas

```
http\://www.mycompany.com/schema/myns/myns.xsd=xml/myns.xsd
```

### 4.2 注册handler

在resources中创建

META-INF/spring.handlers

```
http\://www.mycompany.com/schema/myns=com.jeiker.namespace.MyNamespaceHandler
```

### 5. 测试

### 5.1配置schema-beans.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:myns="http://www.mycompany.com/schema/myns"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.mycompany.com/schema/myns http://www.mycompany.com/schema/myns/myns.xsd">

    <myns:dateformat id="dateFormat"
                     pattern="yyyy-MM-dd HH:mm"
                     lenient="true"/>

</beans>
```

### 5.2测试类


```java
package com.jeiker.namespace;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.text.SimpleDateFormat;
import java.util.Date;

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
```


