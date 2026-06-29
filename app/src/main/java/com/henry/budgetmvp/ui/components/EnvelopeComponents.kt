package com.henry.budgetmvp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.henry.budgetmvp.data.ExpenseEnvelope
import com.henry.budgetmvp.util.ThousandsSeparatorTransformation

@Composable
fun EnvelopeDetailsRow(envelope: ExpenseEnvelope, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(envelope.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$${"%,.2f".format(envelope.allocatedAmount)} / $${"%,.2f".format(envelope.targetAmount)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val remaining = envelope.targetAmount - envelope.allocatedAmount
            Text(
                text = if (remaining <= 0) "Fully Funded!" else "Needs $${"%,.2f".format(remaining)} more",
                style = MaterialTheme.typography.bodySmall,
                color = if (remaining <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvelopeEntrySheet(
    targetEnvelope: ExpenseEnvelope?,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(targetEnvelope?.name ?: "") }
    var targetAmount by remember { mutableStateOf(targetEnvelope?.targetAmount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var allocatedAmount by remember { mutableStateOf(targetEnvelope?.allocatedAmount?.let { if (it == 0.0) "" else it.toString() } ?: "") }

    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commaTransformation = remember { ThousandsSeparatorTransformation() }

    val isFormValid by remember {
        derivedStateOf {
            name.isNotBlank() && (targetAmount.toDoubleOrNull() ?: 0.0) > 0
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (targetEnvelope != null) "Edit ${targetEnvelope.name}" else "Add New Envelope",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Envelope Name (e.g., Rent, Groceries)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true
            )

            OutlinedTextField(
                value = targetAmount,
                onValueChange = { input ->
                    if (input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' }) { targetAmount = input }
                },
                label = { Text("Monthly Target Amount") },
                placeholder = { Text("$0.00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                visualTransformation = commaTransformation,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = allocatedAmount,
                onValueChange = { input ->
                    if (input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' }) { allocatedAmount = input }
                },
                label = { Text("Currently Allocated (Optional)") },
                placeholder = { Text("$0.00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                visualTransformation = commaTransformation,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onConfirm(name, targetAmount.toDoubleOrNull() ?: 0.0, allocatedAmount.toDoubleOrNull() ?: 0.0) },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (targetEnvelope != null) "Update Envelope" else "Save Envelope")
            }

            if (targetEnvelope != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Envelope")
                }
            }
        }
    }
}
