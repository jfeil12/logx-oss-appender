package org.logx.storage;

/**
 * 存储后端类型枚举
 * <p>
 * 定义所有支持的S3兼容存储后端类型，提供统一的后端标识和配置信息。 每个枚举值包含显示名称、默认端点模式等元数据。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public enum StorageOssType {

    /**
     * 阿里云对象存储OSS 优先支持的云存储服务
     * 使用S3协议访问
     */
    ALIYUN_OSS("Aliyun OSS", "oss", ProtocolType.S3, true),

    /**
     * Amazon Web Services S3 原生S3服务
     */
    AWS_S3("AWS S3", "s3", ProtocolType.S3, false),

    /**
     * MinIO开源对象存储 兼容S3 API的开源解决方案
     */
    MINIO("MinIO", "minio", ProtocolType.S3, true),

    /**
     * 腾讯云对象存储COS 通过S3兼容API支持
     */
    TENCENT_COS("Tencent COS", "cos", ProtocolType.S3, true),

    /**
     * 华为云对象存储OBS 通过S3兼容API支持
     */
    HUAWEI_OBS("Huawei OBS", "obs", ProtocolType.S3, true),

    /**
     * SF OSS存储服务
     * 使用个性化OSS协议
     */
    SF_OSS("SF OSS", "sf", ProtocolType.SF_OSS, true),

    /**
     * SF S3兼容存储服务
     * 使用标准S3协议访问SF OSS，需要pathStyleAccess=true和X-Delete-After元数据
     */
    SF_S3("SF S3", "sf", ProtocolType.S3, true),

    /**
     * 通用S3兼容存储 适用于其他S3兼容服务
     */
    GENERIC_S3("Generic S3", "s3", ProtocolType.S3, true);

    private final String displayName;
    private final String urlPrefix;
    private final ProtocolType protocolType;
    private final boolean pathStyleDefault;

    /**
     * 构造函数
     *
     * @param displayName
     *            显示名称
     * @param urlPrefix
     *            URL前缀标识
     * @param protocolType
     *            协议类型枚举（S3或SF_OSS）
     * @param pathStyleDefault
     *            默认是否使用路径风格访问
     */
    StorageOssType(String displayName, String urlPrefix, ProtocolType protocolType, boolean pathStyleDefault) {
        this.displayName = displayName;
        this.urlPrefix = urlPrefix;
        this.protocolType = protocolType;
        this.pathStyleDefault = pathStyleDefault;
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取URL前缀标识
     */
    public String getUrlPrefix() {
        return urlPrefix;
    }

    /**
     * 获取默认路径风格设置
     */
    public boolean isPathStyleDefault() {
        return pathStyleDefault;
    }

    /**
     * 获取协议类型
     *
     * @return 协议类型枚举（S3或SF_OSS）
     */
    public ProtocolType getProtocolType() {
        return protocolType;
    }

    /**
     * 判断是否使用S3协议
     *
     * @return true表示使用S3协议，false表示使用其他协议
     */
    public boolean isS3Protocol() {
        return protocolType == ProtocolType.S3;
    }

    /**
     * 根据端点URL自动检测存储后端类型
     *
     * @param endpoint
     *            S3服务端点URL
     *
     * @return 检测到的存储后端类型，如果无法识别返回GENERIC_S3
     */
    public static StorageOssType detectFromEndpoint(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            return GENERIC_S3;
        }

        String lowerEndpoint = endpoint.toLowerCase(java.util.Locale.ENGLISH);

        // SF S3检测（sf-express或sfcloud，使用S3协议）
        if (lowerEndpoint.contains("sf-express") || lowerEndpoint.contains("sfcloud")) {
            return SF_S3;
        }

        // SF OSS检测（放在前面，避免被其他检测匹配）
        if (lowerEndpoint.contains("sf-oss.com") || lowerEndpoint.contains("sf-oss-")) {
            return SF_OSS;
        }

        // 阿里云OSS检测
        if (lowerEndpoint.contains("aliyuncs.com") || lowerEndpoint.contains("oss-")) {
            return ALIYUN_OSS;
        }

        // AWS S3检测
        if (lowerEndpoint.contains("amazonaws.com") || lowerEndpoint.contains("s3.")) {
            return AWS_S3;
        }

        // 腾讯云COS检测
        if (lowerEndpoint.contains("myqcloud.com") || lowerEndpoint.contains("cos.")) {
            return TENCENT_COS;
        }

        // 华为云OBS检测
        if (lowerEndpoint.contains("myhuaweicloud.com") || lowerEndpoint.contains("obs.")) {
            return HUAWEI_OBS;
        }

        // MinIO检测（通常用户自定义域名，较难自动检测）
        if (lowerEndpoint.contains("minio") || lowerEndpoint.contains("9000")) {
            return MINIO;
        }

        // 默认返回通用S3
        return GENERIC_S3;
    }

    /**
     * 根据配置信息检测存储后端类型
     *
     * @param endpoint
     *            端点URL
     * @param region
     *            区域信息
     *
     * @return 检测到的存储后端类型
     */
    public static StorageOssType detectFromConfig(String endpoint, String region) {
        // 先基于端点检测
        StorageOssType detected = detectFromEndpoint(endpoint);

        // 如果检测为通用S3，进一步基于区域信息判断
        if (detected == GENERIC_S3 && region != null) {
            String lowerRegion = region.toLowerCase(java.util.Locale.ENGLISH);

            if (lowerRegion.startsWith("cn-") || lowerRegion.contains("china")) {
                // 中国区域，可能是阿里云或腾讯云
                if (lowerRegion.contains("beijing") || lowerRegion.contains("shanghai")
                        || lowerRegion.contains("hangzhou") || lowerRegion.contains("shenzhen")) {
                    return ALIYUN_OSS;
                }
            }

            if (lowerRegion.startsWith("us-") || lowerRegion.startsWith("eu-") || lowerRegion.startsWith("ap-")
                    || lowerRegion.startsWith("ca-")) {
                return AWS_S3;
            }
        }

        return detected;
    }

    /**
     * 检查是否为国内云服务商
     */
    public boolean isDomesticCloud() {
        return this == ALIYUN_OSS || this == TENCENT_COS || this == HUAWEI_OBS || this == SF_OSS || this == SF_S3;
    }

    /**
     * 检查是否为国际云服务商
     */
    public boolean isInternationalCloud() {
        return this == AWS_S3;
    }

    /**
     * 检查是否为开源/私有化部署
     */
    public boolean isOpenSource() {
        return this == MINIO || this == GENERIC_S3;
    }

    @Override
    public String toString() {
        return displayName + " (" + name() + ")";
    }
}