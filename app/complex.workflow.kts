
import com.example.workflow.core.*
import com.example.workflow.dsl.*

module("complex-workflow") {
    input("val1", ValueType.INT)
    output("val1", ValueType.INT)
    
    sequential {
        // Call sub-workflow
        val sub = call("sub-workflow")
        step(sub)
        
        val sw = switch("val1") {
            case(10) {
                // Do nothing, just pass through?
                // SequentialBuilder inside case needs to produce something?
                // Our current SequentialModule accumulates values.
                // So if we do nothing, it's fine.
            }
            default {
                // Should not happen
            }
        }
        step(sw)
    }
}
