package ad.data;

import ad.storage.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ad.Config;
import ad.Utils;
import ad.activitypub.ContextCollector;
import ad.activitypub.objects.ActivityPubObject;
import ad.activitypub.objects.LinkOrObject;
import ad.activitypub.objects.LocalImage;
import ad.activitypub.objects.Mention;
import ad.data.attachments.Attachment;
import ad.data.attachments.PhotoAttachment;
import ad.data.attachments.VideoAttachment;
import ad.storage.PostStorage;

public class Post extends ActivityPubObject{

	public static final URI AS_PUBLIC=URI.create("https://www.w3.org/ns/activitystreams#Public");

	public int id;
	public User user;
	public User owner;

	public String userLink;
	public String userLinkAttrs="";

	public int[] replyKey={};

	public List<Post> replies=new ArrayList<>();
	public boolean local;
	public List<User> mentionedUsers=Collections.EMPTY_LIST;
	public boolean isPrivate;
	public static Post fromResultSet(ResultSet res) throws SQLException{
		Post post=new Post();
		post.fillFromResultSet(res);
		return post;
	}



	protected void fillFromResultSet(ResultSet res) throws SQLException{
		id=res.getInt("id");
		content=res.getString("text");
		published=res.getTimestamp("created_at");
		user= User.UserStorage.getById(res.getInt("author_id"));
		owner= User.UserStorage.getById(res.getInt("owner_user_id"));
		summary=res.getString("content_warning");
		isPrivate=res.getBoolean("private");
		attributedTo=user.activityPubID;

		if(user.id==owner.id){
			to=Collections.singletonList(new LinkOrObject(AS_PUBLIC));
			cc=Collections.singletonList(new LinkOrObject(user.getFollowersURL()));
		}else{
			to=Collections.EMPTY_LIST;
			cc=Arrays.asList(new LinkOrObject(AS_PUBLIC), new LinkOrObject(owner.activityPubID));
		}
		String apid=res.getString("ap_id");
		try{
			if(apid==null){
				activityPubID=Config.localURI("/posts/"+id);
				url=activityPubID;
				local=true;
			}else{
				activityPubID=new URI(apid);
				url=new URI(res.getString("ap_url"));
			}
		}catch(URISyntaxException ignore){}

		String att=res.getString("attachments");
		if(att!=null){
			try{
				attachment=parseSingleObjectOrArray(att.charAt(0)=='[' ? new JSONArray(att) : new JSONObject(att));
			}catch(Exception ignore){}
		}

		userLink=user.url.toString();

		byte[] rk=res.getBytes("reply_key");
		replyKey= Utils.INSTANCE.deserializeIntArray(rk);
		if(replyKey==null)
			replyKey=new int[0];

		if(replyKey.length>0){
			inReplyTo= PostStorage.INSTANCE.getActivityPubID(replyKey[replyKey.length-1]);
		}

		int[] mentions= Utils.INSTANCE.deserializeIntArray(res.getBytes("mentions"));
		if(mentions!=null && mentions.length>0){
			mentionedUsers=new ArrayList<>();
			if(tag==null)
				tag=new ArrayList<>();
			for(int id:mentions){
				User user= User.UserStorage.getById(id);
				if(user!=null){
					mentionedUsers.add(user);
					addToCC(user.activityPubID);
					Mention mention=new Mention();
					mention.setHref(user.activityPubID);
					tag.add(mention);
				}
			}
		}
	}

	public boolean hasContentWarning(){
		return summary!=null;
	}

	@Override
	public String getType(){
		return "Note";
	}

	@Override
	public JSONObject asActivityPubObject(JSONObject obj, ContextCollector contextCollector){
		throw new AssertionError("unimplemented");
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JSONObject obj) throws Exception {
		throw new AssertionError();
//		super.parseActivityPubObject(obj);
//		Object _content=obj.get("content");
//		if(_content instanceof JSONArray){
//			content=((JSONArray) _content).getString(0);
//		}
//		user=UserStorage.getUserByActivityPubID(attributedTo);
//		if(url==null)
//			url=activityPubID;
//		if(published==null)
//			published=new Date();
//
//		URI partOf=tryParseURL(obj.optString("partOf", null));
//		if(partOf!=null && inReplyTo==null){
//			if(Config.isLocal(partOf)){
//				String[] parts=partOf.getPath().split("/");
//				if(parts.length==4 && "users".equals(parts[1]) && "outbox".equals(parts[3])){ // "", "users", id, "outbox"
//					int id=Utils.parseIntOrDefault(parts[2], 0);
//					owner=UserStorage.getById(id);
//					if(owner instanceof ForeignUser)
//						owner=null;
//				}
//			}else{
//				owner=UserStorage.getByOutbox(partOf);
//			}
//		}else{
//			owner=user;
//		}

//		return this;
	}

	public String serializeAttachments(){
		if(attachment==null)
			return null;
		return serializeObjectArrayCompact(attachment, new ContextCollector()).toString();
	}

	public boolean canBeManagedBy(User user){
		if(user==null)
			return false;
		return owner.id==user.id || this.user.id==user.id;
	}

	public URI getInternalURL(){
		return Config.localURI("/posts/"+id);
	}

//	public void setParent(Post parent){
//		replyKey=new int[parent.replyKey.length+1];
//		System.arraycopy(parent.replyKey, 0, replyKey, 0, parent.replyKey.length);
//		replyKey[replyKey.length-1]=parent.id;
//		inReplyTo=parent.activityPubID;
//		if(tag==null)
//			tag=new ArrayList<>();
//		else if(!(tag instanceof ArrayList))
//			tag=new ArrayList<>(tag);
//		Mention mention=new Mention();
//		mention.href=parent.user.activityPubID;
//		tag.add(mention);
//		if(mentionedUsers.isEmpty())
//			mentionedUsers=new ArrayList<>();
//		mentionedUsers.add(parent.user);
//	}

	public int getReplyLevel(){
		return replyKey.length;
	}

	public void addToCC(URI uri){
		LinkOrObject l=new LinkOrObject(uri);
		if(!cc.contains(l)){
			if(!(cc instanceof ArrayList)){
				cc=new ArrayList<>(cc);
			}
			cc.add(l);
		}
	}

	public List<Attachment> getProcessedAttachments() throws SQLException{
		ArrayList<Attachment> result=new ArrayList<>();
		int i=0;
		for(ActivityPubObject o:attachment){
			if(o.mediaType==null){
				i++;
				continue;
			}
			if(o.mediaType.startsWith("image/")){
				PhotoAttachment att=new PhotoAttachment();
				if(o instanceof LocalImage){
					String localID = ((LocalImage) o).getLocalID();
					if (localID != null) {
						att.setSizes(((LocalImage) o).getSizes());
						att.setLocalId(localID);
					}
				}else{
					throw new AssertionError();
//					MediaCache.PhotoItem item=(MediaCache.PhotoItem) MediaCache.getInstance().get(o.url);
//					if(item!=null){
//						att.sizes=item.sizes;
//					}else{
//						String pathPrefix="/system/downloadExternalMedia?type=post_photo&post_id="+id+"&index="+i;
//						PhotoSize.Type[] sizes={PhotoSize.Type.XSMALL, PhotoSize.Type.SMALL, PhotoSize.Type.MEDIUM, PhotoSize.Type.LARGE, PhotoSize.Type.XLARGE};
//						for(PhotoSize.Format format : PhotoSize.Format.values()){
//							for(PhotoSize.Type size : sizes){
//								att.sizes.add(new PhotoSize(Config.localURI(pathPrefix+"&size="+size.suffix()+"&format="+format.fileExtension()), PhotoSize.UNKNOWN, PhotoSize.UNKNOWN, size, format));
//							}
//						}
//					}
				}
				result.add(att);
			}else if(o.mediaType.startsWith("video/")){
				VideoAttachment att=new VideoAttachment();
				att.setUrl(o.url);
				result.add(att);
			}
			i++;
		}
		return result;
	}
}
