package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserMemoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorySheet(
    memories: List<UserMemoryEntity>,
    onAddMemory: (String, String, String) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onClearChatHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    var newCategory by remember { mutableStateOf("PROJECT") }
    var newTitle by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF070B14),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .testTag("memory_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x2200F0FF))
                            .border(1.dp, Color(0x6600F0FF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AHI Persistent Memory",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${memories.size} memories preserved across restarts",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Filter Pills
            val categories = listOf("ALL", "PROJECT", "GOAL", "PREFERENCE", "FACT")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF0075FF) else Color(0xFF131D31))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF00F0FF) else Color(0x330075FF),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action row: Add memory button + Clear chat history
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showAddDialog = !showAddDialog },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A2548)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showAddDialog) "Hide Form" else "Save Memory",
                        fontSize = 12.sp,
                        color = Color(0xFF00F0FF)
                    )
                }

                Text(
                    text = "Clear Chat History",
                    fontSize = 12.sp,
                    color = Color(0xFFF43F5E),
                    modifier = Modifier
                        .clickable { onClearChatHistory() }
                        .padding(8.dp)
                )
            }

            // Inline Add Form
            if (showAddDialog) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0E1A2D))
                        .border(1.dp, Color(0x440075FF), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Record Long-Term Fact or Project",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            placeholder = { Text("Title (e.g. Android AI Project, Fitness Goal)", fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00F0FF),
                                unfocusedBorderColor = Color(0x330075FF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newContent,
                            onValueChange = { newContent = it },
                            placeholder = { Text("Details (AHI will proactively remember this)", fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00F0FF),
                                unfocusedBorderColor = Color(0x330075FF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (newTitle.isNotBlank() && newContent.isNotBlank()) {
                                    onAddMemory(newCategory, newTitle.trim(), newContent.trim())
                                    newTitle = ""
                                    newContent = ""
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FF)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save to Memory", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Memory List
            val filteredMemories = memories.filter {
                selectedCategory == "ALL" || it.category.equals(selectedCategory, ignoreCase = true)
            }

            if (filteredMemories.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Text(
                        text = "No saved memories in this category.\nTalk to AHI or tap 'Save Memory' above.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 340.dp)
                ) {
                    items(filteredMemories, key = { it.id }) { mem ->
                        MemoryItemCard(
                            memory = mem,
                            onDelete = { onDeleteMemory(mem.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MemoryItemCard(
    memory: UserMemoryEntity,
    onDelete: () -> Unit
) {
    val icon: ImageVector = when (memory.category.uppercase()) {
        "PROJECT" -> Icons.Default.Work
        "GOAL" -> Icons.Default.Flag
        "PREFERENCE" -> Icons.Default.Bookmark
        else -> Icons.Default.Lightbulb
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F1A2C))
            .border(1.dp, Color(0x330075FF), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x220075FF))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = memory.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = memory.category,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00F0FF)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = memory.content,
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 17.sp
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Memory",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
