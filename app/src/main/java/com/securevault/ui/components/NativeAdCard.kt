// app/src/main/java/com/securevault/ui/components/NativeAdCard.kt
package com.securevault.ui.components

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * NativeAdCard - Material 3 styled native ad component
 *
 * Displays Google AdMob native ads in a card format that matches
 * the SecureVault password card design while being visually distinct
 * with surfaceVariant color and "Ad" label.
 *
 * Privacy Note: Ads are displayed using Google AdMob SDK. No password
 * data is transmitted to ad networks. See README.md for privacy details.
 *
 * @param nativeAd The NativeAd object loaded from AdMob SDK
 */
@Composable
fun NativeAdCard(nativeAd: NativeAd) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ad label for transparency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ad",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Native ad view
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    NativeAdView(context).apply {
                        // Set the NativeAdView's NativeAd
                        setNativeAd(nativeAd)

                        // Create layout for ad components
                        val adContainer = View.inflate(
                            context,
                            android.R.layout.simple_list_item_2,
                            null
                        )

                        // Populate ad content
                        nativeAd.headline?.let { headline ->
                            val headlineView = android.widget.TextView(context).apply {
                                text = headline
                                textSize = 16f
                                setTextColor(context.getColor(android.R.color.black))
                                maxLines = 2
                                ellipsize = android.text.TextUtils.TruncateAt.END
                            }
                            this.headlineView = headlineView
                            addView(headlineView)
                        }

                        nativeAd.body?.let { body ->
                            val bodyView = android.widget.TextView(context).apply {
                                text = body
                                textSize = 14f
                                setTextColor(context.getColor(android.R.color.darker_gray))
                                maxLines = 3
                                ellipsize = android.text.TextUtils.TruncateAt.END
                                setPadding(0, 8, 0, 0)
                            }
                            this.bodyView = bodyView
                            addView(bodyView)
                        }

                        // Call to action button
                        nativeAd.callToAction?.let { cta ->
                            val ctaButton = android.widget.Button(context).apply {
                                text = cta
                                textSize = 14f
                                setPadding(16, 8, 16, 8)
                            }
                            this.callToActionView = ctaButton
                            addView(ctaButton)
                        }
                    }
                }
            )
        }
    }
}

/**
 * Composable variant using Material 3 components
 * Provides a more Compose-native approach to displaying ads
 */
@Composable
fun NativeAdCardCompose(nativeAd: NativeAd) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ad label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sponsored",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Headline
            nativeAd.headline?.let { headline ->
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Body
            nativeAd.body?.let { body ->
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Call to action
            nativeAd.callToAction?.let { cta ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = cta,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Native ad view wrapper (required for AdMob)
            AndroidView(
                modifier = Modifier.height(0.dp), // Hidden but required
                factory = { context ->
                    NativeAdView(context).apply {
                        setNativeAd(nativeAd)
                    }
                }
            )
        }
    }
}
