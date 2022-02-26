package com.bryanconradhart.hobby;

import java.util.Set;

public class Solution {
    final private String solution;
    final private Set<String> guess;
    final private Set<String> matchingAnswers;

    public Solution(String solution, Set<String> guess, Set<String> matchingAnswers) {
        this.solution = solution;
        this.guess = guess;
        this.matchingAnswers = matchingAnswers;
    }

    public String getSolution() {
        return solution;
    }

    public Set<String> getGuess() {
        return guess;
    }

    public Set<String> getMatchingAnswers() {
        return matchingAnswers;
    }
}
