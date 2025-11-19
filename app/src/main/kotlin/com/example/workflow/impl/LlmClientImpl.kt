package com.example.workflow.impl

import com.example.workflow.core.LlmClient

class MockLlmClient : LlmClient {
    override fun generate(prompt: String): String {
        return "Mock response for: $prompt"
    }
}

class OpenAiLlmClient(private val apiKey: String) : LlmClient {
    override fun generate(prompt: String): String {
        // Placeholder for actual OpenAI integration
        // In a real implementation, we would use an HTTP client to call OpenAI API
        return "OpenAI response for: $prompt (API Key: ${apiKey.take(4)}...)"
    }
}
