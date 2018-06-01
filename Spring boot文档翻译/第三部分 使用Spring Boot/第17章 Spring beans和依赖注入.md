## 17.Spring beans和依赖注入

你仍可以使用Spring 框架的任何技术去定义你的bean并进行依赖注入。可以使用`@Component`注解去发现你的bean，并且使用`@Autowired`进行依赖注入。

如果你构建你按照上面的建议（将应用启动类放在根包中）构建你的代码，那么你可以在上面毫无疑问地添加`@ComponentScan`注解。你的应用程序中所有的组件都会被找到（`@Component`、`@Service`、`@Repository`、`@Controller`等）都会被自动注册为一个bean。

这里有一个简单的`@Service`bean的例子，它使用构造器注入一个`RiskAssessor`bean。

    package com.example.service;

    import org.springframework.beans.annotation.Autowired;
    import org.springframework.stereotype.Service;

    @Service
    public class DatabaseAccountService implements AccountService
    {
        private final RiskAssessor riskAssessor;

        @Autowired
        public DatabaseAccountService(RiskAssessor riskAssessor)
        {
            this.riskAssessor = riskAssessor;
        }

        //...
    }

如果一个bean只有一个构造器，你可以省略`@Autowired`。

    @Service
    public class DatabaseAccountService implements AccountService
    {
        private final RiskAssessor riskAssessor;

        public DatabaseAccountService(RiskAssessor riskAssessor)
        {
            this.riskAssessor = riskAssessor;
        }
        //...
    }
