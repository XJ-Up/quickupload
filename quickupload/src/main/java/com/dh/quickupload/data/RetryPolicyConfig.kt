package com.dh.quickupload.data

data class RetryPolicyConfig(
    /**
     * 当上传失败时，设置下一次尝试之前等待的时间 (以秒为单位)
     */
    val initialWaitTimeSeconds: Int,

    /**
     * 设置两次上传尝试之间的最长等待时间 (以秒为单位)。
     */
    val maxWaitTimeSeconds: Int,

    /**
     * 设置退避定时器乘数。例如，如果设置为2，则每次上载
     */
    val multiplier: Int,

    /**
     * 设置每个请求的默认重试次数。
     */
    val defaultMaxRetries: Int
) {
    override fun toString(): String {
        return """{"initialWaitTimeSeconds": $initialWaitTimeSeconds, "maxWaitTimeSeconds": $maxWaitTimeSeconds, "multiplier": $multiplier, "defaultMaxRetries": $defaultMaxRetries}"""
    }
}
