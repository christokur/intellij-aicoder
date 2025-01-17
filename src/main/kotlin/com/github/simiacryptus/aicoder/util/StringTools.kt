package com.github.simiacryptus.aicoder.util

import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.Stream

object StringTools {
    fun stripUnbalancedTerminators(input: CharSequence): CharSequence {
        var openCount = 0
        var inQuotes = false
        val output = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '"' || c == '\'') {
                inQuotes = !inQuotes
            } else if (inQuotes && c == '\\') {
                // Skip the next character
                i++
            } else if (!inQuotes) {
                when (c) {
                    '{', '[', '(' -> openCount++
                    '}', ']', ')' -> openCount--
                }
            }
            if (openCount >= 0) {
                output.append(c)
            } else {
                openCount++ // Dropping character, undo counting close bracket
            }
            i++
        }
        return output.toString()
    }

    fun stripPrefix(text: CharSequence, prefix: CharSequence): CharSequence {
        val startsWith = text.toString().startsWith(prefix.toString())
        return if (startsWith) {
            text.toString().substring(prefix.length)
        } else {
            text.toString()
        }
    }

    fun trimPrefix(text: CharSequence): CharSequence {
        val prefix = getWhitespacePrefix(text)
        return stripPrefix(text, prefix)
    }

    fun trimSuffix(text: CharSequence): String {
        val suffix = getWhitespaceSuffix(text)
        return stripSuffix(text, suffix)
    }

    fun stripSuffix(text: CharSequence, suffix: CharSequence): String {
        val endsWith = text.toString().endsWith(suffix.toString())
        return if (endsWith) {
            text.toString().substring(0, text.length - suffix.length)
        } else {
            text.toString()
        }
    }

    fun lineWrapping(description: CharSequence, width: Int): String {
        val output = StringBuilder()
        val lines = description.toString().split("\n".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        var lineLength = 0
        for (line in lines) {
            val sentenceLength = AtomicInteger(lineLength)
            var sentenceBuffer = wrapSentence(line, width, sentenceLength)
            if (lineLength + sentenceBuffer.length > width && sentenceBuffer.length < width) {
                output.append("\n")
                lineLength = 0
                sentenceLength.set(lineLength)
                sentenceBuffer = wrapSentence(line, width, sentenceLength)
            } else {
                output.append(" ")
                sentenceLength.addAndGet(1)
            }
            output.append(sentenceBuffer)
            lineLength = sentenceLength.get()
        }
        return output.toString()
    }

    private fun wrapSentence(line: CharSequence, width: Int, xPointer: AtomicInteger): String {
        val sentenceBuffer = StringBuilder()
        val words = line.toString().split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (word in words) {
            if (xPointer.get() + word.length > width) {
                sentenceBuffer.append("\n")
                xPointer.set(0)
            } else {
                sentenceBuffer.append(" ")
                xPointer.addAndGet(1)
            }
            sentenceBuffer.append(word)
            xPointer.addAndGet(word.length)
        }
        return sentenceBuffer.toString()
    }

    fun toString(ints: IntArray): CharSequence {
        val chars = CharArray(ints.size)
        for (i in ints.indices) {
            chars[i] = ints[i].toChar()
        }
        return String(chars)
    }

    fun getWhitespacePrefix(vararg lines: CharSequence): CharSequence {
        return Arrays.stream(lines)
            .map { l: CharSequence ->
                toString(
                    l.chars().takeWhile { codePoint: Int ->
                        Character.isWhitespace(
                            codePoint
                        )
                    }.toArray()
                )
            }
            .filter { x: CharSequence -> x.length > 0 }
            .min(Comparator.comparing { obj: CharSequence -> obj.length }).orElse("")
    }

    fun getWhitespacePrefix2(vararg lines: CharSequence): CharSequence {
        return Arrays.stream(lines)
            .map { l: CharSequence ->
                toString(
                    l.chars().takeWhile { codePoint: Int ->
                        Character.isWhitespace(
                            codePoint
                        )
                    }.toArray()
                )
            }
            .min(Comparator.comparing { obj: CharSequence -> obj.length }).orElse("")
    }

    fun getWhitespaceSuffix(vararg lines: CharSequence): String {
        return reverse(Arrays.stream(lines)
            .map { obj: CharSequence? -> reverse(obj!!) }
            .map { l: CharSequence ->
                toString(
                    l.chars().takeWhile { codePoint: Int ->
                        Character.isWhitespace(
                            codePoint
                        )
                    }.toArray()
                )
            }
            .max(Comparator.comparing { obj: CharSequence -> obj.length }).orElse("")
        ).toString()
    }

    private fun reverse(l: CharSequence): CharSequence {
        return StringBuffer(l).reverse().toString()
    }

    fun trim(items: List<CharSequence>, max: Int, preserveHead: Boolean): List<CharSequence> {
        var items = items
        items = ArrayList(items)
        val random = Random()
        while (items.size > max) {
            val index = random.nextInt(items.size)
            if (preserveHead && index == 0) continue
            items.removeAt(index)
        }
        return items
    }

    fun transposeMarkdownTable(table: String, inputHeader: Boolean, outputHeader: Boolean): String {
        val cells = parseMarkdownTable(table, inputHeader)
        val transposedTable = StringBuilder()
        var columns = cells[0].size
        if (outputHeader) columns = columns + 1
        for (column in 0 until columns) {
            transposedTable.append("|")
            for (cell in cells) {
                var cellValue: String
                cellValue = if (outputHeader) {
                    if (column < 1) {
                        cell[column].toString().trim { it <= ' ' }
                    } else if (column == 1) {
                        "---"
                    } else if (column - 1 >= cell.size) {
                        ""
                    } else {
                        cell[column - 1].toString().trim { it <= ' ' }
                    }
                } else {
                    cell[column].toString().trim { it <= ' ' }
                }
                transposedTable.append(" ").append(cellValue).append(" |")
            }
            transposedTable.append("\n")
        }
        return transposedTable.toString()
    }

    private fun parseMarkdownTable(table: String, removeHeader: Boolean): Array<Array<CharSequence>> {
        val rows = Arrays.stream(table.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()).map { x: String ->
            Arrays.stream(x.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()).filter { cell: String -> cell.length > 0 }
                .collect(Collectors.toList<CharSequence>()).toTypedArray()
        }.collect(
            Collectors.toCollection(
                Supplier { ArrayList() })
        )
        if (removeHeader) {
            rows.removeAt(1)
        }
        return rows.toTypedArray()
    }

    fun getPrefixForContext(text: String): CharSequence {
        return getPrefixForContext(text, 512, ".", "\n", ",", ";")
    }

    /**
     * Get the prefix for the given context.
     *
     * @param text        The text to get the prefix from.
     * @param idealLength The ideal length of the prefix.
     * @param delimiters  The delimiters to split the text by.
     * @return The prefix for the given context.
     */
    fun getPrefixForContext(text: String, idealLength: Int, vararg delimiters: CharSequence?): CharSequence {
        val candidates = Stream.of(*delimiters).flatMap { d: CharSequence? ->
            val sb = StringBuilder()
            val split =
                text.split(Pattern.quote(d.toString()).toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            for (s in split) {
                if (Math.abs(sb.length - idealLength) < Math.abs(sb.length + s.length - idealLength)) break
                if (sb.length > 0) sb.append(d)
                sb.append(s)
                if (sb.length > idealLength) break
            }
            if (split.size == 0) return@flatMap Stream.empty<String>()
            Stream.of(sb.toString())
        }.collect(Collectors.toList())
        return candidates.stream().min(Comparator.comparing { s: CharSequence ->
            Math.abs(
                s.length - idealLength
            )
        }).orElse("")
    }

    fun getSuffixForContext(text: String): CharSequence {
        return getSuffixForContext(text, 512, ".", "\n", ",", ";")
    }

    /**
     *
     * Get the suffix for the given context.
     *
     * @param text The text to get the suffix from.
     * @param idealLength The ideal length of the suffix.
     * @param delimiters The delimiters to use when splitting the text.
     * @return The suffix for the given context.
     */
    fun getSuffixForContext(text: String, idealLength: Int, vararg delimiters: CharSequence?): CharSequence {
        val candidates = Stream.of(*delimiters).flatMap { d: CharSequence? ->
            val sb = StringBuilder()
            val split =
                text.split(Pattern.quote(d.toString()).toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            for (i in split.indices.reversed()) {
                val s = split[i]
                if (Math.abs(sb.length - idealLength) < Math.abs(sb.length + s.length - idealLength)) break
                if (sb.length > 0 || text.endsWith(d.toString())) sb.insert(0, d)
                sb.insert(0, s)
                if (sb.length > idealLength) {
                    //if (i > 0) sb.insert(0, d);
                    break
                }
            }
            if (split.size == 0) return@flatMap Stream.empty<String>()
            Stream.of(sb.toString())
        }.collect(Collectors.toList())
        return candidates.stream().min(Comparator.comparing { s: CharSequence ->
            Math.abs(
                s.length - idealLength
            )
        }).orElse("")
    }
}