[toc]



# JWT

## JWT是什么

`JSON Web Token`（`JWT`） 定义了一种紧凑且自包含的方式，用于在各方之间作为 JSON 对象安全地传输信息。该信息可以被验证和信任，因为它是经过数字签名的。`JWT`可以设置有效期。

`JWT`是一个很长的字符串，包含了`Header`，`Playload`和`Signature`三部分内容，中间用`.`进行分隔。

**Headers**

`Headers`部分描述的是`JWT`的基本信息，一般会包含签名算法和令牌类型，数据如下：

```json
{
    "alg": "RS256",
    "typ": "JWT"
}
```

**Playload**

`Playload`就是存放有效信息的地方，`JWT`规定了以下7个字段，建议但不强制使用：

```properties
iss: jwt签发者
sub: jwt所面向的用户
aud: 接收jwt的一方
exp: jwt的过期时间，这个过期时间必须要大于签发时间
nbf: 定义在什么时间之前，该jwt都是不可用的
iat: jwt的签发时间
jti: jwt的唯一身份标识，主要用来作为一次性token
```

除此之外，我们还可以自定义内容

```json
{
    "name":"Java旅途",
    "age":18
}
```

**Signature**

`Signature`是将`JWT`的前面两部分进行加密后的字符串，将`Headers`和`Playload`进行`base64`编码后使用`Headers`中规定的加密算法和密钥进行加密，得到`JWT`的第三部分。

## JWT生成和解析token

在应用服务中引入`JWT`的依赖

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.9.0</version>
</dependency>
```

根据`JWT`的定义生成一个使用`RSA`算法加密的，有效期为`30分钟`的token

```java
public static String createToken(User user) throws Exception{

    return Jwts.builder()
        .claim("name",user.getName())
        .claim("age",user.getAge())
        // rsa加密
        .signWith(SignatureAlgorithm.RS256, RsaUtil.getPrivateKey(PRIVATE_KEY))
        // 有效期30分钟
        .setExpiration(DateTime.now().plusSeconds(30 * 60).toDate())
        .compact();
}
```

登录接口验证通过后，调用`JWT`生成带有用户标识的token响应给用户，在接下来的请求中，头部携带`token`进行验签，验签通过后，正常访问应用服务。

```java
public static Claims parseToken(String token) throws Exception{
    return Jwts
        .parser()
        .setSigningKey(RsaUtil.getPublicKey(PUBLIC_KEY))
        .parseClaimsJws(token)
        .getBody();
}
```

## token续签问题

上面讲述了关于`JWT`验证的过程，现在我们考虑这样一个问题，客户端携带`token`访问下单接口，`token`验签通过，客户端下单成功，返回下单结果，然后客户端带着`token`调用支付接口进行支付，验签的时候发现token失效了，这时候应该怎么办？只能告诉用户`token`失效，然后让用户重新登录获取`token`？这种体验是非常不好的，`oauth2`在这方面做的比较好，除了签发`token`，还会签发`refresh_token`，当`token`过期后，会去调用`refresh_token`重新获取`token`，如果`refresh_token`也过期了，那么再提示用户去登录。现在我们模拟`oauth2`的实现方式来完成`JWT`的`refresh_token`。

思路大概就是用户登录成功后，签发`token`的同时，生成一个加密串作为`refresh_token`，`refresh_token`存放在`redis`中，设置合理的过期时间（一般会将`refresh_token`的过期时间设置的比较久一点）。然后将`token`和`refresh_token`响应给客户端。伪代码如下：

```java
@PostMapping("getToken")
public ResultBean getToken(@RequestBody LoingUser user){

    ResultBean resultBean = new ResultBean();
    // 用户信息校验失败，响应错误
    if(!user){
        resultBean.fillCode(401,"账户密码不正确");
        return resultBean;
    }
    String token = null;
    String refresh_token = null;
    try {
        // jwt 生成的token
        token = JwtUtil.createToken(user);
        // 刷新token
        refresh_token = Md5Utils.hash(System.currentTimeMillis()+"");
        // refresh_token过期时间为24小时
        redisUtils.set("refresh_token:"+refresh_token,token,30*24*60*60);
    } catch (Exception e) {
        e.printStackTrace();
    }

    Map<String,Object> map = new HashMap<>();
    map.put("access_token",token);
    map.put("refresh_token",refresh_token);
    map.put("expires_in",2*60*60);
    resultBean.fillInfo(map);
    return resultBean;
}
```

客户端调用接口时，在请求头中携带`token`，在拦截器中拦截请求，验证`token`的有效性，如果验证`token`失败，则去redis中判断是否是`refresh_token`的请求，如果`refresh_token`验证也失败，则给客户端响应鉴权异常，提示客户端重新登录，伪代码如下：

```java
HttpHeaders headers = request.getHeaders();
// 请求头中获取令牌
String token = headers.getFirst("Authorization");
// 判断请求头中是否有令牌
if (StringUtils.isEmpty(token)) {
    resultBean.fillCode(401,"鉴权失败，请携带有效token");
    return resultBean;
}
if(!token.contains("Bearer")){
    resultBean.fillCode(401,"鉴权失败，请携带有效token");
    return resultBean;
}

token = token.replace("Bearer ","");
// 如果请求头中有令牌则解析令牌
try {
    Claims claims = TokenUtil.parseToken(token).getBody();
} catch (Exception e) {
    e.printStackTrace();
    String refreshToken = redisUtils.get("refresh_token:" + token)+"";
    if(StringUtils.isBlank(refreshToken) || "null".equals(refreshToken)){
        resultBean.fillCode(403,"refresh_token已过期，请重新获取token");
        return resultbean;
    }
}
```

`refresh_token`来换取`token`的伪代码如下：

```java
@PostMapping("refreshToken")
public Result refreshToken(String token){

    ResultBean resultBean = new ResultBean();
    String refreshToken = redisUtils.get(TokenConstants.REFRESHTOKEN + token)+"";
    String access_token = null;
    try {
        Claims claims = JwtUtil.parseToken(refreshToken);
        String username = claims.get("username")+"";
        String password = claims.get("password")+"";
        LoginUser loginUser = new LoginUser();
        loginUser.setUsername(username);
        loginUser.setPassword(password);
        access_token = JwtUtil.createToken(loginUser);
    } catch (Exception e) {
        e.printStackTrace();
    }
    Map<String,Object> map = new HashMap<>();
    map.put("access_token",access_token);
    map.put("refresh_token",token);
    map.put("expires_in",30*60);
    resultBean.fillInfo(map);
    return resultBean;
}
```

通过上面的分析，我们简单的实现了`token`的签发，验签以及续签问题，`JWT`作为一个轻量级的鉴权框架，使用起来非常方便，但是也会存在一些问题，

- `JWT`的`Playload`部分只是经过base64编码，这样我们的信息其实就完全暴露了，一般不要将敏感信息存放在`JWT`中。
- `JWT`生成的`token`比较长，每次在请求头中携带`token`，导致请求偷会比较大，有一定的性能问题。
- `JWT`生成后，服务端无法废弃，只能等待`JWT`主动过期。

下面这段是我网上看到的一段关于`JWT`比较适用的场景:

- 有效期短
- 只希望被使用一次

比如，用户注册后发一封邮件让其激活账户，通常邮件中需要有一个链接，这个链接需要具备以下的特性：能够标识用户，该链接具有时效性（通常只允许几小时之内激活），不能被篡改以激活其他可能的账户，一次性的。这种场景就适合使用`JWT`。