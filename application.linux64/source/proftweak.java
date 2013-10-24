import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.Set; 
import java.util.Arrays; 
import java.io.*; 
import java.awt.*; 
import java.awt.font.*; 
import g4p_controls.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class proftweak extends PApplet {










JSONObject json;
ArrayList MasterVariableArray;
ArrayList MasterTextBoxArray;
ArrayList MasterTextLabelArray;

String[] commonSettings = new String[0];

GSlider sdrBack;
GButton ShowAllButton;
GButton SaveTempButton;
GButton SaveButton;
GButton SaveNewButton;

GTextField newProfileTextBox;
GLabel statusTitle;
GDropList profileSelector;

GTextArea profilenotes;

long startTimer = 0;
long statusTimer = 0;

float Voffset = 0;
int indent = 0;
int lastSlider = 0;
int itemSpacing = 15;

int buttonX = 410;
int buttonWidth = 240;

int windowHeight = 720;

boolean showall = false;
boolean drawSelectBox = false;
float selectBoxY;
boolean unSavedChanges = false;

String profileDir;
String pathForCurrentProfile;

boolean badThingHappenedSoShuttingDown = false;

Font labelFont = new Font("Dialog", Font.PLAIN, 13);
Font boldLabelFont = new Font("Dialog", Font.PLAIN, 13);
Font entryFont = new Font("Dialog", Font.BOLD, 13);


//main structure for storing parsed json info
class JSONEntry { 

  String name;
  ArrayList dataObjectArray;
  JSONObject JSObject;

  JSONEntry(String i_name, ArrayList i_dataObjectArray, JSONObject i_JSObject)
  {    
    name = i_name;
    dataObjectArray = i_dataObjectArray;
    JSObject = i_JSObject;
  }
}

//scroll stuff
public void handleSliderEvents(GValueControl slider, GEvent event) { 

  //boost frame rate whenever we get an event
  makeAppResponsive();

  Voffset ++;
  int slideMove = slider.getValueI() - lastSlider;

  for (int y = 0; y < MasterTextBoxArray.size(); y++)
  {
    GAbstractControl controlToScroll = (GAbstractControl)MasterTextBoxArray.get(y);
    controlToScroll.moveTo(controlToScroll.getX(), controlToScroll.getY() - slideMove);
  }

  for (int y = 0; y < MasterTextLabelArray.size(); y++)
  {
    GAbstractControl controlToScroll = (GAbstractControl)MasterTextLabelArray.get(y);
    controlToScroll.moveTo(controlToScroll.getX(), controlToScroll.getY() - slideMove);
  }

  lastSlider = slider.getValueI();
  selectBoxY = selectBoxY - slideMove;
}

//set color of text fields depending on if they're boolean, string or number 
public void setEntryColor(GEditableTextControl myTextArea)
{
  if (myTextArea.getText().equals("true") || myTextArea.getText().equals("false"))
  {
    myTextArea.setLocalColorScheme(7);
  }
  else
  {
    try
    {
      Float.valueOf(myTextArea.getText()).floatValue();
      myTextArea.setLocalColorScheme(5);
    }
    catch(Exception e) {
      myTextArea.setLocalColorScheme(1);
    } 
    finally {
    }
  }
}

//make app response once events are detected - doing this because java / processing SUCK CPU if allowed to
public void makeAppResponsive()
{ 
  frameRate(60);
  startTimer = millis();
}


//generates list of makerware profiles
public ArrayList getProfiles() {

  ArrayList<File> filesList = new ArrayList<File>();
  try
  {
    String folderPath = profileDir;
    if (folderPath != null) {
      File file = new File(folderPath);
      File[] files = file.listFiles();
      for (int i = 0; i < files.length; i++) {
        if (files[i].isDirectory())
        {
          filesList.add(files[i]);
        }
      }
    }
  }
  catch(Exception e) {   
    if (badThingHappenedSoShuttingDown == false)
    {
      G4P.showMessage(this, "I couldn't find your profile folder.  Try setting it manually in proftweak.json.", "Uh oh", G4P.WARNING);
      badThingHappenedSoShuttingDown = true;
      exit();
    }
  } 
  finally {
  }


  return(filesList);
}


//updates profile selecting gui - optionally choosing specified profile
public void displayProfileList(String profileToSelect)
{
  String x[] = new String[1000];
  int indexOfItemToSelect = 1;

  int arrayIndex = 0;
  ArrayList<File> filesList = getProfiles();
  for (File f : filesList) {
    println(f.getName());    
    x[arrayIndex] = f.getName();
    if (f.getName().equals(profileToSelect)) indexOfItemToSelect = arrayIndex;
    arrayIndex++;
  }

  profileSelector.setItems(x, indexOfItemToSelect);

  if (filesList.size() == 0)
  {
    if (badThingHappenedSoShuttingDown == false)
    {
      G4P.showMessage(this, "Before using ProfTweak - you need to create a custom profile in Makerware. If you've already done this - try setting your profile folder manually in proftweak.json.", "Uh oh", G4P.WARNING);
      badThingHappenedSoShuttingDown = true;
      exit();
    }
  }
}

//initial app setup
public void setup() {


  JSONObject settings = new JSONObject();

try {
    settings = loadJSONObject("proftweak.json");
    }
    catch(Exception e) {
      if (badThingHappenedSoShuttingDown == false)
      {
        G4P.showMessage(this, "Looks like there's a formatting problem in proftweak.json - were you playing with stuff? (see the docs for tips)", "Uh oh", G4P.WARNING);
        badThingHappenedSoShuttingDown = true;
        exit();
      }
    } 
    finally {
    }

  profileDir = settings.getString("profileFolder");

  JSONArray commonSettingsJS =  (settings.getJSONArray("commonSettings"));

  for (int y = 0; y < commonSettingsJS.size(); y++)
  {  
    print((String)commonSettingsJS.getString(y));
    commonSettings = append(commonSettings, (String)commonSettingsJS.getString(y));
  }

  if (profileDir.equals(""))
  {
    if (platform == MACOSX) {
      profileDir = System.getProperty("user.home")+"/Things/Profiles/";
    }
    else if (platform == WINDOWS) {
      println("WINDOWS");
      profileDir = System.getProperty("user.home")+"/My Things/Profiles/";
    }
    else if (platform == LINUX) {
      println("LINUX");
    }
  }

  print(profileDir);


  size(669, windowHeight);
  frameRate(60);
  noSmooth();
  background(0);
  fill(255);

  G4P.setGlobalColorScheme(15);

  GLabel profileLabel = new GLabel(this, 388, 84, 300, 18, "Current profile:"); 
  profileLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
  profileLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  profileLabel.setLocalColorScheme(9);
  profileSelector = new GDropList(this, 390, 103, 260, 18 * (5 + 1), 5);
  profileSelector.setLocalColorScheme(6);

  //status line
  statusTitle = new GLabel(this, 388, 650, 280, 17, "");
  statusTitle.setFont(new Font("Dialog", Font.BOLD, 14));
  statusTitle.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
  statusTitle.setLocalColorScheme(7);

  //did we get a profile name on the command line?
  String requestedProfileName = "";
  if (args.length > 0)
  {      
    try {
      String pathDelim = "/";
      if (platform == WINDOWS) pathDelim = "\\";
      requestedProfileName = args[args.length - 1]; 
      String fileEntities[] = split(requestedProfileName, pathDelim);
      requestedProfileName = fileEntities[fileEntities.length - 2];
    }

    catch(Exception e) {
    } 
    finally {
    }
  }

  displayProfileList(requestedProfileName);

  //scroll bar
  sdrBack = new GSlider(this, 360, 442, 185, 18, 18);
  sdrBack.setLimits(0, 255, 0);
  sdrBack.setRotation(-PI/2);
  sdrBack.setTextOrientation(G4P.ORIENT_RIGHT);
  sdrBack.setEasing(0);
  sdrBack.setLocalColorScheme(6);


  GButton RefreshButton = new GButton(this, 567, 83, 85, 17, "Refresh List");
   RefreshButton.tag = "RefreshButton";
  
  SaveButton = new GButton(this, buttonX, 126, buttonWidth, 30, "Save Current Profile");
  SaveButton.tag = "SaveButton";

  GLabel newProfNameTitle = new GLabel(this, 388, 165, 230, 17, "Name for new profile:"); 
  newProfNameTitle.setFont(new Font("Dialog", Font.PLAIN, 14));
  newProfNameTitle.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  newProfNameTitle.setLocalColorScheme(9);

  newProfileTextBox = new GTextField(this, 390, 183, 260, 17);
  newProfileTextBox.setFont(new Font("Dialog", Font.PLAIN, 13));    
  newProfileTextBox.setLocalColorScheme(3);
  newProfileTextBox.tag = "newprofilename";
  setEntryColor(newProfileTextBox);

  SaveNewButton = new GButton(this, buttonX, 205, buttonWidth, 30, "Save As New Profile");
  SaveNewButton.tag = "SaveNewButton";

  SaveTempButton = new GButton(this, buttonX, 255, buttonWidth, 30, "Save As 'ProfTweakTemp' Profile");
  SaveTempButton.tag = "SaveTempButton";

  GLabel profNotesLabel = new GLabel(this, 388, 300, 200, 17, "Notes:"); 
  profNotesLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
  profNotesLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  profNotesLabel.setLocalColorScheme(9);

  profilenotes = new GTextArea(this, 390, 317, 260, 87);  
  profilenotes.setFont(new Font("Dialog", Font.PLAIN, 13));    
  profilenotes.setLocalColorScheme(5);
  profilenotes.tag = "profilenotes";

  ShowAllButton = new GButton(this, buttonX, 430, buttonWidth, 30, "Show All Profile Settings");
  ShowAllButton.tag = "ShowAllButton";
  ShowAllButton.setText("Show All Profile Settings");

  GButton ShowDocs = new GButton(this, buttonX, 480, buttonWidth, 30, "Profile Reference (makerbot.com)");
  ShowDocs.tag = "ShowDocs";

  GButton showProfiles = new GButton(this, buttonX, 530, buttonWidth, 30, "Open Profile Folder");
  showProfiles.tag = "showProfiles";

  GButton nothingButton = new GButton(this, buttonX, 580, buttonWidth, 30, "Visit nothinglabs.com (project info)");
  nothingButton.tag = "nothingButton";


  GLabel appTitle = new GLabel(this, 385, 20, 200, 17, "ProfTweak"); 
  appTitle.setFont(new Font("Dialog", Font.BOLD, 20));
  appTitle.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  appTitle.setLocalColorScheme(9);

  GLabel appDesc = new GLabel(this, 385, 40, 300, 14, "A MakerWare profile editor (v1.1)"); 
  appDesc.setFont(new Font("Dialog", Font.BOLD, 14));
  appDesc.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  appDesc.setLocalColorScheme(9);

  GLabel appCredit = new GLabel(this, 385, 55, 300, 18, "By Rich Olson / nothinglabs.com"); 
  appCredit.setFont(new Font("Dialog", Font.BOLD, 14));
  appCredit.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  appCredit.setLocalColorScheme(9);

  MasterVariableArray = new ArrayList();
  MasterTextLabelArray = new ArrayList(); 
  MasterTextBoxArray = new ArrayList(); 

  if (setJSONPath()) parseAndRenderJSON();
}

public boolean setJSONPath()
{
  //we're trying to load a new file - so changes are being tossed
  unSavedChanges = false; 
  try
  {

    pathForCurrentProfile = profileDir + profileSelector.getSelectedText() + "/miracle.json";
    print(pathForCurrentProfile);

    json = loadJSONObject(pathForCurrentProfile);
    return (true);
  }
  catch(Exception e) {
    if (badThingHappenedSoShuttingDown == false)
    {
      showMessage("Profile Load FAILED", 9000);
      G4P.showMessage(this, "Error: Invalid profile (missing miracle.json).  This can also be caused by your profile path not being set correctly.", "Uh oh", G4P.WARNING);
    }
    return (false);
  } 
  finally {
  }
}

public void parseAndRenderJSON()
{

  for (int y = 0; y < MasterTextBoxArray.size(); y++)
  {
    GAbstractControl controlToHide = (GAbstractControl)MasterTextBoxArray.get(y);
    controlToHide.setVisible(false);
  }

  for (int y = 0; y < MasterTextLabelArray.size(); y++)
  {
    GAbstractControl controlToHide = (GAbstractControl)MasterTextLabelArray.get(y);
    controlToHide.setVisible(false);
  }

  MasterVariableArray = new ArrayList();

  lastSlider = 0;
  sdrBack.setLimits(0, 255, 0);
  drawSelectBox = false;

  indent = 0;
  Voffset = 0;

  profilenotes.setText("");

  if (json != null) ParseJSONObjectToMainArray(json);

  int scrollLimit = (int)((Voffset * itemSpacing) - windowHeight) + 150;
  if (scrollLimit < 0) scrollLimit = 150;
  sdrBack.setLimits(0, scrollLimit, 0);
}

public void handleDropListEvents(GDropList list, GEvent event) {

  makeAppResponsive();
  if (setJSONPath()) parseAndRenderJSON();
}


public void keyPressed() {
  if (key == ESC) {
    key = 0;
  }
}

public void draw() {

  background(0);
  fill(255);

  if (drawSelectBox == true)
  {
    fill(255, 0, 0, 63);
    rect(20, selectBoxY, 270, itemSpacing);
  }

  if ((millis() - startTimer) > 10000)
  {
    frameRate(4);
  }
  else
  {
    //only update status if we're active to save CPU load
    if ((millis() - statusTimer) > 0)
    {
      statusTitle.setText("");
      if (unSavedChanges == true) statusTitle.setText("Change Made (Remember To Save)");
    }
  }
}


//re-use text fields we've hidden
public GTextField getTextFieldFromArray()
{

  GTextField textFieldToReturn;

  for (int z=0; z < MasterTextBoxArray.size(); z++)
  {
    textFieldToReturn = (GTextField)MasterTextBoxArray.get(z);
    if (!textFieldToReturn.isVisible()) return textFieldToReturn;
  }

  //make new text field if we couldn't find a hidden one
  print("making new text field");
  textFieldToReturn = new GTextField(this, 250, Voffset * itemSpacing, 100, 16);
  MasterTextBoxArray.add(textFieldToReturn);

  print (textFieldToReturn);


  return (textFieldToReturn);
}


public GLabel getLabelFromArray()
{

  GLabel labelToReturn;

  for (int z=0; z < MasterTextLabelArray.size(); z++)
  {
    labelToReturn = (GLabel)MasterTextLabelArray.get(z);
    if (!labelToReturn.isVisible()) return labelToReturn;
  }

  //make new label if we couldn't find a hidden one
  print("making new label");
  labelToReturn = new GLabel(this, 10 + indent * 15, (Voffset * itemSpacing), 230, 15, "");
  MasterTextLabelArray.add(labelToReturn);

  print (labelToReturn);

  return (labelToReturn);
}


public void addItem(JSONEntry JSONAddition)
{

  Voffset = Voffset + 1;

  MasterVariableArray.add(JSONAddition);

  GLabel lblStyleInstr;
  lblStyleInstr = getLabelFromArray(); 

  lblStyleInstr.moveTo(10 + indent * 15, (Voffset * itemSpacing));
  lblStyleInstr.setVisible(true);
  lblStyleInstr.setText(JSONAddition.name);
  lblStyleInstr.setFont(labelFont);
  lblStyleInstr.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  lblStyleInstr.setLocalColorScheme(9);


  String value = "" + JSONAddition.dataObjectArray.get(0) + "";                      

  GTextField txaSample;
  txaSample = getTextFieldFromArray();
  txaSample.moveTo(250, Voffset * itemSpacing);
  txaSample.setVisible(true);

  txaSample.setFont(entryFont);    
  txaSample.setText(value);
  txaSample.setLocalColorScheme(3);
  txaSample.tag = "" + MasterVariableArray.size();
  setEntryColor(txaSample);
}

public void addLabel(String TextLabel)
{

  Voffset = Voffset + 1;
  //if (indent == 1) Voffset = Voffset + 0.25;

  GLabel lblStyleInstr;
  lblStyleInstr = getLabelFromArray(); 

  lblStyleInstr.moveTo(10 + indent * 15, (Voffset * itemSpacing));
  lblStyleInstr.setVisible(true);

  lblStyleInstr.setText(TextLabel);
  lblStyleInstr.setFont(boldLabelFont);
  lblStyleInstr.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  lblStyleInstr.setLocalColorScheme(7);
}



public void handleTextEvents(GEditableTextControl textarea, GEvent event) {

  makeAppResponsive();
  println(event + " " +textarea);  
  if (event == GEvent.GETS_FOCUS)
  {
    drawSelectBox = true;
    selectBoxY = textarea.getY();
  }

  if (textarea.tag == "newprofilename")
  {
    drawSelectBox = false;
  }


  if (textarea.tag == "profilenotes")
  {
    drawSelectBox = false;
    json.setString("myProfileNotes", textarea.getText());
  }  

  if (event == GEvent.CHANGED)
  {
    if (textarea.tag != "newprofilename") unSavedChanges = true;
    JSONEntry entryBeingEditted = (JSONEntry)MasterVariableArray.get(Integer.parseInt(textarea.tag) - 1);

    println (textarea.getText());

    //bolean??
    if (textarea.getText().equals("true"))
    {
      entryBeingEditted.JSObject.setBoolean(entryBeingEditted.name, true);
      println ("set to true");
    }
    else
    {
      if (textarea.getText().equals("false"))
      {
        entryBeingEditted.JSObject.setBoolean(entryBeingEditted.name, false);
        println ("set to false");
      }
      else
      {

        //maybe it's an int...
        boolean itsanINT = false;
        try
        {
          Integer.parseInt(textarea.getText());
          itsanINT = true;
        }
        catch(Exception e) {
          print("not an int");
        } 
        finally {
        }

        if (itsanINT == true)
        {
          entryBeingEditted.JSObject.setInt(entryBeingEditted.name, Integer.parseInt(textarea.getText()));
          println ("its a integer");
        }  
        else
        {

          //maybe it's a float...
          boolean itsaFLOAT = false;
          try
          {
            Float.valueOf(textarea.getText()).floatValue();
            itsaFLOAT = true;
          }
          catch(Exception e) {
            println("not a float");
          } 
          finally {
          }

          if (itsaFLOAT == true)
          {
            entryBeingEditted.JSObject.setDouble(entryBeingEditted.name, Float.valueOf(textarea.getText()).floatValue());
            println ("its a float");
          }  

          //it's a string
          else
          {
            entryBeingEditted.JSObject.setString(entryBeingEditted.name, textarea.getText());
          }
        }
      }
    }

    setEntryColor(textarea);
  }
}

public Object[] filterArray(Object[] myNames)
{

  Object[] filteredNames = new Object[0];
  for (int z = 0; z < commonSettings.length; z++)
  {
    String setting = commonSettings[z];    
    for (int x = 0; x < myNames.length; x++)
    {
      if (myNames[x].equals(setting))
      {

        filteredNames = (Object [])append(filteredNames, myNames[x]);
        //add spacers
        if (z < commonSettings.length -1)
        {      
          if ((commonSettings[z + 1]).equals("-EMPTY_SPACE-")) filteredNames = (Object [])append(filteredNames, "-EMPTY_SPACE-");
        }
      }
    }
  }
  return filteredNames;
}


public void ParseJSONObjectToMainArray(JSONObject JSObject)
{    

  
  indent = indent + 1;

  Set i = JSObject.keys();

  Object[] myNames = i.toArray(new String[0]);

  if (showall == true)
  {
    Arrays.sort( myNames );
  }
  else
  {
    myNames = filterArray(myNames);
  }


  for (int x = 0; x < myNames.length; x++)
  {

    if (((String)myNames[x]).equals("-EMPTY_SPACE-")) addLabel("");

    if (((String)myNames[x]).equals("myProfileNotes"))
    {
      profilenotes.setText(JSObject.getString((String)myNames[x]));
    }
    else  
    {

      //Number value
      try
      {    
        ArrayList DataObjectArray = new ArrayList();
        DataObjectArray.add(JSObject.getFloat((String)myNames[x]));
        JSONEntry JSONAddition = new JSONEntry((String)myNames[x], DataObjectArray, JSObject);
        //println(JSONAddition.name+ "  " +JSONAddition.dataObjectArray.get(0));
        addItem(JSONAddition);
      }
      catch(Exception e) {
      } 
      finally {
      }

      //Boolean
      try
      {

        ArrayList DataObjectArray = new ArrayList();
        DataObjectArray.add(JSObject.getBoolean((String)myNames[x]));
        JSONEntry JSONAddition = new JSONEntry((String)myNames[x], DataObjectArray, JSObject);
        //println(JSONAddition.name+ "  " +JSONAddition.dataObjectArray.get(0));
        addItem(JSONAddition);
      }
      catch(Exception e) {
      } 
      finally {
      }

      //string
      try
      {
        ArrayList DataObjectArray = new ArrayList();
        DataObjectArray.add(JSObject.getString((String)myNames[x]));
        JSONEntry JSONAddition = new JSONEntry((String)myNames[x], DataObjectArray, JSObject);
        //println(JSONAddition.name+ "  " +JSONAddition.dataObjectArray.get(0));
        addItem(JSONAddition);
      }
      catch(Exception e) {
      } 
      finally {
      }


      //JSON Object
      try
      {
        JSONObject JSOFromEntryObject = JSObject.getJSONObject((String)myNames[x]);
        addLabel((String)myNames[x]);
        //println((String)myNames[x]);  
        ParseJSONObjectToMainArray(JSOFromEntryObject);
      }
      catch(Exception e) {
      } 
      finally {
      }

      //JSON Array
      try
      {
        JSONArray JSArrayFromEntryObject = JSObject.getJSONArray((String)myNames[x]);

        for (int y = 0; y < JSArrayFromEntryObject.size(); y++)
        {
          try
          {
            JSONObject objectFromArray = JSArrayFromEntryObject.getJSONObject(y);
            addLabel((String)myNames[x] + " (" + y + ")");
            ParseJSONObjectToMainArray(objectFromArray);
          }

          catch(Exception e) {
          } 
          finally {
          }
        }
      }
      catch(Exception e) {
      } 
      finally {
      }
    }
  }

  indent = indent - 1;  

  showMessage("Profile Loaded", 3000);
}   

public void showMessage(String message, int showTime)
{
  print (message);
  statusTitle.setText(message);
  statusTimer = millis() + showTime;
}


public void handleButtonEvents(GButton button, GEvent event) { 

  makeAppResponsive();

  if (event == GEvent.CLICKED) {
    print ("button clicked:");
    println (button);
    if (button.tag == "SaveButton")
    {
      try {
        saveJSONObject(json, pathForCurrentProfile);
        unSavedChanges = false;
        showMessage("Profile Saved", 3000);
      } 
      catch (Exception e) {
        G4P.showMessage(this, "Save Failed...", "Uh oh", G4P.WARNING);
        showMessage("Save Failed...", 3000);
      } 
      finally {
      }
    }

    if (button.tag == "RefreshButton")
    {
      String currentProfileName = profileSelector.getSelectedText();
      displayProfileList(currentProfileName);
      if (!profileSelector.getSelectedText().equals(currentProfileName))
      {
         if (setJSONPath()) parseAndRenderJSON(); 
      }
      
      showMessage("Profile List Refreshed", 3000);
    }

    if (button.tag == "showProfiles")
    {
      String launchProfile = "file:///" + profileDir; 
      launchProfile = launchProfile.replace("\\", "/");  
      launchProfile = launchProfile.replaceAll(" ", "%20");
      print (launchProfile);

      link(launchProfile);
    }


    if (button.tag == "ShowDocs")
    {
      link("http://www.makerbot.com/support/makerware/documentation/slicer/");
    }

    if (button.tag == "SaveTempButton" || button.tag == "SaveNewButton")
    {
      String saveNameText = "ProfTweakTemp";

      if (button.tag == "SaveNewButton")
      {
        //unSavedChanges = false;
        println("saving new profile:");
        saveNameText = newProfileTextBox.getText(); 
        saveNameText = saveNameText.replace("\\", "");
        saveNameText = saveNameText.replace("/", "");
        saveNameText = saveNameText.replace("%", "");
        saveNameText = saveNameText.replace("&", "");
        saveNameText = saveNameText.replace("*", "");
        saveNameText = saveNameText.replace("?", "");
        saveNameText = saveNameText.replace(":", "");
        saveNameText = saveNameText.replace(">", "");
        saveNameText = saveNameText.replace("<", "");
        saveNameText = saveNameText.replace("|", "");
        saveNameText = saveNameText.replace("\"", "");

        newProfileTextBox.setText(saveNameText);
        
        println(saveNameText);
      }

      try {

        if ((newProfileTextBox.getText().equals("") || newProfileTextBox.getText().equals(" ")) && button.tag == "SaveNewButton")
        { 
          G4P.showMessage(this, "Please enter a valid profile name.", "Uh oh", G4P.WARNING);
        }
        else
        {
          unSavedChanges = false;     
          File f = new File(profileDir + saveNameText); 
          f.mkdir();
          saveJSONObject(json, profileDir + saveNameText + "/miracle.json");


          JCopy cp = new JCopy(); 
          File f1 = new File(profileDir + profileSelector.getSelectedText() + "/end.gcode");
          File f2 = new File(profileDir + saveNameText + "/end.gcode");       
          cp.copyFile(f1, f2);

          f1 = new File(profileDir + profileSelector.getSelectedText() + "/start.gcode");
          f2 = new File(profileDir  + saveNameText +  "/start.gcode");       
          cp.copyFile(f1, f2);

          f1 = new File(profileDir + profileSelector.getSelectedText() + "/profile.json");
          f2 = new File(profileDir + saveNameText + "/profile.json");       
          cp.copyFile(f1, f2);

          if (button.tag == "SaveNewButton")
          {
            displayProfileList(newProfileTextBox.getText());
            newProfileTextBox.setText("");
            setJSONPath();
          }
          showMessage("Profile Saved", 3000);
        }
      } 
      catch (Exception e) {
        showMessage("Save Failed.", 3000);
        G4P.showMessage(this, "Save Failed! Your original profile may have been missing a required file.", "Uh oh", G4P.WARNING);
      } 
      finally {
      }
    }



    if (button.tag == "ShowAllButton")
    {
      showall = !showall;  
      parseAndRenderJSON();
      showMessage("", 0);  //prevents "profile loaded" from showing
      if (showall == true)
      {
        ShowAllButton.setText("Show Only Common Profile Settings");
      }
      else
      {
        ShowAllButton.setText("Show All Profile Settings");
      }
    }


    if (button.tag == "nothingButton")
    {
      link("http://www.nothinglabs.com");
    }
  }
}


public class JCopy {

  JCopy() {
  }

  public void copyFile(File in, File out) throws Exception {
    FileInputStream fis  = new FileInputStream(in);
    FileOutputStream fos = new FileOutputStream(out);
    byte[] buf = new byte[1024];
    int i = 0;
    while ( (i=fis.read (buf))!=-1) {
      fos.write(buf, 0, i);
    }
    fis.close();
    fos.close();
  }
}  

  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "proftweak" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
