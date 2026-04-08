package com.bananvovan.mcworldtopolar.module.extract

import com.google.gson.GsonBuilder
import org.jglrxavpok.hephaistos.mca.RegionFile
import org.jglrxavpok.hephaistos.nbt.NBTCompound
import org.jglrxavpok.hephaistos.nbt.NBTDouble
import org.jglrxavpok.hephaistos.nbt.NBTFloat
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.use

object EntityJsonSaver {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun save(worldFolder: Path) {
        val results = mutableListOf<Map<String, Any>>()

        val entitiesDir = worldFolder.resolve("entities").toFile()
        if (!entitiesDir.exists()) return

        val tempRoot = Files.createTempDirectory("mc-entities-").toFile()
        val tempEntitiesDir = File(tempRoot, "entities")
        tempEntitiesDir.mkdirs()

        try {
            // Копируем region файлы
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

                                val chunk = region.getChunkData(regionX * 32 + cx, regionZ * 32 + cz)
                                    ?: continue
                                val entities = chunk.getList<NBTCompound>("Entities") ?: continue

                                for (nbt in entities) {
                                    val type = nbt.getString("id") ?: continue
                                    val pos = nbt.getList<NBTDouble>("Pos")?.map { it.value } ?: listOf(0.0, 0.0, 0.0)
                                    val rotation = nbt.getList<NBTFloat>("Rotation")?.map { it.value } ?: listOf(0f, 0f)

                                    val base = mutableMapOf<String, Any>(
                                        "type" to type,
                                        "pos" to pos,
                                        "rotation" to rotation
                                    )

                                    when (type) {
                                        "minecraft:item_display" -> {
                                            nbt.getCompound("item")?.let { itemTag ->
                                                base["item"] = mapOf("id" to itemTag.getString("id"))
                                            }

                                            val transformationMap = extractTransformation(nbt)
                                            if (transformationMap != null) {
                                                base["transformation"] = transformationMap
                                            }

                                            nbt.getString("billboard")?.let { base["billboard"] = it }
                                        }

                                        "minecraft:block_display" -> {
                                            base["block"] = nbt.getString("block_state") ?: "minecraft:stone"

                                            val transformationMap = extractTransformation(nbt)
                                            if (transformationMap != null) {
                                                base["transformation"] = transformationMap
                                            }
                                        }

                                        "minecraft:text_display" -> {
                                            base["text"] = nbt.getString("text") ?: "{\"text\":\"\"}"
                                            base["line_width"] = nbt.getInt("line_width") as Any
                                            base["background"] = nbt.getInt("background") as Any
                                            base["see_through"] = nbt.getByte("see_through") == 1.toByte()
                                            base["shadow"] = nbt.getByte("shadow") == 1.toByte()

                                            val transformationMap = extractTransformation(nbt)
                                            if (transformationMap != null) {
                                                base["transformation"] = transformationMap
                                            }
                                        }

                                        "minecraft:armor_stand" -> {
                                            base["invisible"] = nbt.getByte("Invisible") == 1.toByte()
                                            base["no_gravity"] = nbt.getByte("NoGravity") == 1.toByte()
                                            base["small"] = nbt.getByte("Small") == 1.toByte()
                                            base["arms"] = nbt.getByte("ShowArms") == 1.toByte()
                                            base["base_plate"] = nbt.getByte("NoBasePlate") == 1.toByte()
                                            base["marker"] = nbt.getByte("Marker") == 1.toByte()
                                        }

                                        "minecraft:item_frame" -> {
                                            nbt.getCompound("Item")?.let { itemTag ->
                                                base["item"] = mapOf("id" to itemTag.getString("id"))
                                            }
                                            nbt.getByte("Facing")?.let { base["facing"] = it.toInt() }
                                            nbt.getByte("ItemRotation")?.let { base["rotation_item"] = it.toInt() }
                                            base["invisible"] = nbt.getByte("Invisible") == 1.toByte()
                                        }

                                        "minecraft:painting" -> {
                                            base["variant"] = nbt.getString("variant") ?: ""
                                        }
                                    }

                                    results.add(base)
                                }
                            }
                        }
                    }
                }
            }

            val output = File(worldFolder.toFile(), "entities.json")
            output.writeText(gson.toJson(results))
            println("Saved ${results.size} entities → ${output.absolutePath}")

        } finally {
            tempRoot.deleteRecursively()
        }
    }

    private fun extractTransformation(nbt: NBTCompound): Map<String, Any>? {
        val transformationMap = mutableMapOf<String, Any>()
        val transformation = nbt.getCompound("transformation") ?: return null
        transformation.getList<NBTFloat>("scale")?.let { transformationMap["scale"] = it.map { f -> f.value } }
        transformation.getList<NBTFloat>("translation")?.let { transformationMap["translation"] = it.map { f -> f.value } }
        transformation.getList<NBTFloat>("left_rotation")?.let { transformationMap["left_rotation"] = it.map { f -> f.value } }
        transformation.getList<NBTFloat>("right_rotation")?.let { transformationMap["right_rotation"] = it.map { f -> f.value } }

        return transformationMap.ifEmpty { null }
    }
}