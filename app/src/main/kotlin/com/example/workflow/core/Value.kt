package com.example.workflow.core

enum class ValueType {
    STRING,
    INT,
    BOOLEAN,
    OBJECT,
    LIST,
    ANY
}

data class ValueSpec(
    val name: String,
    val type: ValueType
)

data class Value(
    val spec: ValueSpec,
    val value: Any?
)
