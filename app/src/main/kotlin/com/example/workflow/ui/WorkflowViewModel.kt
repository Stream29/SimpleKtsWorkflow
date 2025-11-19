package com.example.workflow.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.workflow.core.*
import com.example.workflow.dsl.ScriptingHost
import com.example.workflow.engine.Engine
import com.example.workflow.impl.MockLlmClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.script.experimental.api.valueOrThrow

data class WorkflowItem(val file: File, val spec: ModuleSpec?)

class WorkflowViewModel {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val scriptingHost = ScriptingHost()
    
    var workflows = mutableStateListOf<WorkflowItem>()
        private set
        
    var selectedWorkflow by mutableStateOf<WorkflowItem?>(null)
    var logs = mutableStateListOf<String>()
        private set
        
    var isRunning by mutableStateOf(false)
        private set

    init {
        loadWorkflows()
    }

    fun loadWorkflows() {
        scope.launch {
            val appDir = File("app")
            val currentDir = File(".")
            
            val files = (appDir.listFiles { _, name -> name.endsWith(".workflow.kts") } ?: emptyArray()) +
                        (currentDir.listFiles { _, name -> name.endsWith(".workflow.kts") } ?: emptyArray())
            
            val items = files.map { loadWorkflowItem(it) }
            
            withContext(Dispatchers.Main) {
                // Keep existing manually added files if any, or just reload everything?
                // For simplicity, let's just reload found files and keep manual ones if we tracked them separately.
                // But here we just clear and add.
                // Let's just add the found ones to the list, avoiding duplicates.
                val existingFiles = workflows.map { it.file.absolutePath }.toSet()
                val newItems = items.filter { it.file.absolutePath !in existingFiles }
                workflows.addAll(newItems)
            }
        }
    }
    
    fun addFile(file: File) {
        scope.launch {
            if (workflows.none { it.file.absolutePath == file.absolutePath }) {
                val item = loadWorkflowItem(file)
                withContext(Dispatchers.Main) {
                    workflows.add(item)
                }
            }
        }
    }
    
    private fun loadWorkflowItem(file: File): WorkflowItem {
        return try {
            val result = scriptingHost.eval(file)
            val returnValue = result.valueOrThrow().returnValue
            val spec = (returnValue as? kotlin.script.experimental.api.ResultValue.Value)?.value as? ModuleSpec
            WorkflowItem(file, spec)
        } catch (e: Exception) {
            WorkflowItem(file, null)
        }
    }
    
    var inputValues = mutableStateListOf<Pair<String, String>>()
        private set

    // ... (init and loadWorkflows remain same)

    fun selectWorkflow(item: WorkflowItem) {
        selectedWorkflow = item
        logs.clear()
        inputValues.clear()
        item.spec?.signature?.input?.forEach { 
            inputValues.add(it.name to "")
        }
        generateStructure(item.spec)
    }
    
    var structure by mutableStateOf("")
        private set
        
    private fun generateStructure(spec: ModuleSpec?) {
        if (spec == null) {
            structure = ""
            return
        }
        val sb = StringBuilder()
        sb.append("Workflow: ${spec.id.id}\n")
        sb.append("Type: ${spec.type}\n")
        sb.append("Inputs: ${spec.signature.input.joinToString { "${it.name}: ${it.type}" }}\n")
        sb.append("Outputs: ${spec.signature.output.joinToString { "${it.name}: ${it.type}" }}\n")
        // In a real app, we would recursively traverse the module structure.
        // Since ModuleSpec doesn't expose children directly in the interface (it's in the implementation),
        // we can't easily show the tree without casting or changing the interface.
        // For this demo, we'll just show the top-level info.
        // To show more, we'd need to expose 'steps' in SequentialModuleSpec, etc.
        structure = sb.toString()
    }
    
    fun updateInput(name: String, value: String) {
        val index = inputValues.indexOfFirst { it.first == name }
        if (index != -1) {
            inputValues[index] = name to value
        }
    }
    
    fun runSelectedWorkflow() {
        val item = selectedWorkflow ?: return
        if (isRunning) return
        
        isRunning = true
        logs.add("Starting execution of ${item.file.name}...")
        
        scope.launch {
            try {
                // ... (compilation logic remains same)
                val loadedSpecs = workflows.mapNotNull { it.spec }.associateBy { it.id }
                
                val compilationContext = object : ModuleCompilationContext {
                    override fun resolveModule(id: ModuleId): ModuleSpec? = loadedSpecs[id]
                }
                
                val compiledModules = loadedSpecs.mapValues { 
                    try {
                        it.value.compile(compilationContext)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            logs.add("Error compiling ${it.key.id}: ${e.message}")
                        }
                        null
                    }
                }.filterValues { it != null } as Map<ModuleId, Module>
                
                val mainSpec = item.spec
                if (mainSpec == null) {
                    withContext(Dispatchers.Main) { logs.add("Error: No module spec found for this file.") }
                    return@launch
                }
                
                val mainModule = compiledModules[mainSpec.id]
                if (mainModule == null) {
                    withContext(Dispatchers.Main) { logs.add("Error: Could not compile main module.") }
                    return@launch
                }
                
                // Prepare Input from UI state
                val input = mainSpec.signature.input.map { spec ->
                    val inputValue = inputValues.find { it.first == spec.name }?.second ?: ""
                    // Basic type conversion (only supporting String and Int for now)
                    val value = when (spec.type) {
                        ValueType.INT -> inputValue.toIntOrNull() ?: 0
                        else -> inputValue
                    }
                    Value(spec, value)
                }
                
                withContext(Dispatchers.Main) { logs.add("Input: $input") }
                
                val executionContext = object : ExecutionContext {
                    override val variables: Map<String, Value> = emptyMap()
                    override val llmClient: LlmClient = MockLlmClient()
                    override val toolRegistry: ToolRegistry = com.example.workflow.core.SimpleToolRegistry()
                    override fun getModule(id: ModuleId): Module? = compiledModules[id]
                }
                
                val engine = Engine(compiledModules)
                // We need to use our custom context or let Engine create it?
                // Engine creates SimpleExecutionContext. We should probably allow passing context or factory.
                // But Engine.execute creates a new context.
                // Let's modify Engine to take context or we just use the Engine's default which uses the modules we passed.
                // But we need to inject LlmClient.
                // The Engine class currently creates SimpleExecutionContext with default LlmClient (Mock).
                // So it should be fine for now.
                
                val output = engine.execute(mainModule, input)
                
                withContext(Dispatchers.Main) {
                    logs.add("Execution finished.")
                    output.forEach { logs.add("Output: ${it.spec.name} = ${it.value}") }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    logs.add("Error during execution: ${e.message}")
                    e.printStackTrace()
                }
            } finally {
                isRunning = false
            }
        }
    }
}
