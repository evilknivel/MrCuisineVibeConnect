package app.toque.core.web

/**
 * Code QR (ISO/IEC 18004), mode octets, correction d'erreurs M, versions 1 à 10 (jusqu'à 213 octets) :
 * de quoi porter l'adresse du serveur de recettes et sa clé. Sans bibliothèque, comme le reste de Toque.
 * [modules] : lignes du haut vers le bas, `true` = module sombre. Pas de marge autour (à ajouter à l'affichage).
 */
class Qr private constructor(val version: Int, val modules: Array<BooleanArray>) {
    val size: Int get() = modules.size

    operator fun get(x: Int, y: Int): Boolean = modules[y][x]

    companion object {
        const val MAX_VERSION = 10

        // Correction M, par version (index 1 à 10) : octets de correction par bloc, nombre de blocs.
        private val ECC_PER_BLOCK = intArrayOf(-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26)
        private val BLOCKS = intArrayOf(-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5)
        private const val FORMAT_M = 0

        /** Le plus petit code qui porte [text] (UTF-8), ou une exception s'il est trop long. */
        fun encode(text: String): Qr = encode(text.toByteArray(Charsets.UTF_8))

        fun encode(data: ByteArray): Qr {
            val version = (1..MAX_VERSION).firstOrNull { v -> 4 + countBits(v) + data.size * 8 <= dataCodewords(v) * 8 }
                ?: throw IllegalArgumentException("trop long pour un code QR version $MAX_VERSION : ${data.size} octets")
            val bits = BitBuffer()
            bits.append(0b0100, 4)
            bits.append(data.size, countBits(version))
            for (b in data) bits.append(b.toInt() and 0xFF, 8)
            val capacity = dataCodewords(version) * 8
            bits.append(0, minOf(4, capacity - bits.size))
            bits.append(0, (8 - bits.size % 8) % 8)
            var pad = 0xEC
            while (bits.size < capacity) {
                bits.append(pad, 8)
                pad = pad xor 0xEC xor 0x11
            }
            val codewords = ByteArray(bits.size / 8)
            for (i in 0 until bits.size) if (bits[i]) codewords[i ushr 3] = (codewords[i ushr 3].toInt() or (1 shl (7 - (i and 7)))).toByte()
            return Builder(version).build(interleave(version, codewords))
        }

        private fun countBits(version: Int) = if (version <= 9) 8 else 16

        internal fun rawModules(v: Int): Int {
            var r = (16 * v + 128) * v + 64
            if (v >= 2) {
                val n = v / 7 + 2
                r -= (25 * n - 10) * n - 55
                if (v >= 7) r -= 36
            }
            return r
        }

        internal fun dataCodewords(v: Int): Int = rawModules(v) / 8 - ECC_PER_BLOCK[v] * BLOCKS[v]

        private fun interleave(v: Int, data: ByteArray): ByteArray {
            val numBlocks = BLOCKS[v]
            val eccLen = ECC_PER_BLOCK[v]
            val raw = rawModules(v) / 8
            val numShort = numBlocks - raw % numBlocks
            val shortLen = raw / numBlocks
            val divisor = Rs.divisor(eccLen)
            val blocks = ArrayList<ByteArray>(numBlocks)
            var k = 0
            for (i in 0 until numBlocks) {
                val len = shortLen - eccLen + if (i < numShort) 0 else 1
                val dat = data.copyOfRange(k, k + len)
                k += len
                val ecc = Rs.remainder(dat, divisor)
                val block = ByteArray(shortLen + 1)
                System.arraycopy(dat, 0, block, 0, dat.size)
                // Les blocs courts ont un octet vide avant leur correction (sauté à l'entrelacement).
                System.arraycopy(ecc, 0, block, shortLen + 1 - eccLen, eccLen)
                blocks += block
            }
            val out = ByteArray(raw)
            var n = 0
            for (i in 0..shortLen) for (j in 0 until numBlocks) {
                if (i != shortLen - eccLen || j >= numShort) out[n++] = blocks[j][i]
            }
            return out
        }
    }

    private class BitBuffer {
        private val bits = ArrayList<Boolean>()
        val size: Int get() = bits.size
        operator fun get(i: Int) = bits[i]
        fun append(value: Int, len: Int) {
            for (i in len - 1 downTo 0) bits += (value ushr i) and 1 != 0
        }
    }

    /** Reed-Solomon sur GF(256), polynôme 0x11D. */
    internal object Rs {
        fun mul(x: Int, y: Int): Int {
            var z = 0
            for (i in 7 downTo 0) {
                z = (z shl 1) xor ((z ushr 7) * 0x11D)
                z = z xor (((y ushr i) and 1) * x)
            }
            return z and 0xFF
        }

        fun divisor(degree: Int): IntArray {
            val r = IntArray(degree)
            r[degree - 1] = 1
            var root = 1
            repeat(degree) {
                for (j in 0 until degree) {
                    r[j] = mul(r[j], root)
                    if (j + 1 < degree) r[j] = r[j] xor r[j + 1]
                }
                root = mul(root, 0x02)
            }
            return r
        }

        fun remainder(data: ByteArray, divisor: IntArray): ByteArray {
            val r = IntArray(divisor.size)
            for (b in data) {
                val factor = (b.toInt() and 0xFF) xor r[0]
                System.arraycopy(r, 1, r, 0, r.size - 1)
                r[r.size - 1] = 0
                for (i in r.indices) r[i] = r[i] xor mul(divisor[i], factor)
            }
            return ByteArray(r.size) { r[it].toByte() }
        }
    }

    private class Builder(val version: Int) {
        val size = version * 4 + 17
        val m = Array(size) { BooleanArray(size) }
        val fn = Array(size) { BooleanArray(size) }

        fun set(x: Int, y: Int, dark: Boolean) {
            m[y][x] = dark
            fn[y][x] = true
        }

        fun build(codewords: ByteArray): Qr {
            functionPatterns()
            placeData(codewords)
            var best = 0
            var bestPenalty = Int.MAX_VALUE
            for (mask in 0 until 8) {
                applyMask(mask)
                format(mask)
                val p = penalty()
                if (p < bestPenalty) { bestPenalty = p; best = mask }
                applyMask(mask) // annule (XOR)
            }
            applyMask(best)
            format(best)
            return Qr(version, m)
        }

        private fun functionPatterns() {
            for (i in 0 until size) {
                set(6, i, i % 2 == 0)
                set(i, 6, i % 2 == 0)
            }
            finder(3, 3); finder(size - 4, 3); finder(3, size - 4)
            val pos = alignmentPositions()
            val last = pos.size - 1
            for (i in pos.indices) for (j in pos.indices) {
                if ((i == 0 && j == 0) || (i == 0 && j == last) || (i == last && j == 0)) continue
                for (dy in -2..2) for (dx in -2..2) set(pos[i] + dx, pos[j] + dy, maxOf(Math.abs(dx), Math.abs(dy)) != 1)
            }
            format(0) // réserve les zones, réécrites après le choix du masque
            versionInfo()
        }

        private fun finder(cx: Int, cy: Int) {
            for (dy in -4..4) for (dx in -4..4) {
                val x = cx + dx
                val y = cy + dy
                if (x !in 0 until size || y !in 0 until size) continue
                val d = maxOf(Math.abs(dx), Math.abs(dy))
                set(x, y, d != 2 && d != 4)
            }
        }

        private fun alignmentPositions(): IntArray {
            if (version == 1) return IntArray(0)
            val n = version / 7 + 2
            val step = (version * 4 + n * 2 + 1) / (n * 2 - 2) * 2
            val r = IntArray(n)
            r[0] = 6
            var p = size - 7
            for (i in n - 1 downTo 1) { r[i] = p; p -= step }
            return r
        }

        private fun format(mask: Int) {
            val data = (FORMAT_M shl 3) or mask
            var rem = data
            repeat(10) { rem = (rem shl 1) xor ((rem ushr 9) * 0x537) }
            val bits = ((data shl 10) or rem) xor 0x5412
            fun bit(i: Int) = (bits ushr i) and 1 != 0
            for (i in 0..5) set(8, i, bit(i))
            set(8, 7, bit(6)); set(8, 8, bit(7)); set(7, 8, bit(8))
            for (i in 9 until 15) set(14 - i, 8, bit(i))
            for (i in 0 until 8) set(size - 1 - i, 8, bit(i))
            for (i in 8 until 15) set(8, size - 15 + i, bit(i))
            set(8, size - 8, true)
        }

        private fun versionInfo() {
            if (version < 7) return
            var rem = version
            repeat(12) { rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25) }
            val bits = (version shl 12) or rem
            for (i in 0 until 18) {
                val b = (bits ushr i) and 1 != 0
                val a = size - 11 + i % 3
                val c = i / 3
                set(a, c, b); set(c, a, b)
            }
        }

        private fun placeData(data: ByteArray) {
            var i = 0
            var right = size - 1
            while (right >= 1) {
                if (right == 6) right = 5
                for (vert in 0 until size) for (j in 0..1) {
                    val x = right - j
                    val upward = ((right + 1) and 2) == 0
                    val y = if (upward) size - 1 - vert else vert
                    if (!fn[y][x] && i < data.size * 8) {
                        m[y][x] = ((data[i ushr 3].toInt() ushr (7 - (i and 7))) and 1) != 0
                        i++
                    }
                }
                right -= 2
            }
        }

        private fun applyMask(mask: Int) {
            for (y in 0 until size) for (x in 0 until size) {
                if (fn[y][x]) continue
                val invert = when (mask) {
                    0 -> (x + y) % 2 == 0
                    1 -> y % 2 == 0
                    2 -> x % 3 == 0
                    3 -> (x + y) % 3 == 0
                    4 -> (x / 3 + y / 2) % 2 == 0
                    5 -> x * y % 2 + x * y % 3 == 0
                    6 -> (x * y % 2 + x * y % 3) % 2 == 0
                    else -> ((x + y) % 2 + x * y % 3) % 2 == 0
                }
                if (invert) m[y][x] = !m[y][x]
            }
        }

        /** Pénalités 1 (suites), 2 (carrés 2×2), 3 (motifs de repère) et 4 (équilibre) de la norme. */
        private fun penalty(): Int {
            var p = 0
            for (horizontal in listOf(true, false)) for (a in 0 until size) {
                var run = 0
                var prev = false
                for (b in 0 until size) {
                    val c = if (horizontal) m[a][b] else m[b][a]
                    if (b > 0 && c == prev) {
                        run++
                        if (run == 5) p += 3 else if (run > 5) p++
                    } else { run = 1; prev = c }
                }
                for (b in 0..size - 11) {
                    fun at(k: Int) = if (horizontal) m[a][b + k] else m[b + k][a]
                    val core = at(0) && !at(1) && at(2) && at(3) && at(4) && !at(5) && at(6)
                    val reversed = at(4) && !at(5) && at(6) && at(7) && at(8) && !at(9) && at(10)
                    if ((core && !at(7) && !at(8) && !at(9) && !at(10)) ||
                        (reversed && !at(0) && !at(1) && !at(2) && !at(3))) p += 40
                }
            }
            for (y in 0 until size - 1) for (x in 0 until size - 1) {
                val c = m[y][x]
                if (c == m[y][x + 1] && c == m[y + 1][x] && c == m[y + 1][x + 1]) p += 3
            }
            var dark = 0
            for (row in m) for (c in row) if (c) dark++
            val total = size * size
            val k = (Math.abs(dark * 20 - total * 10) + total - 1) / total - 1
            p += maxOf(0, k) * 10
            return p
        }
    }
}
