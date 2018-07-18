# Count()和Max()的优化

## Max()优化

1. 示例1：

        explain select max(payment_date) from payment

explain 结果：

          id: 1
    select_type: SIMPLE
        table: payment
    partitions: NULL
         type: ALL
    possible_keys: NULL
          key: NULL
      key_len: NULL
          ref: NULL
         rows: 16086
     filtered: 100.00
        Extra: NULL

这里我们看到mysql扫描的行数有16086行，而且type参数指示的是ALL，也就意味着是一个全表扫描，如果数据量增加，那么这条语句的执行时间会明显增加。

优化方式：`在payment_date上建立索引`。

    create index payment_date_IDX on payment(payment_date);
    explain select max(payment_date) from payment

在这里，我们建立索引后，在进行查询，查询结果如下：

            id: 1
    select_type: SIMPLE
        table: NULL
    partitions: NULL
         type: NULL
    possible_keys: NULL
          key: NULL
      key_len: NULL
          ref: NULL
         rows: NULL
     filtered: NULL
        Extra: Select tables optimized away

现在，我们就大大优化了SQL的执行效率。

`对于MAX()可以使用索引进行优化。`

覆盖索引：完全可以通过索引的信息查找到我们想要的结果。