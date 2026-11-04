package dev.siluet.render

import java.time.ZoneId
import java.util.Locale

data class RenderContext(
    val locale: Locale,
    val zoneId: ZoneId,
) {
    class Builder {
        var locale: Locale = Locale.getDefault()
        var zoneId: ZoneId = ZoneId.systemDefault()

        fun build(): RenderContext {
            return RenderContext(
                locale = locale,
                zoneId = zoneId,
            )
        }
    }
}
