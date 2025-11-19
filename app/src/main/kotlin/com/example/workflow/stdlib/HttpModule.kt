package com.example.workflow.stdlib

import com.example.workflow.core.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class HttpModule(
    override val signature: ModuleSignature
) : Module {
    private val client = HttpClient.newHttpClient()

    override fun execute(input: List<Value>, context: ExecutionContext): List<Value> {
        val url = input.find { it.spec.name == "url" }?.value as? String 
            ?: throw IllegalArgumentException("Missing 'url' input")
        val method = input.find { it.spec.name == "method" }?.value as? String ?: "GET"
        // Body handling omitted for brevity in this demo, assuming GET for now or simple POST
        
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
        
        when (method.uppercase()) {
            "GET" -> requestBuilder.GET()
            "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.noBody()) // Placeholder
            else -> requestBuilder.method(method, HttpRequest.BodyPublishers.noBody())
        }
        
        val response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        
        return listOf(
            Value(signature.output.find { it.name == "status" }!!, response.statusCode()),
            Value(signature.output.find { it.name == "body" }!!, response.body())
        )
    }
}
