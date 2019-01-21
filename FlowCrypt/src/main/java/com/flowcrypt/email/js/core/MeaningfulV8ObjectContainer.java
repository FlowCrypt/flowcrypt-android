/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js.core;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8ResultUndefined;
import com.eclipsesource.v8.V8TypedArray;

/**
 * @author Denis Bondarenko
 * Date: 12/4/18
 * Time: 3:28 PM
 * E-mail: DenBond7@gmail.com
 */
public class MeaningfulV8ObjectContainer {

  protected V8Object v8object;

  public MeaningfulV8ObjectContainer(V8Object o) {
    v8object = o;
  }

  public static V8Array getAttributeAsArray(V8Object obj, String k) {
    try {
      V8Array a = obj.getArray(k);
      return a.isUndefined() ? null : a;
    } catch (V8ResultUndefined e) {
      return null;
    }
  }

  public static V8Object getAttributeAsObject(V8Object obj, String k) {
    try {
      V8Object result = obj.getObject(k);
      return result.isUndefined() ? null : result;
    } catch (V8ResultUndefined e) {
      return null;
    }
  }

  public static Boolean getAttributeAsBoolean(V8Object obj, String k) {
    try {
      return obj.getBoolean(k);
    } catch (V8ResultUndefined e) {
      return null;
    }
  }

  public static Integer getAttributeAsInteger(V8Object obj, String k) {
    try {
      return obj.getInteger(k);
    } catch (V8ResultUndefined e) {
      return null;
    }
  }

  public static String getAttributeAsString(V8Object obj, String k) {
    try {
      return obj.getString(k);
    } catch (V8ResultUndefined e) {
      return null;
    }
  }

  public static byte[] getAttributeAsBytes(V8Object obj, String k) {
    try {
      V8TypedArray typedArray = (V8TypedArray) obj.getObject(k);
      return typedArray.getBytes(0, typedArray.length());
    } catch (V8ResultUndefined e) {
      return null;
    }
  }

  public V8Array getAttributeAsArray(String k) {
    return getAttributeAsArray(v8object, k);
  }

  public V8Object getAttributeAsObject(String name) {
    return getAttributeAsObject(v8object, name);
  }

  public Boolean getAttributeAsBoolean(String name) {
    return getAttributeAsBoolean(v8object, name);
  }

  public Integer getAttributeAsInteger(String name) {
    return getAttributeAsInteger(v8object, name);
  }

  public String getAttributeAsString(String k) {
    return getAttributeAsString(v8object, k);
  }

  public V8Object getV8Object() {
    return v8object;
  }

  public byte[] getAttributeAsBytes(String k) {
    return getAttributeAsBytes(v8object, k);
  }
}
