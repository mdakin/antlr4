package org.antlr.v4.test.runtime.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import org.antlr.v4.runtime.misc.DFAStateEdgeCache;
import org.junit.Assert;
import org.junit.Test;

public class TestDFAEdgeCache {

  @Test
  public void initializesCorrectly() {
    // Check first 1K initial sizes.
    for (int i=1; i < 1000; i++ ) {
			DFAStateEdgeCache c = new DFAStateEdgeCache(i);
      checkSize(c, 0);
    }
  }

  @Test
  public void failsOnInvalidSizes() {
    try {
      DFAStateEdgeCache c;
      c = new DFAStateEdgeCache(0);
      c = new DFAStateEdgeCache(-1);
      c = new DFAStateEdgeCache(Integer.MAX_VALUE);
      c = new DFAStateEdgeCache(Integer.MIN_VALUE);
      Assert.fail("Illegal size should have thrown an exception.");
    } catch (RuntimeException e) {
      // Nothing to do
    }
  }

  // @Test
  // public void expandsCorrectly() {
   //  // Create maps with different sizes and add size * 10 elements to each.
   //  for (int i=1; i < 100; i++ ) {
   //    SymbolMap<String> im = new SymbolMap<>(i);
   //    // Insert i * 10 elements to each and confirm sizes
   //    int elements = i * 10;
   //    for (int j=0; j< elements; j++) {
   //      im.put(j, "" + j);
   //    }
	// 		for (int j=0; j< elements; j++) {
	// 			Assert.assertEquals(im.get(j), "" + j);
	// 		}
	// 		checkSize(im, elements);
   //  }
  // }
  //
  // @Test
  // public void putAddsAndUpdatesElementsCorrectly() {
   //  int span = 100;
   //  for (int i=0; i<span; i++) {
   //    SymbolMap<String> im = new SymbolMap<>();
   //    checkSpanInsertions(im, -i, i);
   //  }
   //  // Do the same, this time overwrite values as well
   //  SymbolMap<String> im = new SymbolMap<>();
   //  for (int i=0; i<span; i++) {
   //    checkSpanInsertions(im, -i, i);
   //    checkSpanInsertions(im, -i, i);
   //    checkSpanInsertions(im, -i, i);
   //  }
  // }
  //
  // @Test
  // public void survivesSimpleFuzzing() {
	// 	List<int[]> fuzzLists = createFuzzingLists();
   //  for (int[] arr : fuzzLists) {
   //    SymbolMap<String> im = new SymbolMap<>();
   //    for (int i=0; i<arr.length; i++) {
   //      im.put(arr[i], "" + arr[i]);
   //      assertEquals(im.get(arr[i]), "" + arr[i]);
   //    }
   //  }
  //
   //  SymbolMap<String> im = new SymbolMap<>();
   //  for (int[] arr : fuzzLists) {
   //    for (int i=0; i<arr.length; i++) {
   //      im.put(arr[i], "" + arr[i]);
   //      assertEquals(im.get(arr[i]), "" + arr[i]);
   //    }
   //  }
  // }
  //
  // private List<int[]> createFuzzingLists() {
	// 	List<int[]> fuzzLists = new ArrayList<>(5000);
   //  int maxListSize = 300;
   //  Random r = new Random(0xBEEFCAFE);
   //  // Random sized lists with values in [0..n] shuffled.
   //  for (int i = 0; i < 1000; i++) {
   //    int[] arr = new int[r.nextInt(maxListSize) + 1];
   //    for (int j = 0; j < arr.length; j++) {
   //      arr[j] = j;
   //    }
   //    shuffle(arr);
   //    fuzzLists.add(arr);
   //  }
   //  // Random sized lists with values in [-n..n] shuffled.
   //  for (int i = 0; i < 1000; i++) {
   //    int size = r.nextInt(maxListSize) + 1;
   //    int[] arr = new int[size * 2];
   //    int idx = 0;
   //    for (int j = 0; j< arr.length; j++) {
   //      arr[idx++] = j - size;
   //    }
   //    shuffle(arr);
   //    fuzzLists.add(arr);
   //  }
   //  // Random sized lists in [-m,m] shuffled. Possible duplicates.
   //  int m = 1 << 10;
   //  for (int i = 0; i < 2000; i++) {
   //    int size = r.nextInt(maxListSize) + 1;
   //    int[] arr = new int[size];
   //    for (int j = 0; j < arr.length; j++) {
   //      arr[j] = r.nextInt(2 * m) - m;
   //    }
   //    shuffle(arr);
   //    fuzzLists.add(arr);
   //  }
   //  return fuzzLists;
  // }
  //
  // private void checkSpanInsertions(SymbolMap<String> im, int start, int end) {
   //  insertSpan(im, start, end);
   //  // Expected size.
   //  int size = Math.abs(start) + Math.abs(end) + 1;
   //  assertEquals(size, im.size());
   //  checkSpan(im, start, end);
  // }
  //
  // private void insertSpan(SymbolMap<String> im, int start, int end) {
   //  int spanStart = Math.min(start, end);
   //  int spanEnd = Math.max(start, end);
   //  for (int i = spanStart; i <= spanEnd; i++) {
   //    im.put(i, "" + i);
   //  }
  // }
  //
  // private void checkSpan(SymbolMap<String> im, int start, int end) {
   //  int spanStart = Math.min(start, end);
   //  int spanEnd = Math.max(start, end);
   //  for (int i = spanStart; i <= spanEnd; i++) {
   //    assertEquals(im.get(i), "" + i);
   //  }
   //  // Check outside of span values do not exist in the map
   //  for (int i = spanStart - 1, idx=0; idx < 100; i--, idx++) {
   //    Assert.assertNull(im.get(i));
   //  }
   //  for (int i = spanEnd + 1, idx=0; idx < 100; i++, idx++) {
   //    Assert.assertNull(im.get(i));
   //  }
  // }
  //

	private void checkSize(DFAStateEdgeCache m, int size) {
		assertEquals(size, m.size());
		assertTrue(m.capacity() > m.size());
		// Check capacity is 2^n
		assertTrue((m.capacity() & (m.capacity() - 1)) == 0);
	}

	// Fisher yates shuffle
  private static void shuffle(int[] array) {
    int index, temp;
    Random random = new Random(0xCAFEBABE);
    for (int i = array.length - 1; i > 0; i--)
    {
      index = random.nextInt(i + 1);
      temp = array[index];
      array[index] = array[i];
      array[i] = temp;
    }
  }

}
