package com.travelbuddy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelbuddy.app.data.RemoteChatMessage
import com.travelbuddy.app.data.RemoteSendMessageRequest
import com.travelbuddy.app.data.TravelRepository
import com.travelbuddy.app.domain.TripMember
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Real-time Trip Group Chat dialog for all members joined in the trip.
 * Automatically polls the backend every 2.5s and delivers messages across all devices in real-time.
 */
@Composable
fun TripGroupChatDialog(
    tripId: String,
    tripName: String,
    currentUserName: String,
    currentUserId: String,
    members: List<TripMember>,
    repository: TravelRepository?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }
    var chatMessages by remember { mutableStateOf<List<RemoteChatMessage>>(emptyList()) }
    var isSending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Real-time chat polling loop (every 2.5s)
    LaunchedEffect(tripId) {
        while (isActive) {
            repository?.getChatMessages(tripId)?.onSuccess { list ->
                if (list.size != chatMessages.size) {
                    chatMessages = list
                    if (list.isNotEmpty()) {
                        listState.animateScrollToItem(list.lastIndex)
                    }
                }
            }
            delay(2500)
        }
    }

    val myName = currentUserName.ifBlank { "Traveler" }
    val myId = currentUserId.ifBlank { "usr-${myName.hashCode()}" }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = null,
        text = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat Header
                    Surface(
                        color = Color(0xFF1D64F2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$tripName • Live Chat",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${members.size} Travelers in Trip • Real-time Sync",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }

                    // Messages List
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        if (chatMessages.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1D64F2).copy(alpha = 0.12f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("💬", fontSize = 26.sp)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No messages yet in this trip",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Send a message to all joined travel buddies!",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().verticalScrollbar(listState),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(chatMessages) { msg ->
                                    val isMe = msg.senderName.equals(myName, ignoreCase = true) ||
                                            msg.senderId.equals(myId, ignoreCase = true)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        if (!isMe) {
                                            val senderMember = members.firstOrNull { it.name.equals(msg.senderName, ignoreCase = true) }
                                            TravelerAvatar(
                                                avatarType = senderMember?.avatarType ?: "MALE_1",
                                                size = 32.dp
                                            )
                                            Spacer(Modifier.width(6.dp))
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isMe) 16.dp else 4.dp,
                                                bottomEnd = if (isMe) 4.dp else 16.dp
                                            ),
                                            color = if (isMe) Color(0xFF1D64F2) else Color(0xFFF1F5F9),
                                            shadowElevation = 1.dp,
                                            modifier = Modifier.widthIn(max = 240.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                                if (!isMe) {
                                                    Text(
                                                        text = msg.senderName.ifBlank { "Buddy" },
                                                        color = Color(0xFF2563EB),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                }
                                                Text(
                                                    text = msg.displayMessage,
                                                    color = if (isMe) Color.White else Color(0xFF0F172A),
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }

                                        if (isMe) {
                                            Spacer(Modifier.width(6.dp))
                                            TravelerAvatar(
                                                avatarType = "MALE_1",
                                                size = 32.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Message Input Field
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Message trip buddies...", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                val text = messageText.trim()
                                if (text.isNotBlank() && !isSending) {
                                    isSending = true
                                    val req = RemoteSendMessageRequest(
                                        senderId = myId,
                                        senderName = myName,
                                        text = text,
                                        message = text
                                    )
                                    // Optimistic local add
                                    chatMessages = chatMessages + RemoteChatMessage(
                                        id = "local-${System.currentTimeMillis()}",
                                        tripId = tripId,
                                        senderId = myId,
                                        senderName = myName,
                                        message = text,
                                        text = text,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    messageText = ""
                                    scope.launch {
                                        repository?.sendChatMessage(tripId, req)
                                        isSending = false
                                        listState.animateScrollToItem(chatMessages.lastIndex)
                                    }
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0xFF1D64F2),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}
