package com.example.workflow.dsl

import com.example.workflow.core.*
import com.example.workflow.engine.*

class WorkflowBuilder(val id: ModuleId) {
    var input: List<ValueSpec> = emptyList()
    var output: List<ValueSpec> = emptyList()
    
    private var rootModuleSpec: ModuleSpec? = null
    
    fun input(name: String, type: ValueType) {
        input = input + ValueSpec(name, type)
    }

    fun output(name: String, type: ValueType) {
        output = output + ValueSpec(name, type)
    }
    
    // DSL for defining the body
    
    fun sequential(block: SequentialBuilder.() -> Unit) {
        val builder = SequentialBuilder()
        builder.block()
        rootModuleSpec = builder.build(id, ModuleSignature(input, output))
    }
    
    fun build(): ModuleSpec {
        return rootModuleSpec ?: object : ModuleSpec {
            override val id: ModuleId = this@WorkflowBuilder.id
            override val type: ModuleType = ModuleType.SEQUENTIAL
            override val signature: ModuleSignature = ModuleSignature(input, output)
            override fun compile(context: ModuleCompilationContext): Module {
                return object : Module {
                    override val signature: ModuleSignature = this@WorkflowBuilder.build().signature
                    override fun execute(input: List<Value>, context: ExecutionContext): List<Value> {
                        println("Empty module executed")
                        return emptyList()
                    }
                }
            }
        }
    }
}

class SequentialBuilder {
    private val steps = mutableListOf<ModuleSpec>()
    private val stepMappings = mutableMapOf<Int, Map<String, String>>()
    
    fun step(spec: ModuleSpec, inputMapping: Map<String, String> = emptyMap()) {
        steps.add(spec)
        if (inputMapping.isNotEmpty()) {
            stepMappings[steps.lastIndex] = inputMapping
        }
    }
    
    fun call(moduleId: String, inputMapping: Map<String, String> = emptyMap()): ModuleSpec {
        // This creates a spec that, when compiled, resolves the sub-module
        return object : ModuleSpec {
            override val id: ModuleId = ModuleId("call_$moduleId")
            override val type: ModuleType = ModuleType.SEQUENTIAL // Wrapper is sequential? Or generic?
            override val signature: ModuleSignature = ModuleSignature(emptyList(), emptyList()) // Unknown signature at this point
            
            override fun compile(context: ModuleCompilationContext): Module {
                val targetId = ModuleId(moduleId)
                val targetSpec = context.resolveModule(targetId) 
                    ?: throw IllegalStateException("Module $moduleId not found during compilation")
                
                return SubWorkflowModule(targetSpec.signature, targetId)
            }
        }
    }

    fun llm(id: String, promptTemplate: String, input: List<ValueSpec>, output: ValueSpec): ModuleSpec {
        return object : ModuleSpec {
            override val id: ModuleId = ModuleId(id)
            override val type: ModuleType = ModuleType.LLM
            override val signature: ModuleSignature = ModuleSignature(input, listOf(output))
            override fun compile(context: ModuleCompilationContext): Module {
                // In a real implementation, we might want to inject the client from context during execution,
                // but Module interface expects client to be part of the module or accessed via context.
                // LlmModule expects client in constructor? No, let's change LlmModule to get client from context.
                // Or we can pass a factory/provider.
                // For now, let's assume LlmModule uses context.llmClient
                return LlmModule(signature, promptTemplate)
            }
        }
    }

    fun http(id: String, urlInput: String? = null): ModuleSpec {
        return object : ModuleSpec {
            override val id: ModuleId = ModuleId(id)
            override val type: ModuleType = ModuleType.TOOL // Using TOOL for now
            override val signature: ModuleSignature = ModuleSignature(
                input = listOf(
                    ValueSpec("url", ValueType.STRING),
                    ValueSpec("method", ValueType.STRING)
                ),
                output = listOf(
                    ValueSpec("status", ValueType.INT),
                    ValueSpec("body", ValueType.STRING)
                )
            )
            override fun compile(context: ModuleCompilationContext): Module {
                return com.example.workflow.stdlib.HttpModule(signature)
            }
        }
    }

    fun jsonExtract(id: String, field: String): ModuleSpec {
        return object : ModuleSpec {
            override val id: ModuleId = ModuleId(id)
            override val type: ModuleType = ModuleType.TOOL
            override val signature: ModuleSignature = ModuleSignature(
                input = listOf(ValueSpec("json", ValueType.STRING)),
                output = listOf(ValueSpec("value", ValueType.STRING))
            )
            override fun compile(context: ModuleCompilationContext): Module {
                return com.example.workflow.stdlib.JsonExtractModule(signature, field)
            }
        }
    }
    
    fun parallel(block: ParallelBuilder.() -> Unit): ModuleSpec {
        val builder = ParallelBuilder()
        builder.block()
        // We need to know the signature of the parallel block. 
        // For now, we assume it takes same inputs as parent and produces combined outputs?
        // Or the user defines it?
        // Simplified: Parallel block doesn't change signature, just executes side effects? 
        // No, it should return values.
        // Let's assume it returns list of values from all branches.
        return builder.build()
    }

    fun switch(conditionVar: String, block: SwitchBuilder.() -> Unit): ModuleSpec {
        val builder = SwitchBuilder()
        builder.block()
        return builder.build(conditionVar)
    }
    
    fun build(id: ModuleId, signature: ModuleSignature): ModuleSpec {
        return object : ModuleSpec {
            override val id: ModuleId = id
            override val type: ModuleType = ModuleType.SEQUENTIAL
            override val signature: ModuleSignature = signature
            override fun compile(context: ModuleCompilationContext): Module {
                val compiledSteps = steps.map { it.compile(context) }
                return SequentialModule(signature, compiledSteps, stepMappings)
            }
        }
    }
}

class ParallelBuilder {
    private val branches = mutableListOf<ModuleSpec>()
    
    fun branch(block: SequentialBuilder.() -> Unit) {
        val builder = SequentialBuilder()
        builder.block()
        // We don't know the signature here easily without more DSL.
        // We'll create a dummy signature or require user to specify.
        // For this demo, we'll create a generic module spec.
        branches.add(builder.build(ModuleId("branch_${branches.size}"), ModuleSignature(emptyList(), emptyList())))
    }
    
    fun build(): ModuleSpec {
        return object : ModuleSpec {
            override val id: ModuleId = ModuleId("parallel")
            override val type: ModuleType = ModuleType.PARALLEL
            override val signature: ModuleSignature = ModuleSignature(emptyList(), emptyList())
            override fun compile(context: ModuleCompilationContext): Module {
                val compiledBranches = branches.map { it.compile(context) }
                return ParallelModule(signature, compiledBranches)
            }
        }
    }
}

class SwitchBuilder {
    private val cases = mutableMapOf<Any, ModuleSpec>()
    private var defaultCase: ModuleSpec? = null
    
    fun case(value: Any, block: SequentialBuilder.() -> Unit) {
        val builder = SequentialBuilder()
        builder.block()
        cases[value] = builder.build(ModuleId("case_$value"), ModuleSignature(emptyList(), emptyList()))
    }
    
    fun default(block: SequentialBuilder.() -> Unit) {
        val builder = SequentialBuilder()
        builder.block()
        defaultCase = builder.build(ModuleId("default"), ModuleSignature(emptyList(), emptyList()))
    }
    
    fun build(conditionVar: String): ModuleSpec {
        return object : ModuleSpec {
            override val id: ModuleId = ModuleId("switch")
            override val type: ModuleType = ModuleType.SWITCH
            override val signature: ModuleSignature = ModuleSignature(emptyList(), emptyList())
            override fun compile(context: ModuleCompilationContext): Module {
                val compiledCases = cases.mapValues { it.value.compile(context) }
                val compiledDefault = defaultCase?.compile(context)
                return SwitchModule(signature, conditionVar, compiledCases, compiledDefault)
            }
        }
    }
}

fun module(id: String, block: WorkflowBuilder.() -> Unit): ModuleSpec {
    val builder = WorkflowBuilder(ModuleId(id))
    builder.block()
    return builder.build()
}
