package com.henry.budgetmvp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.henry.budgetmvp.data.MessageType
import com.henry.budgetmvp.data.StatusMessage

@Composable
fun BudgetNotificationBanner(
    statusMessage: StatusMessage,
    onDismiss: () -> Unit = {}
) {
    val (icon, color) = when (statusMessage.type) {
        MessageType.INFO -> Lucide.Info to MaterialTheme.colorScheme.primary
        MessageType.SUCCESS -> Lucide.CircleCheck to Color(0xFF2E7D32) // Success Green
        MessageType.ERROR -> Lucide.CircleAlert to MaterialTheme.colorScheme.error
        MessageType.OFFLINE -> Lucide.CloudOff to Color(0xFFD32F2F) // Error Red/Orange
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = statusMessage.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            TextButton(onClick = onDismiss) {
                Text("DISMISS", fontWeight = FontWeight.Bold)
            }
        }
    }
}
