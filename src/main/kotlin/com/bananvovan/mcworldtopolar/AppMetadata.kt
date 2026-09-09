package com.bananvovan.mcworldtopolar

import java.net.URL
import java.util.Properties

object AppMetadata {

    private val properties = Properties().apply {
        AppMetadata::class.java.getResourceAsStream("/application.properties")?.use(::load)
    }

    val name: String = properties.getProperty("app.name", "MC World to Polar")
    val version: String = properties.getProperty("app.version", "development")
    val vendor: String = properties.getProperty("app.vendor", "BananVovan34")
    val description: String = properties.getProperty(
        "app.description",
        "Desktop converter for Minecraft Anvil worlds and Polar world data",
    )
    val iconUrl: URL? = AppMetadata::class.java.getResource("/icons/app.png")

    val versionText: String
        get() = "$name $version"
}
