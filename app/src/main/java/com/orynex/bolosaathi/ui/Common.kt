package com.orynex.bolosaathi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    color: Color = C.Surface,
    border: Color = C.Line,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color,
        border = BorderStroke(1.dp, border),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = C.Ink)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = C.Muted)
        }
        trailing?.invoke()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = C.Muted,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp, top = 8.dp),
    )
}
