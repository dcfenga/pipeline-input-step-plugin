Pipeline: Input Step Plugin

Extend the Jenkins Pipeline, add the Pipeline step input to wait for human input or
approval.

人工审批插件中整合了审批超时、发送邮件功能，所以部署该插件需要先部署: Time limit Plugin和Mailer
Plugin.

# 部署Time limit Plugin

参考：
[dcfenga/pipeline-time-limit-plugin](https://github.com/dcfenga/pipeline-time-limit-plugin)

# 部署Mailer Plugin

## 安装Mailer插件

进入Manage Jenkins -> Manage Plugins安装最新版本的Mailer Plugin, 当前测试的插件版本是1.20.

## 配置Email Server

进入Manage Jenkins -> Configure System. 首先在Jenkins Location处配置：

```
System Admin e-mail address：dcfenga@sina.com
```

其次在E-mail Notification处配置：

```
SMTP server: smtp.example.com 
Default user e-mail suffix: @example.com 
```

勾选Use SMTP Authentication，配置下面选项：

```
User Name: dcfenga@sina.com 
Password: yourpassword 
Charset: UTF-8
```

上述配置需要根据不同的环境配置成有效的数据。

# 部署Pipeline Input Step Plugin

## 构建Pipeline Input Step Plugin

```bash
mvn clean package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
```

## 卸载已经安装的Pipeline Input Step Plugin

- 进入Jenkins部署节点

  ```bash
  ssh kube@10.16.117.251
  ```

- 删除pipeline-input-step插件目录结构

  因为pipeline-input-step-plugin被Pipeline Stage View Plugin所依赖，所以没法在Jenkins管理插件模块中直接卸载。

  ```bash
  cd /var/lib/jenkins/plugins
  rm -rf pipeline-input-step* 
  ```

  删除的文件为：
  ```
  pipeline-input-step/
  pipeline-input-step.jpi
  ```

- 重启Jenkins

  ```bash
  service jenkins restart
  ```

## 安装Pipeline Input Step Plugin

以管理员账户登陆Jenkins, 进入下面页面进行安装：
```
Manage Jenkins -> Advanced -> Upload Plugin
```
同时勾选下面选项，让插件安装成功后Jenkins自动重启:

```
Restart Jenkins when installation is complete and no jobs are running
```

## 更新workflow-lib目录

将新开发的Global Variables：approval.groovy更新到当前Jenkins主目录下的workflow-libs/vars目录下，并修改配置：

```bash
vi approval.groovy 
// *********下面3向配置需要在系统部署后更改(共3项)************ 

//平台当前环境访问地址-官方网站 
def website = "http://dcfenga.example.com/" 

// 客服电话 
def serviceline = "15829296996" 

// 客服邮箱 
def mailbox = "dcfenga@sina.com"
```