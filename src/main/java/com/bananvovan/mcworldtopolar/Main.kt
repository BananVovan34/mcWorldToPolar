package com.bananvovan.mcworldtopolar

import com.bananvovan.mcworldtopolar.module.gui.MainFrame
import com.formdev.flatlaf.FlatDarkLaf
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main() {
    UIManager.setLookAndFeel(FlatDarkLaf())

    SwingUtilities.invokeLater {
        MainFrame()
    }
}