
## Section 3

### 1. 管理用户账户

如果要添加账户，你需要以root用户执行如下两条命令之一：

    adduser [new_account]
    useradd [new_account]

当创建新的用户账户时，默认会执行下面的操作。

- 它的主目录就会被创建（一般是“/home/用户名”，除非你特别设置）。
- 一些隐藏文件，如`.bash_logout`，`.bash_profile`以及`.bashrc`会被复制到用户的主目录。他们会为用户的会话提供环境变量。
- 会为您的账号添加一个邮件池目录
- 会创建一个和用户名同样的组（除非你给新用户指定了组）

用户账目的全部信息都被保存在`/etc/passwd`文件，文件格式如下：

    [username]:[x]:[UID]:[GID]:[Comment]:[Home directory]:[Default shell]

- [username]和[Comment]就是用户名和备注
- x表示账户启动了密码保护（记录在/etc/shadow文件），密码用于登录[username]
- [UID]和[GID]是整数，它们表明了[username]的用户ID和所属的主组ID
- [Home directory]显示[username]的主目录的绝对路径
- [Default shell]是当用户登陆后使用的默认shell

另一个你必须要熟悉的重要文件是存储组信息的/etc/group。其格式如下：

    [Group name]:[Group password]:[GID]:[Group members]

- [Group name]组名
- 这个组是否使用了密码(x表示没有)
- [GID]和etc/passwd中一样
- [Group members]用户列表，使用“，”隔开

usermod命令可以用来修改用户账户信息，基本语法如下：

    usermod [options] [username]

#### 1.设置账户过期时间

如果有一些短期使用的账户或者你要在有限的时间内授权访问，你可以使用`--expiredata`参数，后加上YYYY-MM-DD格式的日期，下面命令可以查看是否生效：

    chage -l [username]

#### 2. 向组内追加用户

-aG或者-append-group选项，后跟逗号分割的组名。

#### 3. 修改用户主目录或默认shell

修改默认目录，使用-d或者-home参数，后跟绝对路径来修改主目录。

修改默认shell，使用-shell的参数，后面跟新的shell路径。

#### 4. 展示组内的用户

把用户添加到组之后，可以使用如下命令验证属于哪一个组：

    groups [username]
    id [username]

#### 5. 通过锁定密码来停用账户

如果想关闭账户，可以使用-l或者-lock选项来锁定用户的密码。这将会阻止用户的登录。

#### 6. 解锁密码

如果想要重新启用账户可以继续登录，使用-u或者-unlock命令。

#### 7. 删除组和用户

- 删除组：groupdel [group_name]
- 删除用户：userdel -r [user_name]

有一些文件属于该组，删除组时它们不会也被删除。但是组拥有者的名字会被设置为删除掉的组的GID。

#### 8. 列举，设置，并且修改标准ugo/rwx权限

ls命令使用-l参数时，允许您以长格式查看一个目录中的内容。而且改命令还可以用于单个文件中。在ls输出中的前10个字符表示每个文件的属性。

- -（连字符）:一个标准文件
- d：一个目录
- l:一个符号链接
- c：字符设备（将数组作为字节流，例如终端）
- b：块设备（以块的方式处理数据，例如存储设备）

