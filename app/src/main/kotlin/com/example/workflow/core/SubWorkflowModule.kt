package com.example.workflow.core

class SubWorkflowModule(
    override val signature: ModuleSignature,
    private val subWorkflowId: ModuleId
) : Module {
    override fun execute(input: List<Value>, context: ExecutionContext): List<Value> {
        val module = context.getModule(subWorkflowId)
            ?: throw IllegalStateException("Module with id ${subWorkflowId.id} not found in context")
            
        // Map inputs
        // We assume the input names match the signature of the sub-workflow
        // or that the caller has already prepared the correct values.
        // In a robust system, we would validate against module.signature.input
        
        return module.execute(input, context)
    }
}
