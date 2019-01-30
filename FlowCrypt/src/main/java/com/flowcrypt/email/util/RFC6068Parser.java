/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.net.Uri;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.model.ExtraActionInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class defines the parser of 'mailto' URIs.
 * It depends on the <a href="https://tools.ietf.org/html/rfc6068">document</a> which defines the format of Uniform
 * Resource Identifiers (URIs) to identify resources that are reached using Internet mail.
 * <p>
 * See details here https://github.com/k9mail/k-9/blob/master/k9mail/src/main/java/com/fsck/k9/helper/MailTo.java
 *
 * @author Denis Bondarenko
 * Date: 13.03.2018
 * Time: 14:41
 * E-mail: DenBond7@gmail.com
 */

public class RFC6068Parser {
  private static final String MAILTO_SCHEME = "mailto";
  private static final String TO = "to";
  private static final String BODY = "body";
  private static final String CC = "cc";
  private static final String BCC = "bcc";
  private static final String SUBJECT = "subject";

  public static boolean isMailTo(Uri uri) {
    return uri != null && MAILTO_SCHEME.equals(uri.getScheme());
  }

  public static ExtraActionInfo parse(Uri uri) throws NullPointerException, IllegalArgumentException {
    if (uri == null) {
      throw new NullPointerException("Argument 'uri' must not be null");
    }

    if (!isMailTo(uri)) {
      throw new IllegalArgumentException("Not a mailto scheme");
    }

    String schemaSpecific = uri.getSchemeSpecificPart();
    int end = schemaSpecific.indexOf('?');
    if (end == -1) {
      end = schemaSpecific.length();
    }

    Uri newUri = Uri.parse("foo://bar?" + uri.getEncodedQuery());
    CaseInsensitiveParamWrapper params = new CaseInsensitiveParamWrapper(newUri);

    // Extract the recipient's email address from the mailto URI if there's one.
    String recipient = Uri.decode(schemaSpecific.substring(0, end));

    ArrayList<String> toList = params.getQueryParameters(TO);
    if (recipient.length() != 0) {
      toList.add(0, recipient);
    }

    toList = checkToList(toList);

    ArrayList<String> ccList = params.getQueryParameters(CC);
    ArrayList<String> bccList = params.getQueryParameters(BCC);

    String subject = getFirstParameterValue(params, SUBJECT);
    String body = getFirstParameterValue(params, BODY);

    return new ExtraActionInfo.Builder()
        .setToAddresses(toList)
        .setCcAddresses(ccList)
        .setBccAddresses(bccList)
        .setSubject(subject)
        .setBody(body)
        .create();
  }

  private static ArrayList<String> checkToList(ArrayList<String> toList) {
    ArrayList<String> newToList = new ArrayList<>();
    if (!toList.isEmpty()) {
      for (String section : toList) {
        if (!TextUtils.isEmpty(section)) {
          if (section.indexOf(',') != -1) {
            String[] arraysRecipients = section.split(",");
            newToList.addAll(new ArrayList<>(Arrays.asList(arraysRecipients)));
          } else {
            newToList.add(section);
          }
        }
      }
    }
    return newToList;
  }

  private static String getFirstParameterValue(CaseInsensitiveParamWrapper params, String paramName) {
    List<String> paramValues = params.getQueryParameters(paramName);
    return (paramValues.isEmpty()) ? null : paramValues.get(0);
  }

  private static class CaseInsensitiveParamWrapper {
    private final Uri uri;

    CaseInsensitiveParamWrapper(Uri uri) {
      this.uri = uri;
    }

    ArrayList<String> getQueryParameters(String key) {
      ArrayList<String> params = new ArrayList<>();
      for (String paramName : uri.getQueryParameterNames()) {
        if (paramName.equalsIgnoreCase(key)) {
          params.addAll(uri.getQueryParameters(paramName));
        }
      }

      return params;
    }
  }
}
