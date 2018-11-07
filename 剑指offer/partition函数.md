# Partition函数

Partition函数完成的工作是快排中的一次循环，即给一个索引值n，一个数组arr，经过Partition函数处理后，返回一个索引值m，arr[m]等于原来数组的arr[n]，并且arr[m]左侧的值都小于arr[m]，右侧反之。

下面我们使用例子来进行介绍，使该函数的处理逻辑更加简单易懂，给定一个数组：

    { 1, 4, 2, 6, 3, 7, 5, 8, 9}

如果给定的索引值是6，即arr[6] = 5;

常规的Partition函数，操作规则如下：

指定一个key = arr[6] = 5，将arr[6]与arr[0]调换，结果为：

    key = 5
    arr = { 5, 4, 2, 6, 3, 7, 1, 8, 9}

下面指定两个指针start = 0,end = arr.length - 1 = 9 - 1 = 8;结果如下：

    key   = 5
    start = 0
    end   = 8
    arr = { 5, 4, 2, 6, 3, 7, 1, 8, 9}

紧接着下面两步交替执行，直到start == end;

1. 然后如果arr[end] >= key，那么end --;如果arr[end] < key，那么arr[end]与arr[start]交换，start ++，执行第二步。
2. 如果arr[start] < key，那么 start ++;如果arr[end] >= key，那么arr[end]与arr[start]交换，end++，执行第一步。

上述例子执行具体步骤如下：

第一步（执行步骤1，由于arr[end] >= key，end --）：

    key   = 5
    start = 0
    end   = 7
    arr = { 5, 4, 2, 6, 3, 7, 1, 8, 9}

第二步（执行步骤1，由于arr[end] >= key，end --）：

    key   = 5
    start = 0
    end   = 6
    arr = { 5, 4, 2, 6, 3, 7, 1, 8, 9}

第三步（执行步骤1，由于arr[end] < key，执行arr[end]与arr[start]交换，并且start ++）：

    key   = 5
    start = 1
    end   = 6
    arr = { 1, 4, 2, 6, 3, 7, 5, 8, 9}

第四步（执行步骤2，由于arr[start] < key，start ++）：

    key   = 5
    start = 2
    end   = 6
    arr = { 1, 4, 2, 6, 3, 7, 5, 8, 9}

第五步（执行步骤2，由于arr[start] < key，start ++）：

    key   = 5
    start = 3
    end   = 6
    arr = { 1, 4, 2, 6, 3, 7, 5, 8, 9}

第六步（执行步骤2，由于arr[start] >= key，arr[start] 和arr[end]交换，然后end --）：

    key   = 5
    start = 3
    end   = 5
    arr = { 1, 4, 2, 5, 3, 7, 6, 8, 9}

第七步（执行步骤1，由于arr[end] >= key然后end --）：

    key   = 5
    start = 3
    end   = 4
    arr = { 1, 4, 2, 5, 3, 7, 6, 8, 9}

第七步（执行步骤1，由于arr[end] < key,然后arr[end] 与arr[start]交换，start ++）：

    key   = 5
    start = 4
    end   = 4
    arr = { 1, 4, 2, 3, 5, 7, 6, 8, 9}

第八步（由于start == end，循环结束）最后返回start就好了。

笔者的实现如下：

    private int partition(int[] data,int index,int start,int end) {
		int head = start;
		int tail = end;
		int tempH = data[index];
		data[index] = data[start];
		data[start] = tempH;
		int key = data[start];
		int label = 0;
		while(head != tail) {
			if(label == 0) {
				if(data[tail] >= key) {
					tail --;
				}
				else {
					tempH = data[tail];
					data[tail] = data[head];
					data[head] = tempH;
					head ++;
					label = 1;
				}
			}
			else {
				if(data[head] < key) {
					head ++;
				}
				else {
					tempH = data[head];
					data[head] = data[tail];
					data[tail] = tempH;
					tail --;
					label = 0;
				}
			}
		}
		return head;
	}

非常规解法如下，仍然是原来输入：

    arr    = { 1, 4, 2, 6, 3, 7, 5, 8, 9}
    index  = 6
    arr[6] = 5

该解法操作如下：

先将指定索引值的数字与最后一个数字进行交换：
    
    arr    = { 1, 4, 2, 6, 3, 7, 9, 8, 5}

然后设置一个变量用于存储遍历过程中最早遍历的大于指定数字的索引值，初始化为-1，名为small:

    small  = -1
    arr    = { 1, 4, 2, 6, 3, 7, 9, 8, 5}

然后从头到尾遍历数组，遍历过程中，如果遍历到的值小于数组最后一个值（指定索引的数字），那么就++small，如果small和index不同，那么就交换arr[small]和arr[index]，即将比指定索引的数字大的值移到后面，将比其小的值移到前面。如果遍历到的值大于数组最后一个值（指定索引的数字），直接跳过，进行下一次遍历。

上述例子的操作步骤如下：

第一步（遍历第一个值，小于5，++small，index == small不交换）：

    index = 0
    small  = 0
    arr    = { 1, 4, 2, 6, 3, 7, 9, 8, 5}

第二步（遍历第二个值，小于5，++small，index == small不交换）

    index = 1
    small  = 1
    arr    = { 1, 4, 2, 6, 3, 7, 9, 8, 5}

第三步（遍历第三个值，小于5，++small，index == small不交换）

    index = 2
    small  = 2
    arr    = { 1, 4, 2, 6, 3, 7, 9, 8, 5}

第四步（遍历第四个值，大于5，直接跳过）

    index = 3
    small  = 2
    arr    = { 1, 4, 2, 6, 3, 7, 9, 8, 5}

第五步（遍历第五个值，小于5，++small，index != small，交换）

    index = 4
    small  = 3
    arr    = { 1, 4, 2, 3, 6, 7, 9, 8, 5}

第六步（遍历第六个值，大于5，直接跳过）

    index = 5
    small  = 3
    arr    = { 1, 4, 2, 3, 6, 7, 9, 8, 5}

第七步（遍历第七个值，大于5，直接跳过）

    index = 6
    small  = 3
    arr    = { 1, 4, 2, 3, 6, 7, 9, 8, 5}

第八步（遍历第八个值，大于5，直接跳过）

    index = 7
    small  = 3
    arr    = { 1, 4, 2, 3, 6, 7, 9, 8, 5}

第九步（遍历第九个值，大于5，直接跳过）

    index = 8
    small  = 3
    arr    = { 1, 4, 2, 3, 6, 7, 9, 8, 5}

到达这步，数组除了最后一个数字都已经达到我们想要的结果，最后我们执行++small操作，并将arr[small]和arr[end]进行交换。并返回small，结果如下：

    small  = 4
    arr    = { 1, 4, 2, 3, 5, 7, 9, 8, 6}

代码：

    public static int partition(int[] data,int start,int end,int index) throws Exception {
		if(data == null || data.length == 0)
			throw new Exception();
		if(data.length == 1)
			return 0;
		if(index < start || index > end)
			throw new Exception();
		int small = start - 1;
		swap(data,end-1,index);
		for(index = start; index < end;index ++) {
			if(data[index] < data[end-1]) {
				++ small;
				if(small != index) {
					swap(data,index,small);
				}
			}
		}
		++ small;
		swap(data,small,end-1);
		return small;
	}