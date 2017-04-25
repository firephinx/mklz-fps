package sort_java;
public class Item implements Comparable<Item> {
	private double hash;
	private double data;	
	public Item(int num) {
		this.hash = generateReal(num);
		this.data = num;
	}
	public int hash(int i)
    {
      long v = ((long) i) * 3935559000370003845L + 2691343689449507681L;
      v = v ^ (v >> 21);
      v = v ^ (v << 37);
      v = v ^ (v >> 4);
      v = v * 4768777513237032717L;
      v = v ^ (v << 20);
      v = v ^ (v >> 41);
      v = v ^ (v <<  5);
      return (int) (v & ((((long) 1) << 31) - 1));
    }

    // generates a pseudorandom double precision real from an integer
    public double generateReal(int i) {
        return (double) hash(i);
    }
	public double gethash() {
		return hash;
	}
	public double getData() {
		return data;
	}
	@Override
	public int compareTo(Item item) {
		return Double.valueOf(hash).compareTo(item.gethash());
	}
	public static Item[] getItems(int size) {
		Item[] Items = new Item[size];
		for (int i = 0; i < size;i += 1)
	    {
	        Items[i] = new Item (i);
	    }
		return Items;
	}
} 