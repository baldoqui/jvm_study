package com.example.todo.data

import java.nio.file.Path
import java.nio.file.Paths

/** Resolves the per-user data directory following each platform's conventions. */
object AppDirectories {
    private const val APP_NAME = "TodoDesktop"

    /**
     * - Windows: `%APPDATA%\TodoDesktop`
     * - macOS:   `~/Library/Application Support/TodoDesktop`
     * - Linux:   `$XDG_DATA_HOME/TodoDesktop` (defaults to `~/.local/share/TodoDesktop`)
     */
    fun dataDir(
        osName: String = System.getProperty("os.name"),
        env: (String) -> String? = System::getenv,
        userHome: String = System.getProperty("user.home"),
    ): Path {
        val os = osName.lowercase()
        val base = when {
            os.startsWith("windows") ->
                env("APPDATA")?.let { Paths.get(it) } ?: Paths.get(userHome, "AppData", "Roaming")
            os.startsWith("mac") ->
                Paths.get(userHome, "Library", "Application Support")
            else ->
                env("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }?.let { Paths.get(it) }
                    ?: Paths.get(userHome, ".local", "share")
        }
        return base.resolve(APP_NAME)
    }
}
