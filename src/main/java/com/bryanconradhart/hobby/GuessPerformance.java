package com.bryanconradhart.hobby;


public class GuessPerformance {
    private final String solution;
    private final String[] guess;
    private final long numberOfElimiminatedAnswers;

    public GuessPerformance(String solution, String[] guess, long numberOfEliminatedAnswers) {
        this.solution = solution;
        this.guess = guess;
        this.numberOfElimiminatedAnswers = numberOfEliminatedAnswers;
    }

    public String getSolution() {
        return solution;
    }

    public String[] getGuess() {
        return guess;
    }

    public long getNumberOfElimiminatedAnswers() {
        return numberOfElimiminatedAnswers;
    }
}
