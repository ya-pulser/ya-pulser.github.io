import java.util.*;

public class A {
  static int[] ends1 = new int[]{100,200,300,400};
  static int[] ends2 = new int[]{90,200,275,310,400};

  int hash(int[] ends, int key) {
     int max = 0;
     for (int i : ends) {
       if ( i > max ) max = i;
     }
     int h = key % max;
     for (int i = 0; i < ends.length; i++) {
       if ( h < ends [i] ) {
         return i;
       }
     }
     return 0;
  }
  void dump(int[] ends, int[] probes){
    Map<Integer,List<Integer>> m = new TreeMap<Integer,List<Integer>>();
    for (int key: probes) {
      int h = hash(ends, key);
      if ( m.get(h) == null ) m.put(h, new ArrayList<Integer>());
      m.get(h).add(key);
    }
    System.out.println(m);
  }

  public static void main(String[] args) throws Exception {
    int[] probes = new int[] { 7, 67, 127, 187, 215, 260, 285, 290, 299, 312, 315, 325, 370, 490, 800, 825 };
    A a = new A();
    a.dump(ends1, probes);
    a.dump(ends2, probes);
  }
}
