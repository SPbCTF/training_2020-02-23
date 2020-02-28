package ad.sparkext;

import ad.Utils;
import ad.data.Account;
import ad.data.SessionInfo;
import spark.Request;
import spark.Response;
import spark.Route;

@FunctionalInterface
public interface CSRFRoute extends Route{
	@Override
	default Object handle(Request request, Response response) throws Exception{
		if(!Utils.INSTANCE.requireAccount(request, response) || !Utils.INSTANCE.verifyCSRF(request, response))
			return "";
		SessionInfo info= Utils.INSTANCE.sessionInfo(request);
		return handle(request, response, info.getAccount());
	}

	Object handle(Request request, Response response, Account self) throws Exception;
}
