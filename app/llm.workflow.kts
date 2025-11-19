
import com.example.workflow.core.*
import com.example.workflow.dsl.*

module("llm-workflow") {
    input("topic", ValueType.STRING)
    output("summary", ValueType.STRING)
    
    sequential {
        // 1. Call LLM to generate content
        val llmStep = llm(
            id = "generate-content",
            promptTemplate = "Write a short story about \${topic}",
            input = listOf(ValueSpec("topic", ValueType.STRING)),
            output = ValueSpec("story", ValueType.STRING)
        )
        step(llmStep)
        
        // 2. Call LLM to summarize content, mapping 'story' to 'text'
        val summarizeStep = llm(
            id = "summarize-content",
            promptTemplate = "Summarize this: \${text}",
            input = listOf(ValueSpec("text", ValueType.STRING)),
            output = ValueSpec("summary", ValueType.STRING)
        )
        
        // Explicit mapping: input 'text' of summarizeStep comes from 'story' variable in context
        step(summarizeStep, mapOf("text" to "story"))
    }
}
