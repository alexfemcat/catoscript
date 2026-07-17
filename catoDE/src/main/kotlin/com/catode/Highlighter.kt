package com.catode

// TokenKind is the closed set of categories a stripe of catoscript text can be.
// The Highlighter emits one of these per TokenSpan; EditorPane will `when` on this
// exhaustively to pick a color.
enum class TokenKind {
    COMMENT,
    STRING,
    LABEL,
    VARIABLE,
    NUMBER,
    KEYWORD_CONTROL,
    KEYWORD_CONTROL_FILE,
    KEYWORD_VISUAL,
    KEYWORD_SYSTEM,
    KEYWORD_NEURAL,
    KEYWORD_AUDIO,
    KEYWORD_ACTION,
    KEYWORD_MEOW,
    KEYWORD_OTHER,
}

// A TokenSpan is one colored stripe in the editor: a range of source-text
// offsets and the kind of token that lives there. Both fields travel together
// because no consumer wants one without the other.
data class TokenSpan(
    val range: IntRange,
    val kind: TokenKind,
)

object Highlighter {
    private val commentRegex: Regex = Regex("#.*")
    private val numberRegex: Regex = Regex("""\b[0-9]+\b""")
    private val variableRegex: Regex = Regex("""\$[A-Za-z_][A-Za-z0-9_]*""")
    private val labelRegex: Regex = Regex("""^\s*:[A-Za-z_][A-Za-z0-9_]*""", RegexOption.MULTILINE)
    private val stringRegex: Regex = Regex("\"[^\"]*\"")

    private val keywordRegexes: List<Pair<Regex, TokenKind>> = listOf(
        Regex("""\b(?:jump|pounce|purr_at|hiss_at|reflex|breed|end_breed|instinct)\b""") to TokenKind.KEYWORD_CONTROL,
        Regex("""\binclude\b""") to TokenKind.KEYWORD_CONTROL_FILE,
        Regex("""\b(?:glow|blink|canvas_mode|draw_box|button|gauge|input_field|trail|glitch_box|layer_depth|window|view_port|skin|focus_win|stencil|tilt|magnify)\b""") to TokenKind.KEYWORD_VISUAL,
        Regex("""\b(?:coat|sniff_env|nine_lives|on_idle|def_asset|end_asset|on_hover|on_pet_click|ignore|focus)\b""") to
                TokenKind.KEYWORD_SYSTEM,
        Regex("""\b(?:synapse|train|think|memory_echo|mood_swing)\b""") to TokenKind.KEYWORD_NEURAL,
        Regex("""\b(?:purr|hiss|tempo|chirp|rumble|pat|knock|knead|vibrato|sample)\b""") to TokenKind.KEYWORD_AUDIO,
        Regex("""\b(?:scratch|swat|bat|crouch|spring|leap|zoomies|groom|scurry|tumble|nap|stalk|scare|unravel|shred|fetch_yarn|overclock|battery|partition|loaf|knock_off_table|detach|minimize|overclock_hardware|jam_comms|hijack_camera|spawn|whisper|broadcast|knit|tag|benchmark|prowl|hiss_at_ip|weight|pounce_force|friction|snag|fill_box|fur_coat|twitch|mirror|listen|split|hush|synth|envelope|spawn_pet|pet_move|pet_stat|feed|scent|signal|pixel|glitch|static|litter)\b""") to TokenKind.KEYWORD_ACTION,
        Regex("""\bmeow\b""") to TokenKind.KEYWORD_MEOW,
        Regex("""\b(?:set|sniff|bury|dig|sync)\b""") to TokenKind.KEYWORD_OTHER,
    )


    fun tokenize(text: String): List<TokenSpan> {
        val spans : MutableList<TokenSpan> = mutableListOf()
        for (match in commentRegex.findAll(text)) {
            spans.add(TokenSpan(match.range, TokenKind.COMMENT))
        }
        for (match in numberRegex.findAll(text)) {
            spans.add(TokenSpan(match.range, TokenKind.NUMBER))
        }
        for (match in variableRegex.findAll(text)) {
            spans.add(TokenSpan(match.range, TokenKind.VARIABLE))
        }
        for (match in labelRegex.findAll(text)) {
            spans.add(TokenSpan(match.range, TokenKind.LABEL))
        }
        for (match in stringRegex.findAll(text)) {
            spans.add(TokenSpan(match.range, TokenKind.STRING))
        }
        for ((regex, kind) in keywordRegexes) {
            for (match in regex.findAll(text)) {
                spans.add(TokenSpan(match.range, kind))
            }
        }
        return spans
    }
}




