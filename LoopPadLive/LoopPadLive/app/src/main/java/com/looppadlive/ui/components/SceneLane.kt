package com.looppadlive.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.looppadlive.data.model.Project
import com.looppadlive.data.model.Scene
import com.looppadlive.ui.theme.*

@Composable
fun SceneLane(
    project: Project,
    activeSceneRow: Int,
    onSceneTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(72.dp)
            .background(BackgroundPanel)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(project.effectiveRows) { rowIdx ->
            val scene = project.scenes.find { it.rowIndex == rowIdx }
            val isActive = activeSceneRow == rowIdx
            SceneButton(
                scene = scene,
                rowIndex = rowIdx,
                isActive = isActive,
                onTap = { onSceneTap(rowIdx) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SceneButton(
    scene: Scene?,
    rowIndex: Int,
    isActive: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBreak = scene?.isBreak == true
    val label = scene?.name ?: "S${rowIndex + 1}"

    val bgColor by animateColorAsState(
        targetValue = when {
            isActive && isBreak -> RubyRed.copy(alpha = 0.25f)
            isActive -> ElectricBlue.copy(alpha = 0.25f)
            else -> SurfaceCard
        },
        animationSpec = tween(100),
        label = "sceneBg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isActive && isBreak -> RubyRed
            isActive -> ElectricBlue
            else -> SurfaceBorder
        },
        animationSpec = tween(100),
        label = "sceneBorder"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor)
            .border(if (isActive) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(5.dp))
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isActive) "Scene active" else "Launch scene",
                tint = when {
                    isActive && isBreak -> RubyRed
                    isActive -> ElectricBlue
                    isBreak -> RubyRed.copy(alpha = 0.6f)
                    else -> OnSurfaceVariant
                },
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isActive) Color.White else OnSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
