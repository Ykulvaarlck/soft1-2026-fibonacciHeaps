/**
 * Heap
 *
 * An implementation of Fibonacci heap over positive integers
 * with the possibility of not performing lazy melds and
 * the possibility of not performing lazy decrease keys.
 *
 */
public class Heap {
    public final boolean lazyMelds;
    public final boolean lazyDecreaseKeys;
    public HeapItem min;

    // sentinel node, contains all roots as children
    // i use HeapNode for this mostly to reuse the `extend` and `append` logic.
    // melding heaps can make some roots not have a correct `parent`, but we do not use it for roots
    public HeapNode roots;

    public int rootCount = 0;
    public int itemCount = 0;
    public int markedCount = 0;
    public int linkCount = 0;
    public int cutCount = 0;
    public int heapifyCount = 0;

    /**
     * Constructor to initialize an empty heap.
     */
    public Heap(boolean lazyMelds, boolean lazyDecreaseKeys) {
        this.lazyMelds = lazyMelds;
        this.lazyDecreaseKeys = lazyDecreaseKeys;
        // student code can be added here

        this.min = null;
        this.roots = new HeapNode();
    }

    /**
     * Insert (key,info) into the heap and return the newly generated HeapNode.
     * pre: key > 0
     * complexity: O(1) if lazyMelds==true, otherwise O(log n).
     */
    public HeapItem insert(int key, String info) {
        var item = new HeapItem(key, info);
        var node = new HeapNode(item);
        this.itemCount++;

        if (this.min == null || this.min.key > item.key) {
            this.min = item;
        }

        this.roots.append(node);
        this.rootCount++;
        if (!this.lazyMelds) {
            this.successiveLink();
        }
        return item;
    }

    /**
     * Return the minimal HeapNode, null if empty.
     */
    public HeapItem findMin() {
        return this.min;
    }


    /**
     * Delete the minimal item.
     * complexity: O(log n) because of the successive linking.
     */
    public void deleteMin() {
        var minNode = this.min.node;
        this.itemCount--;
        // this.markedCount does not change because the minimum must be a root, and roots are not marked
        minNode.cut(); // this does not count towards the total cuts
        this.rootCount--;
        this.roots.extend(minNode); // add all children of the minimum to the root list
        this.rootCount += minNode.rank;
        this.successiveLink(); // NOTE: this will update this.min and this.rootCount
    }

    /**
     * Decrease the key of x by diff and fix the heap.
     * pre: 0<=diff<=x.key
     * complexity:
     * depending on `lazyDecreaseKeys`, either heapifyUp or cascading cuts.
     * in either case the depths of the trees are log n, so either of them take O(log n).
     * when lazyMelds==false and lazyDecreaseKeys==true, the series of successive links are still O(log n) together.
     */
    public void decreaseKey(HeapItem x, int diff) {
        x.key -= diff;
        if (x.key < this.min.key) {
            this.min = x;
        }
        if (x.node.isRoot() || x.key >= x.node.parent.item.key) {
            // heap invariant was not broken, no fix needed
            return;
        }
        var curr = x.node;
        if (this.lazyDecreaseKeys) {
            // personal note: cascading cut via melds is a bad idea
            // mutating the data structure in two different ways simultaneously is a great way to get awful bugs
            // it makes more sense to first do all the cuts, then do one successive linking at the end

            while (!curr.isRoot()) {
                var parent = curr.parent;
                // remove current from its tree and meld to the heap
                curr.cut();
                this.cutCount++;
                if (curr.marked) {
                    this.markedCount--;
                    curr.marked = false;
                }
                this.roots.append(curr);
                this.rootCount++;
                if (!this.lazyMelds) {
                    this.successiveLink();
                }
                if (parent.marked) {
                    // cascade the cut
                    curr = parent;
                } else if (!parent.isRoot()) {
                    // mark parent and stop cascading cut
                    this.markedCount++;
                    parent.marked = true;
                    break;
                }
            }

        } else {
            while (!curr.parent.isRoot() && curr.item.key < curr.parent.item.key) {
                var high = curr.parent.item;
                var low = curr.item;
                curr.parent.setItem(low);
                curr.setItem(high);
                this.heapifyCount++;
                curr = curr.parent;
            }
        }
    }

    /**
     * Delete the x from the heap.
     * delete by decreasing x to be the new minimum then deleteMin()ing.
     */
    public void delete(HeapItem x) {
        int D = x.key - this.min.key;
        this.decreaseKey(x, D + 1); // x's key is now guaranteed to be the minimum
        this.deleteMin();
    }

    /**
     * perform successive linking on the heap as it currently is,
     * to be called after deleteMin or when lazy melds are disabled.
     * complexity: linear in the number of roots, so O(log n).
     */
    private void successiveLink() {
        if (this.roots.child == null) {
            // no nodes in the heap so there is nothing to do
            this.min = null;
            return;
        }

        // move all of the roots into a new array to simplify/ignore handling of next/prev pointers
        // rootCount may not be accurate so we have to calculate it
        var start = this.roots.child;
        var curr = start.next;
        int rootCount = 1;
        while (curr != start) {
            curr = curr.next;
            rootCount++;
        }
        curr = this.roots.child;
        HeapNode[] roots = new HeapNode[rootCount];
        for (int i = 0; i < rootCount; i++) {
            roots[i] = curr;
            curr.parent = null;
            curr = curr.next;
        }

        // successively link all roots
        var bins = new HeapNode[64]; // you are not going to surpass rank 64 with any reasonable amount of memory
        for (var root : roots) {
            curr = root;
            // repeatedly link until no other root of same rank exists
            while (bins[curr.rank] != null) {
                var other = bins[curr.rank];
                bins[curr.rank] = null;
                this.linkCount++;
                if (other.item.key < curr.item.key) {
                    other.append(curr);
                    curr = other;
                } else {
                    curr.append(other);
                }
            }
            bins[curr.rank] = curr;
        }
        this.min = null;
        this.roots.child = null; // "clear" the root list
        this.roots.rank = 0;
        // put all new roots into the heap
        for (var root : bins) {
            if (root != null) {
                this.roots.append(root);
                if (this.min == null || this.min.key > root.item.key) {
                    // update the min pointer
                    this.min = root.item;
                }
            }
        }
        this.rootCount = this.roots.rank;
    }

    /**
     * Meld the heap with heap2
     * <p>
     * pre: heap2.lazyMelds = this.lazyMelds AND heap2.lazyDecreaseKeys = this.lazyDecreaseKeys
     */
    public void meld(Heap heap2) {
        // add histories
        this.rootCount += heap2.rootCount;
        this.itemCount += heap2.itemCount;
        this.markedCount += heap2.markedCount;
        this.cutCount += heap2.cutCount;
        this.linkCount += heap2.linkCount;
        this.heapifyCount += heap2.heapifyCount;

        // only necessary if lazyMelds is true
        if (this.min == null || (
                heap2.min != null && this.min.key > heap2.min.key)) {
            this.min = heap2.min;
        }

        this.roots.extend(heap2.roots);

        if (!this.lazyMelds) {
            this.successiveLink();
        }
    }


    /**
     * Return the number of elements in the heap
     */
    public int size() {
        return this.itemCount;
    }


    /**
     * Return the number of trees in the heap.
     */
    public int numTrees() {
        return this.rootCount;
    }


    /**
     * Return the number of marked nodes in the heap.
     */
    public int numMarkedNodes() {
        return this.markedCount;
    }


    /**
     * Return the total number of links.
     */
    public int totalLinks() {
        return this.linkCount;
    }


    /**
     * Return the total number of cuts.
     */
    public int totalCuts() {
        return this.cutCount;
    }


    /**
     * Return the total heapify costs.
     */
    public int totalHeapifyCosts() {
        return this.heapifyCount;
    }


    /**
     * Class implementing a node in a Heap.
     */
    public static class HeapNode {
        public HeapItem item;
        public HeapNode child;
        public HeapNode next;
        public HeapNode prev;
        public HeapNode parent;
        public int rank = 0;
        public boolean marked = false;

        public HeapNode() {
            this.item = null;
        }

        public HeapNode(HeapItem item) {
            this.setItem(item);
        }

        public void setItem(HeapItem item) {
            this.item = item;
            item.node = this;
        }

        /**
         * whether the current node is one of the roots in the heap
         */
        public boolean isRoot() {
            return this.parent != null && this.parent.item == null;
        }

        /**
         * Add all children of `other` to `this`. `other` loses its children.
         * NOTE: this does not update the `parent`s of any node in `other`
         * NOTE: only to be used when `other` is another root containing all roots,
         *   or right before a successive linking (which updates all parent pointers anyway)
         */
        public void extend(HeapNode other) {
            this.rank += other.rank;
            if (this.child == null) {
                // inherit `other`'s childlist
                this.child = other.child;
            } else if (other.child != null) {
                // splice other's children after this's children
                var A = this.child.prev;
                var B = other.child;
                var C = other.child.prev;
                var D = this.child;
                A.next = B;
                B.prev = A;
                C.next = D;
                D.prev = C;
            }
            other.rank = 0;
            other.child = null;
        }

        /**
         * append `other` to the child list.
         * NOTE: this overwrites `other`'s parent/next/prev
         */
        public void append(HeapNode other) {
            this.rank += 1;
            if (this.child == null) {
                // set `other` as the sole child
                this.child = other;
                other.next = other;
                other.prev = other;
            } else {
                // append `other` to the end of the child list
                var A = this.child.prev;
                var B = other;
                var C = this.child;
                A.next = B;
                B.prev = A;
                B.next = C;
                C.prev = B;
            }
            other.parent = this;
        }

        /**
         * cut this node and its subtree from its parent.
         * NOTE: this does not modify or use the `marked` field
         * NOTE: this clears `this`'s `parent`.
         */
        public void cut() {
            if (this.parent != null) {
                // remove `this` from its parent
                this.parent.rank -= 1;
                if (this == this.parent.child) {
                    // handle the case that `this` is the head of the child list
                    if (this.next == this) {
                        // single child, parent now has no children
                        this.parent.child = null;
                    } else {
                        // replace parent's child with `this`'s next sibling
                        this.parent.child = this.next;
                    }
                }
            }
            // splice out `this` from its siblings
            var A = this.prev;
            var B = this.next;
            A.next = B;
            B.prev = A;
            this.parent = null;
        }
    }

    /**
     * Class implementing an item in a Heap.
     */
    public static class HeapItem {
        public HeapNode node;
        public int key;
        public String info;

        public HeapItem(int key, String info) {
            this.key = key;
            this.info = info;
        }
    }
}
