package com.cryptup.api.retrofit.request;

import com.cryptup.api.retrofit.ApiName;
import com.cryptup.api.retrofit.request.model.PostMessagePrototypeModel;

/**
 * This class describes the request to the API "https://api.cryptup.io/message/prototype"
 *
 * @author DenBond7
 *         Date: 24.04.2017
 *         Time: 13:54
 *         E-mail: DenBond7@gmail.com
 */

public class MessagePrototypeRequest extends BaseRequest {
    private PostMessagePrototypeModel postMessagePrototypeModel;

    public MessagePrototypeRequest(PostMessagePrototypeModel postMessagePrototypeModel) {
        super(ApiName.POST_MESSAGE_PROTOTYPE);
        this.postMessagePrototypeModel = postMessagePrototypeModel;
    }

    public PostMessagePrototypeModel getPostMessagePrototypeModel() {
        return postMessagePrototypeModel;
    }
}
