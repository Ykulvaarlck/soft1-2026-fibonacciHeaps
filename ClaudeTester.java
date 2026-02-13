import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClaudeTester {
    public static void main(String[] args) {
        System.out.println("=== Claude Heap Test ===\n");

        boolean allPassed = true;

        // Test all 4 configurations
        allPassed &= testConfiguration(false, false, "Binomial Heap (non-lazy melds, non-lazy decreaseKey)");
        allPassed &= testConfiguration(true, false, "Lazy Binomial Heap (lazy melds, non-lazy decreaseKey)");
        allPassed &= testConfiguration(true, true, "Fibonacci Heap (lazy melds, lazy decreaseKey)");
        allPassed &= testConfiguration(false, true, "Binomial with Cuts (non-lazy melds, lazy decreaseKey)");

        System.out.println("\n=== Final Result ===");
        if (allPassed) {
            System.out.println("✓ ALL CONFIGURATIONS PASSED!");
        } else {
            System.out.println("✗ SOME CONFIGURATIONS FAILED!");
        }
    }

    private static boolean testConfiguration(boolean lazyMelds, boolean lazyDecreaseKeys, String configName) {
        System.out.println("Testing: " + configName);
        System.out.println("  lazyMelds=" + lazyMelds + ", lazyDecreaseKeys=" + lazyDecreaseKeys);

        try {
            Random rand = new Random(42); // Fixed seed for reproducibility
            Heap heap = new Heap(lazyMelds, lazyDecreaseKeys);
            List<Integer> queue = new ArrayList<>();

            // Phase 1: Insert 1000 random values
            System.out.print("  Phase 1: Inserting 1000 random values... ");
            for (int i = 0; i < 1000; i++) {
                int value = rand.nextInt(10000);
                heap.insert(value, "Item" + i);
            }
            System.out.println("✓");

            // Phase 2: Randomly pop min or insert from queue (500 operations)
            System.out.print("  Phase 2: Random pop/insert operations (500 ops)... ");
            for (int i = 0; i < 500; i++) {
                if (heap.size() > 0 && (queue.isEmpty() || rand.nextBoolean())) {
                    // Pop min from heap to queue
                    Heap.HeapItem min = heap.findMin();
                    if (min == null) {
                        System.out.println("✗");
                        System.out.println("    ERROR: findMin returned null but size > 0");
                        return false;
                    }
                    queue.add(min.key);
                    heap.deleteMin();
                } else if (!queue.isEmpty()) {
                    // Insert from queue back to heap
                    int value = queue.remove(rand.nextInt(queue.size()));
                    heap.insert(value, "Reinserted");
                }
            }
            System.out.println("✓");

            // Phase 3: Add everything from queue back to heap
            System.out.print("  Phase 3: Adding queue back to heap (" + queue.size() + " items)... ");
            for (int value : queue) {
                heap.insert(value, "FromQueue");
            }
            queue.clear();
            System.out.println("✓");

            // Phase 4: Extract all elements and verify sorted order
            System.out.print("  Phase 4: Extracting all elements (" + heap.size() + " items)... ");
            int heapSize = heap.size();
            List<Integer> extractedValues = new ArrayList<>();

            int prevMin = Integer.MIN_VALUE;
            while (heap.size() > 0) {
                Heap.HeapItem min = heap.findMin();
                if (min == null) {
                    System.out.println("✗");
                    System.out.println("    ERROR: findMin returned null but size = " + heap.size());
                    return false;
                }

                int currentMin = min.key;
                extractedValues.add(currentMin);

                // Verify heap property: current min >= previous min
                if (currentMin < prevMin) {
                    System.out.println("✗");
                    System.out.println("    ERROR: Heap property violated!");
                    System.out.println("    Previous min: " + prevMin + ", Current min: " + currentMin);
                    return false;
                }

                prevMin = currentMin;
                heap.deleteMin();
            }

            // Verify we extracted the expected number of elements
            if (extractedValues.size() != heapSize) {
                System.out.println("✗");
                System.out.println("    ERROR: Expected " + heapSize + " elements, got " + extractedValues.size());
                return false;
            }

            System.out.println("✓");

            // Verify heap is empty
            if (heap.size() != 0 || heap.findMin() != null) {
                System.out.println("  ✗ ERROR: Heap not empty after extracting all elements");
                return false;
            }

            System.out.println("  ✓ Configuration PASSED\n");
            return true;

        } catch (Exception e) {
            System.out.println("✗");
            System.out.println("  ✗ Configuration FAILED with exception:");
            System.out.println("    " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace(System.out);
            System.out.println();
            return false;
        }
    }
}
