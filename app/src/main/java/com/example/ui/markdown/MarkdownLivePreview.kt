package com.example.ui.markdown

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AnonCyan
import com.example.ui.theme.AnonDarkBorder
import com.example.ui.theme.AnonDarkCard
import com.example.ui.theme.AnonViolet

/**
 * Live Markdown Preview Container displayed dynamically above the user's input box.
 */
@Composable
fun MarkdownLivePreviewCard(
    rawText: String,
    isVisible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && rawText.isNotBlank(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(listOf(AnonCyan.copy(alpha = 0.5f), AnonViolet.copy(alpha = 0.5f))),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                )
                .testTag("markdown_preview_card")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header badge & close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(AnonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Live Preview",
                                tint = AnonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE MARKDOWN PREVIEW",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AnonCyan,
                            letterSpacing = 0.5.sp
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("close_preview_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close preview",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.heightIn(min = 6.dp))

                // Scrollable live formatted content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    MarkdownView(
                        text = rawText,
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Quick Markdown Formatting Toolbar with buttons to inject Markdown tags into user text.
 */
@Composable
fun MarkdownFormatToolbar(
    onInsert: (prefix: String, suffix: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FormatChip(
            label = "Bold",
            icon = Icons.Default.FormatBold,
            onClick = { onInsert("**", "**") }
        )
        FormatChip(
            label = "Italic",
            icon = Icons.Default.FormatItalic,
            onClick = { onInsert("*", "*") }
        )
        FormatChip(
            label = "Heading",
            icon = Icons.Default.Title,
            onClick = { onInsert("### ", "") }
        )
        FormatChip(
            label = "Code",
            icon = Icons.Default.Code,
            onClick = { onInsert("`", "`") }
        )
        FormatChip(
            label = "Code Block",
            icon = Icons.Default.Code,
            onClick = { onInsert("```kotlin\n", "\n```") }
        )
        FormatChip(
            label = "Bullet",
            icon = Icons.Default.FormatListBulleted,
            onClick = { onInsert("- ", "") }
        )
        FormatChip(
            label = "Quote",
            icon = Icons.Default.FormatQuote,
            onClick = { onInsert("> ", "") }
        )
    }
}

@Composable
private fun FormatChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(14.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            labelColor = MaterialTheme.colorScheme.onSurface,
            leadingIconContentColor = AnonCyan
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.heightIn(max = 28.dp).testTag("format_${label.lowercase().replace(' ', '_')}")
    )
}
