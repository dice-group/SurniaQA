package de.upb.ds.surnia.util;

public class SurniaUtil {
  public static double stringSimilarity(String s1, String s2) {
    double len = Math.max(s1.length(), s2.length());
    int[] v0 = new int[s2.length() + 1];
    int[] v1 = new int[s2.length() + 1];
    int[] vtemp;
    for (int i = 0; i < v0.length; i++) {
      v0[i] = i;
    }
    for (int i = 0; i < s1.length(); i++) {
      v1[0] = i + 1;
      for (int j = 0; j < s2.length(); j++) {
        int cost = 1;
        if (s1.charAt(i) == s2.charAt(j)) {
          cost = 0;
        }
        v1[j + 1] = Math.min(
          v1[j] + 1,
          Math.min(v0[j + 1] + 1, v0[j] + cost));
      }
      vtemp = v0;
      v0 = v1;
      v1 = vtemp;
    }
    return 1.0d - (v0[s2.length()] / len);
  }

  public static int levenshtein(String a, String b) {
    a = a.toLowerCase();
    b = b.toLowerCase();
    int[] costs = new int[b.length() + 1];
    for (int j = 0; j < costs.length; j++)
      costs[j] = j;
    for (int i = 1; i <= a.length(); i++) {
      costs[0] = i;
      int nw = i - 1;
      for (int j = 1; j <= b.length(); j++) {
        int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
        nw = costs[j];
        costs[j] = cj;
      }
    }
    return costs[b.length()];
  }
}
