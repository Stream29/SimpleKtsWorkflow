package com.example.workflow.dsl

import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

class ScriptingHost {
    fun eval(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<WorkflowScript>()
        val host = BasicJvmScriptingHost()
        return host.eval(scriptFile.toScriptSource(), compilationConfiguration, null)
    }
}
