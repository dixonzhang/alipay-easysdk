[![Latest Stable Version](https://poser.pugx.org/alipaysdk/easysdk/v/stable)](https://packagist.org/packages/alipaysdk/easysdk)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Falipay%2Falipay-easysdk.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Falipay%2Falipay-easysdk?ref=badge_shield)

欢迎使用 Alipay **Easy** SDK for PHP 。

Alipay Esay SDK for PHP让您不用复杂编程即可访支付宝开放平台开放的各项常用能力，SDK可以自动帮您满足能力调用过程中所需的证书校验、加签、验签、发送HTTP请求等非功能性要求。

下面向您介绍Alipay Easy SDK for PHP 的基本设计理念和使用方法。

## 设计理念
不同于原有的[Alipay SDK](https://openhome.alipay.com/doc/sdkDownload.resource?sdkType=PHP)通用而全面的设计理念，Alipay Easy SDK对开放能力的API进行了更加贴近高频场景的精心设计与裁剪，简化了服务端调用方式，让调用API像使用语言内置的函数一样简便。

Alipay Easy SDK提供了与[能力地图](https://opendocs.alipay.com/mini/00am3f)相对应的代码组织结构，让开发者可以快速找到不同能力对应的API。

Alipay Easy SDK主要目标是提升开发者在**服务端**集成支付宝开放平台开放的各类核心能力的效率。

## 环境要求
1. Alipay Easy SDK for PHP 需要配合`PHP 7.0`或其以上版本。

2. 使用 Alipay Easy SDK for PHP 之前 ，您需要先前往[支付宝开发平台-开发者中心](https://openhome.alipay.com/platform/developerIndex.htm)完成开发者接入的一些准备工作，包括创建应用、为应用添加功能包、设置应用的接口加签方式等。

3. 准备工作完成后，注意保存如下信息，后续将作为使用SDK的输入。

* 加签模式为公钥证书模式时（推荐）

`AppId`、`应用的私钥`、`应用公钥证书文件`、`支付宝公钥证书文件`、`支付宝根证书文件`

* 加签模式为公钥模式时

`AppId`、`应用的私钥`、`支付宝公钥`

## 安装依赖
### 通过[Composer](https://packagist.org/packages/alipaysdk/easysdk/)在线安装依赖（推荐）

`composer require alipaysdk/easysdk:1.0.0`

### 本地手动集成依赖（适用于自己修改源码后的本地重新打包安装）
1. 本机安装配置[Composer](https://getcomposer.org/)工具。
2. 在本`README.md`所在目录下，执行`composer install`，下载SDK依赖。
3. 依赖文件会下载到`vendor`目录下。

## 快速使用
以下这段代码示例向您展示了使用Alipay Easy SDK for PHP调用一个API的3个主要步骤：

1. 设置参数（全局只需设置一次）。
2. 发起API调用。
3. 处理响应或异常。

```php
<?php

class Client
{
    public function __construct($name = null, array $data = [], $dataName = '')
    {
        parent::__construct($name, $data, $dataName);
        //1、设置参数（全局只需设置一次）
        $account = new Account();
        Factory::setOptions($account->getTestAccount());
    }

    public function handle()
    {
        //2、发起API调用（以支付能力下的统一收单交易创建接口为例）
        $result = Factory::payment()->common()->create("Iphone6 16G",
            microtime(), "88.88", "2088002656718920");
        //3、处理响应或异常
        if (!empty($result['code']) && $result['code'] == 10000) {
            echo "成功";
        } else {
            echo "失败";
        }
    }
}


<?php

class Account
{
    public function getCertAccount()
    {
        $options = new Config();
        $options->protocol = 'https';
        $options->gatewayHost = 'openapi.alipay.com';
        $options->signType = 'RSA2';
        
        $options->appId = '<-- 请填写您的appId，例如：2019051064521003 -->';
        
        // 为避免私钥随源码泄露，推荐从文件中读取私钥字符串而不是写入源码中
        $options->merchantPrivateKey = $this->getPrivateKey($options->appId);
        
        $options->alipayCertPath = '<-- 请填写您的支付宝公钥证书文件路径，例如：/foo/alipayCertPublicKey_RSA2.crt -->';
        $options->alipayRootCertPath = '<-- 请填写您的支付宝根证书文件路径，例如：/foo/alipayRootCert.crt" -->';
        $options->merchantCertPath = '<-- 请填写您的应用公钥证书文件路径，例如：/foo/appCertPublicKey_2019051064521003.crt -->';
        
        return $options;
    }

    private function getPrivateKey($appId)
    {
        $filePath = '/foo/privateKey.json';
        $stream = fopen($filePath, 'r');
        fwrite($stream, '$filePath');
        $result = json_decode(stream_get_contents($stream));
        return $result->$appId;
    }
}
```

## API组织规范
在Alipay Easy SDK中，API的引用路径与能力地图的组织层次一致，遵循如下规范

> Factory::能力名称()->场景名称()->接口方法名称( ... )

比如，如果您想要使用[能力地图](https://opendocs.alipay.com/mini/00am3f)中`营销能力`下的`模板消息`场景中的`小程序发送模板消息`，只需按如下形式编写调用代码即可（不同编程语言的连接符号可能不同）。

`Factory::marketing()->templateMessage()->send( ... )`

其中，接口方法名称通常是对其依赖的OpenAPI功能的一个最简概况，接口方法的出入参与OpenAPI中同名参数含义一致，可参照OpenAPI相关参数的使用说明。

Alipay Easy SDK将致力于保持良好的API命名，以符合开发者的编程直觉。
## 已支持的API列表
| 能力类别      | 场景类别            | 接口方法名称                 | 调用的OpenAPI名称                                              |
|-----------|-----------------|------------------------|-----------------------------------------------------------|
| Base      | OAuth           | getToken               | alipay\.system\.oauth\.token                              |
| Base      | OAuth           | refreshToken           | alipay\.system\.oauth\.token                              |
| Base      | Qrcode          | create                 | alipay\.open\.app\.qrcode\.create                         |
| Base      | Image           | upload                 | alipay\.offline\.material\.image\.upload                  |
| Base      | Video           | upload                 | alipay\.offline\.material\.image\.upload                  |
| Member    | Identification  | init                   | alipay\.user\.certify\.open\.initialize                   |
| Member    | Identification  | certify                | alipay\.user\.certify\.open\.certify                      |
| Member    | Identification  | query                  | alipay\.user\.certify\.open\.query                        |
| Payment   | Common          | create                 | alipay\.trade\.create                                     |
| Payment   | Common          | query                  | alipay\.trade\.query                                      |
| Payment   | Common          | refund                 | alipay\.trade\.refund                                     |
| Payment   | Common          | close                  | alipay\.trade\.close                                      |
| Payment   | Common          | cancel                 | alipay\.trade\.close                                      |
| Payment   | Huabei          | create                 | alipay\.trade\.create                                     |
| Payment   | FaceToFace      | pay                    | alipay\.trade\.pay                                        |
| Security  | TextRisk        | detect                 | alipay\.security\.risk\.content\.detect                   |
| Marketing | Pass            | createTemplate         | alipay\.pass\.template\.add                               |
| Marketing | Pass            | updateTemplate         | alipay\.pass\.template\.update                            |
| Marketing | Pass            | addInstance            | alipay\.pass\.instance\.add                               |
| Marketing | Pass            | updateInstance         | alipay\.pass\.instance\.update                            |
| Marketing | TemplateMessage | send                   | alipay\.open\.app\.mini\.templatemessage\.send            |
| Marketing | OpenLife        | createImageTextContent | alipay\.open\.public\.message\.content\.create            |
| Marketing | OpenLife        | modifyImageTextContent | alipay\.open\.public\.message\.content\.modify            |
| Marketing | OpenLife        | sendText               | alipay\.open\.public\.message\.total\.send                |
| Marketing | OpenLife        | sendImageText          | alipay\.open\.public\.message\.total\.send                |
| Marketing | OpenLife        | sendSingleMessage      | alipay\.open\.public\.message\.single\.send               |
| Marketing | OpenLife        | recallMessage          | alipay\.open\.public\.life\.msg\.recall                   |
| Marketing | OpenLife        | setIndustry            | alipay\.open\.public\.template\.message\.industry\.modify |
| Marketing | OpenLife        | getIndustry            | alipay\.open\.public\.setting\.category\.query            |

> 注：更多高频场景的API持续更新中，敬请期待。

## 文档
[API Doc](./../APIDoc.md)

[Alipay Easy SDK](./../README.md)
