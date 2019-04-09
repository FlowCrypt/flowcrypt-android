/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel;

import android.app.Application;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository;
import com.flowcrypt.email.api.retrofit.request.node.ZxcvbnStrengthBarRequest;
import com.nulabinc.zxcvbn.Zxcvbn;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

/**
 * This {@link ViewModel} implementation can be used to check the passphrase strength
 *
 * @author Denis Bondarenko
 * Date: 4/2/19
 * Time: 11:11 AM
 * E-mail: DenBond7@gmail.com
 */
public class PasswordStrengthViewModel extends BaseNodeApiViewModel {
  private Zxcvbn zxcvbn;
  private PgpApiRepository apiRepository;

  public PasswordStrengthViewModel(@NonNull Application application) {
    super(application);
    this.zxcvbn = new Zxcvbn();
  }

  public void init(PgpApiRepository apiRepository) {
    this.apiRepository = apiRepository;
  }

  public void check(final String passphrase) {
    double measure = zxcvbn.measure(passphrase, Arrays.asList(Constants.PASSWORD_WEAK_WORDS)).getGuesses();

    apiRepository.checkPassphraseStrength(R.id.live_data_id_check_passphrase_strength, responsesLiveData,
        new ZxcvbnStrengthBarRequest(measure));
  }
}
