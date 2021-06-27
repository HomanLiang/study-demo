[toc]



# 登录接口

## 1.安全风险

### 1.1.暴力破解！

只要网站是暴露在公网的，那么很大概率上会被人盯上，尝试爆破这种简单且有效的方式：
**通过各种方式获得了网站的用户名之后，通过编写程序来遍历所有可能的密码，直至找到正确的密码为止**

*伪代码如下：*

```
# 密码字典  
password_dict = []  
# 登录接口  
login_url = ''  
def attack(username):  
 for password in password_dict:  
     data = {'username': username, 'password': password}  
       content = requests.post(login_url, data).content.decode('utf-8')  
       if 'login success' in content:  
           print('got it! password is : %s' % password)  
```

那么这种情况，我们要怎么防范呢？

#### 1.1.1.验证码

有聪明的同学就想到了，我可以在它密码错误达到一定次数时，增加验证码校验！比如我们设置，当用户密码错误达到3次之后，则需要用户输入图片验证码才可以继续登录操作：

*伪代码如下：*

```
fail_count = get_from_redis(fail_username)  
if fail_count >= 3:  
 if captcha is None:  
  return error('需要验证码')  
    check_captcha(captcha)  
success = do_login(username, password)  
if not success:  
 set_redis(fail_username, fail_count + 1)  
```

> 伪代码未考虑并发，实际开发可以考虑加锁。

这样确实可以过滤掉一些非法的攻击，但是以目前的OCR技术来说的话，普通的图片验证码真的很难做到有效的防止机器人（*我们就在这个上面吃过大亏*）。

当然，我们也可以花钱购买类似于三方公司提供的滑动验证等验证方案，但是也并不是100%的安全，一样可以被破解（*惨痛教训*）。

#### 1.1.2.登录限制

那这时候又有同学说了，那我可以直接限制非正常用户的登录操作，当它密码错误达到一定次数时，直接拒绝用户的登录，隔一段时间再恢复。比如我们设置某个账号在登录时错误次数达到10次时，则5分钟内拒绝该账号的所有登录操作。

*伪代码如下：*

```
fail_count = get_from_redis(fail_username)  
locked = get_from_redis(lock_username)  
  
if locked:  
 return error('拒绝登录')  
if fail_count >= 3:  
 if captcha is None:  
  return error('需要验证码')  
    check_captcha(captcha)   
success = do_login(username, password)  
if not success:  
 set_redis(fail_username, fail_count + 1)  
    if fail_count + 1 >= 10:  
     # 失败超过10次，设置锁定标记  
     set_redis(lock_username, true, 300s)  
```

umm，这样确实可以解决用户密码被爆破的问题。但是，这样会带来另一个风险：攻击者虽然不能获取到网站的用户信息，但是它可以让我们网站所有的用户都无法登录！

攻击者只需要无限循环遍历所有的用户名（*即使没有，随机也行*）进行登录，那么这些用户会永远处于锁定状态，导致正常的用户无法登录网站！

#### 1.1.3.IP限制

那既然直接针对用户名不行的话，我们可以针对IP来处理，直接把攻击者的IP封了不就万事大吉了嘛。我们可以设定某个IP下调用登录接口错误次数达到一定时，则禁止该IP进行登录操作。

*伪代码如下：*

```
ip = request['IP']  
fail_count = get_from_redis(fail_ip)  
if fail_count > 10:  
 return error('拒绝登录')  
# 其它逻辑  
# do something()  
success = do_login(username, password)  
if not success:  
 set_redis(fail_ip, true, 300s)  
```

这样也可以一定程度上解决问题，事实上有很多的限流操作都是针对IP进行的，比如niginx的限流模块就可以限制一个IP在单位时间内的访问次数。

但是这里还是存在问题：

- 比如现在很多学校、公司都是使用同一个出口IP，如果直接按IP限制，可能会误杀其它正常的用户
- 现在这么多VPN，攻击者完全可以在IP被封后切换VPN来攻击

#### 1.1.4.手机验证

那难道就没有一个比较好的方式来防范吗？　当然有。　我们可以看到近些年来，几乎所有的应用都会让用户绑定手机，一个是国家的实名制政策要求，第二个是手机基本上和身份证一样，基本上可以代表一个人的身份标识了。所以很多安全操作都是基于手机验证来进行的，登录也可以。

1. 当用户输入密码次数大于3次时，要求用户输入验证码（*最好使用滑动验证*）
2. 当用户输入密码次数大于10次时，弹出手机验证，需要用户使用手机验证码和密码双重认证进行登录

> 手机验证码防刷就是另一个问题了，这里不展开，以后再有时间再聊聊我们在验证码防刷方面做了哪些工作。

*伪代码如下：*

```
fail_count = get_from_redis(fail_username)  
  
if fail_count > 3:  
 if captcha is None:  
  return error('需要验证码')  
    check_captcha(captcha)   
      
if fail_count > 10:  
 # 大于10次，使用验证码和密码登录  
 if dynamic_code is None:  
     return error('请输入手机验证码')  
    if not validate_dynamic_code(username, dynamic_code):  
     delete_dynamic_code(username)  
     return error('手机验证码错误')  
  
 success = do_login(username, password, dynamic_code)  
      
 if not success:  
     set_redis(fail_username, fail_count + 1)  
```

我们结合了上面说的几种方式的同时，加上了手机验证码的验证模式，基本上可以阻止相当多的一部分恶意攻击者。但是没有系统是绝对安全的，我们只能够尽可能的增加攻击者的攻击成本。大家可以根据自己网站的实际情况来选择合适的策略。

### 1.2.中间人攻击？

#### 1.2.1.什么是中间人攻击

**中间人攻击(man-in-the-middle attack, abbreviated to MITM)**，简单一点来说就是，A和B在通讯过程中，攻击者通过嗅探、拦截等方式获取或修改A和B的通讯内容。

举个栗子：`小白`给`小黄`发快递，途中要经过快递点A，`小黑`就躲在快递点A，或者干脆自己开一个快递点B来冒充快递点A。然后偷偷的拆了`小白`给`小黄`的快递，看看里面有啥东西。甚至可以把`小白`的快递给留下来，自己再打包一个一毛一样的箱子发给`小黄`。

那在登录过程中，如果攻击者在嗅探到了从客户端发往服务端的登录请求，就可以很轻易的获取到用户的用户名和密码。

#### 1.2.2.HTTPS

防范中间人攻击最简单也是最有效的一个操作，更换HTTPS，把网站中所有的HTTP请求修改为强制使用HTTPS。

**为什么HTTPS可以防范中间人攻击？**

HTTPS实际上就是在HTTP和TCP协议中间加入了SSL/TLS协议，用于保障数据的安全传输。相比于HTTP，HTTPS主要有以下几个特点：

- 内容加密
- 数据完整性
- 身份验证

> 具体的HTTPS原理这里就不再扩展了，大家可以自行Google

#### 1.2.3.加密传输

在HTTPS之外，我们还可以手动对敏感数据进行加密传输：

- 用户名可以在客户端使用非对称加密，在服务端解密
- 密码可以在客户端进行MD5之后传输，防止暴露密码明文

## 2.其它

除了上面我们聊的这些以外，其实还有很多其它的工作可以考虑，比如：

- **操作日志**，用户的每次登录和敏感操作都需要记录日志（包括IP、设备等）
- **异常操作或登录提醒**，有了上面的操作日志，那我们就可以基于日志做风险提醒，比如用户在进行非常登录地登录、修改密码、登录异常时，可以短信提醒用户
- **拒绝弱密码** 注册或修改密码时，不允许用户设置弱密码
- **防止用户名被遍历** 有些网站在注册时，在输入完用户名之后，会提示用户名是否存在。这样会存在网站的所有用户名被泄露的风险（*遍历该接口即可*），需要在交互或逻辑上做限制