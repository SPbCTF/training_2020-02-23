package ad;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class Config{
	public static String dbHost;
	public static String dbUser;
	public static String dbPassword;
	public static String dbName;

//	public static String domain;

	public static String serverIP;
	public static int serverPort;

	public static File uploadPath;
	public static String uploadURLPath;
	public static boolean useHTTP;
	public static String staticFilesPath;



	public static void load(String filePath) throws IOException{
		FileInputStream in=new FileInputStream(filePath);
		Properties props=new Properties();
		props.load(in);
		in.close();

		dbHost=props.getProperty("db.host");
		dbUser=props.getProperty("db.user");
		dbPassword=props.getProperty("db.password");
		dbName=props.getProperty("db.name");


		uploadPath=new File(props.getProperty("upload.path"));
		uploadURLPath=props.getProperty("upload.urlpath");
		useHTTP=true;
		serverIP= "0.0.0.0";
		serverPort= Utils.INSTANCE.parseIntOrDefault(props.getProperty("server.port", "4567"), 4567);
		staticFilesPath=props.getProperty("web.static_files_path");
	}

	public static URI localURI(String path){
		try {
			return new URI(path);
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		}
//		return localURI.resolve(path);
	}

	public static boolean isLocal(URI uri){
		return true;
	}
}
