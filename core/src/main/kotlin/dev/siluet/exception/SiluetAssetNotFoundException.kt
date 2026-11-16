package dev.siluet.exception

class SiluetAssetNotFoundException(val assetType: String, val name: String)
    : SiluetException("Could not find $assetType '$name'")