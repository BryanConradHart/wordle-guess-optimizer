package com.bryanconradhart.hobby;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class App 
{

    private long numGuesses = 0;
    private long completedGuesses = 0;
    private Instant startTime = Instant.now();

    public static void main( String[] args )
    {
        Long start = System.currentTimeMillis();
        try {
           new App().run();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("complete calculation: " + ((System.currentTimeMillis() - start)/1000d));
        }
    }

    private final Data data;

    public App() throws JsonSyntaxException, JsonIOException, IOException, URISyntaxException {
        data = new Gson().fromJson(Files.newBufferedReader(Paths.get(getClass().getClassLoader().getResource("data.json").toURI())), Data.class);
        data.getGuesses().addAll(data.getAnswers());

        System.out.println("answers: " + data.getAnswers().size() + "\t valid words: " + data.getGuesses().size());
    }

    private void run() throws IOException {
        Set<Set<String>> guesses = setupGuesses(data);
        numGuesses = guesses.size();
        System.out.println("Guess combinations: " + numGuesses);

        System.out.println();
        Stream<Solution> remainingAnswers = getRemainingAnswers(data.getAnswers(), guesses);
        Map<Set<String>, Double> guessScores = getAverageNumberOfRemainingAnswers(remainingAnswers);
        StringBuilder outputText = new StringBuilder();

        guessScores.entrySet().stream()
            .sorted((l, r) -> r.getValue().compareTo(l.getValue()))
            .forEach(entry -> outputText.append(entry.getKey() + ": " + entry.getValue() + "\n"));

        try(FileWriter writer = new FileWriter(new File("Results.txt"), false)) {
            writer.write(outputText.toString());
        }
    }

	private Set<Set<String>> setupGuesses(Data data) {
        Set<String> validWords = new HashSet<>(data.getGuesses());
        validWords.addAll(data.getAnswers());
        Set<Set<String>> guesses = new HashSet<Set<String>>(validWords.size());
        for(String guess : validWords) {
            Set<String> guessCombo = new HashSet<>();
            guessCombo.add(guess);
            guesses.add(guessCombo);
        }
        return guesses;
    }

    private Stream<Solution> getRemainingAnswers(Set<String> potentialSolutions, Set<Set<String>> guesses) {
        return guesses.parallelStream()
            .flatMap(guess -> getRaminingAnswers(guess, potentialSolutions));
    }

    private Stream<Solution> getRaminingAnswers(Set<String> guess, Set<String> potentialSolutions) {
		Stream<Solution> remainingAnswers = potentialSolutions.stream()
            .map(potentialSolution -> getRemainingAnswers(guess, potentialSolution));
        onGuessCompleted();
        return remainingAnswers;
	}

	private Solution getRemainingAnswers(Set<String> guess, String potentialSolution) {
		Solution solution =  new Solution(
                potentialSolution,
                guess,
                guess.stream()
                    .flatMap(guessWord -> getAnswersThatFit(guessWord, potentialSolution).stream())
                    .collect(Collectors.toSet()));
        return solution;
	}

    public Collection<String> getAnswersThatFit(String guess, String solution) {
        return data.getAnswers().stream()
            .filter(answer -> doesAnswerFit(answer, guess, solution))
            .collect(Collectors.toList());        
    }

    public boolean doesAnswerFit(String answer, String guess, String solution) {
        //guesses that eliminate the most answers are the best
        return !answerHasAllGreenSquaresAtCorrectPosition(answer, guess, solution) ||
               !answerContainsAllYellowSquareLetters(answer, guess, solution) ||
               !answerContainsNoBlackSquares(answer, guess, solution);
    }

    private boolean answerHasAllGreenSquaresAtCorrectPosition(String answer, String guess, String solution) {
        for(int i = 0; i<5; i++) {
            if(guess.charAt(i)==solution.charAt(i) && guess.charAt(i)!=answer.charAt(i)) return false;
        }
        return true;
    }

    private boolean answerContainsAllYellowSquareLetters(String answer, String guess, String solution) {
        for(char ch : guess.toCharArray()) {
            if(solution.contains(String.valueOf(ch)) && !answer.contains(String.valueOf(ch))) return false;
        }
        return true;
    }

    private boolean answerContainsNoBlackSquares(String answer, String guess, String solution) {
        for(char ch : guess.toCharArray()) {
            if(!solution.contains(String.valueOf(ch)) && answer.contains(String.valueOf(ch))) return false;
        }
        return true;
    }

    private Map<Set<String>, Double> getAverageNumberOfRemainingAnswers(Stream<Solution> remainingAnswers) {
        return remainingAnswers
            .collect(
                Collectors.groupingByConcurrent(
                    Solution::getGuess,
                    Collectors.averagingDouble(solution -> solution.getMatchingAnswers().size())));
    }

    private void onGuessCompleted() {
        if(++completedGuesses%100 == 0) {
            Duration elapsedTime = Duration.between(startTime, Instant.now());
            Duration expectedDuration = Duration.ofMillis(Math.round(elapsedTime.toMillis() * (numGuesses/(double)completedGuesses)));
            Duration expectedRemaining = expectedDuration.minus(elapsedTime);
            System.out.print("\r" + ((100*completedGuesses)/numGuesses) + "% complete: " + completedGuesses + " of " + numGuesses + ". Estimated remaining time: " + expectedRemaining.truncatedTo(ChronoUnit.SECONDS));
        }
    }
}
