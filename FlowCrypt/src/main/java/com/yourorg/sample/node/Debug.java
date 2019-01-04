package com.yourorg.sample.node;

import java.util.Arrays;

public class Debug {

  private static final int maxLength = 20;

  private static void print(String name, byte[] bytes) {
    int unsignedBytes[] = new int[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      unsignedBytes[i] = bytes[i] & 0xFF;
    }
    System.out.println("Debug.printChunk[" + name + "]: " + Arrays.toString(unsignedBytes));
  }

  public static void printChunk(String name, String string) {
    print(name, string.substring(0, Math.min(string.length(), maxLength)).getBytes());
  }

  public static void printChunk(String name, byte[] data) {
    print(name, Arrays.copyOfRange(data, 0, Math.min(data.length, maxLength)));
  }

}
