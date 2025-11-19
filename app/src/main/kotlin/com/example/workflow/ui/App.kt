package com.example.workflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.ui.awt.ComposeWindow

@Composable
fun App(window: ComposeWindow? = null) {
    val viewModel = remember { WorkflowViewModel() }
    
    MaterialTheme {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar: Workflow List
            Column(
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
                    .background(Color.LightGray)
                    .padding(16.dp)
            ) {
                Text("Workflows", style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn {
                    items(viewModel.workflows) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.selectWorkflow(item) },
                            backgroundColor = if (viewModel.selectedWorkflow == item) Color.White else Color(0xFFEEEEEE)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(item.spec?.id?.id ?: item.file.name, style = MaterialTheme.typography.body1)
                                if (item.spec != null) {
                                    Text(item.file.name, style = MaterialTheme.typography.caption)
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { viewModel.loadWorkflows() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Refresh")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { 
                        if (window != null) {
                            val file = openFileDialog(window)
                            if (file != null) {
                                viewModel.addFile(file)
                            }
                        }
                    }, 
                    modifier = Modifier.fillMaxWidth(),
                    enabled = window != null
                ) {
                    Text("Add File")
                }
            }
            
            // Main Content: Execution & Logs
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (viewModel.selectedWorkflow != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Selected: ${viewModel.selectedWorkflow?.spec?.id?.id}", style = MaterialTheme.typography.h5)
                        Button(
                            onClick = { viewModel.runSelectedWorkflow() },
                            enabled = !viewModel.isRunning
                        ) {
                            Text(if (viewModel.isRunning) "Running..." else "Run")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (viewModel.inputValues.isNotEmpty()) {
                        Text("Inputs:", style = MaterialTheme.typography.subtitle1)
                        viewModel.inputValues.forEach { (name, value) ->
                            OutlinedTextField(
                                value = value,
                                onValueChange = { viewModel.updateInput(name, it) },
                                label = { Text(name) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Structure:", style = MaterialTheme.typography.subtitle1)
                    Card(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        backgroundColor = Color(0xFFDDDDDD)
                    ) {
                        Text(
                            text = viewModel.structure,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.body2,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Logs:", style = MaterialTheme.typography.subtitle1)
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        backgroundColor = Color.Black
                    ) {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            items(viewModel.logs) { log ->
                                Text(log, color = Color.Green, style = MaterialTheme.typography.body2)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("Select a workflow to run")
                    }
                }
            }
        }
    }
}
