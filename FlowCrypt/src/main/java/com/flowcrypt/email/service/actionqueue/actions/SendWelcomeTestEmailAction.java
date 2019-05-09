/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions;

import android.content.Context;
import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.ApiHelper;
import com.flowcrypt.email.api.retrofit.ApiService;
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel;
import com.flowcrypt.email.api.retrofit.response.attester.TestWelcomeResponse;
import com.flowcrypt.email.util.exception.ApiException;

import retrofit2.Response;

/**
 * This action describes a task which sends a welcome message to the user
 * using API "https://flowcrypt.com/attester/test/welcome".
 *
 * @author Denis Bondarenko
 * Date: 30.01.2018
 * Time: 18:10
 * E-mail: DenBond7@gmail.com
 */

public class SendWelcomeTestEmailAction extends Action {
  public static final Creator<SendWelcomeTestEmailAction> CREATOR = new Creator<SendWelcomeTestEmailAction>() {
    @Override
    public SendWelcomeTestEmailAction createFromParcel(Parcel source) {
      return new SendWelcomeTestEmailAction(source);
    }

    @Override
    public SendWelcomeTestEmailAction[] newArray(int size) {
      return new SendWelcomeTestEmailAction[size];
    }
  };
  private String publicKey;

  public SendWelcomeTestEmailAction(String email, String publicKey) {
    super(email, ActionType.SEND_WELCOME_TEST_EMAIL);
    this.publicKey = publicKey;
  }

  protected SendWelcomeTestEmailAction(Parcel in) {
    super(in);
    this.publicKey = in.readString();
  }

  @Override
  public void run(Context context) throws Exception {
    ApiService apiService = ApiHelper.getInstance(context).getRetrofit().create(ApiService.class);
    TestWelcomeModel body = new TestWelcomeModel(email, publicKey);
    Response<TestWelcomeResponse> response = apiService.postTestWelcome(body).execute();

    TestWelcomeResponse testWelcomeResponse = response.body();

    if (testWelcomeResponse == null) {
      throw new IllegalArgumentException("The response is null!");
    }

    if (testWelcomeResponse.getApiError() != null) {
      throw new ApiException(testWelcomeResponse.getApiError());
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(this.publicKey);
  }
}
