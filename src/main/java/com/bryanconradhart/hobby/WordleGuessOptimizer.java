package com.bryanconradhart.hobby;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class WordleGuessOptimizer {
    public static record Dictionary (Set<String> answers, Set<String> guesses){}
    
    private final PrintStream logger;
    private final Dictionary dictionary;
    private final int guessDepth;
    private final Writer outputWriter;

    public WordleGuessOptimizer(PrintStream logger, Dictionary dictionary, int guessDepth, Writer outputWriter) {
        this.logger = logger;
        this.dictionary = dictionary;
        this.guessDepth = guessDepth;
        this.outputWriter = outputWriter;
    }

    public void run() throws IOException {
        logger.println("answers: " + dictionary.answers().size() + "\t valid words: " + dictionary.guesses().size());
        Stream<String[]> guesses = setupGuesses(guessDepth, dictionary);

        Stream<GuessPerformance> solutions = getSolutions(dictionary.answers(), guesses);
        Map<String[], Double> guessScores = getAvergageMatchingAnswersPerGuess(solutions);
        
        logger.println("\nwriting guess performance to file:");
        Instant writingStart = Instant.now();
        AtomicLong guessesWritten = new AtomicLong();
        guessScores.entrySet().stream()
            .sorted((l, r) -> r.getValue().compareTo(l.getValue()))
            .peek(guess-> onGuessCompleted(5000, writingStart, guessScores.size(), guessesWritten.incrementAndGet()))
            .forEachOrdered(entry -> appendOutput(outputWriter, entry.getKey(), entry.getValue()));
    }

	private Stream<String[]> setupGuesses(int guessCount, Dictionary data) {
        String[] validGuesses = Stream.concat(data.guesses().stream(), data.answers().stream())
            .unordered()
            .distinct()
            .toArray(n->new String[n]);
        String[][] guesses = new String[(int) Math.pow(validGuesses.length, guessCount)][guessCount];
        
        logger.println("\ngenerating guesses:");
        AtomicLong guessesGenerated = new AtomicLong();
        Instant generatingStartTime = Instant.now();
        IntStream.range(0, guesses.length)
            .unordered()
            .parallel()
            .peek(guess-> onGuessCompleted(5000, generatingStartTime, guesses.length, guessesGenerated.incrementAndGet()))
            .forEach(i -> generateGuess(validGuesses, guesses[i], i));

        logger.println("\nGuess combinations: " +  guesses.length);
        logger.println("solving guesses:");
        AtomicLong solvedGuesses = new AtomicLong();
        Instant solvingStartTime = Instant.now();
        return Arrays.stream(guesses)
            .unordered()
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
        return guesses.parallel()
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
                    .flatMap(guessWord -> getEliminatedAnswers(validAnswers, answer, guessWord))
                    .count());
	}

    public Stream<String> getEliminatedAnswers(Set<String> validAnswers, String answer, String guess) {
        return validAnswers.stream()
            .filter(potentialAnswer -> isAnswerEliminated(potentialAnswer, answer, guess));
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
            logger.print("\r" + ((100*completedGuesses)/totalGuesses) + "% complete: " + completedGuesses + " of " + totalGuesses + ". Estimated remaining time: " + expectedRemaining.truncatedTo(ChronoUnit.SECONDS));
        }
    }

    private void appendOutput(Writer writer, String[] guess, double score) {
        try {
            writer.write(Arrays.toString(guess) + ": " + score + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
