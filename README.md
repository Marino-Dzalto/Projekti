# Epsilon-NFA
# Java implementation of epsilon nondeterministic finite automaton

The input to the automaton simulator is a text record of its definition and the input string,
and the output is a text record of the sets of states the automaton was in for each loaded character of the input string

In the input record, each line ends with a newline character (\n)

When printing the simulator output, it is necessary to preserve the lexicographic order within each set of states.

In the definition of the epsilon-transition, the alphabet symbol will be replaced by the $ sign
(the ε symbol is replaced by the $ symbol)

# An empty set of states is indicated by the symbol #.

Format for recording input string and ε-NKA definitions are:
* Input strings separated by |. The symbols of each individual string are separated by a comma.
* Lexicographically ordered set of comma-separated states.
* Lexicographically ordered set of alphabet symbols separated by a comma
