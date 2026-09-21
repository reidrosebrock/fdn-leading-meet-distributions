import java.util.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class DedekindLexParallel {

    static int n;
    static int size; // number of subsets = 2^n - 2
    static int[] subsets;      // bitmask representation
    static String[] names;
    static BitSet[] comparable; // comparable[i] is a BitSet of indices comparable to i
    static Map<String, AtomicLong> counts = new ConcurrentHashMap<>();
    
    static ExecutorService executor;
    static List<Future<?>> futures = new ArrayList<>();
    static final Object fileLock = new Object();

    public static void main(String[] args) {
        long programStartTime = System.currentTimeMillis();
        
        n = 5; // Change this to 6 for testing, 7 for final run
        generateSubsets();
        precomputeComparable();

        int numThreads = Runtime.getRuntime().availableProcessors();
        executor = Executors.newFixedThreadPool(numThreads);
        System.out.println("Using " + numThreads + " threads");

        File resultsFile = new File("results.txt");

        // Submit all tasks
        for (int firstIdx = 0; firstIdx < size; firstIdx++) {
            final int idx = firstIdx;
            futures.add(executor.submit(() -> processTerm(idx, resultsFile)));
        }

        // Wait for all tasks to complete
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        executor.shutdown();
        printResults();
        
        long programEndTime = System.currentTimeMillis();
        double totalSeconds = (programEndTime - programStartTime) / 1000.0;
        double totalHours = totalSeconds / 3600.0;
        System.out.printf("Total execution time: %.2f seconds (%.2f hours)%n", totalSeconds, totalHours);
    }

    static void processTerm(int firstIdx, File resultsFile) {
        String termName = names[firstIdx];
        long startTime = System.currentTimeMillis();

        // Initial available: all subsets after firstIdx that are incomparable with it
        BitSet available = new BitSet(size);
        for (int j = firstIdx + 1; j < size; j++) {
            if (!comparable[firstIdx].get(j)) {
                available.set(j);
            }
        }

        // Each thread uses its own local counts map
        Map<String, AtomicLong> localCounts = new HashMap<>();
        localCounts.put(termName, new AtomicLong(0));
        
        backtrack(available, termName, firstIdx + 1, localCounts);

        long termCount = localCounts.get(termName).get();
        long endTime = System.currentTimeMillis();
        
        // Merge into global counts
        counts.put(termName, new AtomicLong(termCount));
        
        // Thread-safe file write
        synchronized(fileLock) {
            try (FileWriter fw = new FileWriter(resultsFile, true)) {
                fw.write(termName + ": " + termCount + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        System.out.printf("%s: %d elements, %d ms%n", termName, termCount, (endTime - startTime));
    }

    static void backtrack(BitSet available, String firstName, 
                          int startIdx, Map<String, AtomicLong> localCounts) {
        // Count this antichain
        localCounts.get(firstName).incrementAndGet();

        // Try to add more
        int idx = available.nextSetBit(startIdx);
        while (idx >= 0) {
            BitSet newAvail = (BitSet) available.clone();
            newAvail.andNot(comparable[idx]);
            backtrack(newAvail, firstName, idx + 1, localCounts);
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

    static void printResults() {
        List<String> keys = new ArrayList<>(counts.keySet());
        keys.sort((a, b) -> {
            int sizeA = a.contains("∧") ? a.split("∧").length : 1;
            int sizeB = b.contains("∧") ? b.split("∧").length : 1;
            if (sizeA != sizeB) return Integer.compare(sizeA, sizeB);
            return Integer.compare(bitmaskVal(a), bitmaskVal(b));
        });

        long total = 0;
        for (String key : keys) {
            long cnt = counts.get(key).get();
            System.out.println(key + ": " + cnt);
            total += cnt;
        }
        System.out.println("Total: " + total);
    }

    static int bitmaskVal(String name) {
        int bits = 0;
        if (name.contains("∧")) {
            for (String part : name.split("∧")) {
                bits |= 1 << (part.charAt(0) - 'A');
            }
        } else {
            bits |= 1 << (name.charAt(0) - 'A');
        }
        return bits;
    }
}