package com.example.workflow.core

interface ExecutionContext {
    val variables: Map<String, Value>
    val llmClient: LlmClient
    val toolRegistry: ToolRegistry
    fun getModule(id: ModuleId): Module?
    // Can be extended to support LLM clients, etc.
}

interface ModuleCompilationContext {
    fun resolveModule(id: ModuleId): ModuleSpec?
}
