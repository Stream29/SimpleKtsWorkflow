package com.example.workflow.dsl

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

@KotlinScript(
    fileExtension = "workflow.kts",
    compilationConfiguration = WorkflowScriptConfiguration::class
)
abstract class WorkflowScript

object WorkflowScriptConfiguration : ScriptCompilationConfiguration({
    defaultImports("com.example.workflow.core.*", "com.example.workflow.dsl.*")
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
    }
})
