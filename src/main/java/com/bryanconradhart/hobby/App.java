package com.bryanconradhart.hobby;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.bryanconradhart.hobby.WordleGuessOptimizer.Dictionary;
import com.google.gson.Gson;

public class App 
{
    
    public static void main( String[] args )
    {
        Instant start = Instant.now();
        try (FileWriter writer = new FileWriter(new File("Results.txt"), false)) {
            int guessDepth = args != null && args.length > 0 ? Integer.parseInt(args[0]) : 1;
            Dictionary dictionary = new Gson().fromJson(Files.newBufferedReader(Paths.get(App.class.getClassLoader().getResource("dictionary.json").toURI())), Dictionary.class);
            
            new WordleGuessOptimizer(System.out, dictionary, guessDepth, writer).run();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("\ncomplete calculation: " + Duration.between(start, Instant.now()).truncatedTo(ChronoUnit.SECONDS));
        }
    }
    
    
}
