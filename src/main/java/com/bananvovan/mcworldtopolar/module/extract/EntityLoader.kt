package com.bananvovan.mcworldtopolar.module.extract

import com.google.gson.GsonBuilder
import net.minestom.server.instance.Instance
import org.jglrxavpok.hephaistos.mca.RegionFile
import org.jglrxavpok.hephaistos.nbt.NBTCompound
import org.jglrxavpok.hephaistos.nbt.NBTDouble
import org.jglrxavpok.hephaistos.nbt.NBTFloat
import org.jglrxavpok.hephaistos.nbt.NBTList
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path

object EntityLoader {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun loadEntities(instance: Instance, worldFolder: Path) {
        val results = mutableListOf<Map<String, Any>>()
        val entitiesDir = worldFolder.resolve("entities").toFile()
        if (!entitiesDir.exists()) return

        // Временная папка для копий .mca
        val tempRoot = Files.createTempDirectory("mc-entities-").toFile()
        val tempEntitiesDir = File(tempRoot, "entities")
        tempEntitiesDir.mkdirs()

        try {
            // Копируем .mca файлы
            entitiesDir.listFiles { _, name -> name.endsWith(".mca") }?.forEach { src ->
                val dst = File(tempEntitiesDir, src.name)
                src.copyTo(dst, overwrite = true)
                dst.setWritable(true)
            }

            tempEntitiesDir.listFiles { _, name -> name.endsWith(".mca") }?.forEach { file ->
                val parts = file.name.split(".")
                val regionX = parts[1].toInt()
                val regionZ = parts[2].toInt()

                RandomAccessFile(file, "rw").use { raf ->
                    RegionFile(raf, regionX, regionZ).use { region ->
                        for (cx in 0 until 32) {
                            for (cz in 0 until 32) {
                                val chunkNbt: NBTCompound = region.getChunkData(regionX * 32 + cx, regionZ * 32 + cz)
                                    ?: continue
                                val entities: NBTList<NBTCompound> = chunkNbt.getList("Entities") ?: continue

                                for (entityNbt in entities) {
                                    val id = entityNbt.getString("id") ?: continue

                                    val pos = entityNbt.getList<NBTDouble>("Pos")?.map { it.value } ?: listOf(0.0, 0.0, 0.0)
                                    val rotation = entityNbt.getList<NBTFloat>("Rotation")?.map { it.value } ?: listOf(0f, 0f)
                                    val scale = entityNbt.getList<NBTFloat>("Scale")?.map { it.value } ?: listOf(1f, 1f, 1f)

                                    val additionalData = entityNbt.getCompound("item")?.let { itemTag ->
                                        mapOf(
                                            "itemId" to itemTag.getString("id"),
                                            "model" to itemTag.getCompound("display")?.getString("model")
                                        )
                                    }

                                    results.add(
                                        mapOf(
                                            "type" to id,
                                            "pos" to pos,
                                            "rotation" to rotation,
                                            "scale" to scale,
                                            "data" to (additionalData ?: mapOf<String, Any>())
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Сохраняем JSON рядом с миром
            val output = File(worldFolder.toFile(), "entities.json")
            output.writeText(gson.toJson(results))
            println("Saved ${results.size} entity entries to ${output.absolutePath}")

        } finally {
            tempRoot.deleteRecursively()
        }
    }
}