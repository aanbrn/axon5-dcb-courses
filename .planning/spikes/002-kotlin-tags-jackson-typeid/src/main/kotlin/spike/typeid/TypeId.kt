package spike.typeid

import java.util.UUID

/** TypeID spec v0.3.0 codec (jetify-com/typeid spec/README.md): prefix + "_" + 26-char lowercase Crockford base32. */
object TypeId {
    private const val ALPHABET = "0123456789abcdefghjkmnpqrstvwxyz"
    private val DECODE = IntArray(128) { -1 }.also { t -> ALPHABET.forEachIndexed { i, c -> t[c.code] = i } }
    private val PREFIX = Regex("^([a-z]([a-z_]{0,61}[a-z])?)?$")

    fun isValidPrefix(prefix: String) = PREFIX.matches(prefix)

    fun encode(prefix: String, uuid: UUID): String {
        require(isValidPrefix(prefix)) { "Invalid TypeID prefix '$prefix'" }
        val hi = uuid.mostSignificantBits
        val lo = uuid.leastSignificantBits
        val out = CharArray(26)
        // 130 bits = 2 zero bits + 128 bits; character i covers bits [5i, 5i+5) of that 130-bit string.
        for (i in 0 until 26) {
            val bitStart = 5 * i - 2 // position within the 128-bit value (may be negative for the first char)
            var v = 0
            for (b in 0 until 5) {
                val p = bitStart + b
                val bit = if (p < 0) 0 else if (p < 64) ((hi ushr (63 - p)) and 1L).toInt() else ((lo ushr (127 - p)) and 1L).toInt()
                v = (v shl 1) or bit
            }
            out[i] = ALPHABET[v]
        }
        val suffix = String(out)
        return if (prefix.isEmpty()) suffix else "${prefix}_$suffix"
    }

    /** Returns (prefix, uuid); throws IllegalArgumentException with a precise reason. */
    fun decode(value: String): Pair<String, UUID> {
        val sep = value.lastIndexOf('_')
        val prefix = if (sep < 0) "" else value.substring(0, sep)
        val suffix = if (sep < 0) value else value.substring(sep + 1)
        require(sep != 0) { "Malformed TypeID '$value': empty prefix with separator" }
        require(isValidPrefix(prefix)) { "Malformed TypeID '$value': invalid prefix '$prefix'" }
        require(suffix.length == 26) { "Malformed TypeID '$value': suffix must be 26 characters" }
        var hi = 0L
        var lo = 0L
        for (i in 0 until 26) {
            val c = suffix[i]
            val v = if (c.code < 128) DECODE[c.code] else -1
            require(v >= 0) { "Malformed TypeID '$value': invalid character '$c'" }
            if (i == 0) require(v <= 7) { "Malformed TypeID '$value': suffix overflows 128 bits" }
            for (b in 4 downTo 0) {
                val p = 5 * i - 2 + (4 - b)
                if (p < 0) continue
                val bit = ((v shr b) and 1).toLong()
                if (p < 64) hi = hi or (bit shl (63 - p)) else lo = lo or (bit shl (127 - p))
            }
        }
        return prefix to UUID(hi, lo)
    }
}
