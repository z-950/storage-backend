### 背景
整体开发过程：前后端分离

[可选]nginx处理：静态文件，反代请求，负载均衡，ddos防护，cros，https

部署：单机部署（集群）

### 规范
- deploy service verticle name：packageName + Verticle
- service:
    - use `serviceImplFactory` to create a serviceImpl.
    - proxy service
    - deploy serviceRestApiVerticle
        - createHttpServer
        - publishHttpEndpoint
- name:
    - service api with `SERVICE_NAME = "$service-rest-api` field

### prerequisites
- openjdk 13+
- PostgreSQL 11.x

### gradle task list
in root project:
- startAll: stop all running verticles and start all verticles in background(need add `-Dcmd=start` in `Arguments`)
- list: execute common:list task
- stop: execute common:stop task
- clean: delete `build` and `log` dir

in pers.z950.codegen project:
- gen: generate `EBProxy` and `ProxyHandler` file, for service proxy

in common project:
- list: list running verticles
- stop: stop all running verticles
**notice**: `run` task will run under idea, so `stop` and `list` task cant work, but application can work normally.

in others sub project:
- run: run the main verticle use common.Launcher.
    - switch: cmd
        - run: default. run verticle frontend, only in this switch we can debug with breakpoint
        - start: run verticle background
- shadowJar: make shadowJar
- runJar: run jar in cmd

### run & debug: (in idea)
1. code generate
    - choose `backend:codegen` gradle project
    - choose `run` task
    - press `Run` button
2. start verticle
    - choose `backend:$subproject` gradle project (e.g. `backend:gateway`)
    - choose `run` task (with default switch `-Dcmd=start`)
    - press `Run` or `Debug` button

### deploy
// todo

### notice
#### using
- running in hazelcast cluster (vertx plugin)
- after reducing permissions, need restart the service

#### ignore warning
- hazelcast
    - [WARNING: Hazelcast is starting in a Java modular environment (Java 9 and newer) but without proper access to required Java packages.]
    - [WARNING: Illegal reflective access by com.hazelcast.internal.networking.nio.SelectorOptimizer to field sun.nio.ch.SelectorImpl.selectedKeys]
