package com.example.humsafar.ui.bonus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors

/**
 * Mini 4×4 Sudoku. Each row, column and 2×2 box must contain 1–4 exactly once.
 * Tap an editable cell to cycle 1→2→3→4→blank. Board completes → win.
 */
@Composable
fun SudokuGameScreen(onSolved: () -> Unit) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current

    // Given clues (0 = blank/editable). A valid completion exists.
    val given = remember {
        intArrayOf(
            1, 0, 0, 4,
            0, 4, 1, 0,
            0, 1, 4, 0,
            4, 0, 0, 1
        )
    }
    val isGiven = remember { BooleanArray(16) { given[it] != 0 } }
    val board = remember { mutableStateListOf<Int>().apply { addAll(given.toList()) } }

    fun isValid(): Boolean {
        if (board.any { it == 0 }) return false
        fun ok(vals: List<Int>) = vals.sorted() == listOf(1, 2, 3, 4)
        for (r in 0 until 4) if (!ok((0 until 4).map { board[r * 4 + it] })) return false
        for (c in 0 until 4) if (!ok((0 until 4).map { board[it * 4 + c] })) return false
        val boxes = listOf(
            listOf(0, 1, 4, 5), listOf(2, 3, 6, 7),
            listOf(8, 9, 12, 13), listOf(10, 11, 14, 15)
        )
        for (b in boxes) if (!ok(b.map { board[it] })) return false
        return true
    }

    LaunchedEffect(board.toList()) {
        if (isValid()) onSolved()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("🔢 Mini Sudoku", color = tokens.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text("Fill every row, column & box with 1–4. Tap to cycle.",
            color = tokens.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))

        Box(
            Modifier.clip(RoundedCornerShape(14.dp)).background(tokens.surfaceMuted).padding(6.dp)
        ) {
            Column {
                for (r in 0 until 4) {
                    Row {
                        for (c in 0 until 4) {
                            val idx = r * 4 + c
                            val value = board[idx]
                            val locked = isGiven[idx]
                            Box(
                                Modifier
                                    .padding(
                                        start = if (c == 2) 5.dp else 2.dp,
                                        top = if (r == 2) 5.dp else 2.dp,
                                        end = 2.dp, bottom = 2.dp
                                    )
                                    .size(62.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (locked) tokens.surfaceMuted else tokens.surface)
                                    .border(1.dp, tokens.border, RoundedCornerShape(10.dp))
                                    .clickable(enabled = !locked) {
                                        board[idx] = (board[idx] + 1) % 5   // 0..4 cycle
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (value == 0) "" else "$value",
                                    color = if (locked) tokens.textTertiary else accent.dark,
                                    fontSize = 24.sp,
                                    fontWeight = if (locked) FontWeight.Medium else FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
