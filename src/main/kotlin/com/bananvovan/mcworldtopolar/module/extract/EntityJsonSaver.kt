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
                                    extractEntity(nbt)?.let { results.add(it) }
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

    private fun extractEntity(nbt: NBTCompound): Map<String, Any>? {
        val type = nbt.getString("id") ?: return null
        val pos = nbt.getList<NBTDouble>("Pos")?.map { it.value } ?: listOf(0.0, 0.0, 0.0)
        val rotation = nbt.getList<NBTFloat>("Rotation")?.map { it.value } ?: listOf(0f, 0f)

        val base = mutableMapOf<String, Any>(
            "type" to type,
            "pos" to pos,
            "rotation" to rotation,
        )

        val kept = when (type) {
            "minecraft:item_display" -> extractItemDisplay(nbt, base)
            "minecraft:block_display" -> extractBlockDisplay(nbt, base)
            "minecraft:text_display" -> extractTextDisplay(nbt, base)
            "minecraft:armor_stand" -> extractArmorStand(nbt, base)
            "minecraft:item_frame", "minecraft:glow_item_frame" -> extractItemFrame(nbt, base)
            "minecraft:painting" -> extractPainting(nbt, base)
            else -> true
        }

        return if (kept) base else null
    }

    private fun extractItemDisplay(nbt: NBTCompound, base: MutableMap<String, Any>): Boolean {
        nbt.getCompound("item")?.let { tag ->
            val itemMap = mutableMapOf<String, Any>()
            tag.getString("id")?.let { itemMap["id"] = it }
            tag.getCompound("components")?.getString("minecraft:item_model")?.let { itemMap["model"] = it }
            if (itemMap.isNotEmpty()) base["item"] = itemMap
        }
        extractTransformation(nbt)?.let { base["transformation"] = it }
        nbt.getString("billboard")?.let { base["billboard"] = it }
        extractBrightness(nbt)?.let { base["brightness"] = it }
        return true
    }

    private fun extractBlockDisplay(nbt: NBTCompound, base: MutableMap<String, Any>): Boolean {
        val blockState = nbt.getCompound("block_state") ?: run {
            println("[EntityJsonSaver] block_display без block_state — пропускаем")
            return false
        }
        val name = blockState.getString("Name") ?: run {
            println("[EntityJsonSaver] block_display без Name — пропускаем")
            return false
        }

        val blockMap = mutableMapOf<String, Any>("name" to name)
        blockState.getCompound("Properties")?.let { props ->
            val propsMap = mutableMapOf<String, String>()
            props.keys.forEach { key -> props.getString(key)?.let { propsMap[key] = it } }
            if (propsMap.isNotEmpty()) blockMap["properties"] = propsMap
        }
        base["block"] = blockMap

        extractTransformation(nbt)?.let { base["transformation"] = it }
        nbt.getString("billboard")?.let { base["billboard"] = it }
        extractBrightness(nbt)?.let { base["brightness"] = it }
        return true
    }

    private fun extractTextDisplay(nbt: NBTCompound, base: MutableMap<String, Any>): Boolean {
        base["text"] = nbt.getString("text") ?: "{\"text\":\"\"}"
        nbt.getByte("text_opacity")?.let { base["text_opacity"] = it.toInt() }
        nbt.getInt("alignment")?.let {
            base["alignment"] = when (it) {
                1 -> "left"
                2 -> "right"
                else -> "center"
            }
        }
        nbt.getInt("line_width")?.let { base["line_width"] = it }
        nbt.getInt("background")?.let { base["background"] = it }
        base["see_through"] = nbt.getByte("see_through") == 1.toByte()
        base["shadow"] = nbt.getByte("shadow") == 1.toByte()

        extractTransformation(nbt)?.let { base["transformation"] = it }
        nbt.getString("billboard")?.let { base["billboard"] = it }
        extractBrightness(nbt)?.let { base["brightness"] = it }
        return true
    }

    private fun extractArmorStand(nbt: NBTCompound, base: MutableMap<String, Any>): Boolean {
        base["invisible"] = nbt.getByte("Invisible") == 1.toByte()
        base["no_gravity"] = nbt.getByte("NoGravity") == 1.toByte()
        base["small"] = nbt.getByte("Small") == 1.toByte()
        base["arms"] = nbt.getByte("ShowArms") == 1.toByte()
        base["base_plate"] = nbt.getByte("NoBasePlate") == 1.toByte()
        base["marker"] = nbt.getByte("Marker") == 1.toByte()

        val equipment = mutableMapOf<String, Any>()
        nbt.getList<NBTCompound>("ArmorItems")?.let { armor ->
            armor[0].getString("id")?.let { equipment["feet"] = mapOf("id" to it) }
            armor[1].getString("id")?.let { equipment["legs"] = mapOf("id" to it) }
            armor[2].getString("id")?.let { equipment["chest"] = mapOf("id" to it) }
            armor[3].getString("id")?.let { equipment["head"] = mapOf("id" to it) }
        }
        nbt.getList<NBTCompound>("HandItems")?.let { hands ->
            hands[0].getString("id")?.let { equipment["mainhand"] = mapOf("id" to it) }
            hands[1].getString("id")?.let { equipment["offhand"] = mapOf("id" to it) }
        }
        if (equipment.isNotEmpty()) base["equipment"] = equipment

        nbt.getCompound("Pose")?.let { pose ->
            val poseMap = mutableMapOf<String, Any>()
            fun extract(name: String, key: String) {
                pose.getList<NBTFloat>(name)?.let { list ->
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
        return true
    }

    private fun extractItemFrame(nbt: NBTCompound, base: MutableMap<String, Any>): Boolean {
        nbt.getCompound("Item")?.getString("id")?.let { base["item"] = mapOf("id" to it) }
        nbt.getByte("Facing")?.let { base["facing"] = it.toInt() }
        nbt.getByte("ItemRotation")?.let { base["rotation_item"] = it.toInt() }
        base["invisible"] = nbt.getByte("Invisible") == 1.toByte()
        return true
    }

    private fun extractPainting(nbt: NBTCompound, base: MutableMap<String, Any>): Boolean {
        nbt.getString("variant")?.let { base["variant"] = it }
        return true
    }

    private fun extractTransformation(nbt: NBTCompound): Map<String, Any>? {
        val transformation = nbt.getCompound("transformation") ?: return null
        val transformationMap = mutableMapOf<String, Any>()
        transformation.getList<NBTFloat>("scale")?.let { transformationMap["scale"] = it.map { f -> f.value } }
        transformation.getList<NBTFloat>("translation")?.let { transformationMap["translation"] = it.map { f -> f.value } }
        transformation.getList<NBTFloat>("left_rotation")?.let { transformationMap["left_rotation"] = it.map { f -> f.value } }
        transformation.getList<NBTFloat>("right_rotation")?.let { transformationMap["right_rotation"] = it.map { f -> f.value } }
        return transformationMap.ifEmpty { null }
    }

    private fun extractBrightness(nbt: NBTCompound): Map<String, Any>? {
        val brightness = nbt.getCompound("brightness") ?: return null
        val brightnessMap = mutableMapOf<String, Any>()
        brightness.getInt("block")?.let { brightnessMap["block"] = it }
        brightness.getInt("sky")?.let { brightnessMap["sky"] = it }
        return brightnessMap.ifEmpty { null }
    }
}
