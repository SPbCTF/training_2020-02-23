package ad.storage;

import ad.LruCache;
import ad.data.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ad.Config;
import ad.activitypub.ContextCollector;
import ad.activitypub.objects.ActivityPubObject;
import ad.activitypub.objects.Image;
import ad.activitypub.objects.LocalImage;
//import ad.storage.MediaCache;
import spark.utils.StringUtils;

//latest
public class User extends ActivityPubObject implements Cloneable{
	public static long FLAG_SUPPORTS_FRIEND_REQS=1;
	public static String SUMMARY = "about";
	public static int SUMMARY_LIMIT = 30;
	public static String GENDER = "gender";
	public static String BDATE = "bdate";
	public static String USERNAME = "username";
	public static String LNAME = "lname";
	public static String FNAME = "fname";
	public static String ID = "id";

	public static String SELECT_FROM_USERS_WHERE_ID = "SELECT * FROM \"users\" WHERE \"id\"=?";
	public static String SELECT_FROM_USERS_WHERE_USERNAME = "SELECT * FROM \"users\" WHERE \"username\"=? ";
	public static String SELECT_FOLLOWER_ID_FOLLOWEE_ID_MUTUAL_ACCEPTED_FROM_FOLLOWINGS_WHERE_FOLLOWER_ID_AND_FOLLOWEE_ID_OR_FOLLOWER_ID_AND_FOLLOWEE_ID_LIMIT_1 = "SELECT \"follower_id\",\"followee_id\",\"mutual\",\"accepted\" FROM \"followings\" WHERE (\"follower_id\"=? AND \"followee_id\"=?) OR (\"follower_id\"=? AND \"followee_id\"=?) LIMIT 1";
	public static String SELECT_COUNT_FROM_FRIEND_REQUESTS_WHERE_FROM_USER_ID_AND_TO_USER_ID = "SELECT count(*) FROM \"friend_requests\" WHERE \"from_user_id\"=? AND \"to_user_id\"=?";
	public static String INSERT_INTO_FRIEND_REQUESTS_FROM_USER_ID_TO_USER_ID_MESSAGE_VALUES = "INSERT INTO \"friend_requests\" (\"from_user_id\", \"to_user_id\", \"message\") VALUES (?, ?, ?)";
	public static String SELECT_COUNT_FROM_FOLLOWINGS_WHERE_FOLLOWER_ID_AND_FOLLOWEE_ID = "SELECT COUNT(*) FROM \"followings\" WHERE \"follower_id\"=? AND \"followee_id\"=?";
	public static String INSERT_INTO_FOLLOWINGS_FOLLOWER_ID_FOLLOWEE_ID_ACCEPTED_VALUES = "INSERT INTO \"followings\" (\"follower_id\", \"followee_id\", \"accepted\") VALUES (?, ?, ?)";
	public static String UPDATE_USERS_SET_AVATAR_WHERE_ID = "UPDATE \"users\" SET \"avatar\"=? WHERE \"id\"=?";
	public static String UPDATE_USERS_SET_FNAME_LNAME_ABOUT_ABOUT_PRIVATE_WHERE_ID = "UPDATE \"users\" SET \"fname\"=?, \"lname\"=?, \"about\"=?, \"about_private\"=? WHERE \"id\"=?";
	public static String INSERT_INTO_SIGNUP_INVITATIONS_OWNER_ID_CODE_SIGNUPS_REMAINING_HIDDEN_VALUES = "INSERT INTO \"signup_invitations\" (\"owner_id\", \"code\", \"signups_remaining\", \"hidden\") VALUES (?, ?, ?, ? ); " ;
	public static String UPDATE_FOLLOWINGS_SET_MUTUAL_FALSE_WHERE_FOLLOWER_ID_AND_FOLLOWEE_ID = "UPDATE \"followings\" SET \"mutual\"=false WHERE \"follower_id\"=? AND \"followee_id\"=?";
	public static String DELETE_FROM_FOLLOWINGS_WHERE_FOLLOWER_ID_AND_FOLLOWEE_ID = "DELETE FROM \"followings\" WHERE \"follower_id\"=? AND \"followee_id\"=?";
	public static String DELETE_FROM_FRIEND_REQUESTS_WHERE_FROM_USER_ID_AND_TO_USER_ID = "DELETE FROM \"friend_requests\" WHERE \"from_user_id\"=? AND \"to_user_id\"=?";
	public static String UPDATE_FOLLOWINGS_SET_MUTUAL_TRUE_WHERE_FOLLOWER_ID_AND_FOLLOWEE_ID = "UPDATE \"followings\" SET \"mutual\"=true WHERE \"follower_id\"=? AND \"followee_id\"=?";
	public static String INSERT_INTO_FOLLOWINGS_FOLLOWER_ID_FOLLOWEE_ID_MUTUAL_ACCEPTED_VALUES_TRUE = "INSERT INTO \"followings\" (\"follower_id\", \"followee_id\", \"mutual\", \"accepted\") VALUES(?, ?, true, ?)";
	public static String DELETE_FROM_FRIEND_REQUESTS_WHERE_FROM_USER_ID_AND_TO_USER_ID1 = "DELETE FROM \"friend_requests\" WHERE \"from_user_id\"=? AND \"to_user_id\"=?";
	public static String SELECT_FRIEND_REQUESTS_MESSAGE_USERS_FROM_FRIEND_REQUESTS_INNER_JOIN_USERS_ON_FRIEND_REQUESTS_FROM_USER_ID_USERS_ID_WHERE_TO_USER_ID = "SELECT \"friend_requests\".\"message\", \"users\".* FROM \"friend_requests\" INNER JOIN \"users\" ON \"friend_requests\".\"from_user_id\"=\"users\".\"id\" WHERE \"to_user_id\"=? ";
	public static String SELECT_USERS_FROM_FOLLOWINGS_INNER_JOIN_USERS_ON_USERS_ID_FOLLOWINGS_FOLLOWEE_ID_WHERE_FOLLOWER_ID_AND_MUTUAL_TRUE_ORDER_BY_RANDOM_LIMIT_6 = "SELECT \"users\".* FROM \"followings\" INNER JOIN \"users\" ON \"users\".\"id\"=\"followings\".\"followee_id\" WHERE \"follower_id\"=? AND \"mutual\"=true ORDER BY RANDOM() LIMIT 6";
	public static String SELECT_COUNT_FROM_FOLLOWINGS_WHERE_FOLLOWER_ID_AND_MUTUAL_TRUE = "SELECT COUNT(*) FROM \"followings\" WHERE \"follower_id\"=? AND \"mutual\"=true";
	public static String SELECT_USERS_FROM_FOLLOWINGS_INNER_JOIN_USERS_ON_USERS_ID_FOLLOWINGS_FOLLOWEE_ID_WHERE_FOLLOWER_ID_AND_MUTUAL_TRUE = "SELECT \"users\".* FROM \"followings\" INNER JOIN \"users\" ON \"users\".\"id\"=\"followings\".\"followee_id\" WHERE \"follower_id\"=? AND \"mutual\"=true ORDER BY \"users\".\"id\" DESC LIMIT 256 ";
	public static String SELECT_USERS_FROM_FOLLOWINGS_INNER_JOIN_USERS_ON_USERS_ID_FOLLOWINGS_FOLLOWEE_ID_WHERE_FOLLOWER_ID_AND_MUTUAL_TRUE_short = "SELECT \"users\".\"username\" FROM \"followings\" INNER JOIN \"users\" ON \"users\".\"id\"=\"followings\".\"followee_id\" WHERE \"follower_id\"=? AND \"mutual\"=true ORDER BY \"users\".\"id\" DESC ";
	public static String SELECT_COUNT_FROM_FRIEND_REQUESTS_WHERE_TO_USER_ID = "SELECT COUNT(*) FROM \"friend_requests\" WHERE \"to_user_id\"=?";


	public int id;
	public String firstName;
	public String lastName;
	public String username;
	public java.sql.Date birthDate;
	public Gender gender;
	public long flags;

//	transient public PublicKey publicKey;
//	transient public PrivateKey privateKey;

	// additional profile fields
	public boolean manuallyApprovesFollowers;
	public String about;
	public boolean privateProfile;

	public String getFullName(){
		if(lastName==null || lastName.length()==0)
			return firstName.isEmpty() ? ('@'+username) : firstName;
		return firstName+" "+lastName;
	}

	public String getProfileURL(String action){
		return "/"+getFullUsername()+"/"+action;
	}

	public boolean hasAvatar(){
		return icon!=null;
	}

	public List<PhotoSize> getAvatar(){
		Image icon=this.icon!=null ? this.icon.get(0) : null;
		if(icon==null)
			return null;
		if(icon instanceof LocalImage){
			return ((LocalImage) icon).getSizes();
		}
		return null;
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder("User{");
		sb.append(super.toString());
		sb.append("id=");
		sb.append(id);
		if(firstName!=null){
			sb.append(", firstName='");
			sb.append(firstName);
			sb.append('\'');
		}
		if(lastName!=null){
			sb.append(", lastName='");
			sb.append(lastName);
			sb.append('\'');
		}
		if(username!=null){
			sb.append(", username='");
			sb.append(username);
			sb.append('\'');
		}
		if(birthDate!=null){
			sb.append(", birthDate=");
			sb.append(birthDate);
		}
		if(gender!=null){
			sb.append(", gender=");
			sb.append(gender);
		}
//		if(publicKey!=null){
//			sb.append(", publicKey=");
//			sb.append(publicKey);
//		}
//		if(privateKey!=null){
//			sb.append(", privateKey=");
//			sb.append(privateKey);
//		}
		sb.append('}');
		return sb.toString();
	}

	public static User fromResultSet(ResultSet res) throws SQLException{
		User user=new User();
		user.fillFromResultSet(res);
		return user;
	}

	protected void fillFromResultSet(ResultSet res) throws SQLException{
		id=res.getInt(ID);
		firstName=res.getString(FNAME);
		lastName=res.getString(LNAME);
		username=res.getString(USERNAME);
		birthDate=res.getDate(BDATE);
		gender=Gender.valueOf(res.getInt(GENDER));
		summary=res.getString(SUMMARY);
		if (summary != null) summary = summary.substring(0, Math.min(summary.length(), SUMMARY_LIMIT));
		flags=res.getLong("flags");

//		byte[] key=res.getBytes("public_key");
//		try{
//			X509EncodedKeySpec spec=new X509EncodedKeySpec(key);
//			publicKey=KeyFactory.getInstance("RSA").generatePublic(spec);
//		}catch(Exception ignore){}
//		key=res.getBytes("private_key");
//		if(key!=null){
//			try{
//				PKCS8EncodedKeySpec spec=new PKCS8EncodedKeySpec(key);
//				privateKey=KeyFactory.getInstance("RSA").generatePrivate(spec);
//			}catch(Exception ignore){}
//		}

		about = res.getString("about");
		privateProfile = res.getBoolean("about_private");//todo rename db column name

		String _ava=res.getString("avatar");
		if(_ava!=null){
			if(_ava.startsWith("{")){
				try{
					icon=Collections.singletonList((Image)Image.parse(new JSONObject(_ava)));
				}catch(Exception ignore){}
			}else{
				LocalImage ava=new LocalImage();
				PhotoSize.Type[] sizes={PhotoSize.Type.LARGE, PhotoSize.Type.XLARGE};
				int[] sizeDimens={ 200, 400};
				for(PhotoSize.Format format : PhotoSize.Format.values()){
					for(PhotoSize.Type size : sizes){
						ava.getSizes().add(new PhotoSize(Config.localURI(Config.uploadURLPath+"/avatars/"+_ava+"_"+size.suffix()+"."+format.fileExtension()), sizeDimens[size.ordinal()], sizeDimens[size.ordinal()], size, format));
					}
				}
				icon=Collections.singletonList(ava);
			}
		}

		activityPubID=Config.localURI("/users/"+id);
		url=Config.localURI(username);

		String fields=res.getString("profile_fields");
		if(StringUtils.isNotEmpty(fields)){
			JSONObject o=new JSONObject(fields);
			manuallyApprovesFollowers=o.optBoolean("manuallyApprovesFollowers", false);
		}

	}

	public String getFullUsername(){
		return username;
	}

	public URI getFollowersURL(){
		String userURL=activityPubID.toString();
		return URI.create(userURL+"/followers");
	}

	@Override
	public String getType(){
		return "Person";
	}

	@Override
	public JSONObject asActivityPubObject(JSONObject obj, ContextCollector contextCollector){
		throw new AssertionError("unimplemented");
	}

	@Override
	public boolean equals(Object other){
		if(other==null)
			return false;
		if(other instanceof User){
			return ((User) other).id==id && ((User) other).activityPubID.equals(activityPubID);
		}
		return false;
	}

	public String serializeProfileFields(){
		JSONObject o=new JSONObject();
		if(manuallyApprovesFollowers)
			o.put("manuallyApprovesFollowers", true);
		return o.toString();
	}

	public boolean supportsFriendRequests(){
		return true;
	}

	public enum Gender{
		UNKNOWN,
		MALE,
		FEMALE;

		public static Gender valueOf(int v){
			switch(v){
				case 0:
					return UNKNOWN;
				case 1:
					return MALE;
				case 2:
					return FEMALE;
			}
			throw new IllegalArgumentException("Invalid gender "+v);
		}
	}

	@Override
	public User clone()  {
		try {
			return (User) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	public static class UserStorage{
		private static LruCache<Integer, User> cache=new LruCache<>(128);
		private static LruCache<String, User> cacheByUsername=new LruCache<>(128);
	//	private static LruCache<URI, ForeignUser> cacheByActivityPubID=new LruCache<>(500);
		private static LruCache<Integer, UserNotifications> userNotificationsCache=new LruCache<>(128);

		public static synchronized User getById(int id) throws SQLException{
			return getById(id, /*cached */ true);
		}
		public static synchronized User getById(int id, boolean cached) throws SQLException{
			User user;
			if (cached) {
				user = cache.get(id);
				if(user!=null)
					return user;
			}
			try (PreparedStatement stmt = DatabaseConnectionManager.INSTANCE.getConnection2().prepareStatement(SELECT_FROM_USERS_WHERE_ID)) {
				stmt.setInt(1, id);
				try (ResultSet res = stmt.executeQuery()) {
					if (res.next()) {
						user = fromResultSet(res);
						cache.put(id, user);
						cacheByUsername.put(user.getFullUsername(), user);
						return user;
					}
				}
			}
			return null;
		}

		public static synchronized User getByUsername(@NotNull String username) throws SQLException{
			username=username.toLowerCase();
			User user=cacheByUsername.get(username);
			if(user!=null)
				return user;
			String realUsername;
	//		String domain="";
			realUsername=username;
			try (PreparedStatement stmt = DatabaseConnectionManager.INSTANCE.getConnection2().prepareStatement(SELECT_FROM_USERS_WHERE_USERNAME)) {
				stmt.setString(1, realUsername);
				try (ResultSet res = stmt.executeQuery()) {
					if (res.next()) {
						user = fromResultSet(res);
						cacheByUsername.put(username, user);
						cache.put(user.id, user);
						return user;
					}
				}
			}
			return null;
		}

		public static FriendshipStatus getFriendshipStatus(int selfUserID, int targetUserID) throws SQLException{
			FriendshipStatus status;
			try (PreparedStatement stmt = DatabaseConnectionManager.INSTANCE.getConnection2().prepareStatement(SELECT_FOLLOWER_ID_FOLLOWEE_ID_MUTUAL_ACCEPTED_FROM_FOLLOWINGS_WHERE_FOLLOWER_ID_AND_FOLLOWEE_ID_OR_FOLLOWER_ID_AND_FOLLOWEE_ID_LIMIT_1)) {
				stmt.setInt(1, selfUserID);
				stmt.setInt(2, targetUserID);
				stmt.setInt(3, targetUserID);
				stmt.setInt(4, selfUserID);
				try (ResultSet res = stmt.executeQuery()) {
					if (res.next()) {
						boolean mutual = res.getBoolean(3);
						boolean accepted = res.getBoolean(4);
						if (mutual)
							return FriendshipStatus.FRIENDS;
						int follower = res.getInt(1);
						int followee = res.getInt(2);
						if (follower == selfUserID && followee == targetUserID)
							status = accepted ? FriendshipStatus.FOLLOWING : FriendshipStatus.FOLLOW_REQUESTED;
						else
							status = FriendshipStatus.FOLLOWED_BY;
					} else {
						return FriendshipStatus.NONE;
					}
				}
			}
			try (PreparedStatement stmt = DatabaseConnectionManager.INSTANCE.getConnection2().prepareStatement(SELECT_COUNT_FROM_FRIEND_REQUESTS_WHERE_FROM_USER_ID_AND_TO_USER_ID)) {
				if (status == FriendshipStatus.FOLLOWING) {
					stmt.setInt(1, selfUserID);
					stmt.setInt(2, targetUserID);
				} else {
					stmt.setInt(2, selfUserID);
					stmt.setInt(1, targetUserID);
				}
				try (ResultSet res = stmt.executeQuery()) {
					res.next();
					int count = res.getInt(1);
					if (count == 1) {
						if (status == FriendshipStatus.FOLLOWING)
							return FriendshipStatus.REQUEST_SENT;
						else
							return FriendshipStatus.REQUEST_RECVD;
					}
				}
			}

			return status;
		}

		public static void putFriendRequest(int selfUserID, int targetUserID, String message, boolean followAccepted) throws SQLException{
			Connection conn= DatabaseConnectionManager.INSTANCE.getConnection2();

			conn.setAutoCommit(false);
			try{
				try (PreparedStatement stmt = conn.prepareStatement(INSERT_INTO_FRIEND_REQUESTS_FROM_USER_ID_TO_USER_ID_MESSAGE_VALUES)) {
					stmt.setInt(1, selfUserID);
					stmt.setInt(2, targetUserID);
					stmt.setString(3, message);
					stmt.execute();
				}
				try (PreparedStatement stmt = conn.prepareStatement(SELECT_COUNT_FROM_FOLLOWINGS_WHERE_FOLLOWER_ID_AND_FOLLOWEE_ID)) {
					stmt.setInt(1, selfUserID);
					stmt.setInt(2, targetUserID);
					try (ResultSet res = stmt.executeQuery()) {
						if (!res.next() || res.getInt(1) == 0) {
							try (PreparedStatement stmt2 = conn.prepareStatement(INSERT_INTO_FOLLOWINGS_FOLLOWER_ID_FOLLOWEE_ID_ACCEPTED_VALUES)) {
								stmt2.setInt(1, selfUserID);
								stmt2.setInt(2, targetUserID);
								stmt2.setBoolean(3, followAccepted);
								stmt2.execute();
							}
						}
					}
				}

				synchronized(UserStorage.class){
					UserNotifications res=userNotificationsCache.get(targetUserID);
					if(res!=null)
						res.incNewFriendRequestCount(1);
				}
				conn.commit();
				conn.setAutoCommit(true);
			}catch(SQLException x){
				conn.rollback();
				conn.setAutoCommit(true);
				throw new SQLException(x);
			}
		}

		public static synchronized UserNotifications getNotificationsForUser(int userID) throws SQLException{
			UserNotifications res=userNotificationsCache.get(userID);
			if(res!=null)
				return res;
			res=new UserNotifications();
			Connection conn= DatabaseConnectionManager.INSTANCE.getConnection2();
			try (PreparedStatement stmt = conn.prepareStatement(SELECT_COUNT_FROM_FRIEND_REQUESTS_WHERE_TO_USER_ID)) {
				stmt.setInt(1, userID);
				try (ResultSet r = stmt.executeQuery()) {
					r.next();
					res.incNewFriendRequestCount(r.getInt(1));
				}
			}
			userNotificationsCache.put(userID, res);
			return res;
		}

		public static List<User> getFriendListForUser(int userID) throws SQLException{
			Connection conn= DatabaseConnectionManager.INSTANCE.getConnection2();
			ArrayList<User> friends;
			try (PreparedStatement stmt = conn.prepareStatement(SELECT_USERS_FROM_FOLLOWINGS_INNER_JOIN_USERS_ON_USERS_ID_FOLLOWINGS_FOLLOWEE_ID_WHERE_FOLLOWER_ID_AND_MUTUAL_TRUE)) {
				stmt.setInt(1, userID);
				friends = new ArrayList<>();
				try (ResultSet res = stmt.executeQuery()) {
					if (res.next()) {
						do {
							User user;
							user = fromResultSet(res);
							cache.put(user.id, user);
							cacheByUsername.put(user.getFullUsername(), user);
							friends.add(user);
						} while (res.next());
					}
				}
			}
			return friends;
		}

		public static JSONArray getFriendListForUserShort(int userID) throws SQLException{
			Connection conn= DatabaseConnectionManager.INSTANCE.getConnection2();
			JSONArray friends = new JSONArray();
			try (PreparedStatement stmt = conn.prepareStatement(SELECT_USERS_FROM_FOLLOWINGS_INNER_JOIN_USERS_ON_USERS_ID_FOLLOWINGS_FOLLOWEE_ID_WHERE_FOLLOWER_ID_AND_MUTUAL_TRUE_short)) {
				stmt.setInt(1, userID);

				try (ResultSet res = stmt.executeQuery()) {
					if (res.next()) {
						do {
							String u = res.getString(1);
							friends.put(u);
						} while (res.next());
					}
				}
			}
			return friends;
		}

		public static List<User> getRandomFriendsForProfile(int userID, int[] outTotal) throws SQLException{
			Connection conn= DatabaseConnectionManager.INSTANCE.getConnection2();
			if(outTotal!=null && outTotal.length>=1){
				try (PreparedStatement stmt=conn.prepareStatement(SELECT_COUNT_FROM_FOLLOWINGS_WHERE_FOLLOWER_ID_AND_MUTUAL_TRUE)) {
					stmt.setInt(1, userID);
					try(ResultSet res=stmt.executeQuery()){
						res.next();
						outTotal[0]=res.getInt(1);
					}
				};

			}
			try(PreparedStatement stmt=conn.prepareStatement(SELECT_USERS_FROM_FOLLOWINGS_INNER_JOIN_USERS_ON_USERS_ID_FOLLOWINGS_FOLLOWEE_ID_WHERE_FOLLOWER_ID_AND_MUTUAL_TRUE_ORDER_BY_RANDOM_LIMIT_6)) {
				stmt.setInt(1, userID);
				ArrayList<User> friends=new ArrayList<>();
				try(ResultSet res=stmt.executeQuery()){
					if(res.next()){
						do{
							User user;
							user= fromResultSet(res);
							cache.put(user.id, user);
							cacheByUsername.put(user.getFullUsername(), user);
							friends.add(user);
						}while(res.next());
					}
				}
				return friends;
			}
		}

		public static List<User> getNonMutualFollowers(int userID, boolean followers, boolean accepted) throws SQLException{
			Connection conn= DatabaseConnectionManager.INSTANCE.getConnection2();
			String fld1=followers ? "follower_id" : "followee_id";
			String fld2=followers ? "followee_id" : "follower_id";
			ArrayList<User> friends;
			try (PreparedStatement stmt = conn.prepareStatement("SELECT \"users\".* FROM \"followings\" INNER JOIN \"users\" ON \"users\".\"id\"=\"followings\".\"" + fld1 + "\" WHERE \"" + fld2 + "\"=? AND \"mutual\"=false AND \"accepted\"=?")) {
				stmt.setInt(1, userID);
				stmt.setBoolean(2, accepted);
				friends = new ArrayList<>();
				try (ResultSet res = stmt.executeQuery()) {
					if (res.next()) {
						do {
							User user;
							user = fromResultSet(res);
							cache.put(user.id, user);
							cacheByUsername.put(user.getFullUsername(), user);
							friends.add(user);
						} while (res.next());
					}
				}
			}
			return friends;
		}

		public static List<FriendRequest> getIncomingFriendRequestsForUser(int userID, int offset, int count) throws SQLException{
			Connection conn= DatabaseConnectionManager.INSTANCE.getConnection2();
			ArrayList<FriendRequest> reqs;
			try (PreparedStatement stmt = conn.prepareStatement(SELECT_FRIEND_REQUESTS_MESSAGE_USERS_FROM_FRIEND_REQUESTS_INNER_JOIN_USERS_ON_FRIEND_REQUESTS_FROM_USER_ID_USERS_ID_WHERE_TO_USER_ID)) {
				stmt.setInt(1, userID);
				reqs = new ArrayList<>();
				try (ResultSet res = stmt.executeQuery()) {
					if (res.next()) {
						do {
							FriendRequest req = FriendRequest.Companion.fromResultSet(res);
							cache.put(req.getFrom().id, req.getFrom());
							cacheByUsername.put(req.getFrom().getFullUsername(), req.getFrom());
							reqs.add(req);
						} while (res.next());
					}
				}
			}
			return reqs;
		}

		public static void acceptFriendRequest(int userID, int targetUserID, boolean followAccepted) throws SQLException{
			Connection conn= DatabaseConnectionManager.INSTANCE.getConnection2();
			conn.setAutoCommit(false);
			try{
				try (PreparedStatement stmt = conn.prepareStatement(DELETE_FROM_FRIEND_REQUESTS_WHERE_FROM_USER_ID_AND_TO_USER_ID1)) {
					stmt.setInt(1, targetUserID);
					stmt.setInt(2, userID);
					if (stmt.executeUpdate() != 1) {
						conn.rollback();
						conn.setAutoCommit(true);
						return;
					}
				}
				try (PreparedStatement stmt = conn.prepareStatement(INSERT_INTO_FOLLOWINGS_FOLLOWER_ID_FOLLOWEE_ID_MUTUAL_ACCEPTED_VALUES_TRUE)) {
					stmt.setInt(1, userID);
					stmt.setInt(2, targetUserID);
					stmt.setBoolean(3, followAccepted);
					stmt.execute();
				}
				try (PreparedStatement stmt = conn.prepareStatement(UPDATE_FOLLOWINGS_SET_MUTUAL_TRUE_WHERE_FOLLOWER_ID_AND_FOLLOWEE_ID)) {
					stmt.setInt(1, targetUserID);
					stmt.setInt(2, userID);
					if (stmt.executeUpdate() != 1) {
						conn.rollback();
						conn.setAutoCommit(true);
						return;
					}
				}


				conn.commit();
				conn.setAutoCommit(true);
				UserNotifications n=userNotificationsCache.get(userID);
				if(n!=null)
					n.incNewFriendRequestCount(-1);
			}catch(SQLException x){
				conn.rollback();
				conn.setAutoCommit(true);
				throw new SQLException(x);
			}
		}

		public static void deleteFriendRequest(int userID, int targetUserID) throws SQLException{
			Connection conn= DatabaseConnectionManager.INSTANCE.getConnection2();
			int rows;
			try (PreparedStatement stmt = conn.prepareStatement(DELETE_FROM_FRIEND_REQUESTS_WHERE_FROM_USER_ID_AND_TO_USER_ID)) {
				stmt.setInt(1, targetUserID);
				stmt.setInt(2, userID);
				rows = stmt.executeUpdate();
			}
			UserNotifications n=userNotificationsCache.get(userID);
			if(n!=null)
				n.incNewFriendRequestCount(-rows);
		}

		public static void unfriendUser(int userID, int targetUserID) throws SQLException{
			Connection conn= DatabaseConnectionManager.INSTANCE.getConnection2();
			conn.setAutoCommit(false);
			try{
				try (PreparedStatement stmt = conn.prepareStatement(DELETE_FROM_FOLLOWINGS_WHERE_FOLLOWER_ID_AND_FOLLOWEE_ID)) {
					stmt.setInt(1, userID);
					stmt.setInt(2, targetUserID);
					stmt.execute();

				}
				try (PreparedStatement stmt = conn.prepareStatement(UPDATE_FOLLOWINGS_SET_MUTUAL_FALSE_WHERE_FOLLOWER_ID_AND_FOLLOWEE_ID)) {
					stmt.setInt(1, targetUserID);
					stmt.setInt(2, userID);
					stmt.execute();
				}
				conn.commit();
				conn.setAutoCommit(true);
			}catch(SQLException x){
				conn.rollback();
				conn.setAutoCommit(true);
				throw new SQLException(x);
			}
		}



		public static List<Invitation> getInvites(int userID, boolean onlyValid) throws SQLException{
			Connection conn= DatabaseConnectionManager.INSTANCE.getConnection2();
			ArrayList<Invitation> invitations;
			try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM \"signup_invitations\" WHERE \"hidden\"=false AND \"owner_id\"=?" + (onlyValid ? " AND \"signups_remaining\">0" : "") + " ORDER BY \"created\" DESC")) {
				stmt.setInt(1, userID);
				invitations = new ArrayList<>();
				try (ResultSet res = stmt.executeQuery()) {
					if (res.next()) {
						do {
							invitations.add(Invitation.Companion.fromResultSet(res));
						} while (res.next());
					}
				}
			}
			return invitations;
		}

		public static void putInvite(int userID, byte[] code, int signups, boolean hidden) throws SQLException{
			Connection conn= DatabaseConnectionManager.INSTANCE.getConnection2();
			PreparedStatement stmt=conn.prepareStatement(INSERT_INTO_SIGNUP_INVITATIONS_OWNER_ID_CODE_SIGNUPS_REMAINING_HIDDEN_VALUES);
			stmt.setInt(1, userID);
			stmt.setBytes(2, code);
			stmt.setInt(3, signups);
			stmt.setBoolean(4, hidden);
			stmt.execute();
		}

		public static void changeUser(User u) throws SQLException{
			Connection conn= DatabaseConnectionManager.INSTANCE.getConnection2();
			try (PreparedStatement stmt = conn.prepareStatement(UPDATE_USERS_SET_FNAME_LNAME_ABOUT_ABOUT_PRIVATE_WHERE_ID)) {
				stmt.setString(1, u.firstName);
				stmt.setString(2, u.lastName);
				stmt.setString(3, u.about);
				stmt.setBoolean(4, u.privateProfile );
				stmt.setInt(5, u.id);
				int i = stmt.executeUpdate();
			}
			getById(u.id, false); // refresh cache

		}



		public static void updateProfilePicture(int userID, String serializedPic) throws SQLException{
			Connection conn= DatabaseConnectionManager.INSTANCE.getConnection2();
			try (PreparedStatement stmt = conn.prepareStatement(UPDATE_USERS_SET_AVATAR_WHERE_ID)) {
				stmt.setString(1, serializedPic);
				stmt.setInt(2, userID);
				stmt.execute();
			}
		}



	}
}
