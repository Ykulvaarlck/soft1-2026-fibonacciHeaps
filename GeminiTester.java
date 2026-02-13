import java.util.*;

public class GeminiTester {
    public static void main(String[] args) {
        System.out.println("Running Gemini Tester...\n");

        boolean[][] configs = {
            {true, true},   // Fibonacci Heap
            {true, false},  // Lazy Binomial
            {false, true},  // Binomial with cuts
            {false, false}  // Regular Binomial
        };

        for (boolean[] config : configs) {
            System.out.printf("Testing Config: lazyMelds=%b, lazyDecreaseKeys=%b... ", config[0], config[1]);
            try {
                runStressTest(config[0], config[1]);
                System.out.println("PASSED");
            } catch (Exception e) {
                System.out.println("FAILED: " + e.getMessage());
            }
        }
    }

    private static void runStressTest(boolean lazyMelds, boolean lazyDecreaseKeys) throws Exception {
        Heap heap = new Heap(lazyMelds, lazyDecreaseKeys);
        Queue<Heap.HeapItem> queue = new LinkedList<>();
        Random rand = new Random();

        // 1. Initial Insert: 1000 random values 
        for (int i = 0; i < 1000; i++) {
            int key = rand.nextInt(100000) + 1;
            heap.insert(key, "v" + key);
        }

        // 2. Random Shuffle: 2000 operations
        for (int i = 0; i < 2000; i++) {
            if (rand.nextBoolean() && heap.size() > 0) {
                // Pop min into queue
                Heap.HeapItem minItem = heap.findMin();
                queue.add(new Heap.HeapItem(minItem.key, minItem.info));
                heap.deleteMin();
            } else if (!queue.isEmpty()) {
                // Insert from queue back to heap
                Heap.HeapItem fromQueue = queue.poll();
                heap.insert(fromQueue.key, fromQueue.info);
            }
        }

        // 3. Add remaining queue items back to heap
        while (!queue.isEmpty()) {
            Heap.HeapItem item = queue.poll();
            heap.insert(item.key, item.info);
        }

        // 4. Verification: Exhaustive deleteMin in sorted order 
        int lastKey = -1;
        int initialSize = heap.size();
        while (heap.size() > 0) {
            Heap.HeapItem currentMin = heap.findMin();
            if (currentMin.key < lastKey) {
                throw new Exception("Order violation: Key " + currentMin.key + " appeared after " + lastKey);
            }
            lastKey = currentMin.key;
            heap.deleteMin();
        }

        if (heap.size() != 0) {
            throw new Exception("Heap size mismatch. Expected 0, got " + heap.size());
        }
    }
}
