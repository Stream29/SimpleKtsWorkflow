package com.example.workflow.core

interface LlmClient {
    fun generate(prompt: String): String
}

interface Tool {
    fun execute(args: Map<String, Any>): Any
}

class LlmModule(
    override val signature: ModuleSignature,
    private val promptTemplate: String
) : Module {
    override fun execute(input: List<Value>, context: ExecutionContext): List<Value> {
        // Simplified prompt construction
        var prompt = promptTemplate
        input.forEach { 
            prompt = prompt.replace("\${${it.spec.name}}", it.value.toString())
        }
        val response = context.llmClient.generate(prompt)
        return listOf(Value(signature.output.first(), response))
    }
}

class ToolModule(
    override val signature: ModuleSignature,
    private val tool: Tool
) : Module {
    override fun execute(input: List<Value>, context: ExecutionContext): List<Value> {
        val args = input.associate { it.spec.name to (it.value ?: "") }
        val result = tool.execute(args)
        return listOf(Value(signature.output.first(), result))
    }
}
