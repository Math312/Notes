 1. public 修饰符在 Groovy 中不是必须的。

 2. Groovy 支持范围类型，比如 `1..3` 相当于 Java 中的数组 `{ 1, 2, 3 }`；`1.2..3` 相当于 Java 中的数组 `{ 1.2, 2.2 }`。范围类型是可迭代的。

 3. Groovy 中的数组表示为 `[ v1, v2 ]`，用中括号括起。

 4. Groovy 中的字典可表示为 `[ k1: v1, k2: v2 ]`，键和值的类型都可以随意，可以迭代键值对。

 5. Groovy 支持类型自动推断，使用关键字 `def`，比如 `def var = value;`。

 6. Groovy 的方法的返回值可以指定为某一类型，也可以指定为 `def`。

 7. Groovy 的方法支持默认参数。

 8. 一个 Groovy 脚本就是一个类，定义在脚本中的类可以理解为内部类，直接定义在脚本中的方法（或者说是函数）可以当成脚本类的成员方法。

 9. 字符串中 `$xxx` 可以直接引用变量。

 10. Groovy 可以使用 Java I/O 的所有类，除此之外还有 I/O 操作的快捷方式：

  * 对文件进行逐行处理：

    ```
    // java.io.File
    new File(filepath).eachLine {
        line -> processLine(line);
    };
    ```
  * 获取文件的整个文本：
    ```    
    // java.io.File
    new File(filepath).text
    ```
        
