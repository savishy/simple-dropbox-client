package com.vish.dropboxclient;
import com.dropbox.core.*;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;


public class DropboxHelper {

	/** location of the access token that links a dropbox account with the app */
	private static String accessTokenPath = System.getProperty("user.dir") + File.separator + "DbxAuthToken.txt";

	/** directory in target Dropbox account that you want to browse contents of */
	private static String targetDir = null;
	/** the filename that you need to browse or download */
	private static String fileName = null;
	/** all supported actions */
	private static enum actions { listfiles, listdetails, download, uploadandshare, deleteoldestfiles };
	/** action to perform */
	private static actions action;
	private static HashMap<String,Date> fileModificationDates = new HashMap<String,Date>();

	/**
	 * try to retrieve previous access token from file
	 * if present, return access token. if absent, return null
	 * @return
	 * @throws Exception
	 */
	private static String retrieveAccessTokenFromFile() throws IOException,FileNotFoundException {
		String code = null;
		File f = new File(accessTokenPath);
		if (f.exists()) {
			BufferedReader bf = new BufferedReader(new FileReader(f));
			String line = null;
			while ((line = bf.readLine()) != null) {
				//ignore empty lines if any
				if (line.trim().length() == 0) continue;
				else {
					code = line.trim();
					System.out.println("found code : " + code + " with length: " + code.length());
					break;					
				}
			}
			bf.close();			
		} else 
			System.out.println("did not find previous auth token file:" + accessTokenPath + "; requesting new token");
		return code;
	}

	/**
	 * parse metadata of a particular dropbox file to return the info you need
	 * 
	 * TODO Returns only last modified Date for now
	 * 
	 * e.g. format: 
	 * {@code 
	 * File("/Apps/foo/storage/emulated/0/DCIM/Camera/testgood6.jpg", 
	 * iconName="page_white_picture", 
	 * mightHaveThumbnail=true, 
	 * numBytes=0, 
	 * humanSize="0 bytes", 
	 * lastModified="2014/05/27 10:27:28 UTC", 
	 * clientMtime="2014/05/27 10:26:47 UTC",
	 * rev="62500feff")}
	 * 
	 * @param metadata the entire metadata for a file as returned by dropbox
	 * @param toFind the <i>exact</i> metadata name
	 * @throws java.text.ParseException 
	 */
	private static Date getModificationDate(String metadata, String toFind) throws java.text.ParseException {
		String regex = "(?<=" + toFind + "=\")(.*)(?=\")";
		String rawValue = null;
		for (String metadataSplit : metadata.split(",")) {
			Pattern pattern = Pattern.compile(regex);
			java.util.regex.Matcher matcher = pattern.matcher(metadataSplit);
			if (matcher.find(0)) {
				rawValue = matcher.group(1);
				System.out.println("match group 1: " + rawValue);
				break;
			} 
		}
		if(toFind.equals("lastModified")) {
			SimpleDateFormat dateformat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss z");
			Date date = dateformat.parse(rawValue);
			System.out.println("parsed date:" + date);
			return date;
		} else {
			return null;
		}

	}

	/**
	 * List contents of a dropbox path. At this point path should have been correctly formatted .
	 * @param client a {@link DbxClient} object.
	 * @param withMetadata set this to true to print full details about each file. 
	 * @throws IOException
	 * @throws DbxException
	 */
	private static void listContents(DbxClient client,boolean withMetadata) throws IOException,DbxException {
		try {
			DbxEntry.WithChildren listing = client.getMetadataWithChildren(targetDir);
			if (listing == null) { System.out.println(targetDir + " is possibly empty or does not exist!"); return; }
			int i=1;
			for (DbxEntry child : listing.children) {
				System.out.println("[" + i + "] " + 
									child.name + 
									(withMetadata ? ": " + child.toString() : "")
									);
				i++;
			}
		} catch (IllegalArgumentException e) {
			throw new IOException("Error listing contents of " + targetDir + ": " + e);
		}
	}
	
	/**
	 * Retrieve metadata from dropbox files.<p>
	 * Populates a hashmap {@link #fileModificationDates} with key = filename, value = String[] containing metadata
	 * @param client
	 * @throws IOException
	 * @throws DbxException
	 * 
	 */
	private static void getFileMetadata(DbxClient client, boolean verbose) throws IOException, DbxException, ParseException {
		DbxEntry.WithChildren listing = client.getMetadataWithChildren(targetDir);
		if (verbose) System.out.println("Files in the path " + targetDir + ":");
		if (listing == null) throw new IOException(targetDir + " is possibly empty!");
		for (DbxEntry child : listing.children) {
			if (verbose) System.out.println("	" + child.name + ": " + child.toString());
			fileModificationDates.put(child.name, getModificationDate(child.toString(),"lastModified"));
		}
	}

	/**
	 * does legwork of downloading a file
	 * @param what the filename.
	 * @param client DbxClient Object
	 * @throws IOException
	 * @throws DbxException
	 */
	private static void getFile(String what, DbxClient client) throws IOException, DbxException {
		String dir = targetDir.endsWith("/") ? targetDir : targetDir + "/";
		System.out.println("getFile: dir:" + dir + " file:" + what);
		FileOutputStream outputStream = new FileOutputStream(what);
		try {
			DbxEntry.File downloadedFile = client.getFile(dir + what, null,
					outputStream);
			System.out.println("File Download Success. Metadata: " + downloadedFile.toString());
		} finally {
			outputStream.close();
		}
	}

	public static void usage() {
		System.out.println(
				"\nVishDBTool2:" +
				"\nA simple Dropbox Client application." +
				"\nWorks great as part of an automated system, to interact with a Dropbox account." +
				"\n\nPREREQUISITES:" + 
				"\nThe \"target\" Dropbox account will need to authorize the application (this is a one-time action)." + 
				"\n- To authorize, simply run this tool once from commandline. " +
				"\n- Follow the instructions to get an access token. " +
				"\n- Paste it into the commandline." +
				"\n\nUSAGE:" +
				"\n--------------------------------------------------\n" +
						"java -jar DropboxHelper.jar ACTION ARG1 ARG2 [ARG3]\n\n" +
						"ACTION: required. can be one of:" + Arrays.toString(actions.values()) + "\n" + 
						"ARG2: required. Provide a valid directory in the target dropbox account. \n" + 
						"ARG3: optional. Provide the exact filename that you want to download. Defaults to downloading whatever is the latest file in ARG2.\n" +
				"--------------------------------------------------\n" + 
				"ACTIONS:\n" +
				"listfiles: list files and folders in a path. can be used to browse your dropbox folder-structure.\n" +
				"listdetails: its like ls -l for Dropbox. List files and lots of additional details. Warning: VERBOSE output!\n" +
				"download: download a file\n" +
				"uploadandshare: upload a file to Dropbox and get a shareable URL to it.\n" +
				"deleteoldestfiles: manage free-space on your Dropbox folder by deleting N oldest files.\n" +
				"Examples: \n" + 
				"List filenames and foldernames in path /a/b/c/d: java -jar DropboxClient.jar listfiles /a/b/c/d\n" + 
				"List filenames with details in /a/b/c/d: java -jar DropboxClient.jar listdetails /a/b/c/d\n" + 
				"Download a file: java -jar DropboxClient.jar download file.exe\n" +
				"Upload a file and get a shareable URL to it: java -jar DropboxClient.jar uploadandshare /a/b/c/d file.exe\n" +
				"delete N oldest files from a folder: java -jar DropboxClient.jar deleteoldestfiles\n"
				);
	}

	
	/** 
	 * parse input arguments and populate class vars
	 * @param args
	 */
	private static void parseArgs(String[] args) throws Exception {

		switch (args.length) {
		case 2:
			/** [action] [dir] */
			action = actions.valueOf(args[0]);
			targetDir = correctedDropboxPath(args[1]);
			break;
		case 3:
			/** [auth token] [action] [dir] [filename] */
			action = actions.valueOf(args[0]);
			targetDir = correctedDropboxPath(args[1]);
			fileName = args[2];
			break;
		default:
			usage();
			throw new Exception("invalid argument count: " + args.length);
		}
	}

	/**
	 * iterate over the {@link #fileModificationDates} hashmap and find out which file is the latest.
	 * <p>
	 * The hashmap should already be populated, using the call to {@link #getFileMetadata(DbxClient, boolean)}
	 * @return
	 * @throws IOException
	 * @throws DbxException
	 * @throws ParseException
	 */
	private static String getLatestFile() throws IOException, DbxException, ParseException {
		Date currentDate = new Date();
		Date maxDate = new SimpleDateFormat("MMMM d, yyyy").parse("January 1, 1970");
		String latestRelease = null;
		/** iterate over both the keys and values */
		for (Map.Entry<String, Date> entry : fileModificationDates.entrySet()) {
			String currentKey = entry.getKey();
			currentDate = entry.getValue();

			if (currentDate.after(maxDate)) {
				maxDate = currentDate;
				latestRelease = currentKey;
			}  
			System.out.println("currentKey:" + currentKey + " currentDate:" + currentDate + " maxDate:" + maxDate);
		}


		System.out.println("Latest File:" + latestRelease + " with modification date:" + maxDate);
		return latestRelease;
	}
	
	private static void deleteOldestFiles() throws Exception {
		
	}
	/**
	 * 
	 * @param client
	 * @param which
	 * @throws Exception
	 */
	public static void doDropboxAction(DbxClient client, String which) throws Exception {
		switch (action) {
		case listfiles:
			listContents(client,false);
			break;
		case listdetails:
			listContents(client,true);
			break;
		case download:
			getFileMetadata(client,false);
			/** if not getting latest, simply retrieve latest file and return
			 */
			if (!which.toLowerCase().equals("latest")) {
				getFile(which,client);
				return;
			}
			/** calculate which is the latest file and retrieve it */
			getFile(getLatestFile(), client);
			break;
		case uploadandshare:
			String uploadedFilePath = uploadFile(client,targetDir);
			String shareURL = getShareLink(client,uploadedFilePath);
			System.out.println(shareURL);
			break;
		case deleteoldestfiles:
			deleteOldestFiles();
			break;
		default: throw new IOException(action + " not supported");
		}
		
	}
	
	/**
	 * Dropbox paths must start with "/".
	 * Dropbox paths must not end with "/"
	 * This method corrects bad Dropbox paths. (See issue https://github.com/savishy/simple-dropbox-client/issues/3)
	 * @param path
	 * @return
	 */
	private static String correctedDropboxPath(String path) {
		if (!path.startsWith("/")) {
			System.out.println("WARN: path " + path + " does not start with /, we corrected it.");
			path = "/" + path;			
		}
		while (path.endsWith("/")) {
			System.out.println("WARN: path " + path + " ends with /, we corrected it.");
			path = path.substring(0, path.length()-1);
			//catch corner case where path is zero-length
			if (path.length() == 0) { path = "/"; break; }
		}
		return path;
	}

	/**
	 * upload a file to Dropbox folder
	 * @param client
	 * @return String containing the Dropbox path to uploaded file. 
	 * @throws Exception
	 */
	private static String uploadFile(DbxClient client, String targetPath) throws Exception {
		System.out.println("upload file:" + fileName + " to path:" + targetPath + "...");
		File inputFile = new File(fileName);
		FileInputStream inputStream = new FileInputStream(inputFile);
		try {
		    DbxEntry.File uploadedFile = client.uploadFile(targetPath + "/" + fileName,
		        DbxWriteMode.add(), inputFile.length(), inputStream);
		    System.out.println("Uploaded: " + uploadedFile.toString());
		    return targetPath + "/" + fileName;
		} finally {
		    inputStream.close();
		}
	}
	
	/**
	 * Get a shareable URL for a file already uploaded using {@link #uploadFile(DbxClient, String)}.
	 * @param client
	 * @param filePath
	 * @throws Exception
	 */
	private static String getShareLink(DbxClient client, String filePath) throws Exception {
			String shareURL=client.createShareableUrl(filePath);
			BufferedWriter bw = new BufferedWriter(new FileWriter("shareurl.txt"));
			System.out.println("Storing link in file shareurl.txt");
			bw.write("shareurl=" + shareURL);
			bw.flush();
			bw.close();	
			return shareURL;
	}
	
	/**
	 * get the app key and secret from the properties file. 
	 * @throws Exception
	 */
	private static String[] loadKey() throws Exception {
		// Get your app's key and secret from the Dropbox developers website.
		// This is for the Dropbox DEVELOPER account. 

		Properties props = new Properties();
		props.load(new FileInputStream(new File(System.getProperty("user.dir") + File.separator + "dropbox.properties")));
		return new String[]{
				props.getProperty("APP_KEY"),
				props.getProperty("APP_SECRET")
		};
	}
	
	private static String promptForAccessToken(DbxRequestConfig config,DbxAppInfo appInfo) throws Exception {
		/**
		 * ask user to visit a Dropbox URL and Login as the "Target" account
		 * (IE the account from which we want to retrieve data)
		 * this retrieves the access token 
		 */

		DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
		String authorizeUrl = webAuth.start();

		System.out.println("1. Go to: " + authorizeUrl);
		System.out.println("2. Click \"Allow\" (you might have to log in first)");
		System.out.println("3. Copy the authorization code.");
		String code = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();			
		DbxAuthFinish authFinish = webAuth.finish(code);
		String accessToken = authFinish.accessToken;
		/** store the token in file for future calls */
		File codeFile = new File(accessTokenPath);
		BufferedWriter bw = new BufferedWriter(new FileWriter(codeFile));
		System.out.println("Storing token : " + accessToken + " with length : " + accessToken.length() + " in file " + accessTokenPath + " for future use...");
		bw.write(accessToken);
		bw.flush();
		bw.close();	
		return accessToken;
	}
	
	/**
	 * @param args
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws Exception {

		parseArgs(args);
		String[] foo = loadKey();

		final String APP_KEY = foo[0];
		final String APP_SECRET = foo[1];
		
		if (APP_KEY == null || APP_SECRET == null) throw new Exception ("error loading Dropbox app credentials!");

		DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);

		DbxRequestConfig config = new DbxRequestConfig(
				"JavaTutorial/1.0", Locale.getDefault().toString());

		/**
		 * try to retrieve access token from file. if not found, retrieve  
		 * by asking user
		 */
		String accessToken;
		if ((accessToken = retrieveAccessTokenFromFile()) == null) {
			accessToken = promptForAccessToken(config, appInfo);
		}

		DbxClient client = new DbxClient(config, accessToken);
		try {
			System.out.println("Linked account: " + client.getAccountInfo().displayName);
		} catch (DbxException e) {
			System.err.println(e.getMessage());
			System.out.println("error using existing token: " + accessToken + " request new token...");
			accessToken = promptForAccessToken(config, appInfo);
		}
		if (fileName == null)		
			doDropboxAction(client,"latest");
		else {
			doDropboxAction(client,fileName);
		}

	}

}
