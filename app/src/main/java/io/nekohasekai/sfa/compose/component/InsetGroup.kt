package io.nekohasekai.sfa.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nekohasekai.sfa.compose.theme.IosBorder
import io.nekohasekai.sfa.compose.theme.IosCard
import io.nekohasekai.sfa.compose.theme.IosTextSecondary

@Composable
fun InsetGroup(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!header.isNullOrEmpty()) {
            Text(
                text = header.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                color = IosTextSecondary,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(IosCard)
                .border(BorderStroke(1.dp, IosBorder), RoundedCornerShape(16.dp)),
            content = content,
        )

        if (!footer.isNullOrEmpty()) {
            Text(
                text = footer,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 17.sp,
                color = IosTextSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
            )
        }
    }
}
