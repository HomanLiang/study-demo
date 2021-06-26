[toc]



# Docker 安装

## 1.安装步骤
1. 安装工具包
    ```
    $ sudo yum install -y yum-utils 		#安装工具包，缺少这些依赖将无法完成
    ```
    
    执行结果：

    ```
    Loaded plugins: fastestmirror, langpacks
    base                                                                                          | 3.6 kB  00:00:00
    epel                                                                                          | 4.3 kB  00:00:00
    extras                                                                                        | 3.4 kB  00:00:00
    update                                                                                        | 3.4 kB  00:00:00
    (1/3): epel/7/x86_64/updateinfo                                                               | 797 kB  00:00:00
    (2/3): epel/7/x86_64/primary_db                                                               | 4.7 MB  00:00:00
    (3/3): update/7/x86_64/primary_db                                                             | 4.8 MB  00:00:00
    Loading mirror speeds from cached hostfile
    Package yum-utils-1.1.31-40.el7.noarch already installed and latest version
    Nothing to do
    ```
    
2. 设置远程仓库

    ```
    $sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
    ```

	执行结果：

    ```
    Loaded plugins: fastestmirror, langpacks
    adding repo from: https://download.docker.com/linux/centos/docker-ce.repo
    grabbing file https://download.docker.com/linux/centos/docker-ce.repo to /etc/yum.repos.d/docker-ce.repo
    repo saved to /etc/yum.repos.d/docker-ce.repo
    ```
    
3. 安装

    ```
    $ sudo yum install docker-ce
    ```

	执行结果：

    ```
    Loaded plugins: fastestmirror, langpacks
    docker-ce-stable                                                                              | 2.9 kB  00:00:00
    docker-ce-stable/x86_64/primary_db                                                            | 4.8 kB  00:00:00
    Loading mirror speeds from cached hostfile
    Resolving Dependencies
    --> Running transaction check
    ---> Package docker-ce.x86_64 0:17.03.1.ce-1.el7.centos will be installed
    --> Processing Dependency: docker-ce-selinux >= 17.03.1.ce-1.el7.centos for package: docker-ce-17.03.1.ce-1.el7.centos.x86_64
    --> Processing Dependency: libcgroup for package: docker-ce-17.03.1.ce-1.el7.centos.x86_64
    --> Processing Dependency: libseccomp.so.2()(64bit) for package: docker-ce-17.03.1.ce-1.el7.centos.x86_64
    --> Running transaction check
    ---> Package docker-ce-selinux.noarch 0:17.03.1.ce-1.el7.centos will be installed
    --> Processing Dependency: policycoreutils-python for package: docker-ce-selinux-17.03.1.ce-1.el7.centos.noarch
    ---> Package libcgroup.x86_64 0:0.41-11.el7 will be installed
    ---> Package libseccomp.x86_64 0:2.3.1-2.el7 will be installed
    --> Running transaction check
    ---> Package policycoreutils-python.x86_64 0:2.5-11.el7_3 will be installed
    --> Processing Dependency: setools-libs >= 3.3.8-1 for package: policycoreutils-python-2.5-11.el7_3.x86_64
    --> Processing Dependency: libsemanage-python >= 2.5-5 for package: policycoreutils-python-2.5-11.el7_3.x86_64
    --> Processing Dependency: audit-libs-python >= 2.1.3-4 for package: policycoreutils-python-2.5-11.el7_3.x86_64
    --> Processing Dependency: python-IPy for package: policycoreutils-python-2.5-11.el7_3.x86_64
    --> Processing Dependency: libqpol.so.1(VERS_1.4)(64bit) for package: policycoreutils-python-2.5-11.el7_3.x86_64
    --> Processing Dependency: libqpol.so.1(VERS_1.2)(64bit) for package: policycoreutils-python-2.5-11.el7_3.x86_64
    --> Processing Dependency: libapol.so.4(VERS_4.0)(64bit) for package: policycoreutils-python-2.5-11.el7_3.x86_64
    --> Processing Dependency: checkpolicy for package: policycoreutils-python-2.5-11.el7_3.x86_64
    --> Processing Dependency: libqpol.so.1()(64bit) for package: policycoreutils-python-2.5-11.el7_3.x86_64
    --> Processing Dependency: libapol.so.4()(64bit) for package: policycoreutils-python-2.5-11.el7_3.x86_64
    --> Running transaction check
    ---> Package audit-libs-python.x86_64 0:2.6.5-3.el7_3.1 will be installed
    ---> Package checkpolicy.x86_64 0:2.5-4.el7 will be installed
    ---> Package libsemanage-python.x86_64 0:2.5-5.1.el7_3 will be installed
    ---> Package python-IPy.noarch 0:0.75-6.el7 will be installed
    ---> Package setools-libs.x86_64 0:3.3.8-1.1.el7 will be installed
    --> Finished Dependency Resolution

    Dependencies Resolved

    =====================================================================================================================
     Package                         Arch            Version                             Repository                 Size
    =====================================================================================================================
    Installing:
     docker-ce                       x86_64          17.03.1.ce-1.el7.centos             docker-ce-stable           19 M
    Installing for dependencies:
     audit-libs-python               x86_64          2.6.5-3.el7_3.1                     update                     70 k
     checkpolicy                     x86_64          2.5-4.el7                           base                      290 k
     docker-ce-selinux               noarch          17.03.1.ce-1.el7.centos             docker-ce-stable           28 k
     libcgroup                       x86_64          0.41-11.el7                         base                       65 k
     libseccomp                      x86_64          2.3.1-2.el7                         base                       56 k
     libsemanage-python              x86_64          2.5-5.1.el7_3                       update                    104 k
     policycoreutils-python          x86_64          2.5-11.el7_3                        update                    445 k
     python-IPy                      noarch          0.75-6.el7                          base                       32 k
     setools-libs                    x86_64          3.3.8-1.1.el7                       base                      612 k

    Transaction Summary
    =====================================================================================================================
    Install  1 Package (+9 Dependent packages)

    Total download size: 20 M
    Installed size: 24 M
    Is this ok [y/d/N]: y
    Downloading packages:
    (1/10): audit-libs-python-2.6.5-3.el7_3.1.x86_64.rpm                                          |  70 kB  00:00:00
    (2/10): checkpolicy-2.5-4.el7.x86_64.rpm                                                      | 290 kB  00:00:00
    (3/10): libseccomp-2.3.1-2.el7.x86_64.rpm                                                     |  56 kB  00:00:00
    (4/10): libcgroup-0.41-11.el7.x86_64.rpm                                                      |  65 kB  00:00:00
    (5/10): policycoreutils-python-2.5-11.el7_3.x86_64.rpm                                        | 445 kB  00:00:00
    (6/10): setools-libs-3.3.8-1.1.el7.x86_64.rpm                                                 | 612 kB  00:00:00
    (7/10): libsemanage-python-2.5-5.1.el7_3.x86_64.rpm                                           | 104 kB  00:00:00
    (8/10): python-IPy-0.75-6.el7.noarch.rpm                                                      |  32 kB  00:00:00
    warning: /var/cache/yum/x86_64/7/docker-ce-stable/packages/docker-ce-selinux-17.03.1.ce-1.el7.centos.noarch.rpm: Header V4 RSA/SHA512 Signature, key ID 621e9f35: NOKEY
    Public key for docker-ce-selinux-17.03.1.ce-1.el7.centos.noarch.rpm is not installed
    (9/10): docker-ce-selinux-17.03.1.ce-1.el7.centos.noarch.rpm                                  |  28 kB  00:00:00
    (10/10): docker-ce-17.03.1.ce-1.el7.centos.x86_64.rpm                                         |  19 MB  00:00:00
    ---------------------------------------------------------------------------------------------------------------------
    Total                                                                                 23 MB/s |  20 MB  00:00:00
    Retrieving key from https://download.docker.com/linux/centos/gpg
    Importing GPG key 0x621E9F35:
     Userid     : "Docker Release (CE rpm) <docker@docker.com>"
     Fingerprint: 060a 61c5 1b55 8a7f 742b 77aa c52f eb6b 621e 9f35
     From       : https://download.docker.com/linux/centos/gpg
    Is this ok [y/N]: y
    Running transaction check
    Running transaction test
    Transaction test succeeded
    Running transaction
      Installing : libcgroup-0.41-11.el7.x86_64                                                                     1/10
      Installing : setools-libs-3.3.8-1.1.el7.x86_64                                                                2/10
      Installing : checkpolicy-2.5-4.el7.x86_64                                                                     3/10
      Installing : libsemanage-python-2.5-5.1.el7_3.x86_64                                                          4/10
      Installing : audit-libs-python-2.6.5-3.el7_3.1.x86_64                                                         5/10
      Installing : python-IPy-0.75-6.el7.noarch                                                                     6/10
      Installing : policycoreutils-python-2.5-11.el7_3.x86_64                                                       7/10
      Installing : docker-ce-selinux-17.03.1.ce-1.el7.centos.noarch                                                 8/10
    setsebool:  SELinux is disabled.
    libsemanage.semanage_direct_install_info: Overriding docker module at lower priority 100 with module at priority 400.
      Installing : libseccomp-2.3.1-2.el7.x86_64                                                                    9/10
      Installing : docker-ce-17.03.1.ce-1.el7.centos.x86_64                                                        10/10
      Verifying  : libseccomp-2.3.1-2.el7.x86_64                                                                    1/10
      Verifying  : python-IPy-0.75-6.el7.noarch                                                                     2/10
      Verifying  : audit-libs-python-2.6.5-3.el7_3.1.x86_64                                                         3/10
      Verifying  : libsemanage-python-2.5-5.1.el7_3.x86_64                                                          4/10
      Verifying  : docker-ce-selinux-17.03.1.ce-1.el7.centos.noarch                                                 5/10
      Verifying  : libcgroup-0.41-11.el7.x86_64                                                                     6/10
      Verifying  : policycoreutils-python-2.5-11.el7_3.x86_64                                                       7/10
      Verifying  : docker-ce-17.03.1.ce-1.el7.centos.x86_64                                                         8/10
      Verifying  : checkpolicy-2.5-4.el7.x86_64                                                                     9/10
      Verifying  : setools-libs-3.3.8-1.1.el7.x86_64                                                               10/10

    Installed:
      docker-ce.x86_64 0:17.03.1.ce-1.el7.centos

    Dependency Installed:
      audit-libs-python.x86_64 0:2.6.5-3.el7_3.1                    checkpolicy.x86_64 0:2.5-4.el7
      docker-ce-selinux.noarch 0:17.03.1.ce-1.el7.centos            libcgroup.x86_64 0:0.41-11.el7
      libseccomp.x86_64 0:2.3.1-2.el7                               libsemanage-python.x86_64 0:2.5-5.1.el7_3
      policycoreutils-python.x86_64 0:2.5-11.el7_3                  python-IPy.noarch 0:0.75-6.el7
      setools-libs.x86_64 0:3.3.8-1.1.el7

    Complete!
    ```

4. 启动

    ```
    $ sudo systemctl start docker
    ```

	或者

    ```
    $ sudo service docker start
    service docker start        #启动docker
    chkconfig docker on         #加入开机启动
    ```

5. 查看版本

    ```
    $ sudo docker version
    ```

	执行结果：

    ```
    Client:
     Version:      17.03.1-ce
     API version:  1.27
     Go version:   go1.7.5
     Git commit:   c6d412e
     Built:        Mon Mar 27 17:05:44 2017
     OS/Arch:      linux/amd64
    ```

6. 校验

    ```
    $ sudo docker run hello-world
    ```

	执行结果： 

    ```
    Unable to find image 'hello-world:latest' locally
    latest: Pulling from library/hello-world
    78445dd45222: Pull complete
    Digest: sha256:c5515758d4c5e1e838e9cd307f6c6a0d620b5e07e6f927b07d05f6d12a1ac8d7
    Status: Downloaded newer image for hello-world:latest

    Hello from Docker!
    This message shows that your installation appears to be working correctly.

    To generate this message, Docker took the following steps:
     1. The Docker client contacted the Docker daemon.
     2. The Docker daemon pulled the "hello-world" image from the Docker Hub.
     3. The Docker daemon created a new container from that image which runs the
        executable that produces the output you are currently reading.
     4. The Docker daemon streamed that output to the Docker client, which sent it
        to your terminal.

    To try something more ambitious, you can run an Ubuntu container with:
     $ docker run -it ubuntu bash

    Share images, automate workflows, and more with a free Docker ID:
     https://cloud.docker.com/

    For more examples and ideas, visit:
     https://docs.docker.com/engine/userguide/
    ```

7. 更改镜像源

    进入 `/etc/docker` 查看有没有 `daemon.json`，如果没有新建，有则修改。

    ![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412214650.png)

    ![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412214702.png)

    `daemon.json` 内容如下

    ```
    {
    "registry-mirrors":["https://pee6w651.mirror.aliyuncs.com"]
    }
    ```

	重启 `docker` 服务

	![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412214637.png)

### 1.1.run干了什么
![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412214901.png)