/**
 * Alipay.com Inc. Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.alipay.easysdk.kernel;

import com.alipay.easysdk.kernel.util.JsonUtil;
import com.alipay.easysdk.kernel.util.MultipartUtil;
import com.alipay.easysdk.kernel.util.PageUtil;
import com.alipay.easysdk.kernel.util.SignContentExtractor;
import com.alipay.easysdk.kernel.util.Signer;
import com.aliyun.tea.NameInMap;
import com.aliyun.tea.TeaModel;
import com.aliyun.tea.TeaResponse;
import com.aliyun.tea.Validation;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Tea DSL所需的BaseClient，实现了DSL中定义的虚拟函数
 * <p>
 * 注：以_开头的函数即实现的虚拟函数
 *
 * @author zhongyu
 * @version $Id: BaseClient.java, v 0.1 2019年12月17日 10:53 PM zhongyu Exp $
 */
public class BaseClient {
    /**
     * SHA256WithRSA签名器
     */
    private final Signer signer = new Signer();

    /**
     * 签名原文提取器
     */
    private final SignContentExtractor signContentExtractor = new SignContentExtractor();

    /**
     * 客户端配置参数
     */
    private Map<String, Object> config;

    /**
     * 证书模式运行时环境
     */
    private CertEnvironment certEnvironment;

    /**
     * 通过参数配置初始化客户端
     * <p>
     * 如果参数中配置了证书相关参数，需在此时初始化证书运行时环境对象，缓存证书相关上下文
     *
     * @param config 参数集合
     */
    protected BaseClient(Map<String, Object> config) {
        this.config = config;
        Preconditions.checkArgument(AlipayConstants.RSA2.equals(getConfig(AlipayConstants.SIGN_TYPE_CONFIG_KEY)),
                "Alipay Easy SDK只允许使用RSA2签名方式，RSA签名方式由于安全性相比RSA2弱已不再推荐。");

        if (!Strings.isNullOrEmpty(getConfig(AlipayConstants.ALIPAY_CERT_PATH_CONFIG_KEY))) {
            certEnvironment = new CertEnvironment(
                    getConfig(AlipayConstants.MERCHANT_CERT_PATH_CONFIG_KEY),
                    getConfig(AlipayConstants.ALIPAY_CERT_PATH_CONFIG_KEY),
                    getConfig(AlipayConstants.ALIPAY_ROOT_CERT_PATH_CONFIG_KEY));
        }
    }

    /**
     * 读取配置
     *
     * @param key 配置的Key值
     * @return 配置的Value值
     */
    protected String _getConfig(String key) {
        return getConfig(key);
    }

    private String getConfig(String key) {
        return (String) config.get(key);
    }

    /**
     * 获取时间戳，格式yyyy-MM-dd HH:mm:ss
     *
     * @return 当前时间戳
     */
    protected String _getTimestamp() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return df.format(new Date());
    }

    /**
     * 将业务参数和其他额外文本参数按www-form-urlencoded格式转换成HTTP Body中的字节数组，注意要做URL Encode
     *
     * @param bizParams  业务参数
     * @param textParams 其他额外文本参数
     * @return HTTP Body中的字节数组
     */
    protected byte[] _toUrlEncodedRequestBody(Map<String, Object> bizParams, Map<String, String> textParams) throws Exception {
        Map<String, String> sortedMap = getSortedMap(Collections.<String, String>emptyMap(), bizParams, textParams);
        return buildQueryString(sortedMap).getBytes(AlipayConstants.DEFAULT_CHARSET);
    }

    private String buildQueryString(Map<String, String> sortedMap) throws UnsupportedEncodingException {
        StringBuilder content = new StringBuilder();
        int index = 0;
        for (Entry<String, String> pair : sortedMap.entrySet()) {
            if (!Strings.isNullOrEmpty(pair.getKey()) && !Strings.isNullOrEmpty(pair.getValue())) {
                content.append(index == 0 ? "" : "&")
                        .append(pair.getKey())
                        .append("=")
                        .append(URLEncoder.encode(pair.getValue(), AlipayConstants.DEFAULT_CHARSET.name()));
                index++;
            }
        }
        return content.toString();
    }

    /**
     * 生成随机分界符，用于multipart格式的HTTP请求Body的多个字段间的分隔
     *
     * @return 随机分界符
     */
    protected String _getRandomBoundary() {
        return System.currentTimeMillis() + "";
    }

    /**
     * 字符串拼接
     *
     * @param a 字符串a
     * @param b 字符串b
     * @return 字符串a和b拼接后的字符串
     */
    protected String _concatStr(String a, String b) {
        return a + b;
    }

    /**
     * 将其他额外文本参数和文件参数按multipart/form-data格式转换成HTTP Body中的字节数组流
     *
     * @param textParams 其他额外文本参数
     * @param fileParams 业务文件参数
     * @param boundary   HTTP Body中multipart格式的分隔符
     * @return HTTP Body中的字节数组流
     */
    protected InputStream _toMultipartRequestBody(Map<String, String> textParams, Map<String, String> fileParams,
                                                  String boundary) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        //组装其他额外文本参数
        for (Map.Entry<String, String> pair : textParams.entrySet()) {
            if (!Strings.isNullOrEmpty(pair.getKey()) && !Strings.isNullOrEmpty(pair.getValue())) {
                stream.write(MultipartUtil.getEntryBoundary(boundary));
                stream.write(MultipartUtil.getTextEntry(pair.getKey(), pair.getValue()));
            }
        }

        //组装文件参数
        for (Entry<String, String> pair : fileParams.entrySet()) {
            if (!Strings.isNullOrEmpty(pair.getKey()) && pair.getValue() != null) {
                stream.write(MultipartUtil.getEntryBoundary(boundary));
                stream.write(MultipartUtil.getFileEntry(pair.getKey(), pair.getValue()));
                stream.write(Files.toByteArray(new File(pair.getValue())));
            }
        }

        //添加结束标记
        stream.write(MultipartUtil.getEndBoundary(boundary));

        return new ByteArrayInputStream(stream.toByteArray());
    }

    /**
     * 计算签名，注意要去除key或value为null的键值对
     *
     * @param systemParams 系统参数集合
     * @param bizParams    业务参数集合
     * @param textParams   其他额外文本参数集合
     * @param privateKey   私钥
     * @return 签名值的Base64串
     */
    protected String _sign(Map<String, String> systemParams, Map<String, Object> bizParams,
                           Map<String, String> textParams, String privateKey) throws Exception {
        Map<String, String> sortedMap = getSortedMap(systemParams, bizParams, textParams);

        StringBuilder content = new StringBuilder();
        int index = 0;
        for (Entry<String, String> pair : sortedMap.entrySet()) {
            if (!Strings.isNullOrEmpty(pair.getKey()) && !Strings.isNullOrEmpty(pair.getValue())) {
                content.append(index == 0 ? "" : "&").append(pair.getKey()).append("=").append(pair.getValue());
                index++;
            }
        }
        return signer.sign(content.toString(), privateKey);
    }

    private Map<String, String> getSortedMap(Map<String, String> systemParams, Map<String, Object> bizParams,
                                             Map<String, String> textParams) throws Exception {
        Map<String, String> sortedMap = new TreeMap<>(systemParams);
        if (!bizParams.isEmpty()) {
            sortedMap.put(AlipayConstants.BIZ_CONTENT_FIELD, JsonUtil.toJsonString(bizParams));
        }
        if (textParams != null) {
            sortedMap.putAll(textParams);
        }
        return sortedMap;
    }

    /**
     * 将网关响应发序列化成Map，同时将API的接口名称和响应原文插入到响应Map的method和body字段中
     *
     * @param response HTTP响应
     * @param method   调用的OpenAPI的接口名称
     * @return 响应反序列化的Map
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> _readAsJson(TeaResponse response, String method) throws Exception {
        String responseBody = response.getResponseBody();
        Map map = new Gson().fromJson(responseBody, Map.class);
        map.put(AlipayConstants.BODY_FIELD, responseBody);
        map.put(AlipayConstants.METHOD_FIELD, method);
        return map;
    }

    /**
     * 验证签名
     *
     * @param respMap         响应Map，可以从中提取出sign和body
     * @param alipayPublicKey 支付宝公钥
     * @return true：验签通过；false：验签不通过
     */
    protected boolean _verify(Map<String, Object> respMap, String alipayPublicKey) {
        String sign = (String) respMap.get(AlipayConstants.SIGN_FIELD);
        String content = signContentExtractor.getSignSourceData((String) respMap.get(AlipayConstants.BODY_FIELD),
                (String) respMap.get(AlipayConstants.METHOD_FIELD));
        return signer.verify(content, sign, alipayPublicKey);
    }

    /**
     * 获取商户应用公钥证书序列号，从证书模式运行时环境对象中直接读取
     *
     * @return 商户应用公钥证书序列号
     */
    protected String _getMerchantCertSN() {
        if (certEnvironment == null) {
            return null;
        }
        return certEnvironment.getMerchantCertSN();
    }

    /**
     * 从响应Map中提取返回值对象的Map，并将响应原文插入到body字段中
     *
     * @param respMap 响应Map
     * @return 返回值对象Map
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> _toRespModel(Map<String, Object> respMap) {
        String methodName = (String) respMap.get(AlipayConstants.METHOD_FIELD);
        String responseNodeName = methodName.replace('.', '_') + AlipayConstants.RESPONSE_SUFFIX;
        String errorNodeName = AlipayConstants.ERROR_RESPONSE;

        //先找正常响应节点
        for (Map.Entry<String, Object> pair : respMap.entrySet()) {
            if (responseNodeName.equals(pair.getKey())) {
                Map<String, Object> model = (Map<String, Object>) pair.getValue();
                model.put(AlipayConstants.BODY_FIELD, respMap.get(AlipayConstants.BODY_FIELD));
                return model;
            }
        }

        //再找异常响应节点
        for (Map.Entry<String, Object> pair : respMap.entrySet()) {
            if (errorNodeName.equals(pair.getKey())) {
                Map<String, Object> model = (Map<String, Object>) pair.getValue();
                model.put(AlipayConstants.BODY_FIELD, respMap.get(AlipayConstants.BODY_FIELD));
                return model;
            }
        }

        throw new RuntimeException("响应格式不符合预期，找不到" + responseNodeName + "或" + errorNodeName + "节点");
    }

    /**
     * 获取支付宝根证书序列号，从证书模式运行时环境对象中直接读取
     *
     * @return 支付宝根证书序列号
     */
    protected String _getAlipayRootCertSN() {
        if (certEnvironment == null) {
            return null;
        }
        return certEnvironment.getRootCertSN();
    }

    /**
     * 是否是证书模式
     *
     * @return true：是；false：不是
     */
    protected boolean _isCertMode() {
        return certEnvironment != null;
    }

    /**
     * 从响应Map中提取支付宝公钥证书序列号
     *
     * @param respMap 响应Map
     * @return 支付宝公钥证书序列号
     */
    protected String _getAlipayCertSN(Map<String, Object> respMap) {
        return (String) respMap.get(AlipayConstants.ALIPAY_CERT_SN_FIELD);
    }

    /**
     * 获取支付宝公钥，从证书运行时环境对象中直接读取
     * <p>
     * 如果缓存的用户指定的支付宝公钥证书的序列号与网关响应中携带的支付宝公钥证书序列号不一致，需要报错给出提示或自动更新支付宝公钥证书
     *
     * @param alipayCertSN 网关响应中携带的支付宝公钥证书序列号
     * @return 支付宝公钥
     */
    protected String _extractAlipayPublicKey(String alipayCertSN) {
        if (certEnvironment == null) {
            return null;
        }
        return certEnvironment.getAlipayPublicKey(alipayCertSN);
    }

    /**
     * 生成页面类请求所需URL或Form表单
     *
     * @param method       GET或POST，决定是生成URL还是Form表单
     * @param systemParams 系统参数集合
     * @param bizParams    业务参数集合
     * @param textParams   其他额外文本参数集合
     * @param sign         所有参数的签名值
     * @return 生成的URL字符串或表单
     */
    protected String _generatePage(String method, Map<String, String> systemParams, Map<String, Object> bizParams,
                                   Map<String, String> textParams, String sign) throws Exception {
        if (AlipayConstants.GET.equalsIgnoreCase(method)) {
            //采集并排序所有参数
            Map<String, String> sortedMap = getSortedMap(systemParams, bizParams, textParams);
            sortedMap.put(AlipayConstants.SIGN_FIELD, sign);

            //将所有参数置于URL中
            return getGatewayServerUrl() + "?" + buildQueryString(sortedMap);
        } else if (AlipayConstants.POST.equalsIgnoreCase(method)) {
            //将系统参数排序后置于URL中
            Map<String, String> urlParams = new TreeMap<>(systemParams);
            urlParams.put(AlipayConstants.SIGN_FIELD, sign);
            String actionUrl = getGatewayServerUrl() + "?" + buildQueryString(urlParams);

            //将非系统参数排序后置于form表单中
            Map<String, String> formParams = new TreeMap<>(textParams);
            formParams.put(AlipayConstants.BIZ_CONTENT_FIELD, JsonUtil.toJsonString(bizParams));
            return PageUtil.buildForm(actionUrl, formParams);
        } else {
            throw new RuntimeException("_generatePage中method只支持传入GET或POST");
        }
    }

    private String getGatewayServerUrl() {
        return getConfig(AlipayConstants.PROTOCOL_CONFIG_KEY) + "://" + getConfig(AlipayConstants.HOST_CONFIG_KEY) + "/gateway.do";
    }

    /**
     * 获取SDK版本信息
     *
     * @return SDK版本信息
     */
    protected String _getSdkVersion() {
        return AlipayConstants.SDK_VERSION;
    }

    /**
     * 配置参数模型
     */
    public static class Config extends TeaModel {
        @NameInMap("protocol")
        @Validation(required = true)
        public String protocol;

        @NameInMap("gatewayHost")
        @Validation(required = true)
        public String gatewayHost;

        @NameInMap("appId")
        @Validation(required = true)
        public String appId;

        @NameInMap("signType")
        @Validation(required = true)
        public String signType;

        @NameInMap("alipayPublicKey")
        public String alipayPublicKey;

        @NameInMap("merchantPrivateKey")
        @Validation(required = true)
        public String merchantPrivateKey;

        @NameInMap("merchantCertPath")
        public String merchantCertPath;

        @NameInMap("alipayCertPath")
        public String alipayCertPath;

        @NameInMap("alipayRootCertPath")
        public String alipayRootCertPath;
    }
}
