package com.github.simiacryptus.aicoder.actions.code

import com.github.simiacryptus.aicoder.config.AppSettingsState
import com.github.simiacryptus.aicoder.util.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import java.util.*

/**
 * The DescribeAction class is an action that can be used to describe a piece of code in plain language.
 * It is triggered when the user selects a piece of code and then selects the action.
 * The action will then generate a description of the code in the user's chosen language.
 * The description will be formatted according to the user's chosen style and will be inserted prior to the code as a comment.
 */
class DescribeAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isEnabled(e)
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val caretModel = editor.caretModel
        val primaryCaret = caretModel.primaryCaret
        var selectionStart = primaryCaret.selectionStart
        var selectionEnd = primaryCaret.selectionEnd
        var selectedText = primaryCaret.selectedText
        val language = ComputerLanguage.getComputerLanguage(event)!!
        if (null == selectedText || selectedText.isEmpty()) {
            val document = editor.document
            val lineNumber = document.getLineNumber(selectionStart)
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            val lineEndOffset = document.getLineEndOffset(lineNumber)
            val currentLine = document.text.substring(lineStartOffset, lineEndOffset)
            selectionStart = lineStartOffset
            selectionEnd = lineEndOffset
            selectedText = currentLine
        }
        actionPerformed(
            event, editor, selectionStart, selectionEnd, selectedText,
            language
        )
    }

    companion object {
        private fun isEnabled(e: AnActionEvent): Boolean {
            return null != ComputerLanguage.getComputerLanguage(e)
        }

        private fun actionPerformed(
            event: AnActionEvent,
            editor: Editor,
            selectionStart: Int,
            selectionEnd: Int,
            selectedText: String,
            language: ComputerLanguage
        ) {
            val indent = UITools.getIndent(event)
            val settings = AppSettingsState.getInstance()
            val request = settings.createTranslationRequest()
                .setInputType(language.name)
                .setOutputType(settings.humanLanguage)
                .setInstruction(UITools.getInstruction("Explain this " + language.name + " in " + settings.humanLanguage))
                .setInputAttribute("type", "code")
                .setOutputAttrute("type", "description")
                .setOutputAttrute("style", settings.style)
                .setInputText(IndentedText.fromString(selectedText).textBlock.toString().trim { it <= ' ' })
                .buildCompletionRequest()
            UITools.redoableRequest(request, indent, event,
                { newText: CharSequence? ->
                    transformCompletion(
                        selectedText,
                        language,
                        indent,
                        newText!!
                    )
                },
                { newText: CharSequence? ->
                    UITools.replaceString(
                        editor.document, selectionStart, selectionEnd,
                        newText!!
                    )
                })
        }

        private fun transformCompletion(
            selectedText: String,
            language: ComputerLanguage,
            indent: CharSequence,
            x: CharSequence
        ): CharSequence {
            val wrapping = StringTools.lineWrapping(x.toString().trim { it <= ' ' }, 120)
            val commentStyle: TextBlockFactory<*>?
            commentStyle = if (wrapping.trim { it <= ' ' }.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray().size == 1) {
                language.lineComment
            } else {
                language.blockComment
            }
            return """
                
                $indent${Objects.requireNonNull(commentStyle)!!.fromString(wrapping)!!.withIndent(indent)}
                $indent$selectedText
                """.trimIndent()
        }
    }
}