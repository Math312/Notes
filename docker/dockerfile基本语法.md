

## Dockerfile 基本语法

### 1.FROM指定基础镜像

FROM语句用于指定基础镜像，一个FROM语句必须是第一条指令。我们可以指定已有的镜像或者是空镜像，其中空镜像语法如下：

    FROM scratch

如果以scratch为基础证明你不以任何镜像为基础,接下来所写的指令将会作为镜像的第一层。

### 2.RUN指令

RUN指令是用来执行命令行命令的，格式存在两种:
1. shell格式:RUN<命令>就像直接在命令行中输入的格式一样。
2. exec格式：RUN["可执行文件","参数1","参数2"]

由于RUN指令每条指令都会增加一层，而Docker是存在层数限制的，因此RUN语法的正确形式如下：

    FROM debian:jessie
    RUN buildDeps='gcc libc6-dev make' \
    && apt-get update \
    && apt-get install -y $buildDeps \
    && wget -O redis.tar.gz "http://download.redis.io/releases/redis-3.2.5.tar.gz" \
    && mkdir -p /usr/src/redis \
    && tar -xzf redis.tar.gz -C /usr/src/redis --strip-components=1 \
    && make -C /usr/src/redis \
    && make -C /usr/src/redis install \
    && rm -rf /var/lib/apt/lists/* \
    && rm redis.tar.gz \
    && rm -r /usr/src/redis \
    && apt-get purge -y --auto-remove $buildDeps

注意，这里的所有指令都以`RUN`指令来运行，不过这条指令为1条指令，因此只会增加1层。

构建镜像采用docker build 命令，该命令格式如下：

    docker build [选项]<上下文路径/URL/>

我们一般都会使用

    docker build -t imageName .

这里最后的.表示相对于执行该条命令的路径来说明你构建镜像的上下文，不是Dockerfile目录的意思，但是两者往往完全重合。

### 3.COPY命令

COPY指令用于文件复制，例如

    COPY ./package.json /app/

这样会将当前上下文中的package.json复制到镜像的/app/

值得注意的是：

    docker build -t imageName .

上条命令会将`.`（即上下文目录）所代表的目录下所有东西都打包交给Docker引擎帮助构建镜像。由于这种机制我们可以用.dockerignore将不希望用来构建的文件剔除出去，方法形如.gitignore。

COPY指令格式：
1. COPY <源路经>...<目标路径>
2. COPY ["<源路径1>",..."<目标路径>"]

其中源路径可以是多个，甚至是通配符，目标路径可以是容器内的绝对路径，也可以是相对于工作目录的相对路径（工作目录可以由`WORKDIR`指令来指定），目标目录不需要创建，如果目标目录不存在会在复制前现行创建目录。

### 4.ADD 更高级的复制文件

ADD 指令和COPY 的格式和性质基本一致，但是在COPY的基础上增加了许多功能。

ADD的`<源路径>`可以是一个`URL`，这种情况下Docker引擎会试图下载这个链接的文件放到`<目标路径>`中，下载后的文件权限默认为`600`，如果这不是你想要的权限需要使用`RUN`指令进行调节，而且这样下载的压缩包不会自动解压。

ADD的`<源路径>`可以是一个`tar`压缩文件，压缩格式为`gzip`、`bzip2`、`xz`的情况下，ADD 指令会执行自动解压。

Docker官方文档推荐使用COPY命令，因为COPY指令的职能相对更清晰，而且`ADD`指令会使镜像缓存构建失效，因此会使镜像构建变得比较缓慢。

所有文件的复制均使用`COPY`指令，只有在需要自动解压缩的场合使用`ADD`指令。

### 5. CMD 容器启动命令

`CMD`指令格式：
1. `shell`格式：CMD <命令>
2. `exec`格式：CMD["可执行文件"，“参数1”，“参数2”]
3. 参数列表格式：CMD ["参数1","参数2"...]。在指定了`ENTRYPOINT`指令后，用`CMD`指令指定参数。

`CMD`指令就是用于指定默认的容器住进成启动命令的。

### 6.ENTRYPOINT 入口点

`ENTRYPOINT`的格式和`RUN`的格式一样，同样也分为`exec`格式和`shell`格式。

`ENTRYPOINT`的目的和`CMD`一样，都是在指定容器的启动程序和参数。`ENTRYPOINT`在运行时可以替代，不过比`CMD`显得繁琐，需要通过`docker run`的参数`--entrypoint`来指定。

当指定了`ENTRYPOINT`了之后，`CMD`的含义就发生了改变，不再是直接的运行其命令，而是将`CMD`的内容作为参数传给`ENTRYPOINT`指令，换句话说执行时，将变为：

    <ENTRYPOINT>:"<CMD>"

注意每个Dockerfile只能有一个`ENTRYPOINT`和一个`CMD`，多了会被覆盖。

### 7.ENV 设置环境变量

`ENV` 指令的格式有两种：

1. ENV <key><value>
2. ENV < key1>= < value1> < key2>=< value2>...

例如：

    ENV VERSION=1.0 DEBUG=on \
    NAME="Happy Feet"

如果要使用已定义的环境变量，则使用`$`符号即可，下面给出一个例子：

    ENV NODE_VERSION 7.2.0
    RUN curl -SLO "https://nodejs.org/dist/v$NODE_VERSION/node-v$NODE_VERSION-linux-x64.ta
    r.xz" \
    && curl -SLO "https://nodejs.org/dist/v$NODE_VERSION/SHASUMS256.txt.asc" \
    && gpg --batch --decrypt --output SHASUMS256.txt SHASUMS256.txt.asc \
    && grep " node-v$NODE_VERSION-linux-x64.tar.xz\$" SHASUMS256.txt | sha256sum -c - \
    && tar -xJf "node-v$NODE_VERSION-linux-x64.tar.xz" -C /usr/local --strip-components=
    1 \
    && rm "node-v$NODE_VERSION-linux-x64.tar.xz" SHASUMS256.txt.asc SHASUMS256.txt \
    && ln -s /usr/local/bin/node /usr/local/bin/nodejs

### 8.ARG 构建参数

格式： ARG<参数名>[=<默认值>]

构建参数和`ENV`效果是一样的，都是设置环境变量。所不同的是，ARG所设置的环境变量在将来容器运行时是不会存在的，但是`docker history`仍可以看到。

`DOckerfile`中的`ARG`指令是定义参数名称并指定其默认值，该默认值可以在构建命令`docker build`中用`--build-arg <参数名>=<值>`来覆盖。

### 9. VOLUME定义匿名卷

格式为：
- VOLUME ["<路径1>","<路径2>"...]
- VOLUME <路径>

例如：

    VOLUME mydata:/data

使用了 Dockerfile mydata 这个命名卷挂载到了 /data 这个位置,替代了中定义的匿名卷的挂载配置。

### 10. EXPOSE 声明端口

格式为： EXPOSE<端口1>[<端口2>...]

EXPOSE指令只是声明运行时容器提供服务端口，在运行时并不会因为这个声明应用就会开启这个端口的服务。

### 11. WORKDIR 指定工作目录

格式为： WORKDIR <工作目录路径>

使用`WORKDIR`指令可以来指定工作目录（或者成为当前目录），以后各层的当前目录就被改为指定的目录，该目录需要已经存在，`WORKDIR`不会帮你建立目录。

类似于cd命令。由于在Docker的分层存储，导致每条RUN的语句都是新开一个容器，执行之后存储其存储层的东西，那么

    RUN cd XX

这种命令跑完是没有效果的，应该是说工作目录虽然变了，但是，容器也关了，因此，执行了这条语句也没用。所以，我们更改工作目录要使用`WORKDIR`指令，这样以后的层的工作目录才会改变。

### 12. USER 指定当前用户

`USER`指令和`WORKDIR`指令类似，都是改变环境状态并影响以后的层。`WORKDIR`是改变工作目录，`USER`是改变之后层的执行`RUN`，`CMD`，`ENTRYPOINT`这类命令的身份。

和`WORKDIR`一样，`USER`只帮你切换到指定用户而已，之歌用户必须是事先建立好的，否则无法切换。

### 13. HEALTHCHECK 健康检查

格式为：

1. HEALTHCHECK [选项] CMD <命令> ：设置检查容器健康状况的命令。
2. HEALTHCHECK NONE：如果基础镜像有检查指令，使用这行可以屏蔽掉其健康检查指令。

`HEALTHCHECK`指令通过执行指定的一条命令，用这行命令来判断容器主进程的服务状态是否还正常，从而比较真实地反映出容器的实际状态。

当一个镜像指定了`HEALTHCHECK`指令后，用其启动容器，初始状态会变为`starting`，在`HEALTHCHECK`指令检查成功后会变为`healthy`，如果连续一定次数失败，则会变为`unhealthy`。

`HEALTHCHECK`支持下列选项：

- `--interval=<时长>`：两次健康检查的间隔，默认为30秒。
- `--timeout=<时长>`：健康检查运行命令超时时间，如果超过这个时间，本次健康检查就视为失败，默认为30秒。
- `--retries=<次数>`：当连续失败指定次数后，则将容器状态设置为`unhealthy`，默认为3次。

和`CMD`以及`ENTRYPOINT`一样，`HEALTHCHECK`只可以出现一次，重复会被覆盖。

`HEALTHCHECK [选项] CMD`后面的命令的返回值决定了该次检查的成功与否
- 0： 成功
- 1： 失败
- 2： 保留，不要使用这个值。

### 14. ONBUILD 

`ONBUILD` 是一个特殊的指令,它后面跟的是其它指令,比如 `RUN` , `COPY`等,而这些指令,在当前镜像构建时并不会被执行。只有当以当前镜像为基础镜像,去构建下一级镜像的时候才会被执行。

