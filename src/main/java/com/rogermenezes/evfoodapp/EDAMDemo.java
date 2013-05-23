package com.rogermenezes.evfoodapp;

/*
Evernote API sample code, structured as a simple command line
application that demonstrates several API calls.

To compile (Unix):
  javac -classpath .:../../lib/libthrift.jar:../../lib/evernote-api-*.jar EDAMDemo.java

To run:
   java -classpath .:../../lib/libthrift.jar:../../lib/evernote-api-*.jar EDAMDemo

Full documentation of the Evernote API can be found at
http://dev.evernote.com/documentation/cloud/
*/

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import com.evernote.edam.error.EDAMErrorCode;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.notestore.NoteStore;
import com.evernote.edam.notestore.SyncChunk;
import com.evernote.edam.notestore.SyncChunkFilter;
import com.evernote.edam.notestore.SyncState;
import com.evernote.edam.type.Data;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteAttributes;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.Resource;
import com.evernote.edam.type.ResourceAttributes;
import com.evernote.edam.type.Tag;
import com.evernote.edam.type.User;
import com.evernote.edam.userstore.Constants;
import com.evernote.edam.userstore.UserStore;
import com.evernote.thrift.protocol.TBinaryProtocol;
import com.evernote.thrift.transport.THttpClient;
import com.evernote.thrift.transport.TTransportException;
import com.rogermenezes.evfoodapp.awsclient.AWSClient;

public class EDAMDemo {

/***************************************************************************
 * You must change the following values before running this sample code    *
 ***************************************************************************/

// Real applications authenticate with Evernote using OAuth, but for the
// purpose of exploring the API, you can get a developer token that allows
// you to access your own Evernote account. To get a developer token, visit
// https://sandbox.evernote.com/api/DeveloperToken.action
private String authToken = "S=s1:U=676a3:E=145af78f8ef:C=13e57c7ccf1:P=1cd:A=en-devtoken:V=2:H=c7d0f89a9cf26345e3f6abec291e6110";

public String getAuthToken() {
	return authToken;
}

public void setAuthToken(String authToken) {
	this.authToken = authToken;
}

/***************************************************************************
 * You shouldn't need to change anything below here to run sample code     *
 ***************************************************************************/

// Initial development is performed on our sandbox server. To use the production
// service, change "sandbox.evernote.com" to "www.evernote.com" and replace your
// developer token above with a token from
// https://www.evernote.com/api/DeveloperToken.action
private static final String evernoteHost = "sandbox.evernote.com";
private static final String userStoreUrl = "https://" + evernoteHost + "/edam/user";

// In a real application, you would change the User Agent to a string that describes
// your application, using the form company name/app name and version. Using a unique
// user agent string helps us provide you with better support.
private static final String userAgent = "Evernote/EDAMDemo (Java) " +
                                        Constants.EDAM_VERSION_MAJOR + "." +
                                        Constants.EDAM_VERSION_MINOR;

private NoteStore.Client noteStore;
private String newNoteGuid;

private User user;

private int lastUpdateCount = 0;

public int getLastUpdateCount() {
	return lastUpdateCount;
}

public void setLastUpdateCount(int lastUpdateCount) {
	this.lastUpdateCount = lastUpdateCount;
}

AWSClient awsc;

public EDAMDemo() {
	awsc = new AWSClient();
}

/**
 * Console entry point.
 */
public static void main(String args[])
  throws Exception
{
//  if ("your developer token".equals(authToken)) {
//    System.err.println("Please fill in your developer token");
//    System.err.println("To get a developer token, go to https://sandbox.evernote.com/api/DeveloperToken.action");
//    return;
//  }

  EDAMDemo demo = new EDAMDemo();
  if (args.length > 0)
  {
	  demo.setLastUpdateCount(Integer.parseInt(args[0]));
  }
  if (demo.intitialize()) {
    try {
      //demo.listNotes();
    	demo.getFoodNotes();
      //demo.createNote();
      //demo.searchNotes();
      //demo.updateNoteTag();
    } catch (EDAMUserException e) {
      // These are the most common error types that you'll need to handle
      // EDAMUserException is thrown when an API call fails because a
      // paramter was invalid.
      if (e.getErrorCode() == EDAMErrorCode.AUTH_EXPIRED) {
        System.err.println("Your authentication token is expired!");
      } else if (e.getErrorCode() == EDAMErrorCode.INVALID_AUTH) {
        System.err.println("Your authentication token is invalid!");
      } else if (e.getErrorCode() == EDAMErrorCode.QUOTA_REACHED) {
        System.err.println("Your authentication token is invalid!");
      } else {
        System.err.println("Error: " + e.getErrorCode().toString() +
          " parameter: " + e.getParameter());
      }
    } catch (EDAMSystemException e) {
      System.err.println("System error: " + e.getErrorCode().toString());
    } catch (TTransportException t) {
      System.err.println("Networking error: " + t.getMessage());
    }
  }
}

/**
 * Intialize UserStore and NoteStore clients. During this step, we authenticate
 * with the Evernote web service. All of this code is boilerplate - you can copy
 * it straight into your application.
 */
boolean intitialize()
  throws Exception
{
  // Set up the UserStore client and check that we can speak to the server
  THttpClient userStoreTrans = new THttpClient(userStoreUrl);
  userStoreTrans.setCustomHeader("User-Agent", userAgent);
  TBinaryProtocol userStoreProt = new TBinaryProtocol(userStoreTrans);
  UserStore.Client userStore = new UserStore.Client(userStoreProt, userStoreProt);

  boolean versionOk = userStore.checkVersion("Evernote EDAMDemo (Java)",
      com.evernote.edam.userstore.Constants.EDAM_VERSION_MAJOR,
      com.evernote.edam.userstore.Constants.EDAM_VERSION_MINOR);
  if (!versionOk) {
    System.err.println("Incomatible Evernote client protocol version");
    return false;
  }

  // Get the URL used to interact with the contents of the user's account
  // When your application authenticates using OAuth, the NoteStore URL will
  // be returned along with the auth token in the final OAuth request.
  // In that case, you don't need to make this call.
  String notestoreUrl = userStore.getNoteStoreUrl(authToken);
  user = userStore.getUser(authToken);

  // Set up the NoteStore client
  THttpClient noteStoreTrans = new THttpClient(notestoreUrl);
  noteStoreTrans.setCustomHeader("User-Agent", userAgent);
  TBinaryProtocol noteStoreProt = new TBinaryProtocol(noteStoreTrans);
  noteStore = new NoteStore.Client(noteStoreProt, noteStoreProt);

  return true;
}

/**
 * Retrieve and display a list of the user's notes.
 */
private void listNotes()
  throws Exception
{
  // List the notes in the user's account
  System.out.println("Listing notes:");

  // First, get a list of all notebooks
  List<Notebook> notebooks = noteStore.listNotebooks(authToken);


  for (Notebook notebook : notebooks) {
    System.out.println("Notebook: " + notebook.getName());

    // Next, search for the first 100 notes in this notebook, ordering by creation date
    NoteFilter filter = new NoteFilter();
    filter.setNotebookGuid(notebook.getGuid());
    filter.setOrder(NoteSortOrder.CREATED.getValue());
    filter.setAscending(true);

    NoteList noteList = noteStore.findNotes(authToken, filter, 0, 100);
    List<Note> notes = noteList.getNotes();
    for (Note note : notes) {
      System.out.println(" * " + note.getTitle());
      NoteAttributes na = note.getAttributes();
      if(na.getContentClass() == null || !na.getContentClass().startsWith("evernote.food.meal"))
      {
    	  continue;
      }

      String noteContent = noteStore.getNoteContent(authToken, note.getGuid());
      note.setContent(noteContent);
      System.out.println("noteContent    * " + noteContent);
      List<Resource> list = note.getResources();
      //Resource r = noteStore.getResourceByHash(authToken, note.getGuid(), "8eaf7dc16ea55edafbd6d9092584a5f5".getBytes(), false, false, false);

      Resource r = list.get(0);
      String url = "https://" + evernoteHost +"/shard/s1/res/" + list.get(0).getGuid();
      System.out.println("url    * " + url + "");



      //r.getGuid();
      //notebook.get
    }
  }
  System.out.println();
}

public List<String> getFoodNotes() throws Exception
{
    String noteReturn = "";
    List<String> notesReturn = new LinkedList<String>();


    SyncState currentState = noteStore.getSyncState(authToken);
    int currentUpdateCount = currentState.getUpdateCount();
	System.out.print("currentUpdateCount: " + currentUpdateCount);


    if (currentUpdateCount > lastUpdateCount)
    {
	    SyncChunkFilter filter1 = new SyncChunkFilter();
	    filter1.setIncludeNotebooks(true);
	    filter1.setIncludeNotes(true);
	    filter1.setRequireNoteContentClass("evernote.food.meal");
    	SyncChunk syncChunk = noteStore.getFilteredSyncChunk(authToken, lastUpdateCount, 50, filter1);
    	System.out.print(syncChunk);

    }


	  List<Notebook> notebooks = noteStore.listNotebooks(authToken);
	  for (Notebook notebook : notebooks) {
		    System.out.println("Notebook: " + notebook.getName());

		    // Next, search for the first 100 notes in this notebook, ordering by creation date
		    NoteFilter filter = new NoteFilter();
		    filter.setNotebookGuid(notebook.getGuid());
		    filter.setOrder(NoteSortOrder.CREATED.getValue());
		    filter.setAscending(true);

		    NoteList noteList = noteStore.findNotes(authToken, filter, 0, 100);
		    List<Note> notes = noteList.getNotes();
		    for (Note note : notes) {
		      System.out.println(" * " + note.getTitle());
		      NoteAttributes nas = note.getAttributes();
		      if(nas.getContentClass() == null || !nas.getContentClass().startsWith("evernote.food.meal"))
		      {
		    	  continue;
		      }
		      String noteContent = noteStore.getNoteContent(authToken, note.getGuid());
		      System.out.println("    * " + noteContent);
		      List<Resource> list = note.getResources();
		      Resource res = list.get(0);
		      Resource resDownload = noteStore.getResource(authToken, res.getGuid(), true, false, true, false);
		      Data d = resDownload.getData();
		      writeImage("c:/evernote/" + res.getGuid(), d.getBody());

		      awsc.uploadFiletoS3(res.getGuid());

		      //d.getBody()
		      String url = "http://evernotefood.s3.amazonaws.com/"+ res.getGuid();
		    		  //"https://" + evernoteHost +"/shard/s1/res/" + list.get(0).getGuid() + ".jpg?resizeSmall&width=600";

		      noteReturn = noteContent;
		      noteReturn = noteReturn.replaceAll("<en-media[^>]*>", "<img src=\"" + url + "\" width=\"100%\" >");
		      noteReturn = noteReturn.replaceAll("en-note", "div");
		      notesReturn.add(noteReturn);
		    }
		  }
		  System.out.println();
		  return notesReturn;
}

private void writeImage(String filename, byte[] image) throws Exception
{
	File f = new File(filename);
	if (f.exists())
	{
		return;
	}
	BufferedImage bi = ImageIO.read(new ByteArrayInputStream(image));
	ImageIO.write(bi, "jpeg", new File(filename));
	}







/**
 * Create a new note containing a little text and the Evernote icon.
 */
private void createNote()
  throws Exception
{
  // To create a new note, simply create a new Note object and fill in
  // attributes such as the note's title.
  Note note = new Note();
  note.setTitle("Test note from EDAMDemo.java");

  String fileName = "enlogo.png";
  String mimeType = "image/png";

  // To include an attachment such as an image in a note, first create a Resource
  // for the attachment. At a minimum, the Resource contains the binary attachment
  // data, an MD5 hash of the binary data, and the attachment MIME type. It can also
  // include attributes such as filename and location.
  Resource resource = new Resource();
  resource.setData(readFileAsData(fileName));
  resource.setMime(mimeType);
  ResourceAttributes attributes = new ResourceAttributes();
  attributes.setFileName(fileName);
  resource.setAttributes(attributes);

  // Now, add the new Resource to the note's list of resources
  note.addToResources(resource);

  // To display the Resource as part of the note's content, include an <en-media>
  // tag in the note's ENML content. The en-media tag identifies the corresponding
  // Resource using the MD5 hash.
  String hashHex = bytesToHex(resource.getData().getBodyHash());

  // The content of an Evernote note is represented using Evernote Markup Language
  // (ENML). The full ENML specification can be found in the Evernote API Overview
  // at http://dev.evernote.com/documentation/cloud/chapters/ENML.php
  String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
      + "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">"
      + "<en-note>"
      + "<span style=\"color:green;\">Here's the Evernote logo:</span><br/>"
      + "<en-media type=\"image/png\" hash=\"" + hashHex + "\"/>"
      + "</en-note>";
  note.setContent(content);

  // Finally, send the new note to Evernote using the createNote method
  // The new Note object that is returned will contain server-generated
  // attributes such as the new note's unique GUID.
  Note createdNote = noteStore.createNote(authToken, note);
  newNoteGuid = createdNote.getGuid();

  System.out.println("Successfully created a new note with GUID: " + newNoteGuid);
  System.out.println();
}

/**
 * Search a user's notes and display the results.
 */
private void searchNotes()
  throws Exception
{
  // Searches are formatted according to the Evernote search grammar. Learn more at
  // http://dev.evernote.com/documentation/cloud/chapters/Searching_notes.php

  // In this example, we search for notes that have the term "EDAMDemo" in the title.
  // This should return the sample note that we created in this demo app.
  String query = "intitle:EDAMDemo";

  // To search for notes with a specific tag, we could do something like this:
  // String query = "tag:tagname";

  // To search for all notes with the word "elephant" anywhere in them:
  // String query = "elephant";

  NoteFilter filter = new NoteFilter();
  filter.setWords(query);
  filter.setOrder(NoteSortOrder.UPDATED.getValue());
  filter.setAscending(false);

  // Find the first 50 notes matching the search
  System.out.println("Searching for notes matching query: " + query);
  NoteList notes = noteStore.findNotes(authToken, filter, 0, 50);
  System.out.println("Found " + notes.getTotalNotes() + " matching notes");

  Iterator<Note> iter = notes.getNotesIterator();
  while (iter.hasNext()) {
    Note note = iter.next();
    System.out.println("Note: " + note.getTitle());

    // Note objects returned by findNotes() only contain note attributes
    // such as title, GUID, creation date, update date, etc. The note content
    // and binary resource data are omitted, although resource metadata is included.
    // To get the note content and/or binary resources, call getNote() using the note's GUID.
    Note fullNote = noteStore.getNote(authToken, note.getGuid(), true, true, false, false);
    System.out.println("Note contains " + fullNote.getResourcesSize() + " resources");
    System.out.println();
  }
}

/**
 * Update the tags assigned to a note. This method demonstrates
 * how only modified fields need to be sent in calls to updateNote.
 */
private void updateNoteTag()
  throws Exception
{
  // When updating a note, it is only necessary to send Evernote the
  // fields that have changed. For example, if the Note that you
  // send via updateNote does not have the resources field set, the
  // Evernote server will not change the note's existing resources.
  // If you wanted to remove all resources from a note, you would
  // set the resources field to a new List<Resource> that is empty.

  // If you are only changing attributes such as the note's title or tags,
  // you can save time and bandwidth by omitting the note content and resources.

  // In this sample code, we fetch the note that we created earlier, including
  // the full note content and all resources. A real application might
  // do something with the note, then update a note attribute such as a tag.
  Note note = noteStore.getNote(authToken, newNoteGuid, true, true, false, false);

  // Do something with the note contents or resources...

  // Now, update the note. Because we're not changing them, we unset
  // the content and resources. All we want to change is the tags.
  note.unsetContent();
  note.unsetResources();

  // We want to apply the tag "TestTag"
  note.addToTagNames("TestTag");

  // Now update the note. Because we haven't set the content or resources,
  // they won't be changed.
  noteStore.updateNote(authToken, note);
  System.out.println("Successfully added tag to existing note");

  // To prove that we didn't destory the note, let's fetch it again and
  // verify that it still has 1 resource.
  note = noteStore.getNote(authToken, newNoteGuid, false, false, false, false);
  System.out.println("After update, note has " + note.getResourcesSize() + " resource(s)");
  System.out.println("After update, note tags are: ");
  for (String tagGuid : note.getTagGuids()) {
    Tag tag = noteStore.getTag(authToken, tagGuid);
    System.out.println("* " + tag.getName());
  }

  System.out.println();
}

/**
 * Helper method to read the contents of a file on disk and create a new Data object.
 */
private static Data readFileAsData(String fileName) throws Exception {

  // Read the full binary contents of the file
  FileInputStream in = new FileInputStream(fileName);
  ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
  byte[] block = new byte[10240];
  int len;
  while ((len = in.read(block)) >= 0) {
    byteOut.write(block, 0, len);
  }
  in.close();
  byte[] body = byteOut.toByteArray();

  // Create a new Data object to contain the file contents
  Data data = new Data();
  data.setSize(body.length);
  data.setBodyHash(MessageDigest.getInstance("MD5").digest(body));
  data.setBody(body);

  return data;
}

/**
 * Helper method to convert a byte array to a hexadecimal string.
 */
public static String bytesToHex(byte[] bytes) {
  StringBuilder sb = new StringBuilder();
  for (byte hashByte : bytes) {
    int intVal = 0xff & hashByte;
    if (intVal < 0x10) {
      sb.append('0');
    }
    sb.append(Integer.toHexString(intVal));
  }
  return sb.toString();
}
}
