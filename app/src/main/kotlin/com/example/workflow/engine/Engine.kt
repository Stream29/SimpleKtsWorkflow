package com.example.workflow.engine

import com.example.workflow.core.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

class SimpleExecutionContext(
    override val variables: Map<String, Value> = emptyMap(),
    private val modules: Map<ModuleId, Module> = emptyMap(),
    override val llmClient: LlmClient = com.example.workflow.impl.MockLlmClient(),
    override val toolRegistry: ToolRegistry = SimpleToolRegistry()
) : ExecutionContext {
    override fun getModule(id: ModuleId): Module? = modules[id]
}

class Engine(private val modules: Map<ModuleId, Module> = emptyMap()) {
    fun execute(module: Module, input: List<Value>): List<Value> {
        val context = SimpleExecutionContext(modules = modules)
        return module.execute(input, context)
    }
}

class SequentialModule(
    override val signature: ModuleSignature,
    private val steps: List<Module>,
    // Map of Step Index -> (Output Name -> Input Name for next step)
    // This is a simplification. In a real system, we'd have a more complex mapping definition.
    // For now, we'll assume implicit mapping by name match, or just pass all outputs to next inputs.
    private val mappings: Map<Int, Map<String, String>> = emptyMap()
) : Module {
    override fun execute(input: List<Value>, context: ExecutionContext): List<Value> {
        var currentValues = input
        
        for ((index, step) in steps.withIndex()) {
            val mapping = mappings[index] ?: emptyMap()
            
            // Filter/Map inputs for the step
            val stepInput = step.signature.input.mapNotNull { inputSpec ->
                // 1. Try to find in mapping
                val mappedName = mapping[inputSpec.name]
                if (mappedName != null) {
                    currentValues.find { it.spec.name == mappedName }?.let { 
                        Value(inputSpec, it.value)
                    }
                } else {
                    // 2. Fallback to implicit name match
                    currentValues.find { it.spec.name == inputSpec.name }?.let { 
                        Value(inputSpec, it.value)
                    }
                }
            }
            
            // Execute step
            val stepOutput = step.execute(stepInput, context)
            
            // Merge output into current values (overwriting if name exists, or adding)
            // In a real scope, we might want to keep them separate, but for sequential flow, accumulating is common.
            val newValues = currentValues.toMutableList()
            for (outVal in stepOutput) {
                val existingIdx = newValues.indexOfFirst { it.spec.name == outVal.spec.name }
                if (existingIdx >= 0) {
                    newValues[existingIdx] = outVal
                } else {
                    newValues.add(outVal)
                }
            }
            currentValues = newValues
        }
        
        // Filter final output based on signature
        return signature.output.mapNotNull { outSpec ->
            currentValues.find { it.spec.name == outSpec.name }?.let {
                Value(outSpec, it.value)
            }
        }
    }
}

class ParallelModule(
    override val signature: ModuleSignature,
    private val branches: List<Module>
) : Module {
    override fun execute(input: List<Value>, context: ExecutionContext): List<Value> {
        return runBlocking {
            val deferredResults = branches.map { branch ->
                async {
                    // Filter inputs for the branch
                    val branchInput = branch.signature.input.mapNotNull { inputSpec ->
                        input.find { it.spec.name == inputSpec.name }?.let { Value(inputSpec, it.value) }
                    }
                    branch.execute(branchInput, context)
                }
            }
            val results = deferredResults.awaitAll()
            results.flatten()
        }
    }
}

class SwitchModule(
    override val signature: ModuleSignature,
    private val conditionVariable: String,
    private val cases: Map<Any, Module>,
    private val defaultCase: Module? = null
) : Module {
    override fun execute(input: List<Value>, context: ExecutionContext): List<Value> {
        val conditionValue = input.find { it.spec.name == conditionVariable }?.value
        
        val selectedModule = cases[conditionValue] ?: defaultCase
            ?: throw IllegalArgumentException("No case matched for value $conditionValue and no default provided")
            
        val stepInput = selectedModule.signature.input.mapNotNull { inputSpec ->
            input.find { it.spec.name == inputSpec.name }?.let { Value(inputSpec, it.value) }
        }
        
        return selectedModule.execute(stepInput, context)
    }
}
