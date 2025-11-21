package com.antigravity.browser.data.ai

data class RateLimitConfig(
    val rpm: Int, // Requests per minute
    val tpm: Int, // Tokens per minute
    val rpd: Int  // Requests per day
) {
    companion object {
        // Gemini 2.5 Flash limits
        val FLASH = RateLimitConfig(
            rpm = 8,
            tpm = 238_999,
            rpd = 239
        )
        
        // Gemini 2.5 Pro limits
        val PRO = RateLimitConfig(
            rpm = 1,
            tpm = 123_999,
            rpd = 42
        )
    }
}

/**
 * Tracks API usage and enforces rate limits
 */
class RateLimitTracker(
    private val config: RateLimitConfig
) {
    private val minuteRequests = mutableListOf<Long>()
    private val dayRequests = mutableListOf<Long>()
    private var tokenCount = 0
    private var lastMinuteReset = System.currentTimeMillis()
    private var lastDayReset = System.currentTimeMillis()
    
    /**
     * Check if we can make a request
     */
    fun canMakeRequest(estimatedTokens: Int = 1000): Boolean {
        cleanupOldRequests()
        
        val now = System.currentTimeMillis()
        
        // Check RPM
        if (minuteRequests.size >= config.rpm) {
            return false
        }
        
        // Check TPM
        if (tokenCount + estimatedTokens > config.tpm) {
            return false
        }
        
        // Check RPD
        if (dayRequests.size >= config.rpd) {
            return false
        }
        
        return true
    }
    
    /**
     * Record a request
     */
    fun recordRequest(tokensUsed: Int) {
        val now = System.currentTimeMillis()
        minuteRequests.add(now)
        dayRequests.add(now)
        tokenCount += tokensUsed
    }
    
    /**
     * Get current usage stats
     */
    fun getUsageStats(): UsageStats {
        cleanupOldRequests()
        return UsageStats(
            requestsThisMinute = minuteRequests.size,
            requestsToday = dayRequests.size,
            tokensThisMinute = tokenCount,
            rpmLimit = config.rpm,
            rpdLimit = config.rpd,
            tpmLimit = config.tpm
        )
    }
    
    /**
     * Reset counters (for testing or manual reset)
     */
    fun reset() {
        minuteRequests.clear()
        dayRequests.clear()
        tokenCount = 0
        lastMinuteReset = System.currentTimeMillis()
        lastDayReset = System.currentTimeMillis()
    }
    
    private fun cleanupOldRequests() {
        val now = System.currentTimeMillis()
        
        // Reset minute counters if a minute has passed
        if (now - lastMinuteReset >= 60_000) {
            minuteRequests.clear()
            tokenCount = 0
            lastMinuteReset = now
        }
        
        // Reset day counters if a day has passed
        if (now - lastDayReset >= 86_400_000) {
            dayRequests.clear()
            lastDayReset = now
        }
        
        // Remove requests older than 1 minute from minute list
        minuteRequests.removeAll { now - it > 60_000 }
        
        // Remove requests older than 1 day from day list
        dayRequests.removeAll { now - it > 86_400_000 }
    }
}

data class UsageStats(
    val requestsThisMinute: Int,
    val requestsToday: Int,
    val tokensThisMinute: Int,
    val rpmLimit: Int,
    val rpdLimit: Int,
    val tpmLimit: Int
) {
    val rpmPercentage: Float get() = (requestsThisMinute.toFloat() / rpmLimit) * 100
    val rpdPercentage: Float get() = (requestsToday.toFloat() / rpdLimit) * 100
    val tpmPercentage: Float get() = (tokensThisMinute.toFloat() / tpmLimit) * 100
    
    val isNearLimit: Boolean get() = rpmPercentage > 80 || rpdPercentage > 80 || tpmPercentage > 80
    val isAtLimit: Boolean get get() = requestsThisMinute >= rpmLimit || requestsToday >= rpdLimit || tokensThisMinute >= tpmLimit
}
