## 第18章 Java IO系统

### 1. File

File类指代一个文件的名称或一个文件夹的名称。

构造方法如下：

构造器|描述
---|---
File(File parent, String child)| 使用一个抽象的父级路径和一个作为子级路径的字符串创建一个新的实例。
File(String pathname)| 使用一个字符串作为被给定的抽象路径名创建一个实例
File(String parent.String child)|使用一个指定父级路径的字符窗和一个子级路径的字符串创建实例
File(URI uri)|使用给定的URI创建一个新的实例

常用方法：

返回值|方法|方法描述
---|---|---
boolean|canExecute()|测试该文件是否能被执行
boolean|canRead()|测试该文件是否能被读取
boolean|canWrite()|测试该文件是否能被更改
boolean|createNewFile()|当且仅当给定文件名的文件不存在时，根据所给的文件名，自动创建一个新的空的文件
boolean|delete()|删除被文件名指定的文件或目录
exists()|判断该文件和文件夹是否存在
File|getAbsuloteFile()|返回完整路径名的File
String|getAbsolutePath()|返回文件的完整路径名
String|getName()|返回创建时指定的文件名
String|getParent()|返回文件的父级文件的文件名(注意，如果创建文件时使用"a.txt"，这类调用该方法时会返回null，只有创建爱你文件时使用"xx/xx/a.txt"，才会正确返回，该方法最好和getAbsolutePath()一同使用)
boolean|isDirectory()|判断该文件是否是一个文件夹
boolean|isFile|判断该文件是否是一个文件
long|length()|返回文件的大小(Byte为单位)
String[]|list()|返回子目录下文件名集合
String[]|list(FilenameFilter filter)|返回被filter筛选后的文件名的数组
File[]|listFiles()|返回该文件夹子目录下的所有文件
File[]|listFiles(FileFilter filer)|返回该文件夹下所有被filter筛选过后的文件
boolean|mkdir()|根据文件名创建一个文件夹
boolean|mkdirs()|根据文件名创建多层嵌套的文件夹
boolean|setExeable(boolean executable)|赋予执行权限
boolean|setReadOnly()|标记文件为只读
boolean|setWritable(boolean writable)|赋予文件写权限

### 2. 输入、输出

#### InputStream
InputStream的作用是用来表示那些从不同数据源输入的类。这些数据源包括：
- 字节数组
- String对象
- 文件
- 管道
- 一个由其他种类的流组成的序列
- 其他数据源，如Internet连接等

由于Java.IO库使用装饰器模式进行实现的，下面的类用来被装饰，而装饰器的基类为FilterInputStream。

类|功能|构造器参数
---|---|---
ByteArrayInputStream|允许将内存的缓冲区当作InputStream使用|缓冲区，字节从中读出
StringBufferInputStream|将String转换成InputStream|字符串，底层由StringBuffer实现
FileInputStream|用于从文件中读取信息|字符串，表示文件名、文件或FileDescriptor对象
PipedInputStream|产生用于写入相关PipedOutputStream的数据，实现通道化概念|PipedOutputStream
SequenceInputStream|将两个或多个InputStream转化为一个InputStream|两个InputStream对象或一个容纳InputStream对象的容器Enumeration

#### OutputStream

OutputStream决定了输出要去往的目标：字节数组、文件或管道。

类|功能|构造器参数
---|---|---
ByteArrayOutputStream|在内存中创建缓冲区。所有送往“流”的数据都要放置在此缓冲区|缓冲区初始化尺寸(可选)
FileOutputStream|用于将信息写至文件|字符串，表示文件名、文件或FileDescriptor对象
PipedOutputStream|任何写入其中的信息都会自动作为相关PipedInputStream的输出。实现管道化概念|PipedInputStream

### 3.装饰接口

FilterInputStream和FilterOutputStream是用来提供装饰器接口以控制特定输入流(InputStream)和输出流(OutputStream)两个类。这两个类分别继承子InputStream和OutputStream，是装饰器类的两个基类。

下面介绍FilterInputStream中的常用类型：

类|功能|构造器参数
---|---|---
DataInputStream|与DataOutputStream搭配使用，因此，我们可以按照可移植方式从流读取基本数据类型(int,char,long等)|InputStream 包含用于读取基本数据类型的全部接口
BufferedInputStream|使用它可以防止每次读取时都得进行实际的写操作。代表“使用缓冲区”|InputStream，可以指定缓冲区大小
LineNumberInputStream|跟踪输入流中的行号；可以调用getLineNumber()和setLineNumber(int)|InputStream，仅增加了行号，因此可能要与接口对象搭配使用
PushbackInputStream|具有“能弹出一个字节的缓冲区”。因此可以将读到的最后一个字符回退|InputStream，通常作为编译器的扫描器，之所以包含在内是因为Java编译器需要

紧接着是FilterOutputStream中的常用类型：

类|功能|构造器参数
---|---|---
DataOutputStream|与DataInputStream搭配使用，因此可以按照可移植方式向流中写入基本类型数据(int，char,long等)|OutputStream，包含用于写入基本类型数据的全部接口
PrintStream|用于产生格式化输出。其中DataOutputStream处理数据的存储，PrintStream处理显示|OutputStram，可以用boolean值指示是否在每次换行时清空缓冲区(可选的)应该时对OutputStream对象的“final”封装。可能会经常使用到它。
BufferedOutputStream|使用它以避免每次发送数据都要进行实际的写操作。代表“使用缓冲区”，可以调用flush()清空缓冲区。|OutputStream，可以指定缓冲区大小(可以提供大小)

### 4. Writer、Reader

InputStream和OutputStream在以面向字节形式的I/O中仍可以提供极有价值的可能，Reader和Writer则提供兼容Unicode与面向字符的I/O功能。

InputStreamReader可以把InputStream转换Reader，而OutputStreamReader可以将OutputStream转化为Writer。

设计Reader和Writer主要是为了国际化，老的字节流只支持8位的字节流，并且不能很好的处理16位的Unicode字符。也因此，几乎所有原始的Java I/O流类都有相应的Reader和Writer类来提供天然的Unicode操作。

下面介绍Reader和Stream的对应关系：

Stream|Reader
---|---
InputStream|Reader 适配器：InputStreamReader
OutputStream| Writer 适配器：OutStreamWriter
FileInputStream|FileReader
FileOutputStream|FileWriter
StringBUfferInputStream(已弃用)|StringReader
(无对应的类)|StringWriter
ByteArrayInputStream|CharArrayReader
ByteArrayOutputStream|CharArrayWriter
PipedInputStream|PipedReader
PipedOutputStream|PipedWriter
FilterInputStream|FilterReader
FilterOutputStream|FilterWriter(抽象类，没有子类)
BufferedInputStream|BufferedReader(也有ReadLine())
BufferedOutputStream|BufferedWriter
DataInputStream|使用DataInputStream(除了当需要使用readLine()之外，这时应该使用BufferedReader)
PrintStream|PrintWriter
LineNumberInputStream(已弃用)|LineNumberReader
StreamTokenizer|StreamTokenizer(使用接受Reader的构造器)
PushbackInputStream|PushbackReader

DataOutputStream、File、RandomAccessFile、SequenceInputStream未发生变化

### 5.RandomAccessFile

RandomAccessFile适用于由大小已知的记录组成的文件，它和InputStream和OutputStream的继承结构没有任何关联。他甚至不使用InputStream和OutputStream类中已有的任何功能。而是由Object直接派生。

RandomAccessFile的工作方式相当于把DataInputStream和DataOutputStream组合起来使用，还添加了一些方法。
 
构造方法：

构造器|描述
---|---
RandomAccessFile(File file,String mode)| 创建指定文件的读、读写随机访问流
RandomAccessFile(String name,String mode)| 创建指定文件的读、读写随机访问流

mode参数的形式和表示意义如下：

参数值|意义
---|---
"r"|以只读的方式打开，调用写方法会抛出IOException
"rw"|以可读可写的方式打开，如果文件不存在就会尝试创建一个文件
“rws”|以可读可写的方式打开,文件的内容和元数据进行更改时都会同步写到硬盘中
"rwd"|以可读可写的方式打开,文件的内容进行更改时会同步写到硬盘中

常用方法如下：

返回值|方法|描述
---|---|---
long|getFilePointer()|查找当前所处的位置
void|seek(long pos))|移动光标到文件开头起的某位置。
long|length()|用于判断文件大小
int|skipBytes(int n)|跳过n个字节

### 8. 标准IO

名称|类型|描述
---|---|---
System.out|PrintStream|标准输出流
System.in|InputStream|标准输入流
System.err|PrintStream|标准错误流

标准I/O重定向

System类提供了3个方法进行标准IO重定向，即setIn(InputStream),setOut(PrintStream),setErr(PrintStream)。

但要注意，使用完标准IO重定向一定要重定向回来。

### 9. nio

旧的IO其实已经用nio重新实现过了，nio对于传统IO来说速度有了很大的提高，但是内存的消耗，和很多书写方式要处理更多，但是这对于速度的提升绝对是值得的。速度的提高来自所使用的结构更接近与操作系统执行IO 的方式：通道和缓冲器。

唯一直接与通道交互的缓冲器是ByteBuffer，可以存储未加工的缓冲器。他有几个静态方法通过告知分配多少存储空间来创建一个ByteBuffer对象(ByteBuffer.allocate(int)或allocateDirect(int))，或者通过将一个byte数组包装起来(ByteBuffer.wrap(byte[])或ByteBuffer.wrap(byte[] array,int offset,int length))。使用ByteBuffer.allocateDirect()方法会产生一个与系统耦合度更高的缓冲器，但是会产生更大的开支，并且随着操作系统不同，表现效果也不同。

旧的IO类库只有3个类被修改了可以产生FileChannel，他们分别是FileInputStream、FileOutputStream、RandomAccessFile，它们都可以使用getChannel()方法来产生通道,这些都是字节操纵流，与低层的nio性质一致，Writer、Reader等字符模式都不能用于产生通道。

FileChannel的常用方法如下：

返回值|方法名|描述
---|---|---
int|read(ByteBuffer)|将文件读到ByteBuffer中
int|write(ByteBuffer)|将ByteBuffer中的数据写到文件中。
FileLock|tryLock()|尝试对文件加锁，如果不能加锁返回null
FileLock|lock()|对文件加锁，如果不能加锁持续等待，直到可以为止。

ByteBuffer提供了很多视图缓冲器，用来将ByteBuffer看成集本数据类型的数组进行操作。注意如果要对ByteBuffer使用视图缓冲器操作，必须要先使用asXXXBuffer()方法将ByteBuffer进行转化，然后再进行操作，否则会出错。而且，ByteBuffer遵循高位优先的方式进行存储，可以使用order(ByteOrder.LITTLE_ENDIAN)将其改为低位优先。

Buffer由数据和可以高效访问和操纵这些数据的4个索引组成，这4个索引分别是:mark(标记)、position(位置)、limit(界限)、capacity(容量))。下面是操纵这4个索引的方法：

方法|描述
---|---
capacity()|返回缓冲区容量
clear()|清空缓冲区，将position置为0，limit设置为容量，我们可以调用该方法覆写缓冲区
flip()|将limit设置为position,position设置为0。此方法用于准备从缓冲区读取已经写入的数据
limit()|返回limit的值
limit(int lim)|设置limit的值
mark()|将mark设置为position
reset()|将position设置为mark
position()|返回position的值
position(int pos)|设置position的值
remaining()|返回(limit-position)
hasRemaining()|若有介于position和limit之间的元素返回true。

注意写入通道之前，必须调用ByteBuffer的flip()方法，读入之前必须调用ByteBuffer的clear()方法。

#### 内存映射文件

有些文件太大，导致不能全部放入内存，对此我们可以使用内存映射文件，即对文件分部分访问。

    MappedByteBuffer out = new RandomAccessFile("test.dat","rw").getChannel()
    .map(FileChannel.MapMode.READ_WRITE,0,length);

就可以实现映射某个大文件的较小的部分。然后对其进行操作。

#### 文件加锁

由于文件对本机上的所有软件（操作系统、其他软件）都是可见的，因此算是共享资源，访问时需要同步，就存在加锁的问题。

FileChannel调用tryLock()或lock()可以对整个文件加锁，如果我们要对部分文件加锁，
可以使用

    FileChanenel.lock(start,end,false)

来加锁。