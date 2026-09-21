import java.util.*;
import java.io.*;
import java.util.concurrent.*;

public class FDLEnumerator {

    static int n;
    static int size;
    static int[] subsets;
    static String[] names;
    static BitSet[] comparable;
    
    static ExecutorService executor;
    static List<Future<List<String>>> futures = new ArrayList<>();
    public static void main(String[] args) {
        n = 3; // 4 or 5 only!
        generateSubsets();
        precomputeComparable();

        int numThreads = Runtime.getRuntime().availableProcessors();
        executor = Executors.newFixedThreadPool(numThreads);
        System.out.println("Using " + numThreads + " threads");

        // Submit all tasks, collecting results in order
        List<Future<List<String>>> futures = new ArrayList<>();
        for (int firstIdx = 0; firstIdx < size; firstIdx++) {
            final int idx = firstIdx;
            futures.add(executor.submit(() -> collectTerm(idx)));
        }

        // Gather results in original order
        Map<Integer, List<String>> orderedResults = new TreeMap<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                orderedResults.put(i, futures.get(i).get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        executor.shutdown();

        // Write to file in lex order
        long total = 0;
        try (PrintWriter out = new PrintWriter("fdl_elements_n" + n + ".txt")) {
            for (int i = 0; i < size; i++) {
                List<String> elems = orderedResults.get(i);
                out.println("\n=== " + names[i] + " (" + elems.size() + " elements) ===\n");
                for (String elem : elems) {
                    out.println(elem);
                }
                total += elems.size();
            }
            out.println("\nTOTAL ELEMENTS: " + total);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("Done. Total elements: " + total);
    }

    static List<String> collectTerm(int firstIdx) {
        String termName = names[firstIdx];
        System.out.println(Thread.currentThread().getName() + " collecting " + termName);

        BitSet available = new BitSet(size);
        for (int j = firstIdx + 1; j < size; j++) {
            if (!comparable[firstIdx].get(j)) {
                available.set(j);
            }
        }

        List<String> elements = new ArrayList<>();
        collectElements(available, termName, firstIdx + 1, new ArrayList<>(), elements);
        return elements;
    }

    static void collectElements(BitSet available, String currentElem, 
                                int startIdx, List<Integer> chosen, List<String> out) {
        // Record current element
        if (!chosen.isEmpty()) {
            StringBuilder sb = new StringBuilder(currentElem);
            for (int idx : chosen) {
                sb.append(" ∨ ").append(names[idx]);
            }
            out.add(sb.toString());
        } else {
            out.add(currentElem); // Just the first term alone
        }

        // Try to add more
        int idx = available.nextSetBit(startIdx);
        while (idx >= 0) {
            chosen.add(idx);
            BitSet newAvail = (BitSet) available.clone();
            newAvail.andNot(comparable[idx]);
            collectElements(newAvail, currentElem, idx + 1, chosen, out);
            chosen.remove(chosen.size() - 1);
            idx = available.nextSetBit(idx + 1);
        }
    }

    static void generateSubsets() {
        List<Integer> subsetList = new ArrayList<>();
        List<String> nameList = new ArrayList<>();

        for (int sz = 1; sz < n; sz++) {
            for (int bits = 1; bits < (1 << n) - 1; bits++) {
                if (Integer.bitCount(bits) == sz) {
                    subsetList.add(bits);
                    if (sz == 1) {
                        int idx = Integer.numberOfTrailingZeros(bits);
                        nameList.add(String.valueOf((char) ('A' + idx)));
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < n; i++) {
                            if ((bits & (1 << i)) != 0) {
                                if (sb.length() > 0) sb.append('∧');
                                sb.append((char) ('A' + i));
                            }
                        }
                        nameList.add(sb.toString());
                    }
                }
            }
        }

        size = subsetList.size();
        subsets = new int[size];
        names = new String[size];
        for (int i = 0; i < size; i++) {
            subsets[i] = subsetList.get(i);
            names[i] = nameList.get(i);
        }
    }

    static void precomputeComparable() {
        comparable = new BitSet[size];
        for (int i = 0; i < size; i++) {
            BitSet bs = new BitSet(size);
            for (int j = 0; j < size; j++) {
                if (i == j || (subsets[i] & ~subsets[j]) == 0 || (subsets[j] & ~subsets[i]) == 0) {
                    bs.set(j);
                }
            }
            comparable[i] = bs;
        }
    }
}