# 如何通过慢日志查找有问题的SQL

1. 查询次数多且每次查询占用时间比较长的SQL，通常是pt-query-digest分析的前几个查询
2. IO大的SQL，注意pt-query-digest分析中的Rows examine项（扫描行数），一个SQL扫描行数越多，IO消耗越大
3. 未命中索引的SQL，注意pt-query-digest分析中的Rows examine项和Rows send项的对比。如果前者远远大于后者，则证明SQL的索引命中率并不高