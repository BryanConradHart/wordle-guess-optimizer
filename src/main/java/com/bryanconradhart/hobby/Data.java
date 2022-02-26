package com.bryanconradhart.hobby;
import java.util.Set;

public class Data {
    private Set<String> answers;
    private Set<String> guesses;

    public Set<String> getAnswers() {
        return answers;
    }
    public void setAnswers(Set<String> answers) {
        this.answers = answers;
    }

    public Set<String> getGuesses() {
        return guesses;
    }
    public void setGuesses(Set<String> guesses) {
        this.guesses = guesses;
    }
}
