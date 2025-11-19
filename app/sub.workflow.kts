
import com.example.workflow.core.*
import com.example.workflow.dsl.*

module("sub-workflow") {
    input("val1", ValueType.INT)
    output("val1", ValueType.INT)
    
    sequential {
        // Pass through
    }
}
