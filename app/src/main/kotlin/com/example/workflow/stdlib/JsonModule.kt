package com.example.workflow.stdlib

import com.example.workflow.core.*

// Simple JSON parser wrapper. In a real app, use kotlinx.serialization or Jackson.
// For this demo, we'll just return the string as is or do very basic "parsing" if needed,
// but since our Value system is simple, we might just treat JSON as a String or Map.
// Let's assume we want to extract a field from a JSON string using a simple regex or string search for the demo,
// to avoid adding heavy dependencies and configuration.

class JsonExtractModule(
    override val signature: ModuleSignature,
    private val fieldName: String
) : Module {
    override fun execute(input: List<Value>, context: ExecutionContext): List<Value> {
        val json = input.find { it.spec.name == "json" }?.value as? String 
            ?: throw IllegalArgumentException("Missing 'json' input")
        
        // Very naive extraction for demo: "key": "value"
        // This is NOT a real JSON parser.
        val regex = "\"$fieldName\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val match = regex.find(json)
        val value = match?.groupValues?.get(1) ?: ""
        
        return listOf(Value(signature.output.first(), value))
    }
}
