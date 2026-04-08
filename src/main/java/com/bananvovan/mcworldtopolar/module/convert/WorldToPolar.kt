package com.bananvovan.mcworldtopolar.module.convert

import com.bananvovan.mcworldtopolar.module.extract.EntityJsonSaver
import net.hollowcube.polar.AnvilPolar
import net.hollowcube.polar.PolarWriter
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.InstanceContainer
import java.nio.file.Files
import java.nio.file.Paths

object WorldToPolar {

    fun convert(path: String) {
        println("Converting: $path")

        // Инициализация Minestom
        MinecraftServer.init()

        val anvilPath = Paths.get(path)
        val resultPath = Paths.get("$path.polar")

        try {
            // Конвертация Anvil -> Polar
            val polarWorld = AnvilPolar.anvilToPolar(anvilPath)
            Files.write(resultPath, PolarWriter.write(polarWorld))

            // Парсим item_display и сохраняем JSON
            EntityJsonSaver.save(anvilPath)

            println("Done: $resultPath")
        } finally {
            MinecraftServer.stopCleanly()
        }
    }
}