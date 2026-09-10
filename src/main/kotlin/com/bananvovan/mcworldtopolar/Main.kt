package com.bananvovan.mcworldtopolar

import com.bananvovan.mcworldtopolar.module.gui.MainFrame
import com.formdev.flatlaf.FlatDarkLaf
import javax.swing.SwingUtilities

fun main(args: Array<String>) {
    if (args.contains("--version")) {
        println(AppMetadata.versionText)
        return
    }

    SwingUtilities.invokeLater {
        FlatDarkLaf.setup()
        MainFrame()
    }
}