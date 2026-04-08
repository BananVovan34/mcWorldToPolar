package com.bananvovan.mcworldtopolar.module.gui

import com.bananvovan.mcworldtopolar.module.convert.WorldToPolar
import java.awt.FlowLayout
import java.io.File
import javax.swing.*

class MainFrame : JFrame("Конвертер мира Minecraft в Polar") {

    private val btnOpenDir = JButton("Выбрать директорию мира")
    private val btnSaveFile = JButton("Сохранить как Polar")
    private val fileChooser = JFileChooser()
    private var selectedWorldDirectory: File? = null

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = FlowLayout()

        btnSaveFile.isEnabled = false
        fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY

        add(btnOpenDir)
        add(btnSaveFile)

        addListeners()

        setSize(400, 120)
        setLocationRelativeTo(null)
        isVisible = true
    }

    private fun addListeners() {
        btnOpenDir.addActionListener {
            val result = fileChooser.showOpenDialog(this)
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedWorldDirectory = fileChooser.selectedFile
                btnSaveFile.isEnabled = true
            }
        }

        btnSaveFile.addActionListener {
            val dir = selectedWorldDirectory ?: return@addActionListener

            btnSaveFile.isEnabled = false

            Thread {
                try {
                    WorldToPolar.convert(dir.absolutePath)

                    SwingUtilities.invokeLater {
                        JOptionPane.showMessageDialog(
                            this,
                            "Готово!"
                        )
                        btnSaveFile.isEnabled = true
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }
}