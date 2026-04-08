package com.bananvovan.mcworldtopolar.module.convert

import com.bananvovan.mcworldtopolar.module.extract.EntityLoader
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

        // Создаём временный инстанс для спавна сущностей
        val instance: InstanceContainer = MinecraftServer.getInstanceManager().createInstanceContainer()

        val anvilPath = Paths.get(path)
        val resultPath = Paths.get("$path.polar")

        try {
            // Конвертация Anvil -> Polar
            val polarWorld = AnvilPolar.anvilToPolar(anvilPath)
            Files.write(resultPath, PolarWriter.write(polarWorld))

            // Парсим item_display и сохраняем JSON
            EntityLoader.loadEntities(instance, anvilPath)

            println("Done: $resultPath")
        } finally {
            MinecraftServer.stopCleanly()
        }
    }
}