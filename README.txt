Ezra Palmers (ID: 1293020)
Minh Nguyen (ID: 1618568)

This project consists of two Java programs: REcompile and REsearch

REcompile compiles a regular expression into a Fintie State Machine (FSM) represented as a list of states. 
Each state includes a type (branching, literal, or wildcard) and two next-state transitions.
The FSM is compressed to reduce redundant states after compiling.

REsearch uses the FSM to search a text file, attempting to match the regex starting at each character position in each line.
It uses a custom Double Ended Queue (Deque) to mangage states during the search.

REcompile Usage:
java REcompile <regex> [-c]
    <regex> (required): The regular expression to compile a FSM for. The expression must follow the given phrase structure rules.
    [-c] (optional): Disables state compression.

REsearch Usage
java REsearch <textfile.txt> < fsm.txt (or pipe from 'java REcompile "regex"')
    <textfile.txt> (required): The text file to search for matches to the regular expression

Build/Run:
javac REcompile.java REsearch.java
java REcompile java REcompile "aardvark|zebra" | java REsearch simple.txt

Regular Expression Grammar

    |       Alternation
    *       0 or more
    ?       0 or 1
    +       1 or more
    .       Wildcard (any literal)
    (E)     Raise precedence of the non empty regular expression E
    \       Escape character (Next char is interpreted as a literal and loses special meaning)
    Literal All non-speical characters that match them selves

Phrase Structure Rules
Expression -> Term -> Factor -> Atom

E -> T
E -> T|E		    Alternation
T -> F       
T -> FT			    Concatenation
F -> A
F -> A*			    Zero or more
F -> A?			    Zero or one
F -> A+			    One or more
A -> Literal		Non-special characters
A -> .			    Wildcard
A -> \ AnySymbol	Escape character
A -> (E)			Parentheses

Precedence
- Mathing literals
- Escaped characters (Symbols preceded by \ lose special meaning and a treated as literals)
- Parentheses (Most deeply nested regex have the highest precedence)
- Repetition operators * ? +
- Concatenation
- Alternation |