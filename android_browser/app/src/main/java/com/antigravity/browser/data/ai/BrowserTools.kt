package com.antigravity.browser.data.ai

import com.antigravity.browser.core.BrowserCommand
import com.antigravity.browser.core.ScrollDirection

object BrowserTools {

    fun getTools(): List<FunctionDeclaration> {
        return listOf(
            FunctionDeclaration(
                name = "open_url",
                description = "Open a specific URL in the current tab",
                parameters = FunctionParameters(
                    properties = mapOf(
                        "url" to PropertySchema(
                            type = "string",
                            description = "The full URL to open (e.g., https://www.example.com)"
                        )
                    ),
                    required = listOf("url")
                )
            ),
            FunctionDeclaration(
                name = "search_google",
                description = "Search Google for a query",
                parameters = FunctionParameters(
                    properties = mapOf(
                        "query" to PropertySchema(
                            type = "string",
                            description = "The search query"
                        )
                    ),
                    required = listOf("query")
                )
            ),
            FunctionDeclaration(
                name = "scroll",
                description = "Scroll the web page",
                parameters = FunctionParameters(
                    properties = mapOf(
                        "direction" to PropertySchema(
                            type = "string",
                            description = "Direction to scroll: UP, DOWN, LEFT, RIGHT",
                            enum = listOf("UP", "DOWN", "LEFT", "RIGHT")
                        ),
                        "amount" to PropertySchema(
                            type = "number",
                            description = "Amount to scroll (0.0 to 1.0, where 1.0 is a full screen)"
                        )
                    ),
                    required = listOf("direction")
                )
            ),
            FunctionDeclaration(
                name = "video_control",
                description = "Control video playback (play/pause, seek)",
                parameters = FunctionParameters(
                    properties = mapOf(
                        "action" to PropertySchema(
                            type = "string",
                            description = "Action to perform: PLAY_PAUSE, SEEK_FORWARD, SEEK_BACKWARD",
                            enum = listOf("PLAY_PAUSE", "SEEK_FORWARD", "SEEK_BACKWARD")
                        ),
                        "seconds" to PropertySchema(
                            type = "integer",
                            description = "Seconds to seek (only for SEEK actions)"
                        )
                    ),
                    required = listOf("action")
                )
            ),
            FunctionDeclaration(
                name = "set_video_speed",
                description = "Set the playback speed of videos on the page",
                parameters = FunctionParameters(
                    properties = mapOf(
                        "speed" to PropertySchema(
                            type = "number",
                            description = "Playback speed (e.g., 1.0, 1.5, 2.0)"
                        )
                    ),
                    required = listOf("speed")
                )
            ),
            FunctionDeclaration(
                name = "manage_tabs",
                description = "Manage browser tabs",
                parameters = FunctionParameters(
                    properties = mapOf(
                        "action" to PropertySchema(
                            type = "string",
                            description = "Action: CREATE, CLOSE, SWITCH",
                            enum = listOf("CREATE", "CLOSE", "SWITCH")
                        ),
                        "index" to PropertySchema(
                            type = "integer",
                            description = "Tab index (0-based) for CLOSE and SWITCH actions"
                        )
                    ),
                    required = listOf("action")
                )
            ),
            FunctionDeclaration(
                name = "toggle_extension",
                description = "Enable or disable a browser extension",
                parameters = FunctionParameters(
                    properties = mapOf(
                        "extension" to PropertySchema(
                            type = "string",
                            description = "Extension name: UBLOCK, VIDEO_SPEED",
                            enum = listOf("UBLOCK", "VIDEO_SPEED")
                        ),
                        "enable" to PropertySchema(
                            type = "boolean",
                            description = "True to enable, False to disable"
                        )
                    ),
                    required = listOf("extension", "enable")
                )
            )
        )
    }
}
