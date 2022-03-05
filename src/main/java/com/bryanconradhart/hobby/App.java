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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class App 
{
    public static void main( String[] args )
    {
        Instant start = Instant.now();
        try {
            int guessDepth = args != null && args.length > 0 ? Integer.parseInt(args[0]) : 1;
            new App().run(guessDepth);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("\ncomplete calculation: " + Duration.between(start, Instant.now()).truncatedTo(ChronoUnit.SECONDS));
        }
    }
    
    private final Data data;

    public App() throws JsonSyntaxException, JsonIOException, IOException, URISyntaxException {
        data = new Gson().fromJson(Files.newBufferedReader(Paths.get(getClass().getClassLoader().getResource("data.json").toURI())), Data.class);
        data.getGuesses().addAll(data.getAnswers());

        System.out.println("answers: " + data.getAnswers().size() + "\t valid words: " + data.getGuesses().size());
    }

    private void run(int guessDepth) throws IOException {
        Stream<String[]> guesses = setupGuesses(guessDepth, data);

        Stream<GuessPerformance> solutions = getSolutions(data.getAnswers(), guesses);
        Map<String[], Double> guessScores = getAvergageMatchingAnswersPerGuess(solutions);
        
        try(FileWriter writer = new FileWriter(new File("Results.txt"), false)) {
            guessScores.entrySet().stream()
                .sorted((l, r) -> r.getValue().compareTo(l.getValue()))
                .forEachOrdered(entry -> appendOutput(writer, entry.getKey(), entry.getValue()));
        }
    }

    private void appendOutput(Writer writer, String[] guess, double score) {
        try {
            writer.write(Arrays.toString(guess) + ": " + score + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	private Stream<String[]> setupGuesses(int guessCount, Data data) {
        String[] validGuesses = Stream.concat(data.getGuesses().stream(), data.getAnswers().stream())
            .unordered()
            .distinct()
            .toArray(n->new String[n]);
        String[][] guesses = new String[(int) Math.pow(validGuesses.length, guessCount)][guessCount];
        
        System.out.println("\ngenerating guesses:");
        AtomicLong guessesGenerated = new AtomicLong();
        Instant generatingStartTime = Instant.now();
        IntStream.range(0, guesses.length)
            .unordered()
            .parallel()
            .peek(guess-> onGuessCompleted(5000, generatingStartTime, guesses.length, guessesGenerated.incrementAndGet()))
            .forEach(i -> generateGuess(validGuesses, guesses[i], i));

        System.out.println("\nGuess combinations: " +  guesses.length);
        System.out.println("solving guesses:");
        AtomicLong solvedGuesses = new AtomicLong();
        Instant solvingStartTime = Instant.now();
        return Arrays.stream(guesses)
            .unordered()
            .parallel()
            .distinct()
            .peek(guess-> onGuessCompleted(10, solvingStartTime, guesses.length, solvedGuesses.incrementAndGet()));
    }

    private void generateGuess(String[] validGuesses, String[] emptyGuess, int guessNumber) {
        for(int j=0; j<emptyGuess.length; j++) {
            emptyGuess[j] = validGuesses[guessNumber/(int)Math.pow(validGuesses.length, j)%validGuesses.length];
        }
        Arrays.sort(emptyGuess);
    }

    private Stream<GuessPerformance> getSolutions(Set<String> validAnswers, Stream<String[]> guesses) {
        return guesses
            .flatMap(guess -> getSolutionsForGuess(validAnswers, guess));
    }

    private Stream<GuessPerformance> getSolutionsForGuess(Set<String> validAnswers, String[] guess) {
		return validAnswers.stream()
            .map(answer -> getSolutionForGuess(validAnswers, answer, guess));
	}

	private GuessPerformance getSolutionForGuess(Set<String> validAnswers, String answer, String[] guess) {
		return new GuessPerformance(
                answer,
                guess,
                Arrays.stream(guess)
                    .flatMap(guessWord -> getEliminatedAnswers(validAnswers, answer, guessWord).stream())
                    .count());
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

    private Map<String[], Double> getAvergageMatchingAnswersPerGuess(Stream<GuessPerformance> solutions) {
        return solutions
            .collect(
                Collectors.groupingByConcurrent(
                    GuessPerformance::getGuess,
                    Collectors.averagingDouble(GuessPerformance::getNumberOfElimiminatedAnswers)));
    }

    private void onGuessCompleted(int printPeriod, Instant startTime, long totalGuesses, long completedGuesses) {
        if(completedGuesses%printPeriod == 0) {
            Duration elapsedTime = Duration.between(startTime, Instant.now());
            Duration expectedDuration = Duration.ofMillis(Math.round(elapsedTime.toMillis() * (totalGuesses/(double)completedGuesses)));
            Duration expectedRemaining = expectedDuration.minus(elapsedTime);
            System.out.print("\r" + ((100*completedGuesses)/totalGuesses) + "% complete: " + completedGuesses + " of " + totalGuesses + ". Estimated remaining time: " + expectedRemaining.truncatedTo(ChronoUnit.SECONDS));
        }
    }
}
