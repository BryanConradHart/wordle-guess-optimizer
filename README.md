# Wordle Guess Optimizer
## What
Find the best average wordle guess for a given dictionary of answers and valid guesses.

## Features
* multi-guess combinations. What is the best 3-word starting combo? No problem. 
Well, maybe _some_ problem:
increasing the guess combo means the computing time and memory requirements scale exponentially
versus the dictionary size.
* Change the dictionary. Got your own words? no problem, just replace our dictionary file.
By default, this has the wordle dictionary, from before it was bought by NYT.
To change the dictionary, update `src/main/resources/dictionary.json`

## Run
Just run it like any java app.
You _can_ pass in a command line argument for the size of the guess-combo to solve for, but it defualts to 1.
Just make sure you give the JVM enough memory -
solving for a single-word opening guess takes about 11GB,
solving for a two-word opening combo takes about 18GB