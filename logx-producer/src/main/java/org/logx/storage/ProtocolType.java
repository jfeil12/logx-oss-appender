package org.logx.storage;

/**
 * 存储协议类型枚举
 * <p>
 * 定义所有支持的存储协议类型，用于适配器匹配和服务加载。
 * 协议类型决定了使用哪个具体的存储服务实现。
 * <p>
 * 与StorageOssType的区别：
 * <ul>
 * <li>StorageOssType: 云服务商类型（如SF_S3、MINIO、AWS_S3），包含个性化配置</li>
 * <li>ProtocolType: 协议类型（如S3、SF_OSS），用于适配器匹配</li>
 * </ul>
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public enum ProtocolType {

    /**
     * 标准S3协议
     * <p>
     * 适用于所有S3兼容存储服务：
     * <ul>
     * <li>AWS S3</li>
     * <li>阿里云OSS（S3兼容模式）</li>
     * <li>腾讯云COS（S3兼容模式）</li>
     * <li>华为云OBS（S3兼容模式）</li>
     * <li>MinIO</li>
     * <li>SF S3（顺丰S3兼容存储）</li>
     * </ul>
     */
    S3("S3"),

    /**
     * SF OSS专有协议
     * <p>
     * 顺丰OSS的专有存储协议，使用SF OSS SDK实现
     */
    SF_OSS("SF_OSS");

    private final String value;

    ProtocolType(String value) {
        this.value = value;
    }

    /**
     * 获取协议类型的字符串值
     *
     * @return 协议类型字符串
     */
    public String getValue() {
        return value;
    }

    /**
     * 从字符串值解析协议类型
     *
     * @param value 协议类型字符串
     * @return 对应的协议类型枚举
     * @throws IllegalArgumentException 如果协议类型不支持
     */
    public static ProtocolType fromValue(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Protocol type value cannot be null or empty");
        }

        for (ProtocolType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown protocol type: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
