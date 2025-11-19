
import com.example.workflow.core.*
import com.example.workflow.dsl.*

module("demo-module") {
    input("query", ValueType.STRING)
    output("result", ValueType.STRING)
    
    // In a real scenario, we would add steps here.
    // For this demo, the builder just creates a placeholder module.
}
