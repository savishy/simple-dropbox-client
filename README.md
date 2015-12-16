# simple-dropbox-client
A simple Dropbox client I built for my own use. Works great when run as part of automated tasks (e.g. Jenkins). 

## Introduction

This tool allows you to programmatically interact with a Dropbox account, e.g. 
* list contents (files, folders) of a Dropbox path.
* list filenames and details of a Dropbox path.
* download a file
* upload a file and then get a shareable URL to it. 

### Notes

* This tool uses the [Dropbox Java Core API](https://www.dropbox.com/developers-v1/core/start/java). The core libraries are already packaged into the repo. 
* This is an Eclipse project, so you can import it into Eclipse as File > Import > Existing Projects into Workspace.  

## Syntax

## Version History

1.0 (2015-12-16): 
* initial commit. 
* added ability to upload files to dropbox and simultaneously get a Shareable URL. 