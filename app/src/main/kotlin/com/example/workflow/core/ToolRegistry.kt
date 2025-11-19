package com.example.workflow.core

interface ToolRegistry {
    fun getTool(name: String): Tool?
    fun registerTool(name: String, tool: Tool)
}

class SimpleToolRegistry : ToolRegistry {
    private val tools = mutableMapOf<String, Tool>()
    
    override fun getTool(name: String): Tool? = tools[name]
    
    override fun registerTool(name: String, tool: Tool) {
        tools[name] = tool
    }
}
