package com.yanivian.riddlr.frontend.endpoint;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.inject.Inject;
import com.yanivian.riddlr.backend.operation.GetRiddlesForTopicOp;
import com.yanivian.riddlr.backend.operation.proto.GetRiddlesForTopicRequest;
import com.yanivian.riddlr.backend.operation.proto.RiddlesForTopic;
import com.yanivian.riddlr.service.Endpoint;
import com.yanivian.riddlr.service.Endpoint.AllowGet;

@WebServlet(name = "GetRiddlesForTopicEndpoint", urlPatterns = {"/riddles/getForTopic"})
@AllowGet
public final class GetRiddlesForTopicEndpoint extends Endpoint {
  @Inject GetRiddlesForTopicOp getRiddlesForTopicOp;

  @Override
  protected void process(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    GetRiddlesForTopicRequest opReq = GetRiddlesForTopicRequest.newBuilder()
                                          .setUID(getRequiredParameter(req, Params.UID))
                                          .setTopic(getRequiredParameter(req, Params.Topic))
                                          .build();
    RiddlesForTopic result = getRiddlesForTopicOp.apply(opReq);
    writeJsonResponse(resp, result);
  }

  static final class Params {
    static final String Topic = "topic";
    static final String UID = "uid";
  }
}
