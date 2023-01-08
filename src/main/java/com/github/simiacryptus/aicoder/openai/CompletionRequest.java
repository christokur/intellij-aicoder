package com.github.simiacryptus.aicoder.openai;

import com.github.simiacryptus.aicoder.text.IndentedText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static com.github.simiacryptus.aicoder.StringTools.stripPrefix;
import static com.github.simiacryptus.aicoder.StringTools.stripUnbalancedTerminators;

/**
 * The CompletionRequest class is used to create a request for completion of a given prompt.
 */
public class CompletionRequest {
    public String prompt;
    public double temperature;
    public int max_tokens;
    public String[] stop;
    public Integer logprobs;
    public boolean echo;

    public CompletionRequest() {
    }

    public CompletionRequest(String prompt, double temperature, int max_tokens, Integer logprobs, boolean echo, String... stop) {
        this.prompt = prompt;
        this.temperature = temperature;
        this.max_tokens = max_tokens;
        this.stop = stop;
        this.logprobs = logprobs;
        this.echo = echo;
    }

    @Nullable
    public String complete(String indent) throws IOException, ModerationException {
        return getCompletionText(OpenAI.INSTANCE.complete(this), indent);
    }

    @Nullable
    public String getCompletionText(CompletionResponse response, String indent) {
        return response
                .getFirstChoice()
                .map(completion -> stripPrefix(completion, this.prompt))
                .map(completion -> stripUnbalancedTerminators(completion))
                .map(IndentedText::fromString)
                .map(indentedText -> indentedText.withIndent(indent))
                .map(IndentedText::toString)
                .map(indentedText -> indent + indentedText)
                .orElse(null);
    }

    public @NotNull CompletionRequest appendPrompt(String prompt) {
        this.prompt = this.prompt + prompt;
        return this;
    }

    public @NotNull CompletionRequest addStops(String @NotNull [] stop) {
        ArrayList<String> stops = new ArrayList<>();
        Arrays.stream(this.stop).forEach(stops::add);
        Arrays.stream(stop).forEach(stops::add);
        this.stop = stops.stream().distinct().toArray(String[]::new);
        return this;
    }
}
