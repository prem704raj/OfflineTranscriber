package app.offlinetranscriber.mobile.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object AppShapes {
    val Micro = RoundedCornerShape(4.dp)
    val Control = RoundedCornerShape(8.dp)
    val Button = RoundedCornerShape(12.dp)
    val Hero = RoundedCornerShape(16.dp)

    // Backward-compatible aliases for components during migration
    val Pill = RoundedCornerShape(999.dp)
    val Card = Button
    val LargeCard = Hero

    /*
     * Do not create 20/22/24dp blobs for normal content.
     * Large rounding is reserved for genuine hero/physical-card moments.
     */
    val material = Shapes(
        extraSmall = Micro,
        small = Control,
        medium = Button,
        large = Hero,
        extraLarge = Hero
    )
}
