package com.aitrader.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitrader.app.ui.neon.*
import com.aitrader.app.viewmodel.ChatMessage
import com.aitrader.app.viewmodel.ChatViewModel
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val messages by vm.messages.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Track IME visibility so we can re-scroll to the bottom when the keyboard
    // appears (the list shrinks as the input lifts, so the latest message
    // would otherwise scroll out of view).
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)

    LaunchedEffect(messages.size, isLoading, imeBottomPx) {
        val target = messages.size + if (isLoading) 1 else 0
        if (target > 0) listState.animateScrollToItem(target - 1)
    }

    fun send(text: String) {
        val q = text.trim()
        if (q.isEmpty() || isLoading) return
        vm.sendMessage(q)
        inputText = ""
        keyboardController?.hide()
    }

    val suggestions = listOf(
        "Scan for breakouts",
        "Why is my portfolio down?",
        "Close all losing positions",
        "Market regime today?",
    )

    Column(
        modifier = Modifier.fillMaxSize().background(NeonTokens.Bg),
    ) {
        // ── Header ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border(1.dp, NeonTokens.Neon),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "[AI]",
                    color = NeonTokens.Neon,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AGENT",
                        color = NeonTokens.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = NeonTokens.DisplayFamily,
                    )
                    Text(
                        "/01",
                        color = NeonTokens.Neon,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = NeonTokens.DisplayFamily,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    LiveDot(color = NeonTokens.Bull, size = 5.dp)
                    Text(
                        "ONLINE",
                        color = NeonTokens.TextMute,
                        fontSize = 9.sp,
                        fontFamily = NeonTokens.MonoFamily,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clickable { vm.clearChat() }
                    .padding(8.dp),
            ) {
                Text(
                    "CLEAR",
                    color = NeonTokens.TextMute,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
        }
        Divider()

        // ── Messages ───────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages) { msg -> MessageBubble(msg) }
            if (isLoading) {
                item { TypingBubble() }
            }
        }

        // ── Suggestions ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            suggestions.forEach { s ->
                Box(
                    modifier = Modifier
                        .border(1.dp, NeonTokens.Border)
                        .clickable { send(s) }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                ) {
                    Text(
                        s,
                        color = NeonTokens.TextMute,
                        fontSize = 10.sp,
                        fontFamily = NeonTokens.MonoFamily,
                    )
                }
            }
        }

        // ── Input ──────────────────────────────────────────
        // imePadding lifts the input row above the soft keyboard. The
        // Scaffold above us doesn't resize around the IME on its own (edge-to-edge
        // + fixed bottom nav bar), so the Row itself has to consume the insets.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(NeonTokens.BgElev1)
                    .border(1.dp, NeonTokens.Border)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    ">",
                    color = NeonTokens.Neon,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NeonTokens.MonoFamily,
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (inputText.isEmpty()) {
                        Text(
                            "ask agent...",
                            color = NeonTokens.TextDim,
                            fontSize = 12.sp,
                            fontFamily = NeonTokens.MonoFamily,
                        )
                    }
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        textStyle = TextStyle(
                            color = NeonTokens.Text,
                            fontSize = 12.sp,
                            fontFamily = NeonTokens.MonoFamily,
                        ),
                        cursorBrush = SolidColor(NeonTokens.Neon),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send(inputText) }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Box(
                    modifier = Modifier
                        .background(if (inputText.isNotBlank() && !isLoading) NeonTokens.Neon else NeonTokens.Border)
                        .clickable(enabled = inputText.isNotBlank() && !isLoading) { send(inputText) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "SEND",
                        color = if (inputText.isNotBlank() && !isLoading) Color.Black else NeonTokens.TextDim,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = NeonTokens.MonoFamily,
                    )
                }
            }
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NeonTokens.Border),
    )
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    if (isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(NeonTokens.BgElev3)
                    .border(1.dp, NeonTokens.Border)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    msg.content,
                    color = NeonTokens.Text,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(1.dp, NeonTokens.Neon),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "*",
                    color = NeonTokens.Neon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.widthIn(max = 300.dp)) {
                Text(
                    "AGENT",
                    color = NeonTokens.Neon,
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .background(NeonTokens.BgElev1)
                        .border(1.dp, NeonTokens.Border)
                        .padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    if (msg.isAction) {
                        // Action-style messages (trade confirmations) use a
                        // fixed bull-color mono line; no markdown.
                        Text(
                            msg.content,
                            color = NeonTokens.Bull,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = NeonTokens.MonoFamily,
                        )
                    } else {
                        AgentMarkdown(msg.content)
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentMarkdown(content: String) {
    val baseText = TextStyle(
        color = NeonTokens.Text,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontFamily = NeonTokens.MonoFamily,
    )
    Markdown(
        content = content,
        colors = markdownColor(
            text = NeonTokens.Text,
            codeText = NeonTokens.Neon,
            inlineCodeText = NeonTokens.Neon,
            linkText = NeonTokens.Neon,
            codeBackground = NeonTokens.BgElev2,
            inlineCodeBackground = NeonTokens.BgElev2,
            dividerColor = NeonTokens.Border,
        ),
        typography = markdownTypography(
            text = baseText,
            paragraph = baseText,
            h1 = baseText.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeonTokens.Neon, fontFamily = NeonTokens.DisplayFamily),
            h2 = baseText.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeonTokens.Neon, fontFamily = NeonTokens.DisplayFamily),
            h3 = baseText.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonTokens.Text, fontFamily = NeonTokens.DisplayFamily),
            h4 = baseText.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonTokens.Text),
            h5 = baseText.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonTokens.Text),
            h6 = baseText.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonTokens.TextMute),
            code = baseText.copy(color = NeonTokens.Neon),
            inlineCode = baseText.copy(color = NeonTokens.Neon),
            quote = baseText.copy(color = NeonTokens.TextMute),
            bullet = baseText,
            list = baseText,
            ordered = baseText,
            link = baseText.copy(color = NeonTokens.Neon),
        ),
    )
}

@Composable
private fun TypingBubble() {
    val transition = rememberInfiniteTransition(label = "typing")
    val alphas = (0..2).map { i ->
        transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, delayMillis = i * 150, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dot$i",
        ).value
    }
    Row(
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(1.dp, NeonTokens.Neon),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "*",
                color = NeonTokens.Neon,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NeonTokens.MonoFamily,
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .background(NeonTokens.BgElev1)
                .border(1.dp, NeonTokens.Border)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            alphas.forEach { a ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .graphicsLayer { alpha = a }
                        .background(NeonTokens.Neon),
                )
            }
        }
    }
}
