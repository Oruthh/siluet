package dev.siluet.exception

/**
 * Thrown when a JSON blueprint (a template or a section group) is invalid, e.g. when
 * `sectionsOrder` or `blocksOrder` lists a key that is not defined or lists a key more than once.
 *
 * JSON blueprints are reported by file only, without line and column. The message ends with
 * the source name, e.g.
 * `'heroo' listed in sectionsOrder is not defined. Defined: 'hero', 'promo'. in /theme/templates/home.json`.
 *
 * @param message Diagnostic message explaining the failure, without the source name.
 * @property sourceName The [dev.siluet.loader.SiluetAsset.sourceName] of the invalid template or section group.
 * @param cause The underlying exception, if any.
 */
class SiluetBlueprintException(message: String, val sourceName: String, cause: Throwable? = null)
    : SiluetException("$message in $sourceName", cause)
