package com.dailywell.app.ai

import android.content.Context

/**
 * @deprecated Replaced by [SLMService] which runs the on-device Qwen model via llama.cpp.
 * This alias is kept only for source compatibility with older references.
 */
@Deprecated("Use SLMService instead", replaceWith = ReplaceWith("SLMService"))
typealias GemmaService = SLMService


