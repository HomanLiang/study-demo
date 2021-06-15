[toc]



# 企业内部应用开发

## 钉钉API在线调试工具

1. [后端API调试工具](https://open-dev.dingtalk.com/apiExplorer)
   - [获取企业凭证](https://open-dev.dingtalk.com/apiExplorer#/?devType=org&api=dingtalk.oapi.gettoken)
   - [通过免登码获取用户信息(v2)](https://open-dev.dingtalk.com/apiExplorer#/?devType=org&api=dingtalk.oapi.v2.user.getuserinfo) 
   - [根据userid获取用户详情](https://open-dev.dingtalk.com/apiExplorer#/?devType=org&api=dingtalk.oapi.v2.user.get)
2. [前端API模拟工具](https://open-dev.dingtalk.com/apiExplorer#/jsapi)
   - [获取微应用免登授权码](https://open-dev.dingtalk.com/apiExplorer#/jsapi?api=runtime.permission.requestAuthCode)

## 常用文档说明

[企业内部应用免登](https://developers.dingtalk.com/document/app/enterprise-internal-application-logon-free)

[获取企业内部应用的access_token](https://developers.dingtalk.com/document/app/obtain-orgapp-token)

[通过免登码获取用户信息](https://developers.dingtalk.com/document/app/obtain-the-userid-of-a-user-by-using-the-log-free)

[根据userid获取用户详情](https://developers.dingtalk.com/document/app/query-user-details)

[基础概念](https://developers.dingtalk.com/document/app/basic-concepts)

[通讯录事件--事件订阅](https://developers.dingtalk.com/document/app/address-book-events)

[快速入门--开发小程序](https://developers.dingtalk.com/document/app/develop-org-mini-programs)

[错误码](https://developers.dingtalk.com/document/app/server-api-error-codes-1)



CorpId: ding604330ee9b78aaf035c2f4657eb6378f

AppKey：dingw40grbemzwynvhte

AppSecret：lIp_4cEMKMrj0El9bFOBZBpKRtyBggnR1FZ3ITsFNaiYemeouzdhRVBRIrdYQTnF



登录接口
	获取access_token
	通过免登陆码获取用户
	通过user_id判断是否能登录
		登录成功--返回用户信息（token）
		登录失败--返回绑定用户异常
绑定用户接口
	用户名，密码登录接口
		登录成功自动绑定钉钉user_id
		
鉴权方法
	通过拦截器处理
	token获取