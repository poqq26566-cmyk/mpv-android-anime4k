package com.fam4k007.videoplayer.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import com.fam4k007.videoplayer.R
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.compose.SettingsColors as SettingsPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    email: String,
    githubUrl: String,
    onBack: () -> Unit,
    onOpenEmail: () -> Unit,
    onOpenGithub: () -> Unit
) {
    Scaffold(
        topBar = {
            ImmersiveTopAppBar(
                title = { Text(text = stringResource(R.string.feedback_title), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsPalette.ScreenBackground)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            item { FeedbackContactCard(email = email, githubUrl = githubUrl) }
            item {
                FeedbackActionCard(
                    email = email,
                    githubUrl = githubUrl,
                    onOpenEmail = onOpenEmail,
                    onOpenGithub = onOpenGithub
                )
            }
            item { FeedbackTipsCard() }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun FeedbackContactCard(
    email: String,
    githubUrl: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsPalette.CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.feedback_contact), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SettingsPalette.PrimaryText)
            Spacer(Modifier.height(12.dp))

            ContactRow(icon = Icons.Default.Email, label = stringResource(R.string.feedback_email_label), value = email)
            Divider(modifier = Modifier.padding(vertical = 12.dp), color = SettingsPalette.Divider)
            ContactRow(icon = Icons.Default.OpenInNew, label = "GitHub Issue", value = githubUrl)
        }
    }
}

@Composable
private fun ContactRow(icon: ImageVector, label: String, value: String) {
    val chipColor = MaterialTheme.colorScheme.primary

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SettingsPalette.IconContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = chipColor)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label, fontSize = 14.sp, color = SettingsPalette.SecondaryText)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SettingsPalette.PrimaryText)
        }
    }
}

@Composable
private fun FeedbackActionCard(
    email: String,
    githubUrl: String,
    onOpenEmail: () -> Unit,
    onOpenGithub: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsPalette.CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.feedback_now), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SettingsPalette.PrimaryText)
            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.feedback_choose_method),
                fontSize = 14.sp,
                color = SettingsPalette.SecondaryText
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onOpenEmail,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SettingsPalette.AccentText,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.feedback_email, email))
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onOpenGithub,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SettingsPalette.AccentText
                ),
                border = BorderStroke(1.dp, SettingsPalette.AccentText)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.feedback_github_issue))
            }
        }
    }
}

@Composable
private fun FeedbackTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsPalette.CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.feedback_guide), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SettingsPalette.PrimaryText)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.feedback_guide_1), color = SettingsPalette.PrimaryText)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.feedback_guide_2), color = SettingsPalette.PrimaryText)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.feedback_guide_3), color = SettingsPalette.PrimaryText)
        }
    }
}
