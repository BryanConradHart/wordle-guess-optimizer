package com.bryanconradhart.hobby;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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
    private long completedGuesses = 0;
    private Instant startTime = Instant.now();
    private long numGuesses = 0;

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
        Stream<Solution> solutions = getSolutions(data.getAnswers(), guesses);
        Map<Set<String>, Double> guessScores = getAvergageMatchingAnswersPerGuess(solutions);
        
        try(FileWriter writer = new FileWriter(new File("Results.txt"), false)) {
            guessScores.entrySet().stream()
                .sorted((l, r) -> r.getValue().compareTo(l.getValue()))
                .forEachOrdered(entry -> appendOutput(writer, entry.getKey(), entry.getValue()));
        }
    }

    private void appendOutput(Writer writer, Set<String> guess, double score) {
        try {
            writer.write(guess + ": " + score + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	private Set<Set<String>> setupGuesses(Data data) {
        //TODO add 2nd word
        Set<String> validWords = new HashSet<>(data.getGuesses());
        validWords.addAll(data.getAnswers());
        Set<Set<String>> guesses = new HashSet<>(validWords.size());
        for(String guess : validWords) {
            Set<String> guessCombo = new HashSet<>();
            guessCombo.add(guess);
            guesses.add(guessCombo);
        }
        return guesses;
    }

    private Stream<Solution> getSolutions(Set<String> validAnswers, Set<Set<String>> guesses) {
        return guesses.parallelStream()
            .flatMap(guess -> getSolutionsForGuess(validAnswers, guess));
    }

    private Stream<Solution> getSolutionsForGuess(Set<String> validAnswers, Set<String> guess) {
		Stream<Solution> remainingAnswers = validAnswers.stream()
            .map(answer -> getSolutionForGuess(validAnswers, answer, guess));
        onGuessCompleted();
        return remainingAnswers;
	}

	private Solution getSolutionForGuess(Set<String> validAnswers, String answer, Set<String> guess) {
		return new Solution(
                answer,
                guess,
                guess.stream()
                    .flatMap(guessWord -> getEliminatedAnswers(validAnswers, answer, guessWord).stream())
                    .collect(Collectors.toSet()));
	}

    public Collection<String> getEliminatedAnswers(Set<String> validAnswers, String answer, String guess) {
        return validAnswers.stream()
            .filter(potentialAnswer -> isAnswerEliminated(potentialAnswer, answer, guess))
            .collect(Collectors.toList());        
    }

    public boolean isAnswerEliminated(String potentialAnswer, String actualAnswer, String guess) {
        //guesses that eliminate the most answers are the best
        return !answerHasAllGreenSquareLettersAtCorrectPosition(potentialAnswer, actualAnswer, guess) ||
               !answerContainsAllYellowSquareLetters(potentialAnswer, actualAnswer, guess) ||
                answerContainsBlackSquareLetters(potentialAnswer, actualAnswer, guess);
    }

    private boolean answerHasAllGreenSquareLettersAtCorrectPosition(String potentialAnswer, String actualAnswer, String guess) {
        for(int i = 0; i<5; i++) {
            if(guess.charAt(i)==actualAnswer.charAt(i) && guess.charAt(i)!=potentialAnswer.charAt(i)) return false;
        }
        return true;
    }

    private boolean answerContainsAllYellowSquareLetters(String potentialAnswer, String actualAnswer, String guess) {
        for(char ch : guess.toCharArray()) {
            if(actualAnswer.contains(String.valueOf(ch)) && !potentialAnswer.contains(String.valueOf(ch))) return false;
        }
        return true;
    }

    private boolean answerContainsBlackSquareLetters(String potentialAnswer, String actualAnswer, String guess) {
        for(char ch : guess.toCharArray()) {
            if(!actualAnswer.contains(String.valueOf(ch)) && potentialAnswer.contains(String.valueOf(ch))) return true;
        }
        return false;
    }

    private Map<Set<String>, Double> getAvergageMatchingAnswersPerGuess(Stream<Solution> solutions) {
        return solutions
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
