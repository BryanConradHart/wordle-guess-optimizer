package com.bryanconradhart.hobby;

import java.util.Set;

public class GuessPerformance {
    private final String solution;
    private final Set<String> guess;
    private final long numberOfElimiminatedAnswers;

    public GuessPerformance(String solution, Set<String> guess, long numberOfEliminatedAnswers) {
        this.solution = solution;
        this.guess = guess;
        this.numberOfElimiminatedAnswers = numberOfEliminatedAnswers;
    }

    public String getSolution() {
        return solution;
    }

    public Set<String> getGuess() {
        return guess;
    }

    public long getNumberOfElimiminatedAnswers() {
        return numberOfElimiminatedAnswers;
    }
}
