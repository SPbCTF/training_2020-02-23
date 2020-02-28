package ad.routes;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.jtwig.JtwigModel;

import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import ad.Utils;
//import ad.activitypub.ActivityPubWorker;
import ad.data.Account;
//import ad.data.ForeignUser;
import ad.data.FriendRequest;
import ad.data.FriendshipStatus;
import ad.data.PhotoSize;
import ad.data.Post;
import ad.data.SessionInfo;
import ad.storage.User;
import ad.lang.Lang;
import ad.storage.MediaStorageUtils;
import ad.storage.PostStorage;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

public class ProfileRoutes{
	public static Object profile(Request req, Response resp) throws SQLException{
		SessionInfo info= Utils.INSTANCE.sessionInfo(req);
		@Nullable Account self=info!=null ? info.getAccount() : null;
		String username=req.params(":username");
		User user = User.UserStorage.getByUsername(username);
		if(user!=null){
			user = user.clone();//user modifications only for rendering
			int[] postCount={0};

			JtwigModel model=JtwigModel.newModel();
			boolean privateAllowed = !user.privateProfile || self != null && self.user.id == user.id ;
			if(self!=null){
				FriendshipStatus status= User.UserStorage.getFriendshipStatus(self.getUser().id, user.id);
				if(status==FriendshipStatus.FRIENDS) {
					model.with("isFriend", true);
					privateAllowed = true;
				}
				else if(status==FriendshipStatus.REQUEST_SENT)
					model.with("friendRequestSent", true);
				else if(status==FriendshipStatus.REQUEST_RECVD)
					model.with("friendRequestRecvd", true);

				if (user.privateProfile && !(status == FriendshipStatus.FRIENDS) && user.id != self.getUser().id) {
					user.about = null;
				}
			}
			List<Post> wall= PostStorage.INSTANCE.getUserWall(user.id, 0, 0, postCount, privateAllowed);


			model.with("title", user.getFullName())
					.with("user", user)
					.with("wall", wall)
					.with("own", self!=null && self.getUser().id==user.id)
					.with("postCount", postCount[0]);

			int[] friendCount={0};
			List<User> friends= User.UserStorage.getRandomFriendsForProfile(user.id, friendCount);
			model.with("friendCount", friendCount[0]).with("friends", friends);
			if(info!=null && self!=null){
				model.with("draftAttachments", info.getPostDraftAttachments());
			}
			if(self!=null){
				FriendshipStatus status= User.UserStorage.getFriendshipStatus(self.getUser().id, user.id);
				if(status==FriendshipStatus.FRIENDS)
					model.with("isFriend", true);
				else if(status==FriendshipStatus.REQUEST_SENT)
					model.with("friendRequestSent", true);
				else if(status==FriendshipStatus.REQUEST_RECVD)
					model.with("friendRequestRecvd", true);

				if (user.privateProfile && !(status == FriendshipStatus.FRIENDS) && user.id != self.getUser().id) {
					user.about = null;
				}
			}else{
				if (user.privateProfile) {
					user.about = null;
				}
				HashMap<String, String> meta=new LinkedHashMap<>();
				meta.put("og:type", "profile");
				meta.put("og:uid", String.valueOf(user.id));
				meta.put("og:site_name", "AD");
				meta.put("og:title", user.getFullName());
				meta.put("og:url", user.url.toString());
				meta.put("og:username", user.getFullUsername());
				if(StringUtils.isNotEmpty(user.firstName))
					meta.put("og:first_name", user.firstName);
				if(StringUtils.isNotEmpty(user.lastName))
					meta.put("og:last_name", user.lastName);
				Lang l= Utils.INSTANCE.lang(req);
				String descr=l.plural("X_friends", friendCount[0])+", "+l.plural("X_posts", postCount[0]);
				if(StringUtils.isNotEmpty(user.summary))
					descr+="\n"+user.summary;
				meta.put("og:description", descr);
				if(user.gender==User.Gender.MALE)
					meta.put("og:gender", "male");
				else if(user.gender==User.Gender.FEMALE)
					meta.put("og:gender", "female");
				if(user.hasAvatar()){
					PhotoSize size= MediaStorageUtils.INSTANCE.findBestPhotoSize(user.getAvatar(), PhotoSize.Format.WEBP, PhotoSize.Type.XLARGE);
					if(size!=null){
						meta.put("og:image", size.getSrc().toString());
						meta.put("og:image:width", size.getWidth() +"");
						meta.put("og:image:height", size.getHeight() +"");
					}
				}
				model.with("metaTags", meta);
			}
			Utils.INSTANCE.jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm");
			return Utils.INSTANCE.renderTemplate(req, "profile", model);
		}else{
			resp.status(404);
			return Utils.INSTANCE.wrapError(req, resp, "err_user_not_found");
		}
	}

	public static Object confirmSendFriendRequest(Request req, Response resp, Account self) throws SQLException{
		req.attribute("noHistory", true);
		String username=req.params(":username");
		User user= User.UserStorage.getByUsername(username);
		if(user!=null){
			if(user.id== self.getUser().id){
				return Utils.INSTANCE.wrapError(req, resp, "err_cant_friend_self");
			}
			FriendshipStatus status= User.UserStorage.getFriendshipStatus(self.getUser().id, user.id);
			if(status==FriendshipStatus.NONE){
				JtwigModel model=JtwigModel.newModel();
				model.with("targetUser", user);
				return Utils.INSTANCE.renderTemplate(req, "send_friend_request", model);
			}else if(status==FriendshipStatus.FRIENDS){
				return Utils.INSTANCE.wrapError(req, resp, "err_already_friends");
			}else if(status==FriendshipStatus.REQUEST_RECVD){
				return Utils.INSTANCE.wrapError(req, resp, "err_have_incoming_friend_req");
			}else{ // REQ_SENT
				return Utils.INSTANCE.wrapError(req, resp, "err_friend_req_already_sent");
			}
		}else{
			resp.status(404);
			return Utils.INSTANCE.wrapError(req, resp, "user_not_found");
		}
	}

	public static Object doSendFriendRequest(Request req, Response resp, Account self) throws SQLException{
		String username=req.params(":username");
		User user= User.UserStorage.getByUsername(username);
		if(user!=null){
			if(user.id== self.getUser().id){
				return Utils.INSTANCE.wrapError(req, resp, "err_cant_friend_self");
			}
			FriendshipStatus status= User.UserStorage.getFriendshipStatus(self.getUser().id, user.id);
			if(status==FriendshipStatus.NONE){
				User.UserStorage.putFriendRequest(self.getUser().id, user.id, req.queryParams("message"), true);
				resp.redirect(Utils.INSTANCE.back(req));
				return "";
			}else if(status==FriendshipStatus.FRIENDS){
				return Utils.INSTANCE.wrapError(req, resp, "err_already_friends");
			}else if(status==FriendshipStatus.REQUEST_RECVD){
				return Utils.INSTANCE.wrapError(req, resp, "err_have_incoming_friend_req");
			}else{ // REQ_SENT
				return Utils.INSTANCE.wrapError(req, resp, "err_friend_req_already_sent");
			}
		}else{
			resp.status(404);
			return Utils.INSTANCE.wrapError(req, resp, "user_not_found");
		}
	}

	public static Object confirmRemoveFriend(Request req, Response resp, Account self) throws SQLException{
		req.attribute("noHistory", true);
		String username=req.params(":username");
		User user= User.UserStorage.getByUsername(username);
		if(user!=null){
			FriendshipStatus status= User.UserStorage.getFriendshipStatus(self.getUser().id, user.id);
			if(status==FriendshipStatus.FRIENDS || status==FriendshipStatus.REQUEST_SENT || status==FriendshipStatus.FOLLOWING){
				Lang l= Utils.INSTANCE.lang(req);
				String back= Utils.INSTANCE.back(req);
				JtwigModel model=JtwigModel.newModel().with("message", l.get("confirm_unfriend_X", user.getFullName())).with("formAction", user.getProfileURL("doRemoveFriend")+"?_redir="+URLEncoder.encode(back)).with("back", back);
				return Utils.INSTANCE.renderTemplate(req, "generic_confirm", model);
			}else{
				return Utils.INSTANCE.wrapError(req, resp, "err_not_friends");
			}
		}else{
			resp.status(404);
			return Utils.INSTANCE.wrapError(req, resp, "user_not_found");
		}
	}

	public static Object friends(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		User user= User.UserStorage.getByUsername(username);
		if(user!=null){
			if (req.queryParams("ajax") != null) {
				JSONArray fs = User.UserStorage.getFriendListForUserShort(user.id);
				return fs.toString();
			} else {
				JtwigModel model=JtwigModel.newModel();
				List<User> fs = User.UserStorage.getFriendListForUser(user.id);
				model.with("friendList", fs).with("owner", user).with("tab", 0);
				return Utils.INSTANCE.renderTemplate(req, "friends", model);
			}
		}
		resp.status(404);
		return Utils.INSTANCE.wrapError(req, resp, "user_not_found");
	}

	public static Object followers(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		User user= User.UserStorage.getByUsername(username);
		if(user!=null){
			JtwigModel model=JtwigModel.newModel();
			model.with("friendList", User.UserStorage.getNonMutualFollowers(user.id, true, true)).with("owner", user).with("followers", true).with("tab", 1);
			return Utils.INSTANCE.renderTemplate(req, "friends", model);
		}
		resp.status(404);
		return Utils.INSTANCE.wrapError(req, resp, "user_not_found");
	}

	public static Object following(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		User user= User.UserStorage.getByUsername(username);
		if(user!=null){
			JtwigModel model=JtwigModel.newModel();
			model.with("friendList", User.UserStorage.getNonMutualFollowers(user.id, false, true)).with("owner", user).with("following", true).with("tab", 2);
			return Utils.INSTANCE.renderTemplate(req, "friends", model);
		}
		resp.status(404);
		return Utils.INSTANCE.wrapError(req, resp, "user_not_found");
	}

	public static Object incomingFriendRequests(Request req, Response resp, Account self) throws SQLException{
		String username=req.params(":username");
		if(!self.getUser().username.equalsIgnoreCase(username)){
			resp.redirect(Utils.INSTANCE.back(req));
			return "";
		}
		List<FriendRequest> requests= User.UserStorage.getIncomingFriendRequestsForUser(self.getUser().id, 0, 100);
		JtwigModel model=JtwigModel.newModel();
		model.with("friendRequests", requests);
		return Utils.INSTANCE.renderTemplate(req, "friend_requests", model);
	}

	public static Object respondToFriendRequest(Request req, Response resp, Account self) throws SQLException{
		String username=req.params(":username");
		User user= User.UserStorage.getByUsername(username);
		if(user!=null){
			if(req.queryParams("accept")!=null){
				User.UserStorage.acceptFriendRequest(self.getUser().id, user.id, true);
			}else if(req.queryParams("decline")!=null){
				User.UserStorage.deleteFriendRequest(self.getUser().id, user.id);
				if(false){
//					ActivityPubWorker.getInstance().sendRejectFriendRequestActivity(self.user, (ForeignUser) user);
				}
			}
			resp.redirect(Utils.INSTANCE.back(req));
		}else{
			resp.status(404);
			return Utils.INSTANCE.wrapError(req, resp, "user_not_found");
		}
		return "";
	}

	public static Object doRemoveFriend(Request req, Response resp, Account self) throws SQLException{
		String username=req.params(":username");
		User user= User.UserStorage.getByUsername(username);
		if(user!=null){
			FriendshipStatus status= User.UserStorage.getFriendshipStatus(self.getUser().id, user.id);
			if(status==FriendshipStatus.FRIENDS || status==FriendshipStatus.REQUEST_SENT || status==FriendshipStatus.FOLLOWING){
				User.UserStorage.unfriendUser(self.getUser().id, user.id);
				resp.redirect(Utils.INSTANCE.back(req));
				if(false){
//					ActivityPubWorker.getInstance().sendUnfriendActivity(self.user, user);
				}
			}else{
				return Utils.INSTANCE.wrapError(req, resp, "err_not_friends");
			}
		}else{
			resp.status(404);
			return Utils.INSTANCE.wrapError(req, resp, "user_not_found");
		}
		return "";
	}
}
