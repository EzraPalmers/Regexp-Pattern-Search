// Authors: Ezra Palmers (ID: 1293020), Minh Nguyen (ID: 1618568)
// Last modified: 16/05/25

import java.io.*;
import java.util.*;

public class REsearch {
    // Lists to store FSM states
    private static final List<String> stateType = new ArrayList<>();
    private static final List<Integer> next1 = new ArrayList<>();
    private static final List<Integer> next2 = new ArrayList<>();
    // Global variables
    private static final int SCAN = -2;

    private static class MyDeque<T> {
        private static class Node<T> {
            T val;
            Node<T> next;

            Node(T val) {
                this.val = val;
            }
        }

        private Node<T> head = null;
        private Node<T> tail = null;
        private int size = 0;

        // Adds a value to the front of the deque (used for branch states).
        void addFirst(T val) {
            Node<T> node = new Node<>(val);
            if (head == null) {
                head = tail = node;
            } else {
                node.next = head;
                head = node;
            }
            size++;
        }

        // Adds a value to the end of the deque (used for wildcard, literal, SCAN).
        void addLast(T val) {
            Node<T> node = new Node<>(val);
            if (tail == null) {
                head = tail = node;
            } else {
                tail.next = node;
                tail = node;
            }
            size++;
        }

        // Removes and returns the value at the front (or null if empty).
        T poll() {
            if (head == null) {
                return null;
            }
            T val = head.val;
            head = head.next;
            if (head == null) {
                tail = null;
            }
            size--;
            return val;
        }

        // Returns the value at the front without removing (or null if empty).
        T peek() {
            return head != null ? head.val : null;
        }
    }

    public static void main(String[] args) throws Exception {
        // Ensure the user provides exactly one argument (filename to search)
        if (args.length != 1) {
            System.err.println("Usage: java REsearch textfile.txt < fsm.txt (or pipe from 'java REcompile \"regex\"')");
            return;
        }
        // Input text file
        String filename = args[0];
        // Read FSM definition from standard input
        BufferedReader fsmReader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = fsmReader.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length == 4) { // Normal case
                stateType.add(parts[1]);
                next1.add(Integer.valueOf(parts[2]));
                next2.add(Integer.valueOf(parts[3]));
            } else { // Comma literal (parts.length == 5)
                stateType.add(",");
                next1.add(Integer.valueOf(parts[3]));
                next2.add(Integer.valueOf(parts[4]));
            }
        }
        try (// Read the input file line by line and simulate the FSM on each line
                BufferedReader fileReader = new BufferedReader(new FileReader(filename))) {
            String fileLine;
            boolean fsmMatchedEmptyString = false; // Track if any line matched without matching a char
            while ((fileLine = fileReader.readLine()) != null) {
                boolean matched = false; // Flag to indicate if the line matches the regex
                boolean matchedChar = false; // Track if a char has been matched this line
                MyDeque<Integer> queue = new MyDeque<>();
                Set<Integer> visited = new HashSet<>();
                // Try matching starting from each character in the line
                for (int i = 0; i < fileLine.length(); i++) {
                    queue.addLast(0); // New possible match from state 0
                    queue.addLast(SCAN); // Marker to seperate possible current from possible next states
                    while (queue.peek() != SCAN) {
                        int s = queue.poll();
                        if (s == -1) { // Accept State
                            if (!matchedChar) {
                                fsmMatchedEmptyString = true;
                            }
                            matched = true;
                            break;
                        }
                        if (visited.contains(s)) {
                            continue; // Skip state if already visited
                        }
                        visited.add(s);
                        int n1 = next1.get(s);
                        int n2 = next2.get(s);
                        String type = stateType.get(s);

                        if (type.equals("BR")) {
                            // Branching state: Add next states to front of queue
                            queue.addFirst(n1);
                            if (n1 != n2) {
                                queue.addFirst(n2);
                            }
                        } else if (type.equals("WC") || fileLine.charAt(i) == type.charAt(0)) {
                            // Wildcard: Match any character || Literal: if char in text matches
                            matchedChar = true;
                            queue.addLast(n1);
                            if (n1 != n2) {
                                queue.addLast(n2); // Next is literal or WC
                            }
                        }
                    }
                    queue.poll(); // Remove SCAN
                    visited.clear();
                    // If a match was found, print the line and skip to the next line
                    if (matched) {
                        System.out.println(fileLine);
                        break;
                    }
                }
                // Process any remaining states on queue
                // Input is consumed so only BR and -1 states are valid
                if (!matched) {
                    while (queue.peek() != null) {
                        int s = queue.poll();
                        if (s == -1) {
                            System.out.println(fileLine);
                            break;
                        }
                        if (stateType.get(s).equals("BR")) {
                            int n1 = next1.get(s);
                            int n2 = next2.get(s);
                            queue.addFirst(n1);
                            if (n1 != n2) {
                                queue.addFirst(n2);
                            }
                        }
                    }
                }
            }
            if (fsmMatchedEmptyString) {
                System.err.println("Regex matched without a character match");
            }
        }
    }
}
