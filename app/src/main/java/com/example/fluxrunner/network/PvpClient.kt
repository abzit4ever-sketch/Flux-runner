package com.example.fluxrunner.network

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class PvpPlayerState(
    val playerId: String,
    val displayName: String,
    val score: Int,
    val distance: Float,
    val alive: Boolean,
    val finished: Boolean
)

data class PvpMatchState(
    val matchId: String,
    val seed: Int,
    val status: String,
    val playerId: String,
    val opponentId: String,
    val opponentName: String,
    val players: List<PvpPlayerState>,
    val winnerId: String,
    val tokenReward: Int
) {
    val hasOpponent: Boolean get() = opponentId.isNotBlank()
    val isFinished: Boolean get() = status == "finished"
    val opponent: PvpPlayerState?
        get() = players.firstOrNull { it.playerId == opponentId }
}

class PvpClient(
    private val baseUrl: String
) {
    fun queue(playerId: String, displayName: String): PvpMatchState {
        val payload = JSONObject()
            .put("playerId", playerId)
            .put("displayName", displayName)
        return parseMatch(post("/matchmaking/queue", payload))
    }

    fun getState(matchId: String, playerId: String): PvpMatchState {
        return parseMatch(get("/matches/$matchId/state?playerId=$playerId"))
    }

    fun submitState(
        matchId: String,
        playerId: String,
        score: Int,
        distance: Float,
        alive: Boolean,
        finished: Boolean
    ): PvpMatchState {
        val payload = JSONObject()
            .put("playerId", playerId)
            .put("score", score)
            .put("distance", distance)
            .put("alive", alive)
            .put("finished", finished)
        return parseMatch(post("/matches/$matchId/state", payload))
    }

    private fun get(path: String): JSONObject {
        val connection = open(path, "GET")
        return readJson(connection)
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val connection = open(path, "POST")
        connection.doOutput = true
        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(body.toString())
        }
        return readJson(connection)
    }

    private fun open(path: String, method: String): HttpURLConnection {
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 3500
        connection.readTimeout = 3500
        connection.setRequestProperty("Content-Type", "application/json")
        return connection
    }

    private fun readJson(connection: HttpURLConnection): JSONObject {
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        if (status !in 200..299) {
            val message = runCatching { JSONObject(text).optString("error") }.getOrDefault(text)
            throw IllegalStateException(message.ifBlank { "Server error $status" })
        }
        return JSONObject(text)
    }

    private fun parseMatch(json: JSONObject): PvpMatchState {
        val players = json.optJSONArray("players").orEmptyList { item ->
            PvpPlayerState(
                playerId = item.optString("playerId"),
                displayName = item.optString("displayName", "Runner"),
                score = item.optInt("score"),
                distance = item.optDouble("distance").toFloat(),
                alive = item.optBoolean("alive", true),
                finished = item.optBoolean("finished", false)
            )
        }
        return PvpMatchState(
            matchId = json.optString("matchId"),
            seed = json.optInt("seed"),
            status = json.optString("status"),
            playerId = json.optString("playerId"),
            opponentId = json.optString("opponentId"),
            opponentName = json.optString("opponentName", "Waiting"),
            players = players,
            winnerId = json.optString("winnerId"),
            tokenReward = json.optInt("tokenReward", 10)
        )
    }

    private fun JSONArray?.orEmptyList(mapper: (JSONObject) -> PvpPlayerState): List<PvpPlayerState> {
        if (this == null) return emptyList()
        val result = ArrayList<PvpPlayerState>(length())
        for (i in 0 until length()) {
            result.add(mapper(optJSONObject(i) ?: JSONObject()))
        }
        return result
    }

}
