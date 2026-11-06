package com.example.ui.markdown

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AnonCyan
import com.example.ui.theme.AnonDarkBorder
import com.example.ui.theme.AnonDarkSurfaceVariant
import com.example.ui.theme.AnonViolet

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data object Divider : MarkdownBlock()
}

/**
 * Parses markdown text into structured blocks.
 */
fun parseMarkdown(raw: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = raw.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
            i++
            continue
        }

        when {
            trimmed.startsWith("### ") -> {
                blocks.add(MarkdownBlock.Header(3, trimmed.removePrefix("### ").trim()))
            }
            trimmed.startsWith("## ") -> {
                blocks.add(MarkdownBlock.Header(2, trimmed.removePrefix("## ").trim()))
            }
            trimmed.startsWith("# ") -> {
                blocks.add(MarkdownBlock.Header(1, trimmed.removePrefix("# ").trim()))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                blocks.add(MarkdownBlock.BulletItem(trimmed.substring(2).trim()))
            }
            trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                val num = trimmed.substringBefore(".")
                val rest = trimmed.substringAfter(".").trim()
                blocks.add(MarkdownBlock.NumberedItem(num, rest))
            }
            trimmed.startsWith("> ") -> {
                blocks.add(MarkdownBlock.BlockQuote(trimmed.removePrefix("> ").trim()))
            }
            trimmed == "---" || trimmed == "***" -> {
                blocks.add(MarkdownBlock.Divider)
            }
            trimmed.isNotEmpty() -> {
                blocks.add(MarkdownBlock.Paragraph(line))
            }
        }
        i++
    }

    return blocks
}

/**
 * Parses inline formatting like bold, italic, and inline code.
 */
fun buildFormattedText(raw: String, primaryColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val pattern = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*|`.*?`|~~.*?~~)")
        val matches = pattern.findAll(raw).toList()

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > cursor) {
                append(raw.substring(cursor, start))
            }

            val value = match.value
            when {
                value.startsWith("**") && value.endsWith("**") && value.length >= 4 -> {
                    val inner = value.substring(2, value.length - 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(inner)
                    pop()
                }
                value.startsWith("`") && value.endsWith("`") && value.length >= 2 -> {
                    val inner = value.substring(1, value.length - 1)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = primaryColor.copy(alpha = 0.12f),
                            color = primaryColor,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(" $inner ")
                    pop()
                }
                value.startsWith("*") && value.endsWith("*") && value.length >= 2 -> {
                    val inner = value.substring(1, value.length - 1)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(inner)
                    pop()
                }
                else -> {
                    append(value)
                }
            }
            cursor = end
        }

        if (cursor < raw.length) {
            append(raw.substring(cursor))
        }
    }
}

@Composable
fun MarkdownView(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val blocks = parseMarkdown(text)
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val fontSize = when (block.level) {
                        1 -> 22.sp
                        2 -> 18.sp
                        else -> 16.sp
                    }
                    Text(
                        text = buildFormattedText(block.text, primaryColor),
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockCard(
                        language = block.language.ifEmpty { "code" },
                        code = block.code,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("ANONBOT Code", block.code)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = buildFormattedText(block.text, primaryColor),
                            color = textColor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = buildFormattedText(block.text, primaryColor),
                            color = textColor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                is MarkdownBlock.BlockQuote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(primaryColor.copy(alpha = 0.08f))
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(22.dp)
                                .background(primaryColor, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildFormattedText(block.text, primaryColor),
                            fontStyle = FontStyle.Italic,
                            color = textColor.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildFormattedText(block.text, primaryColor),
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                }
                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(
    language: String,
    code: String,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D1117))
            .border(1.dp, Color(0xFF30363D), RoundedCornerShape(8.dp))
    ) {
        // Header with language tag and copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AnonCyan,
                letterSpacing = 1.sp
            )
            IconButton(
                onClick = onCopy,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("copy_code_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = Color(0xFF8B949E),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Code content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = Color(0xFFE6EDF3),
                lineHeight = 18.sp
            )
        }
    }
}
