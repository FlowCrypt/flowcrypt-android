/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.api;

import com.flowcrypt.email.api.retrofit.ApiName;
import com.flowcrypt.email.api.retrofit.request.BaseRequest;
import com.flowcrypt.email.api.retrofit.request.model.PostHelpFeedbackModel;

/**
 * This class describes the next request:
 * <p>
 * <pre>
 * <code>POST /help/feedback  {
 * "email" (<type 'str'>)  # user email or "unknown@cryptup.org"
 * "message" (<type 'str'>)  # user feedback message text
 * }
 * </code>
 * </pre>
 *
 * @author DenBond7
 *         Date: 30.05.2017
 *         Time: 12:32
 *         E-mail: DenBond7@gmail.com
 */

public class PostHelpFeedbackRequest extends BaseRequest<PostHelpFeedbackModel> {
    public PostHelpFeedbackRequest(PostHelpFeedbackModel postHelpFeedbackModel) {
        super(ApiName.POST_HELP_FEEDBACK, postHelpFeedbackModel);
    }
}
