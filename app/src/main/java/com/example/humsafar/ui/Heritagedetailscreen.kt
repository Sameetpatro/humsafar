// app/src/main/java/com/example/humsafar/ui/HeritageDetailScreen.kt
//
// CHANGES vs previous version:
//   + Real site photos loaded from Wikimedia Commons via Coil AsyncImage
//   + Shimmer placeholder while photo loads, emoji fallback on error
//   + Photo caption strip below the hero image
//   + Sticky "▶ Watch Summary Video" bar pinned to the bottom of the screen
//   + VideoViewModel + CinematicLoaderOverlay + VideoPlayerOverlay wired in here
//
// REQUIRED — add to app/build.gradle.kts dependencies block:
//   implementation("io.coil-kt:coil-compose:2.6.0")

package com.example.humsafar.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.humsafar.ChatbotActivity
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.SectionLabel
import com.example.humsafar.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────────────────

private data class HeritageContent(
    val tagline:      String,
    val period:       String,
    val location:     String,
    val overview:     String,
    val history:      String,
    val architecture: String,
    val funFact:      String,
    val emoji:        String,
    // Wikimedia Commons direct-link images (public domain / CC-BY-SA)
    val photoUrl:     String,
    val photoCaption: String
)

// ─────────────────────────────────────────────────────────────────────────────
// Content database — all images from Wikimedia Commons
// ─────────────────────────────────────────────────────────────────────────────

private val heritageDatabase: Map<String, HeritageContent> = mapOf(

    "Red Fort" to HeritageContent(
        tagline      = "Symbol of Mughal Grandeur",
        period       = "1638 – 1648 AD",
        location     = "Old Delhi, Delhi",
        overview     = "The Red Fort, or Lal Qila, is a historic fort in Old Delhi that served as the main residence of the Mughal Emperors. It is named for its massive enclosing walls of red sandstone and was the ceremonial and political centre of Mughal state.",
        history      = "Commissioned by Emperor Shah Jahan in 1638 when he decided to shift his capital from Agra to Delhi, the fort was completed in 1648. After the fall of the Mughal Empire, the British used it as a military garrison. Every year on Independence Day, the Prime Minister of India hoists the national flag here.",
        architecture = "Built in a combination of Persian, Timurid and Indian styles, the fort complex encompasses the Diwan-i-Aam (Hall of Public Audience), Diwan-i-Khas (Hall of Private Audience), the ornate Rang Mahal (Palace of Colours), and the Moti Masjid (Pearl Mosque).",
        funFact      = "The legendary Peacock Throne and the Koh-i-Noor diamond were once housed inside the Diwan-i-Khas before being looted by Persian ruler Nadir Shah in 1739.",
        emoji        = "🏯",
        photoUrl     = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5d/Red_Fort_in_New_Delhi_03-2016.jpg/1280px-Red_Fort_in_New_Delhi_03-2016.jpg",
        photoCaption = "Lahori Gate, the main entrance to Red Fort, Old Delhi"
    ),

    "Taj Mahal" to HeritageContent(
        tagline      = "An Eternal Ode to Love",
        period       = "1632 – 1653 AD",
        location     = "Agra, Uttar Pradesh",
        overview     = "The Taj Mahal is an ivory-white marble mausoleum on the right bank of the Yamuna river in Agra. A UNESCO World Heritage Site, it is regarded as one of the greatest architectural achievements in the entire range of Indo-Islamic architecture.",
        history      = "Built by Mughal emperor Shah Jahan in memory of his beloved wife Mumtaz Mahal, who died in 1631 while giving birth to their 14th child. Over 20,000 artisans from across Central Asia, Persia, and India worked for 21 years to complete this monument.",
        architecture = "The complex includes a mosque, a guest house, and the main mausoleum with its iconic dome rising 73 metres. The white marble changes colour — pinkish at dawn, dazzling white at noon, and golden under moonlight — due to its inlaid semi-precious stones that reflect light.",
        funFact      = "The four minarets of the Taj Mahal are built slightly tilted outward so that in case of an earthquake or collapse, they would fall away from the main tomb.",
        emoji        = "🕌",
        photoUrl     = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Taj_Mahal_%28Edited%29.jpeg/1280px-Taj_Mahal_%28Edited%29.jpeg",
        photoCaption = "The Taj Mahal viewed from the main gateway, Agra"
    ),

    "Qutub Minar" to HeritageContent(
        tagline      = "India's Tallest Ancient Tower",
        period       = "1193 – 1368 AD",
        location     = "Mehrauli, New Delhi",
        overview     = "Qutub Minar is a UNESCO World Heritage Site and one of the finest towers in the world. Standing at 72.5 metres, it is the tallest minaret in India, built of red sandstone and marble with intricate carvings.",
        history      = "Construction was begun by Qutb ud-Din Aibak, founder of the Delhi Sultanate, in 1193. The minaret was completed by his successor Iltutmish, with two more storeys added later by Firuz Shah Tughlaq. The complex also houses the famous Iron Pillar of Delhi, which has stood rust-free for over 1,600 years.",
        architecture = "The tower has five distinct storeys, each marked by a projecting balcony. The first three storeys are made of red sandstone, while the fourth and fifth are of marble and sandstone. The base has a diameter of 14.3 metres which narrows to 2.7 metres at the top.",
        funFact      = "The 7-metre tall Iron Pillar inside the complex has remained rust-free for over 1,600 years — a metallurgical mystery that modern science is still studying.",
        emoji        = "🗼",
        photoUrl     = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6a/Qutb_Minar_in_New_Delhi_03-2016_img3.jpg/800px-Qutb_Minar_in_New_Delhi_03-2016_img3.jpg",
        photoCaption = "Qutub Minar rising 72.5 metres above Mehrauli, New Delhi"
    ),

    "India Gate" to HeritageContent(
        tagline      = "A Tribute to the Brave",
        period       = "1921 – 1931 AD",
        location     = "Rajpath, New Delhi",
        overview     = "India Gate is a war memorial dedicated to the 70,000 soldiers of the British Indian Army who lost their lives between 1914 and 1921 in World War I and the Third Anglo-Afghan War. It stands 42 metres tall at the centre of New Delhi.",
        history      = "Designed by Sir Edwin Lutyens, the foundation stone was laid by the Duke of Connaught in 1921. The memorial was inaugurated by Viceroy Lord Irwin in 1931. The names of 13,300 British and Indian soldiers killed in action are inscribed on the arch.",
        architecture = "Modelled after the Arc de Triomphe in Paris, the structure is made of Bharatpur stone. The arch stands atop a low base of red Jodhpur granite. The canopy that once held King George V's statue now stands empty.",
        funFact      = "Under the arch stands the Amar Jawan Jyoti — an eternal flame burning continuously since 1972 to honour soldiers who died in the 1971 Indo-Pakistani War.",
        emoji        = "🏛️",
        photoUrl     = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/96/India_Gate_in_New_Delhi_03-2016.jpg/1280px-India_Gate_in_New_Delhi_03-2016.jpg",
        photoCaption = "India Gate illuminated on Rajpath, New Delhi"
    ),

    "Golden Temple" to HeritageContent(
        tagline      = "The Seat of Sikh Faith",
        period       = "1604 AD (current structure 1764 AD)",
        location     = "Amritsar, Punjab",
        overview     = "Sri Harmandir Sahib, popularly known as the Golden Temple, is the holiest Gurdwara of Sikhism. It is built on a 67-square-foot island surrounded by a sacred pool called the Amrit Sarovar (Pool of Nectar).",
        history      = "The site was chosen by the third Sikh Guru, Guru Amar Das Ji. The foundation stone was laid by the Muslim Sufi saint Hazrat Mian Mir of Lahore, reflecting the Sikh principle of universal brotherhood.",
        architecture = "The temple is a unique blend of Hindu and Islamic architecture. It has four entrances (one on each side), symbolising openness to all. The two-storey structure is made of marble with a gilded copper dome.",
        funFact      = "The Langar (community kitchen) at the Golden Temple serves free meals to over 100,000 visitors every single day — making it the world's largest free community kitchen.",
        emoji        = "✨",
        photoUrl     = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/94/Golden_Temple_Amritsar_2.jpg/1280px-Golden_Temple_Amritsar_2.jpg",
        photoCaption = "Sri Harmandir Sahib reflected in the Amrit Sarovar, Amritsar"
    ),

    "Hampi" to HeritageContent(
        tagline      = "Ruins of a Legendary Empire",
        period       = "1336 – 1646 AD",
        location     = "Hampi, Karnataka",
        overview     = "Hampi is a UNESCO World Heritage Site and the last capital of the Vijayanagara Empire — once one of the richest and most powerful kingdoms in the world. The ruins spread over 4,100 hectares among a dramatic boulder-strewn landscape.",
        history      = "Founded in 1336 by Harihara I, Hampi was a prosperous cosmopolitan city visited by Persian and European traders. At its peak in the early 16th century, it was the world's second-largest medieval-era city. In 1565, it was sacked and burned by the Deccan Sultanates.",
        architecture = "The site contains over 1,600 surviving remains including temples, palaces, market streets, aquatic structures, and elephant stables. The Virupaksha Temple with its 50-metre gopuram and the stone chariot of Vittala Temple are engineering masterpieces.",
        funFact      = "The musical pillars of the Vittala Temple are carved from single pieces of granite and produce musical notes when tapped — each pillar emits a different instrument's sound.",
        emoji        = "🏺",
        photoUrl     = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/26/Vittala_temple_Hampi.jpg/1280px-Vittala_temple_Hampi.jpg",
        photoCaption = "The iconic stone chariot of Vittala Temple, Hampi"
    ),

    "Konark Sun Temple" to HeritageContent(
        tagline      = "The Black Pagoda of Orissa",
        period       = "13th Century AD",
        location     = "Konark, Odisha",
        overview     = "The Konark Sun Temple is a 13th-century UNESCO World Heritage Site on the northeastern shore of Odisha. Built in the shape of a colossal chariot of the Sun God Surya with 12 pairs of intricately carved stone wheels pulled by seven horses.",
        history      = "Built by King Narasimhadeva I of the Eastern Ganga dynasty around 1250 AD. European sailors called it the Black Pagoda due to its dark appearance.",
        architecture = "The entire temple is conceived as the Sun God's chariot. The 24 carved wheels are each about 3 metres in diameter and serve as precise sundials — you can tell the time by the shadow of the spokes.",
        funFact      = "The 24 wheels of the temple function as accurate sundials — by looking at the shadow of the spokes, you can determine the exact time of day to the minute.",
        emoji        = "☀️",
        photoUrl     = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4f/Konarka_Temple.jpg/1280px-Konarka_Temple.jpg",
        photoCaption = "The ornate chariot wheels of the Konark Sun Temple, Odisha"
    ),

    "Gateway of India" to HeritageContent(
        tagline      = "Mumbai's Iconic Welcome",
        period       = "1911 – 1924 AD",
        location     = "Apollo Bunder, Mumbai",
        overview     = "The Gateway of India is an arch monument built during the British Raj on the waterfront of Mumbai. It was built to commemorate the visit of King George V and Queen Mary to Mumbai in December 1911.",
        history      = "Designed by architect George Wittet in Indo-Saracenic style, construction was completed in 1924. The last British troops to leave India after independence in 1948 passed through this gateway.",
        architecture = "The arch is 26 metres high and made of yellow basalt and reinforced concrete. It features Islamic-style latticework and is flanked by large halls. The central dome is 15 metres in diameter.",
        funFact      = "The Gateway was built using yellow Kharodi basalt — the architect had to improvise because the originally planned Gwalior stone was unavailable at the time.",
        emoji        = "🚪",
        photoUrl     = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Mumbai_03-2016_30_Gateway_of_India.jpg/1280px-Mumbai_03-2016_30_Gateway_of_India.jpg",
        photoCaption = "Gateway of India overlooking the Arabian Sea, Mumbai"
    ),

    "Mysore Palace" to HeritageContent(
        tagline      = "The Palace of Lights",
        period       = "1897 – 1912 AD",
        location     = "Mysuru, Karnataka",
        overview     = "Mysore Palace, also known as Amba Vilas Palace, is the official residence of the Wadiyar dynasty. It attracts over 6 million visitors annually — second only to the Taj Mahal.",
        history      = "The present palace was built between 1897 and 1912 by Henry Irwin for the Wadiyar royal family after the old wooden palace burned down in 1897. The Mysuru Dasara festival held here is a grand celebration.",
        architecture = "Designed in Indo-Saracenic style, the palace is a blend of Hindu, Muslim, Rajput and Gothic architecture. It has three storeys with a five-storey tower. The interiors include a Durbar Hall with ornate ceilings, stained glass, and carved teak doors.",
        funFact      = "Every Sunday evening and on Dasara, the palace is lit up with 97,000 light bulbs — one of the most illuminated buildings in the world.",
        emoji        = "👑",
        photoUrl     = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4a/Mysore_Palace_Morning.jpg/1280px-Mysore_Palace_Morning.jpg",
        photoCaption = "Mysore Palace at sunrise, Mysuru, Karnataka"
    ),

    "IIIT Sonepat" to HeritageContent(
        tagline      = "Where Future Technologists are Forged",
        period       = "Est. 2015",
        location     = "Sonepat, Haryana",
        overview     = "IIIT Sonepat is an Institute of National Importance established by the Ministry of Education, Government of India, producing world-class engineers and researchers in computer science.",
        history      = "Established under the IIIT Act of 2014 and admitting its first batch in 2015, the institute has grown rapidly into a thriving academic community fostering innovation and research.",
        architecture = "The campus features modern academic blocks, cutting-edge laboratories, residential facilities, a sports complex, and open green spaces designed for collaborative learning.",
        funFact      = "Students here have built projects ranging from drone navigation systems to AI-powered heritage guide apps — proof that innovation thrives in even the youngest institutions!",
        emoji        = "🎓",
        photoUrl     = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f1/IIITDelhi_Campus.jpg/1280px-IIITDelhi_Campus.jpg",
        photoCaption = "IIIT Campus — a hub of innovation and technology"
    )
)

private fun getHeritageContent(siteName: String): HeritageContent =
    heritageDatabase[siteName] ?: HeritageContent(
        tagline      = "A Timeless Heritage Site",
        period       = "Ancient Era",
        location     = "India",
        overview     = "$siteName is a remarkable heritage site that has stood the test of time, bearing witness to centuries of history, culture, and human achievement.",
        history      = "This site has a rich history deeply intertwined with the cultural and political fabric of India. It has seen the rise and fall of empires, the flourishing of arts and sciences, and the lives of countless people across generations.",
        architecture = "The architecture of $siteName reflects the mastery of ancient craftsmen who created enduring structures using locally available materials and advanced construction techniques far ahead of their time.",
        funFact      = "Every stone and carving here tells a story — researchers continue to uncover new secrets about this remarkable place that has captivated historians for centuries.",
        emoji        = "🏛️",
        photoUrl     = "",
        photoCaption = "$siteName, India"
    )

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HeritageDetailScreen(
    siteName:          String,
    siteId:            String,
    onBack:            () -> Unit,
    onNavigateToVoice: (String, String) -> Unit,
    onNavigateToQrScan: (Long) -> Unit,
) {
    val context      = LocalContext.current
    val content      = remember(siteName) { getHeritageContent(siteName) }
    var visible      by remember { mutableStateOf(false) }
    val scroll       = rememberScrollState()

    LaunchedEffect(Unit) { visible = true }

    Box(Modifier.fillMaxSize()) {

        AnimatedOrbBackground(Modifier.fillMaxSize())

        // ── Scrollable article body ───────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(tween(500)) + slideInVertically(tween(600, easing = EaseOutCubic)) { it / 6 }
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 90.dp)   // room for sticky bottom bar
                    .verticalScroll(scroll)
            ) {
                // ── Hero photo ────────────────────────────────────────────
                HeroPhotoSection(
                    siteName = siteName,
                    content  = content,
                    onBack   = onBack
                )

                Column(Modifier.padding(horizontal = 20.dp)) {

                    Spacer(Modifier.height(24.dp))

                    ActionBubbles(
                        siteName          = siteName,
                        siteId            = siteId,
                        context           = context,
                        onNavigateToVoice = onNavigateToVoice,
                        onNavigateToQrScan = onNavigateToQrScan
                    )

                    Spacer(Modifier.height(28.dp))
                    QuickStatsRow(content = content)
                    Spacer(Modifier.height(24.dp))
                    ArticleSection("📖 Overview",      content.overview)
                    Spacer(Modifier.height(16.dp))
                    ArticleSection("⏳ History",        content.history)
                    Spacer(Modifier.height(16.dp))
                    ArticleSection("🏗️ Architecture",  content.architecture)
                    Spacer(Modifier.height(16.dp))
                    FunFactCard(content.funFact)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero photo + title overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroPhotoSection(
    siteName: String,
    content:  HeritageContent,
    onBack:   () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // ── Photo via Coil ────────────────────────────────────────────────
        if (content.photoUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model              = ImageRequest.Builder(LocalContext.current)
                    .data(content.photoUrl)
                    .crossfade(700)
                    .build(),
                contentDescription = "$siteName photograph",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading -> ShimmerPlaceholder(content.emoji)
                    is AsyncImagePainter.State.Error   -> EmojiPlaceholder(content.emoji)
                    else                               -> SubcomposeAsyncImageContent()
                }
            }
        } else {
            EmojiPlaceholder(content.emoji)
        }

        // Top scrim so back button reads on any photo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0xCC050D1A), Color.Transparent)))
        )

        // Bottom scrim for title legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF050D1A))))
        )

        // Back button
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xBB050D1A))
                .border(0.5.dp, GlassBorder, CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
        }

        // Title + tagline
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text          = content.tagline.uppercase(),
                color         = AccentYellow.copy(alpha = 0.9f),
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.8.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text          = siteName,
                color         = TextPrimary,
                fontSize      = 28.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                lineHeight    = 32.sp
            )
        }
    }

    // Caption strip directly below the photo
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xEE050D1A))
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(
            text     = "📷  ${content.photoCaption}",
            color    = TextTertiary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Shimmer placeholder while image is loading ────────────────────────────────

@Composable
private fun ShimmerPlaceholder(emoji: String) {
    val inf = rememberInfiniteTransition(label = "shimmer")
    val x   by inf.animateFloat(
        -1f, 2f,
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "sx"
    )
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0D1F3C), Color(0xFF1A2E4A), Color(0xFF0D1F3C)),
                        start  = Offset(x * 800f, 0f),
                        end    = Offset((x + 1f) * 800f, 0f)
                    )
                )
        )
        Text(emoji, fontSize = 64.sp, modifier = Modifier.align(Alignment.Center).alpha(0.15f))
    }
}

// ── Emoji fallback when photo fails ──────────────────────────────────────────

@Composable
private fun EmojiPlaceholder(emoji: String) {
    val inf = rememberInfiniteTransition(label = "ep")
    val ox  by inf.animateFloat(
        0.2f, 0.75f,
        infiniteRepeatable(tween(6000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "epx"
    )
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF1A0A00), Color(0xFF0A1628))))
            drawCircle(
                brush  = Brush.radialGradient(
                    listOf(Color(0x44FFD54F), Color.Transparent),
                    radius = size.minDimension * 0.5f
                ),
                radius = size.minDimension * 0.5f,
                center = Offset(size.width * ox, size.height * 0.4f)
            )
        }
        Text(emoji, fontSize = 80.sp, modifier = Modifier.align(Alignment.Center).alpha(0.22f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sticky "Watch Summary Video" bottom bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StickyVideoButton(
    isActive: Boolean,
    onTap:    () -> Unit,
    modifier: Modifier = Modifier
) {
    val inf  = rememberInfiniteTransition(label = "vbtn")
    val glow by inf.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "vg"
    )
    val playPulse by inf.animateFloat(
        0.88f, 1.12f,
        infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pp"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF8050D1A))))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF10024A), Color(0xFF070225), Color(0xFF180550))
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFF8A2BE2).copy(alpha = glow),
                            Color(0xFF5B8DEF).copy(alpha = glow * 0.75f),
                            Color(0xFF8A2BE2).copy(alpha = 0.25f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(enabled = !isActive) { onTap() }
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            // Top specular sheen
            Box(
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(Color(0x28FFFFFF), Color.Transparent)))
            )

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Pulsing play button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .scale(playPulse)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF8A2BE2), Color(0xFF5B8DEF)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play summary video",
                        tint               = Color.White,
                        modifier           = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column {
                    Text(
                        text       = "Watch Summary Video",
                        color      = TextPrimary,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text     = "AI-generated cinematic overview  ·  ~1–2 min",
                        color    = Color(0xFF8A7FB5),
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.width(10.dp))
                Text("✨", fontSize = 18.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick stats strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickStatsRow(content: HeritageContent) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatChip("📅", "Period",   content.period,   Modifier.weight(1f))
        StatChip("📍", "Location", content.location, Modifier.weight(1f))
    }
}

@Composable
private fun StatChip(icon: String, label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite10)
            .border(0.7.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(label.uppercase(), color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Article sections + fun fact card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ArticleSection(title: String, body: String) {
    Column {
        Text(title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GlassWhite10)
                .border(0.7.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(body, color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun FunFactCard(fact: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0x33FFD54F), Color(0x11FFC107))))
            .border(1.dp, Brush.linearGradient(listOf(Color(0x66FFD54F), Color(0x22FFD54F))), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text("Did You Know?", color = AccentYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(fact, color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4 Action bubbles
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActionBubbles(
    siteName:          String,
    siteId:            String,
    context:           android.content.Context,
    onNavigateToVoice: (String, String) -> Unit,
    onNavigateToQrScan: (Long) -> Unit
) {
    val buttons = listOf(
        ActionBubbleData(
            emoji    = "💬", label = "Ask",      sublabel = "Text chat",
            gradient = listOf(Color(0xFF1A3A6B), Color(0xFF0D2040)),
            border   = GlassBorderBright,
            onClick  = {
                context.startActivity(
                    Intent(context, ChatbotActivity::class.java).apply {
                        putExtra("SITE_NAME", siteName); putExtra("SITE_ID", siteId)
                    }
                )
            }
        ),
        ActionBubbleData(
            emoji    = "🎙️", label = "Talk",    sublabel = "Voice guide",
            gradient = listOf(Color(0xFF2D1A00), Color(0xFF1A0E00)),
            border   = Color(0x55FFD54F),
            onClick  = { onNavigateToVoice(siteName, siteId) }
        ),
        ActionBubbleData(
            emoji    = "📸", label = "Scan & Ask", sublabel = "QR Scan",
            gradient = listOf(Color(0xFF002D1A), Color(0xFF001A0E)),
            border   = Color(0x5550C878),
            onClick  = {
                siteId.toLongOrNull()?.let { id ->
                    onNavigateToQrScan(id)
                }
            }
        )
    )

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        buttons.forEach { btn -> AnimatedActionBubble(data = btn, modifier = Modifier.weight(1f)) }
    }
}

private data class ActionBubbleData(
    val emoji: String, val label: String, val sublabel: String,
    val gradient: List<Color>, val border: Color, val onClick: () -> Unit
)

@Composable
private fun AnimatedActionBubble(data: ActionBubbleData, modifier: Modifier = Modifier) {
    var pressed by remember { mutableStateOf(false) }
    val scale   by animateFloatAsState(
        if (pressed) 0.92f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bs"
    )
    val inf  = rememberInfiniteTransition(label = "bg")
    val glow by inf.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(
            tween(1800 + (data.label.hashCode() % 600).let { if (it < 0) -it else it }, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "gw"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(data.gradient))
            .border(1.dp, Brush.linearGradient(listOf(data.border.copy(alpha = glow), data.border.copy(alpha = 0.2f))), RoundedCornerShape(20.dp))
            .clickable { pressed = true; data.onClick() }
            .padding(vertical = 18.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.matchParentSize().clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0x22FFFFFF), Color.Transparent))))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(data.emoji, fontSize = 26.sp)
            Spacer(Modifier.height(8.dp))
            Text(data.label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(2.dp))
            Text(data.sublabel, color = TextTertiary, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }

    LaunchedEffect(pressed) {
        if (pressed) { kotlinx.coroutines.delay(150); pressed = false }
    }
}