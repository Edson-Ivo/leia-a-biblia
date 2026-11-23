package com.edsonivo.m633.core

import org.json.JSONObject
import java.io.InputStream

data class Verse(val ref: String, val text: String)

data class Message(val theme: String, val text: String, val verses: List<Verse>)

class MessageBank(val themes: Map<String, String>, val messages: List<Message>) {

    /** Mensagens intercaladas por tema, para o personagem não repetir o mesmo assunto em seguida. */
    val interleaved: List<Message> by lazy {
        val groups = themes.keys.map { key -> messages.filter { it.theme == key } }
        val out = mutableListOf<Message>()
        val longest = groups.maxOfOrNull { it.size } ?: 0
        for (i in 0 until longest) groups.forEach { g -> g.getOrNull(i)?.let(out::add) }
        out
    }

    fun themeLabel(key: String): String = themes[key] ?: key

    fun forTalk(index: Int): Message = interleaved[Math.floorMod(index, interleaved.size)]

    /** Um versículo por dia, girando pelo banco inteiro. */
    fun ofDay(epochDay: Long): Message {
        val list = interleaved
        return list[Math.floorMod(epochDay * 7 + 3, list.size.toLong()).toInt()]
    }

    companion object {
        fun load(input: InputStream): MessageBank = parse(input.bufferedReader().use { it.readText() })

        fun parse(json: String): MessageBank {
            val root = JSONObject(json)
            val themesJson = root.getJSONObject("temas")
            val themes = linkedMapOf<String, String>()
            themesJson.keys().forEach { themes[it] = themesJson.getString(it) }
            val arr = root.getJSONArray("mensagens")
            val messages = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val vs = o.getJSONArray("versiculos")
                Message(
                    theme = o.getString("tema"),
                    text = o.getString("texto"),
                    verses = (0 until vs.length()).map { j ->
                        val v = vs.getJSONObject(j)
                        Verse(v.getString("ref"), v.getString("texto"))
                    },
                )
            }
            return MessageBank(themes, messages)
        }
    }
}
