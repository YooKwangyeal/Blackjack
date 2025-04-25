package com.example.blackjack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme



data class Card(val suit: String, val value: String, var altValue: Int? = null)
data class Player(var hand: MutableList<Card> = mutableListOf(), var stopped: Boolean = false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlackjackApp()
        }
    }
}

@Composable
fun BlackjackApp() {
    var screen by remember { mutableStateOf("start") }
    var playerCount by remember { mutableStateOf(2) }

    if (screen == "start") {
        StartScreen(playerCount = playerCount, onPlayerCountChange = {
            playerCount = it
        }) {
            screen = "game"
        }
    } else {
        GameScreen(playerCount = playerCount) {
            screen = "start"
        }    }
}

@Composable
fun StartScreen(playerCount: Int, onPlayerCountChange: (Int) -> Unit, onStartGame: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎴 멀티 블랙잭 🎴", fontSize = 32.sp)
        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("플레이어 수: ", fontSize = 18.sp)

            IconButton(
                onClick = { if (playerCount > 2) onPlayerCountChange(playerCount - 1) }
            ) {
                Text("◀")
            }

            Text("$playerCount 명", fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))

            IconButton(
                onClick = { if (playerCount < 6) onPlayerCountChange(playerCount + 1) }
            ) {
                Text("▶")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onStartGame) {
            Text("게임 시작")
        }
    }
}


fun createDeck(): MutableList<Card> {
    val suits = listOf("♠", "♥", "♣", "◆")
    val values = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    val deck = mutableListOf<Card>()

    for (suit in suits) {
        for (value in values) {
            deck.add(Card(suit, value))
        }
    }

    // 조커 추가
    deck.add(Card("🃏", "JOKER-B"))
    deck.add(Card("🃏", "JOKER-C"))

    deck.shuffle()
    return deck
}

fun calcScore(hand: List<Card>): Int {
    var sum = 0
    var aceCount = 0

    for (card in hand) {
        val value = when (card.value) {
            "JOKER-B", "JOKER-C" -> 0
            "A" -> {
                aceCount++
                card.altValue ?: 11
            }
            "J", "Q", "K" -> 10
            else -> card.value.toIntOrNull() ?: 0
        }
        sum += value
    }

    while (sum > 21 && aceCount > 0) {
        sum -= 10
        aceCount--
    }

    return sum
}

@Composable
fun GameScreen(playerCount: Int, onReset: () -> Unit) {
    var deck by remember { mutableStateOf(createDeck()) }
    var players by remember {
        mutableStateOf(
            List(playerCount) {
                Player(hand = mutableListOf(deck.removeAt(0), deck.removeAt(0)))
            }
        )
    }
    var currentPlayer by remember { mutableStateOf(0) }
    var gameEnded by remember { mutableStateOf(false) }

    fun nextTurn() {
        val active = players.withIndex().filter { !it.value.stopped }
        if (active.isEmpty()) {
            gameEnded = true
            return
        }
        do {
            currentPlayer = (currentPlayer + 1) % players.size
        } while (players[currentPlayer].stopped)
    }

    fun drawCard(index: Int) {
        if (!players[index].stopped && deck.isNotEmpty()) {
            players[index].hand.add(deck.removeAt(0))
            if (calcScore(players[index].hand) > 21) {
                players[index].stopped = true
            }
            nextTurn()
        }
    }

    fun stopPlayer(index: Int) {
        players[index].stopped = true
        nextTurn()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🃏 블랙잭 게임 중... 🃏", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        players.forEachIndexed { i, player ->
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .then(if (i == currentPlayer) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) else Modifier),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Player ${i + 1}", fontSize = 20.sp)
                    Row {
                        player.hand.forEach { card ->
                            Text(
                                text = "${card.suit}${card.value}",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                    Text("점수: ${calcScore(player.hand)}")

                    if (!player.stopped && i == currentPlayer && !gameEnded) {
                        Row {
                            Button(onClick = { drawCard(i) }) {
                                Text("Draw")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { stopPlayer(i) }) {
                                Text("Stop")
                            }
                        }
                    } else if (player.stopped) {
                        Text("✔ 스톱", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        if (gameEnded) {
            AlertDialog(
                onDismissRequest = { /* 닫기 비활성화 */ },
                confirmButton = {},
                title = { Text("🎉 결과 발표 🎉") },
                text = {
                    Column {
                        val results = players.mapIndexed { i, p ->
                            val score = calcScore(p.hand)
                            val isBest = players.maxByOrNull { calcScore(it.hand).coerceAtMost(21) } == p && score <= 21
                            val isBlackjack = score == 21 && p.hand.size == 2 &&
                                    p.hand.any { it.value == "A" } &&
                                    p.hand.any { it.value in listOf("10", "J", "Q", "K") }
                            val result = when {
                                isBlackjack -> "🂡 블랙잭!"
                                isBest -> "🏆 승리!"
                                score > 21 -> "💀 꼴지!"
                                else -> ""
                            }
                            "Player ${i + 1}: $score 점 $result"
                        }

                        results.forEach {
                            Text(it, fontSize = 16.sp)
                        }
                    }
                },
                dismissButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = {
                            // 다시 시작
                            deck = createDeck()
                            players = List(playerCount) {
                                Player(hand = mutableListOf(deck.removeAt(0), deck.removeAt(0)))
                            }
                            currentPlayer = 0
                            gameEnded = false
                        }) {
                            Text("🔁 다시하기")
                        }

                        Button(onClick = {
                            gameEnded = false
                            onReset()
                        }) {
                            Text("🏠 초기화면")
                        }
                    }
                }
            )
        }
    }
}
