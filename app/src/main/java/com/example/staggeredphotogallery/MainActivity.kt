package com.example.staggeredphotogallery

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.staggeredphotogallery.ui.theme.StaggeredPhotoGalleryTheme
import java.io.InputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StaggeredPhotoGalleryTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PhotoGallery(
                            photos = parseXML(this),
                            modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoGallery(photos: List<Photo>, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier =
                    modifier.fillMaxWidth()
                            .padding(top = 32.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) { items(photos) { photo -> PhotoCard(photo = photo) } }
}

@Composable
fun PhotoCard(photo: Photo, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isEnlarged by remember { mutableStateOf(false) }
    val scale by
            animateFloatAsState(
                    targetValue = if (isEnlarged) 1.5f else 1f,
                    animationSpec =
                            spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                            ),
                    label = "scale animation"
            )

    val imageId =
            remember(photo.file) {
                context.resources.getIdentifier(
                        photo.file.substringBeforeLast("."),
                        "drawable",
                        context.packageName
                )
            }

    Card(
            modifier = modifier.clickable { isEnlarged = !isEnlarged },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            if (imageId != 0) {
                Image(
                        painter = painterResource(id = imageId),
                        contentDescription = photo.title,
                        modifier =
                                Modifier.fillMaxWidth().aspectRatio(1f).graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                        contentScale = ContentScale.Crop
                )
            } else {
                // Handle the case where the image resource is not found
                Text("Image not found")
            }
            Text(
                    text = photo.title,
                    modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

fun parseXML(context: Context): List<Photo> {
    val photos = mutableListOf<Photo>()
    var inputStream: InputStream? = null

    try {
        inputStream = context.resources.openRawResource(R.raw.photos)
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        var currentTitle: String? = null
        var currentFile: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "title" -> {
                            currentTitle = parser.nextText()
                        }
                        "file" -> {
                            currentFile = parser.nextText()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "photo") {
                        if (currentTitle != null && currentFile != null) {
                            photos.add(Photo(currentTitle, currentFile))
                            currentTitle = null
                            currentFile = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        inputStream?.close()
    }

    return photos
}

data class Photo(val title: String, val file: String)
