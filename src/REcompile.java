/*
 * Author: Ezra Palmers (ID: 1293020)
 * Last modified: 16/05/25
 * 
 * Phrase Structure Rules
 * Expression -> Term -> Factor -> Atom
 * 
 * E -> T
 * E -> T|E			Alternation
 * T -> F
 * T -> FT			Concatenation
 * F -> A
 * F -> A*			Zero or more
 * F -> A?			Zero or one
 * F -> A+			One or more
 * A -> Literal		Non-special characters
 * A -> .			Wildcard (Any literal)
 * A -> \ AnySymbol	Escape character (Next char is interpreted as a literal)
 * A -> (E)			Parentheses
 *
 */

import java.util.*;

public class REcompile {

    // Lists to store FSM states
    private static List<String> stateType = new ArrayList<>();
    private static List<Integer> next1 = new ArrayList<>();
    private static List<Integer> next2 = new ArrayList<>();
    // Global variables
    private static final String OPERATORS = ".*+?|()\\";
    private static final int TEMP = -2;
    private static int nextState = 0;
    private static int j = 0;
    private static char[] regex;

    public static void main(String[] args) {
        // Validate argument count
        if (args.length < 1 || args.length > 2) {
            System.err.println("Error: Incorrect number of arguments");
            printUsage();
            System.exit(1);
        }
        regex = args[0].toCharArray();

        // Check for compression flag
        boolean compress = true; // Default no compression
        if (args.length == 2) {
            if (args[1].equals("-c")) {
                compress = false;
            } else {
                System.err.println("Error: Invalid second argument");
                printUsage();
                System.exit(1);
            }
        }
        checkBrackets(); // Preprocessing check for matching brackets
        // State 0
        setState("BR", TEMP, TEMP);
        int start = expression();
        // Update state 0 to point to start state of FSM
        updateTEMP(0, start);

        // Check if all input consumed (valid regex)
        if (j < regex.length) {
            printFSM();
            error("Unexpected character");
        }
        // Check lists have the expected size
        if (stateType.size() != nextState || next1.size() != nextState || next2.size() != nextState) {
            throw new IllegalStateException("Error in state creation, lists are different sizes");
        }
        // Check TEMP transitions have been resolved except for last state
        for (int i = 0; i < nextState - 1; i++) {
            if (next1.get(i) == TEMP || next2.get(i) == TEMP) {
                printFSM();
                throw new IllegalStateException("Unresolved temporary transition at state " + i);
            }
        }
        // Create final state
        updateTEMP(nextState - 1, nextState); // Update end state to point to final state
        setState("BR", -1, -1); // Create final state
        // Compress if arg given
        if (compress)
            compressStates();
        printFSM(); // Print complete FSM
    }

    private static int expression() {
        int tStart = term();
        // Check if expression is in alternation
        if (j == regex.length || regex[j] != '|') {
            return tStart;
        }
        // Alternation
        j++; // Consume '|'
        int tEnd = nextState - 1; // End of first term (TEMP transitions)
        // Create br state for alternation
        int branchState = nextState;
        setState("BR", tStart, TEMP);
        int eStart = expression();
        int eEnd = nextState - 1; // End of second expression (TEMP transitions)
        // Create dummy state to synchronize end states
        int dummyState = nextState;
        setState("BR", TEMP, TEMP);
        // Update TEMP transitions
        updateTEMP(branchState, eStart);
        updateTEMP(tEnd, dummyState);
        updateTEMP(eEnd, dummyState);
        return branchState;
    }

    private static int term() {
        int fStart = factor();
        // Concatenation - Next char must either be literal . \ (
        // Return if no more input or next char is '|' or ')'
        if (j == regex.length || regex[j] == '|' || regex[j] == ')') {
            return fStart;
        }
        if (isLiteral(regex[j]) || regex[j] == '.' || regex[j] == '\\' || regex[j] == '(') {
            int fEnd = nextState - 1;
            int tStart = term();
            updateTEMP(fEnd, tStart);
            return fStart;
        } else {
            error("Unexpected input");
            return fStart; // Unreachable
        }
    }

    private static int factor() {
        int aStart = atom();
        // Repetition - Next char must be one of * + ?
        // * Zero or more
        // + One or more
        // ? Zero or one
        // Return if next char not a repetition operator or at end of input
        if (j == regex.length || !(regex[j] == '*' || regex[j] == '+' || regex[j] == '?')) {
            return aStart;
        }
        char op = regex[j];
        j++; // Consume operator
        // Check for multiple repetition operators
        if (j < regex.length && (regex[j] == '*' || regex[j] == '+' || regex[j] == '?')) {
            error("Multiple repetition operators");
        }
        int aEnd = nextState - 1;
        int branchState = nextState;
        if (op == '*' || op == '+') {
            setState("BR", aStart, TEMP);
            updateTEMP(aEnd, branchState);
        }
        if (op == '?') {
            int dummyState = nextState + 1;
            updateTEMP(aEnd, dummyState);
            setState("BR", aStart, dummyState);
            setState("BR", TEMP, TEMP); // Dummy state
        }
        return (op == '+') ? aStart : branchState;
    }

    private static int atom() {
        if (j == regex.length)
            error("Unexpected end of input");
        if (isLiteral(regex[j])) {
            setState(regex[j], TEMP, TEMP);
            j++; // Consume literal
            return nextState - 1;
        } else if (regex[j] == '.') {
            setState("WC", TEMP, TEMP);
            j++; // Consume .
            return nextState - 1;
        } else if (regex[j] == '\\') {
            j++; // Consume \
            if (j == regex.length)
                error("Incomplete escape sequence");
            setState(regex[j], TEMP, TEMP);
            j++; // Consume AnySymbol
            return nextState - 1;
        } else if (regex[j] == '(') {
            j++; // Consume (
            int eStart = expression();
            if (j == regex.length || regex[j] != ')')
                error("Missing closing parenthesis");
            j++; // Consume )
            return eStart;
        } else {
            error("Unexpected input");
            return nextState - 1; // Unreachable
        }
    }

    // Returns true if char c is not an operator
    private static boolean isLiteral(char c) {
        return OPERATORS.indexOf(c) == -1;
    }

    // Create new state with index nextState
    private static void setState(String c, int n1, int n2) {
        stateType.add(c);
        next1.add(n1);
        next2.add(n2);
        nextState++;
    }

    private static void setState(char c, int n1, int n2) {
        setState(String.valueOf(c), n1, n2);
    }

    // Update TEMP transition values with start state of next machine
    private static void updateTEMP(int s, int n) {
        if (s >= 0 && s < nextState) {
            if (next1.get(s) == TEMP)
                next1.set(s, n);
            if (next2.get(s) == TEMP)
                next2.set(s, n);
        }
    }

    // Print comma seperated FSM
    private static void printFSM() {
        for (int i = 0; i < stateType.size(); i++) {
            System.out.printf("%d,%s,%d,%d%n",
                    i,
                    stateType.get(i),
                    next1.get(i),
                    next2.get(i));
        }
    }

    // Throws IllegalArgumentExcpetion with input messsage
    private static void error(String message) {
        if (j < regex.length) {
            throw new IllegalArgumentException(
                    String.format("%s at position %d: '%c'", message, j, regex[j]));
        } else {
            throw new IllegalArgumentException(
                    String.format("%s at position %d: end of input", message, j));
        }
    }

    // Print usage details
    private static void printUsage() {
        System.err.println("Usage: REcompile <regex> [-c]");
        System.err.println("  <regex>  Required. The regular expression to compile");
        System.err.println("  -c       Optional. Disable FSM compression");
    }

    // Checks that all open brackets are followed by equal number of close brackets
    private static void checkBrackets() {
        int brackets = 0;
        for (int i = 0; i < regex.length; i++) {
            if (regex[i] == '\\') {
                i++; // Skip escaped character
                continue;
            }
            if (regex[i] == '(')
                brackets++;
            else if (regex[i] == ')')
                brackets--;
            if (brackets < 0)
                break;
        }
        if (brackets != 0)
            throw new IllegalArgumentException("Invalid regex: Mismatched brackets");
    }

    // Compress states
    private static void compressStates() {
        // Swap state 0 with start state
        int startState = next1.get(0);
        Collections.swap(stateType, 0, startState);
        Collections.swap(next1, 0, startState);
        Collections.swap(next2, 0, startState);
        // Update any references to startState to 0
        for (int i = 0; i < stateType.size(); i++) {
            if (next1.get(i) == startState)
                next1.set(i, 0);
            if (next2.get(i) == startState)
                next2.set(i, 0);
        }
        // Mark BR->DUMMY and record indicies
        Set<Integer> dummyIndices = new HashSet<>();
        for (int i = 0; i < stateType.size(); i++) {
            if (stateType.get(i).equals("BR") && next1.get(i).equals(next2.get(i))) {
                stateType.set(i, "DUMMY");
                dummyIndices.add(i);
            }
        }
        // Update non branching states to point to 2 different states
        for (int i = 0; i < stateType.size(); i++) {
            // Skip dummy states
            if (dummyIndices.contains(i))
                continue;
            // If next1 and next2 are equal, and pointing at a BR or DUMMY then update
            while (next1.get(i).equals(next2.get(i))) {
                int next = next1.get(i);
                if (next == -1) // Dont update accepting state
                    break;
                // Only collapse states if next state is DUMMY or BR
                if (!Arrays.asList("DUMMY", "BR").contains(stateType.get(next))) {
                    break;
                }
                // Update both transitions
                next1.set(i, next1.get(next));
                next2.set(i, next2.get(next));
            }
            // Check if n1 or n2 pointing at dummy state
            while (dummyIndices.contains(next1.get(i))) {
                next1.set(i, next1.get(next1.get(i))); // Update to match dummy state
            }
            while (dummyIndices.contains(next2.get(i))) {
                next2.set(i, next2.get(next2.get(i))); // Update to match dummy state
            }
        }
        Set<Integer> referencedStates = new TreeSet<>();
        referencedStates.add(0); // In case no state references state 0
        // Collect all state indiceis referenced by non-dummy state
        for (int i = 0; i < stateType.size(); i++) {
            if (!dummyIndices.contains(i)) {
                referencedStates.add(next1.get(i));
                referencedStates.add(next2.get(i));
            }
        }
        referencedStates.remove(-1); // Remove accept state as its not an index
        // Create mapping from old to new indices
        Map<Integer, Integer> indexMap = new HashMap<>();
        indexMap.put(-1, -1);
        int newIndex = 0;
        for (int oldIndex : referencedStates) {
            indexMap.put(oldIndex, newIndex++);
        }
        // Rebuild the FSM
        List<String> newStateType = new ArrayList<>();
        List<Integer> newNext1 = new ArrayList<>();
        List<Integer> newNext2 = new ArrayList<>();
        for (int oldIndex : referencedStates) {
            newStateType.add(stateType.get(oldIndex));
            newNext1.add(indexMap.get(next1.get(oldIndex)));
            newNext2.add(indexMap.get(next2.get(oldIndex)));
        }
        // Replace old lists with new compressed ones
        stateType = newStateType;
        next1 = newNext1;
        next2 = newNext2;
    }
}
