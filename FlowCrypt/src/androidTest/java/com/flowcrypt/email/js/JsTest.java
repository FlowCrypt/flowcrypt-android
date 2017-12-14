/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.flowcrypt.email.security.SecurityStorageConnector;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * @author Denis Bondarenko
 *         Date: 13.12.2017
 *         Time: 15:01
 *         E-mail: DenBond7@gmail.com
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JsTest {
    private static final String TAG = JsTest.class.getSimpleName();
    private Js js;
    private StorageConnectorInterface storageConnectorInterface;

    @Before
    public void setUp() throws Exception {
        long startInitTime = System.currentTimeMillis();
        Log.d(TAG, "Js init time = " + (System.currentTimeMillis() - startInitTime));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void method_1_prepareStorageConnectorInterface() throws Exception {
        this.storageConnectorInterface = new SecurityStorageConnector(InstrumentationRegistry.getTargetContext());
    }

    @Test
    public void method_2_prepareJs() throws Exception {
        this.js = new Js(InstrumentationRegistry.getTargetContext(), storageConnectorInterface);
    }
}