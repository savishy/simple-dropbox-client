# simple-dropbox-client
A simple Dropbox client I built for my own use. Works great when run as part of automated tasks (e.g. Jenkins). 
Requires you to have a Dropbox Developer account (available for free) and a Dropbox App. 
## Introduction

This tool allows you to programmatically interact with a Dropbox account, e.g. 
* list contents (files, folders) of a Dropbox path.
* list filenames and details of a Dropbox path.
* download a file
* upload a file and then get a shareable URL to it. 

### Notes

* This tool uses the [Dropbox Java Core API](https://www.dropbox.com/developers-v1/core/start/java). The core libraries are already packaged into the repo. 
* This is an Eclipse project, so you can import it into Eclipse as File > Import > Existing Projects into Workspace.
* You will need ant if you want to build from the command-line.
* This tool depends on a dropbox.properties in `pwd` with the following properties. 

	APP_KEY=Dropbox App Key
	APP_SECRET=Dropbox App Secret

## Syntax

	java -jar DropboxHelper.jar ACTION ARG1 ARG2 [ARG3]
	ACTION: required. can be one of 
			listfiles
			listdetails
			download
			uploadandshare 
	ARG2: 	required. Provide a valid directory in the target dropbox account.
	ARG3: 	required if ACTION = download | uploadandshare. 
			Provide the exact filename that you want to interact with.

##Examples 

List filenames in path /a/b/c/d: 

	java -jar DropboxClient.jar listfiles /a/b/c/d\n

List filenames with details in /a/b/c/d

	java -jar DropboxClient.jar listdetails /a/b/c/d\n

Upload a local file to Dropbox and get a shareable URL to it: 

	java -jar DropboxClient.jar uploadandshare /a/b/c/d file.exe

## Version History

1.0 (2015-12-16): 
* initial commit. 
* added ability to upload files to dropbox and simultaneously get a Shareable URL. 