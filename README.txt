Author: Ezra Palmers
ID: 1293020
Partner ID: 1618568

REcompile - Regular Expression FSM compiler

REcompile is a java program that takes an input regular expression and compiles an equivalent FSM, then outputs it in comma seperated format to standard outputs

Command-Line Arguments
- regex (required): The regular expression to compile a FSM for. The expression must follow the following phrase structure rules.

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
A -> .			    Wildcard (Any literal)
A -> \ AnySymbol	Escape character (Next char is interpreted as a literal) 
A -> (E)			Parentheses

Precedence
- Mathing literals
- Escaped characters (Symbols preceded by \ lose special meaning and a treated as literals)
- Parentheses (Most deeply nested regex have the highest precedence)
- Repetition operators * ? +
- Concatenation
- Alternation |