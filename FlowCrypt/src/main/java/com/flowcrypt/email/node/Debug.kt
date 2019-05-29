/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.node;

import java.util.Arrays;

public class Debug {

  private static final int MAX_LENGTH = 20;

  public static void printChunk(String name, String string) {
    print(name, string.substring(0, Math.min(string.length(), MAX_LENGTH)).getBytes());
  }

  public static void printChunk(String name, byte[] data) {
    print(name, Arrays.copyOfRange(data, 0, Math.min(data.length, MAX_LENGTH)));
  }

  private static void print(String name, byte[] bytes) {
    int[] unsignedBytes = new int[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      unsignedBytes[i] = bytes[i] & 0xFF;
    }
    System.out.println("Debug.printChunk[" + name + "]: " + Arrays.toString(unsignedBytes));
  }

}
