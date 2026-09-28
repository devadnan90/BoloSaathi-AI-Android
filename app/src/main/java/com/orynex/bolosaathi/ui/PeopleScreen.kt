package com.orynex.bolosaathi.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orynex.bolosaathi.core.Person

private val avatarTones = listOf(
    listOf(Color(0xFF1E4FA3), Color(0xFF38A3E8)),
    listOf(Color(0xFF0E8A7A), Color(0xFF3CC3A8)),
    listOf(Color(0xFFB26A00), Color(0xFFF2A33A)),
    listOf(Color(0xFF6A3FB5), Color(0xFF9B7BE0)),
)

@Composable
fun PeopleScreen(vm: MainViewModel, modifier: Modifier) {
    val people by vm.people.collectAsState()
    var editing by remember { mutableStateOf<Person?>(null) }
    var adding by remember { mutableStateOf(false) }
    val contactsPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Box(modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
            item { PeopleHero(people.size, people.count { it.upi.isNotBlank() }) }

            item {
                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .offset(y = (-26).dp)
                        .enterStagger(1)
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = C.Brand.copy(alpha = 0.16f), ambientColor = C.Brand.copy(alpha = 0.08f))
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .bouncyClick { contactsPerm.launch(Manifest.permission.READ_CONTACTS) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(C.BrandSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Contacts, null, tint = C.Brand)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("फ़ोन के कॉन्टैक्ट भी जोड़ें", style = MaterialTheme.typography.titleMedium, color = C.Ink)
                        Text("तब किसी का भी नाम बोलकर संदेश भेज सकेंगे", style = MaterialTheme.typography.bodySmall, color = C.Muted)
                    }
                }
            }

            if (people.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp).enterStagger(2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(84.dp).clip(CircleShape).background(C.BrandSoft),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Outlined.People, null, tint = C.Brand, modifier = Modifier.size(40.dp)) }
                        Spacer(Modifier.height(14.dp))
                        Text("अभी कोई नहीं जुड़ा", color = C.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "मम्मी, बेटा या किसी को भी जोड़िए, फिर बस बोलिए 'मम्मी को 500 भेज दो'",
                            style = MaterialTheme.typography.bodyMedium, color = C.Muted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            } else {
                item {
                    Text(
                        "आपके लोग", color = C.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 20.dp, bottom = 6.dp),
                    )
                }
            }

            itemsIndexed(people, key = { _, p -> p.name + p.phone }) { i, p ->
                PersonCard(
                    p, i,
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp).enterStagger(i.coerceAtMost(8) + 2).animateItem(),
                    onEdit = { editing = p },
                    onDelete = { vm.deletePerson(p) },
                )
            }
        }

        // add button
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .shadow(14.dp, RoundedCornerShape(30.dp), spotColor = C.Brand.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.horizontalGradient(listOf(C.Brand, C.Accent)))
                .bouncyClick { adding = true }
                .padding(start = 18.dp, end = 22.dp, top = 15.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Add, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("जोड़ें", color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }

    if (adding || editing != null) {
        PersonSheet(editing, onDismiss = { adding = false; editing = null }) { np ->
            vm.savePerson(editing, np); adding = false; editing = null
        }
    }
}

@Composable
private fun PeopleHero(total: Int, withUpi: Int) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0B1F4B), Color(0xFF1E4FA3), Color(0xFF38A3E8))))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 46.dp)
    ) {
        Column {
            Text("मेरे लोग", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "जिन्हें आप अक्सर पैसे या संदेश भेजते हैं",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill("$total", "लोग")
                StatPill("$withUpi", "UPI जुड़े")
            }
        }
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun PersonCard(p: Person, index: Int, modifier: Modifier, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = C.Brand.copy(alpha = 0.13f), ambientColor = C.Brand.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(Brush.linearGradient(avatarTones[index % avatarTones.size])),
            contentAlignment = Alignment.Center,
        ) {
            Text(p.name.take(1), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, color = C.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (p.phone.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Phone, null, tint = C.Muted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(p.phone, style = MaterialTheme.typography.bodySmall, color = C.Muted)
                }
            }
            Spacer(Modifier.height(6.dp))
            val hasUpi = p.upi.isNotBlank()
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).background(if (hasUpi) C.OkSoft else Color(0xFFEEF3F9))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasUpi) {
                    Icon(Icons.Outlined.VerifiedUser, null, tint = C.Ok, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    if (hasUpi) "UPI जुड़ा" else "सिर्फ़ नंबर",
                    style = MaterialTheme.typography.labelSmall, color = if (hasUpi) C.Ok else C.Muted,
                )
            }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "विकल्प", tint = C.Muted) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(14.dp), containerColor = Color.White) {
                DropdownMenuItem(
                    text = { Text("बदलें") },
                    leadingIcon = { Icon(Icons.Outlined.Edit, null, tint = C.Brand) },
                    onClick = { menu = false; onEdit() },
                )
                DropdownMenuItem(
                    text = { Text("हटाएँ", color = C.Warn) },
                    leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = C.Warn) },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PersonSheet(old: Person?, onDismiss: () -> Unit, onSave: (Person) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(old?.name ?: "") }
    var phone by remember { mutableStateOf(old?.phone ?: "") }
    var upi by remember { mutableStateOf(old?.upi ?: "") }
    val valid = name.isNotBlank() && (phone.length >= 10 || upi.contains("@"))
    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color(0xFFF1F5FA),
        unfocusedContainerColor = Color(0xFFF1F5FA),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        cursorColor = C.Brand,
        focusedLabelColor = C.Brand,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
            Text(if (old == null) "नया व्यक्ति जोड़ें" else "जानकारी बदलें", color = C.Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))

            if (old == null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("मम्मी", "पापा", "बेटा", "बेटी", "भैया", "दीदी").forEach { r ->
                        val on = name == r
                        Box(
                            Modifier.clip(RoundedCornerShape(18.dp))
                                .background(if (on) C.Brand else C.BrandSoft)
                                .bouncyClick { name = r }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) { Text(r, color = if (on) Color.White else C.Brand, style = MaterialTheme.typography.labelLarge) }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            TextField(name, { name = it }, label = { Text("नाम, जैसा आप बोलते हैं") }, singleLine = true,
                shape = RoundedCornerShape(16.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            TextField(phone, { v -> phone = v.filter { it.isDigit() }.take(12) }, label = { Text("मोबाइल नंबर") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(16.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            TextField(upi, { upi = it.trim() }, label = { Text("UPI आईडी (पैसे भेजने के लिए)") }, placeholder = { Text("98xxxxxx@ybl") },
                singleLine = true, shape = RoundedCornerShape(16.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.fillMaxWidth().height(58.dp)
                    .clip(RoundedCornerShape(29.dp))
                    .background(
                        if (valid) Brush.horizontalGradient(listOf(C.Brand, C.Accent))
                        else Brush.linearGradient(listOf(Color(0xFFC7D3E2), Color(0xFFC7D3E2)))
                    )
                    .bouncyClick(enabled = valid) { onSave(Person(name.trim(), phone.trim(), upi.trim())) },
                contentAlignment = Alignment.Center,
            ) { Text("सेव करें", color = Color.White, style = MaterialTheme.typography.labelLarge) }
        }
    }
}
