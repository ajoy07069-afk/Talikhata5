package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.ui.components.TalikhataBrandLogo
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalTalikhataColors
import com.example.ui.theme.PrimaryBlue
import com.example.util.BiometricHelper
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    shopName: String,
    onFinished: () -> Unit
) {
    val scale = remember { Animatable(0.72f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
        delay(850)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DeepNavy, Color(0xFF1E3A8A), Color(0xFF0F172A))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TalikhataBrandLogo(
                size = 104.dp,
                modifier = Modifier.scale(scale.value)
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Talikhata",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = EmeraldGreen.copy(alpha = 0.22f),
                modifier = Modifier.border(1.dp, EmeraldGreen.copy(alpha = 0.6f), RoundedCornerShape(50))
            ) {
                Text(
                    text = shopName.ifBlank { "Amol Pharmacy" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFA7F3D0),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "স্মার্ট অফলাইন হিসাব ও ডিজিটাল খাতা",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFBFDBFE)
            )
            Spacer(modifier = Modifier.height(40.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .width(180.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50)),
                color = EmeraldGreen,
                trackColor = Color.White.copy(alpha = 0.18f)
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    initialShopName: String,
    onComplete: (
        shopName: String,
        ownerName: String,
        phone: String,
        address: String,
        pin: String?,
        fingerprintEnabled: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalTalikhataColors.current
    val biometricSupported = remember { BiometricHelper.isBiometricAvailable(context) }

    var step by remember { mutableIntStateOf(0) }
    var shopName by remember { mutableStateOf(initialShopName.ifBlank { "Amol Pharmacy" }) }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    var enablePin by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var enableFingerprint by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 560.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TalikhataBrandLogo(size = 42.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Talikhata সেটআপ",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "ধাপ ${step + 1} / 3",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.customerBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { (step + 1) / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = PrimaryBlue
            )
            Spacer(modifier = Modifier.height(18.dp))

            Image(
                painter = painterResource(id = R.drawable.img_pharmacy_banner),
                contentDescription = "Amol Pharmacy Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(18.dp))

            AnimatedContent(targetState = step, label = "onboarding_step") { currentStep ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        when (currentStep) {
                            0 -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Storefront,
                                        contentDescription = null,
                                        tint = colors.customerBlue
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "দোকানের তথ্য সেটআপ",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colors.textPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "আপনার ফার্মেসির নাম ও যোগাযোগের তথ্য দিন। এটি রসিদ ও প্রিন্টে ব্যবহৃত হবে।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = shopName,
                                    onValueChange = { shopName = it },
                                    label = { Text("দোকানের নাম") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_shop_name")
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = ownerName,
                                    onValueChange = { ownerName = it },
                                    label = { Text("মালিকের নাম (ঐচ্ছিক)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("মোবাইল নম্বর (ঐচ্ছিক)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it },
                                    label = { Text("ঠিকানা (ঐচ্ছিক)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            1 -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = colors.supplierPurple
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "৪-সংখ্যার পিন নিরাপত্তা",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colors.textPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "অ্যাপ লক পিন চালু করুন",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary
                                    )
                                    Switch(
                                        checked = enablePin,
                                        onCheckedChange = {
                                            enablePin = it
                                            pinError = null
                                        },
                                        modifier = Modifier.testTag("onboarding_pin_switch")
                                    )
                                }
                                if (enablePin) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = pin,
                                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
                                        label = { Text("নতুন ৪-সংখ্যার পিন") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = confirmPin,
                                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) confirmPin = it },
                                        label = { Text("পিন নিশ্চিত করুন") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (pinError != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = pinError!!,
                                            color = colors.warningRed,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "আপনি চাইলে পরেও সেটিংস থেকে পিন লক চালু করতে পারবেন।",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            2 -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Fingerprint,
                                        contentDescription = null,
                                        tint = colors.paymentGreen
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ফিঙ্গারপ্রিন্ট আনলক",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colors.textPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (biometricSupported) {
                                        "আপনার ডিভাইসে ফিঙ্গারপ্রিন্ট/বায়োমেট্রিক সেন্সর পাওয়া গেছে। দ্রুত ও নিরাপদ প্রবেশের জন্য এটি চালু করতে পারেন।"
                                    } else {
                                        "এই ডিভাইসে বায়োমেট্রিক সেন্সর সক্রিয় নেই। আপনি সরাসরি ড্যাশবোর্ডে প্রবেশ করতে পারবেন।"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                                if (biometricSupported && enablePin) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ফিঙ্গারপ্রিন্ট লক চালু রাখুন",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary
                                        )
                                        Switch(
                                            checked = enableFingerprint,
                                            onCheckedChange = { enableFingerprint = it }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.paymentContainer
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = colors.paymentGreen
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "সম্পূর্ণ অফলাইন প্রস্তুত • ${shopName.ifBlank { "Amol Pharmacy" }}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = colors.paymentGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step-- },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("onboarding_back_btn")
                    ) {
                        Text("পূর্ববর্তী")
                    }
                } else {
                    TextButton(
                        onClick = {
                            onComplete("Amol Pharmacy", "", "", "", null, false)
                        },
                        modifier = Modifier.testTag("onboarding_skip_btn")
                    ) {
                        Text("এড়িয়ে যান")
                    }
                }

                Button(
                    onClick = {
                        when (step) {
                            0 -> {
                                if (shopName.isBlank()) shopName = "Amol Pharmacy"
                                step = 1
                            }
                            1 -> {
                                if (enablePin) {
                                    if (pin.length != 4) {
                                        pinError = "৪ সংখ্যার পিন লিখুন"
                                        return@Button
                                    }
                                    if (pin != confirmPin) {
                                        pinError = "পিন দুটি মেলেনি"
                                        return@Button
                                    }
                                }
                                pinError = null
                                step = 2
                            }
                            else -> {
                                onComplete(
                                    shopName,
                                    ownerName,
                                    phone,
                                    address,
                                    if (enablePin) pin else null,
                                    enablePin && enableFingerprint
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.testTag("onboarding_next_btn")
                ) {
                    Text(if (step < 2) "পরবর্তী" else "শুরু করুন")
                }
            }
        }
    }
}

@Composable
fun PinLockScreen(
    shopName: String,
    fingerprintEnabled: Boolean,
    onVerifyPin: (String) -> Boolean,
    onBiometricSuccess: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalTalikhataColors.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val triggerBiometric: () -> Unit = {
        val activity = context as? FragmentActivity
        if (activity != null && BiometricHelper.isBiometricAvailable(context)) {
            BiometricHelper.authenticate(
                activity = activity,
                title = "Talikhata আনলক করুন",
                subtitle = "$shopName এর হিসাব দেখতে ফিঙ্গারপ্রিন্ট দিন",
                negativeButtonText = "পিন ব্যবহার করুন",
                onSuccess = onBiometricSuccess,
                onError = { msg -> errorMsg = msg }
            )
        }
    }

    LaunchedEffect(fingerprintEnabled) {
        if (fingerprintEnabled) {
            triggerBiometric()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(DeepNavy, Color(0xFF0F172A)))
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TalikhataBrandLogo(size = 68.dp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = shopName,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "আপনার ৪-সংখ্যার পিন দিন",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFBFDBFE)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { idx ->
                    val filled = idx < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (filled) EmeraldGreen else Color.White.copy(alpha = 0.25f))
                    )
                }
            }

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMsg!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFCA5A5),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(if (fingerprintEnabled) "FP" else "", "0", "DEL")
            )
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                .size(68.dp)
                                .clip(CircleShape)
                                .then(
                                    if (key.isNotEmpty()) {
                                        Modifier
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .clickable {
                                                errorMsg = null
                                                when (key) {
                                                    "DEL" -> if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                    }
                                                    "FP" -> triggerBiometric()
                                                    else -> {
                                                        if (enteredPin.length < 4) {
                                                            enteredPin += key
                                                            if (enteredPin.length == 4) {
                                                                if (!onVerifyPin(enteredPin)) {
                                                                    errorMsg = "ভুল পিন! আবার চেষ্টা করুন"
                                                                    enteredPin = ""
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when (key) {
                                "DEL" -> Icon(
                                    Icons.AutoMirrored.Rounded.Backspace,
                                    contentDescription = "Backspace",
                                    tint = Color.White
                                )
                                "FP" -> Icon(
                                    Icons.Rounded.Fingerprint,
                                    contentDescription = "Fingerprint Unlock",
                                    tint = colors.paymentGreen,
                                    modifier = Modifier.size(30.dp)
                                )
                                else -> Text(
                                    text = key,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
