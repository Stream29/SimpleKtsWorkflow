package com.example.workflow.core

@JvmInline
value class ModuleId(val id: String)

data class ModuleSignature(
    val input: List<ValueSpec>,
    val output: List<ValueSpec>,
    val dependencies: List<ModuleId> = emptyList()
)

interface Module {
    val signature: ModuleSignature
    fun execute(input: List<Value>, context: ExecutionContext): List<Value>
}

enum class ModuleType {
    SEQUENTIAL,
    PARALLEL,
    SWITCH,
    LLM,
    TOOL,
    SCRIPT
}

interface ModuleSpec {
    val id: ModuleId
    val type: ModuleType
    val signature: ModuleSignature
    fun compile(context: ModuleCompilationContext): Module
}
