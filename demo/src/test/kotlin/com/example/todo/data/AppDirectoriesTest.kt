package com.example.todo.data

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

class AppDirectoriesTest {
    @Test
    fun `windows uses APPDATA`() {
        val dir = AppDirectories.dataDir(
            osName = "Windows 11",
            env = { if (it == "APPDATA") "C:/Users/me/AppData/Roaming" else null },
            userHome = "C:/Users/me",
        )
        assertEquals(Paths.get("C:/Users/me/AppData/Roaming", "TodoDesktop"), dir)
    }

    @Test
    fun `linux prefers XDG_DATA_HOME`() {
        val dir = AppDirectories.dataDir(osName = "Linux", env = { "/data" }, userHome = "/home/me")
        assertEquals(Paths.get("/data", "TodoDesktop"), dir)
    }

    @Test
    fun `linux falls back to local share`() {
        val dir = AppDirectories.dataDir(osName = "Linux", env = { null }, userHome = "/home/me")
        assertEquals(Paths.get("/home/me/.local/share/TodoDesktop"), dir)
    }
}
