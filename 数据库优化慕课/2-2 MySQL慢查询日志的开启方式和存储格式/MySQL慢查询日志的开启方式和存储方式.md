# MySQL慢查询日志的开启方式和存储格式

## 慢查询日志相关SQL

1. `show variables like 'slow_query_log'` 查询慢查询日志的存储位置
2. `set global log_queries_not_using_indexes = on;` 不使用索引的 查询是否记录入慢查询日志。由于我们的优化过程中索引占据很大的比重，所以推荐开启。
3. `set global long_query_time = 1` 指定慢查询时间，这里指超过1秒的查询就是慢查询，要记录在日志中。
4. `set global slow_query_log_file = '文件目录'` 设置慢查询日志存储位置。

## 慢查询日志的存储格式

    # Time: 2018-07-31T02:09:21.720403Z
    # User@Host: root[root] @ localhost [::1]  Id:    26
    # Query_time: 0.007168  Lock_time: 0.004707 Rows_sent: 3  Rows_examined: 29
    SET timestamp=1533002961;
    SHOW INDEX FROM `sakila`.`city`;

1. Time:查询的执行时间
2. User:查询来自的用户主机是什么
3. Query_time：查询的执行时间
4. Lock_time:锁定的时间
5. Row_sent:所发送的行数
6. Row_examined:所扫描的行数
7. 时间戳形式记录SQL的执行时间
8. SQL的具体内容