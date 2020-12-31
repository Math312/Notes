# 目录

本系列文章主要对Mybatis持久化框架进行介绍。该框架已经广泛应用于国内开发，国外的开发人员还是更喜欢JPA。

接下来，我们将从如下几方面按顺序对Mybatis框架进行介绍。

首先，学习一个框架需要知道他的功能，以及简单使用，否则直接解析源码无异于空中楼阁，因此第一部分将参考[Mybatis的文档](https://mybatis.org/mybatis-3/zh/index.html)对Mybatis的基本功能进行介绍。

[1.mybatis快速开始](./1.mybatis快速开始/1.mybatis快速开始.md)

了解了基本功能后，我们就会对这个框架如何实现的这些基础功能产生了兴趣。紧接着，笔者会对实现第一节中基础功能的实现进行源码解析。

[2.SqlSessionFactory的创建](./2.SqlSessionFactory的创建/2.SqlSessionFactory的创建.md)

而SqlSessionFactory的创建离不开Mybatis的配置解析，这也是SqlSessionFactory创建的重要一步，因此，笔者在此对一些比较重要的配置解析模块进行分析，如果你不了解这部分功能也没有关系，笔者会整合Mybatis的文档，对这部分功能的使用进行简单介绍，帮助理解：

[2-1.类型别名功能的解析](../mybatis/2.SqlSessionFactory的创建/2-1.typeAlias的解析.md)
