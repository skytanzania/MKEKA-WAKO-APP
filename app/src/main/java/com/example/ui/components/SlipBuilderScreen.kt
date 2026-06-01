package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.MainViewModel
import kotlin.random.Random
import kotlinx.coroutines.delay

// Mock Matches data model
data class MockMatch(
    val id: Int,
    val homeTeam: String,
    val awayTeam: String,
    val league: String,
    val time: String,
    val homeOdds: Double,
    val drawOdds: Double,
    val awayOdds: Double
)

// Active Slips data model for on-device list
data class SimulatedSlip(
    val id: String,
    val date: String,
    val totalOdds: Double,
    val stake: Double,
    val potentialPayout: Double,
    val bookingCode: String,
    val matchPredictions: List<String>
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SlipBuilderScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Preloaded Soccer Matches
    val matches = remember {
        listOf(
            MockMatch(1, "Arsenal", "Chelsea", "Premier League", "18:30 GMT", 1.85, 3.45, 3.80),
            MockMatch(2, "Real Madrid", "Barcelona", "La Liga", "20:00 GMT", 2.10, 3.60, 3.10),
            MockMatch(3, "Man City", "Liverpool", "Premier League", "15:00 GMT", 1.95, 3.75, 3.40),
            MockMatch(4, "Bayern Munich", "Dortmund", "Bundesliga", "17:30 GMT", 1.62, 4.20, 4.80),
            MockMatch(5, "PSG", "Marseille", "Ligue 1", "19:45 GMT", 1.45, 4.50, 5.80),
            MockMatch(6, "Inter Milan", "AC Milan", "Serie A", "19:30 GMT", 2.22, 3.25, 3.15),
            MockMatch(7, "Napoli", "Juventus", "Serie A", "17:00 GMT", 2.05, 3.10, 3.65),
            MockMatch(8, "Atletico Madrid", "Sevilla", "La Liga", "14:15 GMT", 1.70, 3.50, 4.60)
        )
    }

    // Maps Match ID -> Chosen outcome ("1", "X", "2", or null)
    var selectedPredictions by remember { mutableStateOf(mapOf<Int, String>()) }
    var stakeInput by remember { mutableStateOf("10000") }
    var activeSavedSlips by remember { mutableStateOf(listOf<SimulatedSlip>()) }
    
    // Dialog state for a newly generated Slip
    var createdSlipForDialog by remember { mutableStateOf<SimulatedSlip?>(null) }
    var isSimulatingProcess by remember { mutableStateOf(false) }

    // Dynamic math logic
    val selectedMatchesCount = selectedPredictions.filter { it.value != null }.size
    
    val calculatedTotalOdds = remember(selectedPredictions) {
        if (selectedPredictions.isEmpty()) 1.0 else {
            var product = 1.0
            selectedPredictions.forEach { (matchId, outcome) ->
                val match = matches.find { it.id == matchId }
                if (match != null) {
                    val odds = when (outcome) {
                        "1" -> match.homeOdds
                        "X" -> match.drawOdds
                        "2" -> match.awayOdds
                        else -> 1.0
                    }
                    product *= odds
                }
            }
            product
        }
    }

    val finalOddsToDisplay = if (selectedMatchesCount == 0) 0.0 else calculatedTotalOdds
    val numericStake = stakeInput.toDoubleOrNull() ?: 0.0
    val potentialPayout = finalOddsToDisplay * numericStake

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F13)) // High-end luxury deep background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Hero Banner with ambient gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E3A8A).copy(alpha = 0.40f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "PREDICTION SLIP BUILDER",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD700) // Golden Accent
                        )
                        Text(
                            text = "Simulate slips completely offline with no network load.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1F2937), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Sims",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "MOCK MODE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                }
            }

            // Real-time odds & payout summary meter
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A24)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Selected Events",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "$selectedMatchesCount match(es)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Accumulated Odds",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = String.format("%.2fx", finalOddsToDisplay),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981) // Vibrant green
                            )
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Virtual Stake Amount Input
                        OutlinedTextField(
                            value = stakeInput,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.all { it.isDigit() }) {
                                    stakeInput = input
                                }
                            },
                            label = { Text("Mock Stake (TZS)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color(0xFF12121A),
                                unfocusedContainerColor = Color(0xFF12121A)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(56.dp)
                                .testTag("mock_stake_input")
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Simulated Win Out
                        Column(
                            modifier = Modifier.weight(1.0f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Potential Win",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = String.format("%,.0f TZS", potentialPayout),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD700),
                                textAlign = TextAlign.End
                            )
                        }
                    }

                    // Simulated Generation CTA
                    Button(
                        onClick = {
                            if (selectedMatchesCount == 0) {
                                Toast.makeText(context, "Select at least 1 match prediction event!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSimulatingProcess = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("simulate_slip_cta")
                    ) {
                        Text(
                            text = "CREATE VIRTUAL MOCK SLIP",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Matches and archived Slips List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "TODAY'S SPORTS ODDS PLAYGROUND",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                items(matches) { match ->
                    val chosen = selectedPredictions[match.id]

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF161620)
                        ),
                        border = BorderStroke(
                            1.dp, 
                            if (chosen != null) Color(0xFFFFD700).copy(alpha = 0.40f) else Color.White.copy(alpha = 0.05f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // League and Time Info Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = match.league.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = match.time,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }

                            // Match Teams Heading
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = match.homeTeam,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start
                                )
                                Text(
                                    text = "VS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Text(
                                    text = match.awayTeam,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }

                            // Interactive Prediction Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Home Odds Button (1)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (chosen == "1") Color(0xFFFFD700).copy(alpha = 0.15f) else Color(0xFF1E1E2A)
                                        )
                                        .border(
                                            1.dp,
                                            if (chosen == "1") Color(0xFFFFD700) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedPredictions = if (chosen == "1") {
                                                selectedPredictions - match.id
                                            } else {
                                                selectedPredictions + (match.id to "1")
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "1", 
                                            style = MaterialTheme.typography.labelSmall, 
                                            fontWeight = FontWeight.Black, 
                                            color = if (chosen == "1") Color(0xFFFFD700) else Color.White.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            String.format("%.2f", match.homeOdds), 
                                            style = MaterialTheme.typography.labelMedium, 
                                            fontWeight = FontWeight.Bold, 
                                            color = if (chosen == "1") Color(0xFFFFD700) else Color.White
                                        )
                                    }
                                }

                                // Draw Odds Button (X)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (chosen == "X") Color(0xFFFFD700).copy(alpha = 0.15f) else Color(0xFF1E1E2A)
                                        )
                                        .border(
                                            1.dp,
                                            if (chosen == "X") Color(0xFFFFD700) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedPredictions = if (chosen == "X") {
                                                selectedPredictions - match.id
                                            } else {
                                                selectedPredictions + (match.id to "X")
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "X", 
                                            style = MaterialTheme.typography.labelSmall, 
                                            fontWeight = FontWeight.Black, 
                                            color = if (chosen == "X") Color(0xFFFFD700) else Color.White.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            String.format("%.2f", match.drawOdds), 
                                            style = MaterialTheme.typography.labelMedium, 
                                            fontWeight = FontWeight.Bold, 
                                            color = if (chosen == "X") Color(0xFFFFD700) else Color.White
                                        )
                                    }
                                }

                                // Away Odds Button (2)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (chosen == "2") Color(0xFFFFD700).copy(alpha = 0.15f) else Color(0xFF1E1E2A)
                                        )
                                        .border(
                                            1.dp,
                                            if (chosen == "2") Color(0xFFFFD700) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedPredictions = if (chosen == "2") {
                                                selectedPredictions - match.id
                                            } else {
                                                selectedPredictions + (match.id to "2")
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "2", 
                                            style = MaterialTheme.typography.labelSmall, 
                                            fontWeight = FontWeight.Black, 
                                            color = if (chosen == "2") Color(0xFFFFD700) else Color.White.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            String.format("%.2f", match.awayOdds), 
                                            style = MaterialTheme.typography.labelMedium, 
                                            fontWeight = FontWeight.Bold, 
                                            color = if (chosen == "2") Color(0xFFFFD700) else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // If saved mock slips exist offline
                if (activeSavedSlips.isNotEmpty()) {
                    item {
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))
                        Text(
                            text = "MY GENERATED COMPLETED SLIPS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }

                    items(activeSavedSlips) { archivedSlip ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1C1B22)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = archivedSlip.bookingCode,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = archivedSlip.date,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Total Odds: " + String.format("%.2f", archivedSlip.totalOdds),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Stake: " + String.format("%,.0f TZS", archivedSlip.stake),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "Potential: " + String.format("%,.0f TZS", archivedSlip.potentialPayout),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFFD700),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(archivedSlip.bookingCode))
                                        Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.08f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .height(36.dp)
                                ) {
                                    Text("Copy Booking Code", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Simulating Spinner blocking modal
        if (isSimulatingProcess) {
            Dialog(onDismissRequest = { isSimulatingProcess = false }) {
                LaunchedEffect(Unit) {
                    delay(1200) // Beautiful simulated loading lag
                    val generatedCode = "MK-MOCK-" + Random.nextInt(10000, 99999) + "-TZ"
                    val descList = selectedPredictions.map { (id, op) ->
                        val m = matches.find { it.id == id }
                        "${m?.homeTeam} vs ${m?.awayTeam}: Pick $op"
                    }
                    val newSlip = SimulatedSlip(
                        id = UUIDGenerator.get(),
                        date = "Today",
                        totalOdds = finalOddsToDisplay,
                        stake = numericStake,
                        potentialPayout = potentialPayout,
                        bookingCode = generatedCode,
                        matchPredictions = descList
                    )
                    activeSavedSlips = listOf(newSlip) + activeSavedSlips
                    isSimulatingProcess = false
                    createdSlipForDialog = newSlip
                }

                Card(
                    modifier = Modifier
                        .size(200.dp)
                        .shadow(16.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFFD700))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "GENERATING SLIP...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Detailed Printable Digital Ticket Dialog
        createdSlipForDialog?.let { slip ->
            Dialog(onDismissRequest = { createdSlipForDialog = null }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White // Receipt-look white backing style
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .shadow(24.dp, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "MKEKA APP EXCLUSIVE TICKET",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "OFFLINE TEST SLIP RECEIPT",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                        
                        Divider(color = Color.Black.copy(alpha = 0.15f), thickness = 2.dp)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            slip.matchPredictions.forEach { pred ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Event",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = pred,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Divider(color = Color.Black.copy(alpha = 0.15f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Accumulator Odds:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            Text(String.format("%.2f", slip.totalOdds), color = Color.Black, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Virtual Stake:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            Text(String.format("%,.0f TZS", slip.stake), color = Color.Black, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Potential Return:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            Text(String.format("%,.0f TZS", slip.potentialPayout), color = Color(0xFF10B981), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Custom Canvas-drawn Barcode! This draws stripes like a real ticket receipt.
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(Color.White)
                        ) {
                            val barcodeWidth = size.width
                            val stripeCount = 42
                            val random = java.util.Random(slip.hashCode().toLong())
                            
                            var currentX = 0f
                            for (i in 0 until stripeCount) {
                                val drawBlack = random.nextBoolean()
                                val stripeWidth = random.nextInt(3, 16).toFloat()
                                if (drawBlack) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(currentX, 0f),
                                        size = androidx.compose.ui.geometry.Size(stripeWidth, size.height)
                                    )
                                }
                                currentX += stripeWidth
                                if (currentX >= barcodeWidth) break
                            }
                        }

                        Text(
                            text = slip.bookingCode,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Copy Action
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(slip.bookingCode))
                                    Toast.makeText(context, "Code copied into memory!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("COPY CODE")
                            }

                            // Dismiss Action
                            OutlinedButton(
                                onClick = { createdSlipForDialog = null },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color.Black),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("CLOSE RECIPT")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simple unique counter for simulator objects
object UUIDGenerator {
    private var idx = 0
    fun get(): String = "Slip-${System.currentTimeMillis()}-${idx++}"
}
