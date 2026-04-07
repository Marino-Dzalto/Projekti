import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

/*
 *	Troughout the code it will be used 
 *	instance of a class called "constante"
 *	and that will be implemented with object 
 *	constante c = new constante();
 */
public class SimEnka{
//public class epsilonNFA{
	
	// Define a TreeMap 'map' to store state transitions
	private static Map<String, Set<String>> map = new TreeMap<>();
	// Define a StringBuilder 'str' to build the output string
	private static StringBuilder str = new StringBuilder();

	// Check the next state(s) for a given symbol
	public static Set<String> checkNext(Set<String> states, String symbol) {
		constante c = new constante();

		// Create an empty set to hold the resulting states
		Set<String> result = new HashSet<>();

		// For each state in the input set
		for (String state : states) {
			// If there is a transition from the state with the symbol
			if (map.containsKey(state + c.COMMA_SEPARATOR + symbol)) {
				// Add the resulting states to the result set
				result.addAll(map.get(state + c.COMMA_SEPARATOR + symbol));
			}
		}
		// Return the resulting set of states
		return result;
	}

	// Print the set of current states
	public static void printStates(Set<String> set) {
		constante c = new constante();

		// Check if the set is empty
		if (!set.isEmpty()) {
			List<String> list = new ArrayList<>(set);
			// Check if the set contains only the empty state
			if (list.size() == 1 && list.get(0).equals(c.EMPTY_STATE)) {
				str.append(c.EMPTY_STATE);
				str.append(c.VERTICAL_BAR_SEPARATOR);
				return;
			}
			Collections.sort(list);
			for (String s : list) {
				if (!s.equals(c.EMPTY_STATE)) str.append(s + c.COMMA_SEPARATOR);
			}
			str.delete(str.length() - 1, str.length());
		} else {
			str.append(c.EMPTY_STATE);
		}
		str.append(c.VERTICAL_BAR_SEPARATOR);
	}

	// Main method
	public static void main(String[] args) {
		constante c = new constante();

		try (Scanner sc = new Scanner(System.in)) {
			// Read the input string(s)
			String line = sc.nextLine();
			List<String> inputStrings = new ArrayList<>();
			if (line.contains(c.VERTICAL_BAR_SEPARATOR)) {
				Collections.addAll(inputStrings, line.split("\\|"));
			} else {
				inputStrings.add(line);
			}
			// Skip the next three lines
			int i = 0;
			do {
				sc.nextLine();
				i++;
			} while (i < 3);

			// Read the initial state
			String initial = sc.nextLine();

			// Read the state transitions and store them in 'map'
			while (sc.hasNextLine()) {
				line = sc.nextLine();
				String[] parts = line.split(c.SPLITER);
				// Add new transition to existing state
				if (map.containsKey(parts[0])) {
					Collections.addAll(map.get(parts[0]), parts[1].split((String)c.COMMA_SEPARATOR));
				}
				// Create a new state and add transition to it
				Set<String> value = new HashSet<>();
				Collections.addAll(value, parts[1].split((String)c.COMMA_SEPARATOR));
				map.put(parts[0], value);
			}

			// Declare and initialize variables
			List<String> symbols;
			Set<String> currentStates;
			Set<String> newStatesWithSymbol;
			Set<String> newStatesWithEpsilon;
			symbols = new ArrayList<>();
			currentStates = new HashSet<>();
			newStatesWithSymbol = new HashSet<>();
			newStatesWithEpsilon = new HashSet<>();

			// Process each input string
			Process(inputStrings,symbols,currentStates,newStatesWithSymbol,newStatesWithEpsilon,initial);
		}
	}
	private static void Process(List<String> inputStrings, List<String> symbols, Set<String> currentStates, Set<String> newStatesWithSymbol, Set<String> newStatesWithEpsilon,String initial) {
		constante c = new constante();
		for (String input : inputStrings) {
			symbols.clear();
			Collections.addAll(symbols, input.split(c.COMMA_SEPARATOR));
			currentStates.clear();
			currentStates.add(initial);
			for (String symbol : symbols) {
				// Check next states with epsilon transitions
				newStatesWithEpsilon = checkNext(currentStates, c.EPSILON);
				// Add new states with epsilon transition to current states
				if (!newStatesWithEpsilon.isEmpty()) {
					currentStates.addAll(findAllNewWithEps(newStatesWithEpsilon));
				}
				// Print current states
				printStates(currentStates);
				// Check next states with current symbol
				newStatesWithSymbol.addAll(checkNext(currentStates, symbol));
				currentStates.clear();
				currentStates.addAll(newStatesWithSymbol);
				newStatesWithSymbol.clear();
			}
			newStatesWithEpsilon.clear();
			newStatesWithEpsilon.addAll(findAllNewWithEps(currentStates));
			currentStates.addAll(newStatesWithEpsilon);
			newStatesWithEpsilon.clear();
			// Print the final states and clear the StringBuilder
			printStates(currentStates);
			str.delete(str.length() - 1, str.length());
			System.out.println(str);
			str.delete(0, str.length());
		}
		
	}

	private static Collection<String> findAllNewWithEps(Set<String> newStatesWithEpsilon) { 
		constante c = new constante();
		Set<String> newStates;  // set to hold all new states reached through epsilon transitions
		Set<String> processed;  // set to hold all processed states

		newStates = new HashSet<>();
		processed = new HashSet<>();

		while (!newStatesWithEpsilon.isEmpty()) {  // iterate until all reachable states have been processed
			for (String state : newStatesWithEpsilon) {  // iterate through all states in the input set
				newStates.addAll(checkNext(Collections.singleton(state), c.EPSILON));  // add all states reachable by epsilon transitions to the new set
			}
			processed.addAll(newStatesWithEpsilon);  // add all processed states to the processed set
			if (processed.containsAll(newStates)) {  // if all new states have already been processed, break out of the loop
				newStatesWithEpsilon.clear();
				break;
			}
			newStatesWithEpsilon.clear();  // clear the input set and add all new states to it
			newStatesWithEpsilon.addAll(newStates);
			newStates.clear();
		}

		return processed;  // return the collection of all processed states that can be reached by epsilon transitions
	}

}
