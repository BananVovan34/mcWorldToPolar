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
import kotlin.collections.forEach
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
                                            val itemTag = nbt.getCompound("item")

                                            base["item"] = itemTag?.let { tag ->
                                                val itemMap = mutableMapOf<String, Any>(
                                                    "id" to tag.getString("id") as Any
                                                )

                                                val components = tag.getCompound("components")
                                                val modelRaw = components?.getString("minecraft:item_model")
                                                if (modelRaw != null) {
                                                    val model = modelRaw as Any

                                                    itemMap["model"] = model
                                                }

                                                itemMap
                                            } as Any

                                            val transformationMap = extractTransformation(nbt)
                                            if (transformationMap != null) {
                                                base["transformation"] = transformationMap
                                            }

                                            nbt.getString("billboard")?.let { base["billboard"] = it }
                                        }

                                        "minecraft:block_display" -> {
                                            val blockState = nbt.getCompound("block_state")

                                            blockState?.let {
                                                val blockMap = mutableMapOf<String, Any>()

                                                blockMap["name"] = it.getString("Name") ?: "minecraft:stone"

                                                val props = it.getCompound("Properties")
                                                props?.let { p ->
                                                    val propsMap = mutableMapOf<String, String>()
                                                    p.keys.forEach { key ->
                                                        propsMap[key] = props.getString(key) as String
                                                    }
                                                    if (propsMap.isNotEmpty()) {
                                                        blockMap["properties"] = propsMap
                                                    }
                                                }

                                                base["block"] = blockMap

                                                nbt.getString("billboard")?.let { base["billboard"] = it }
                                            }

                                            val transformationMap = extractTransformation(nbt)
                                            if (transformationMap != null) {
                                                base["transformation"] = transformationMap
                                            }
                                        }

                                        "minecraft:text_display" -> {
                                            base["text"] = nbt.getString("text") ?: "{\"text\":\"\"}"
                                            base["text_opacity"] = nbt.getByte("text_opacity") as Any
                                            nbt.getInt("alignment")?.let {
                                                base["alignment"] = when (it) {
                                                    0 -> "center"
                                                    1 -> "left"
                                                    2 -> "right"
                                                    else -> "center"
                                                }
                                            }
                                            base["line_width"] = nbt.getInt("line_width") as Any
                                            base["background"] = nbt.getInt("background") as Any
                                            base["see_through"] = nbt.getByte("see_through") == 1.toByte()
                                            base["shadow"] = nbt.getByte("shadow") == 1.toByte()

                                            val transformationMap = extractTransformation(nbt)
                                            if (transformationMap != null) {
                                                base["transformation"] = transformationMap
                                            }

                                            nbt.getString("billboard")?.let { base["billboard"] = it }
                                        }

                                        "minecraft:armor_stand" -> {
                                            base["invisible"] = nbt.getByte("Invisible") == 1.toByte()
                                            base["no_gravity"] = nbt.getByte("NoGravity") == 1.toByte()
                                            base["small"] = nbt.getByte("Small") == 1.toByte()
                                            base["arms"] = nbt.getByte("ShowArms") == 1.toByte()
                                            base["base_plate"] = nbt.getByte("NoBasePlate") == 1.toByte()
                                            base["marker"] = nbt.getByte("Marker") == 1.toByte()

                                            val armorItems = nbt.getList<NBTCompound>("ArmorItems")
                                            val handItems = nbt.getList<NBTCompound>("HandItems")

                                            val equipment = mutableMapOf<String, Any>()

                                            armorItems?.let {
                                                equipment["feet"] = mapOf("id" to it[0].getString("id"))
                                                equipment["legs"] = mapOf("id" to it[1].getString("id"))
                                                equipment["chest"] = mapOf("id" to it[2].getString("id"))
                                                equipment["head"] = mapOf("id" to it[3].getString("id"))
                                            }

                                            handItems?.let {
                                                equipment["mainhand"] = mapOf("id" to it[0].getString("id"))
                                                equipment["offhand"] = mapOf("id" to it[1].getString("id"))
                                            }

                                            if (equipment.isNotEmpty()) base["equipment"] = equipment

                                            val pose = nbt.getCompound("Pose")
                                            pose?.let {
                                                val poseMap = mutableMapOf<String, Any>()

                                                fun extract(name: String, key: String) {
                                                    it.getList<NBTFloat>(name)?.let { list ->
                                                        poseMap[key] = list.map { f -> f.value }
                                                    }
                                                }

                                                extract("Head", "head")
                                                extract("Body", "body")
                                                extract("LeftArm", "left_arm")
                                                extract("RightArm", "right_arm")
                                                extract("LeftLeg", "left_leg")
                                                extract("RightLeg", "right_leg")

                                                if (poseMap.isNotEmpty()) base["pose"] = poseMap
                                            }
                                        }

                                        "minecraft:item_frame" -> {
                                            nbt.getCompound("Item")?.let { itemTag ->
                                                base["item"] = mapOf("id" to itemTag.getString("id"))
                                            }
                                            nbt.getByte("Facing")?.let { base["facing"] = it.toInt() }
                                            nbt.getByte("ItemRotation")?.let { base["rotation_item"] = it.toInt() }
                                            base["invisible"] = nbt.getByte("Invisible") == 1.toByte()
                                        }

                                        "minecraft:glow_item_frame" -> {
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