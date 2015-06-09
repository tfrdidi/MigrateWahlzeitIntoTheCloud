# Wahlzeit - An Open Source Package to create Photo Rating Sites



## PART I - INTRODUCTION

Wahlzeit is an open source web application that lets users upload photos and rate photos of other users, usually on a 1..10 scale. Users get to present their best photos and learn what other users thought of their photos. 

Wahlzeit is used to teach agile methods and open source software development at the Professorship of Open Source Software at the University of Erlangen.

It is an easy-to-learn yet complete Java web application that is available under the GNU Affero Public License v3 license, see the [LICENSE.txt](/LICENSE.txt) file.

Wahlzeit is a Google App Engine app, that can be used on your local machine or be deployed to Google App Engine to reach it online.

For more information, see http://github.com/dirkriehle/wahlzeit and http://osr.cs.fau.de.



## PART II: WAHLZEIT SETUP

**First download repository:**
  1. Fulfill prerequisites: install **Java JDK**, set **JAVA_HOME**, and install **git**
  2. open console and change to the directory where the directory ```MigrateWahlzeitIntoTheCloud``` containing the sourcecode should be created
  3. ```git clone https://github.com/tfrdidi/MigrateWahlzeitIntoTheCloud.git```


### Run Wahlzeit on your local machine
  1. ```cd <projectsDirectory>/MigrateWahlzeitIntoTheCloud```
  2. ```./gradlew appengineRun```
  3. wait until gradle and project dependencies are downloaded and the local instance is started

Open [http://localhost:8080](http://localhost:8080) to use Wahlzeit on your local machine.


### Upload Wahlzeit to Google App Engine

**Create an Google App Engine instance:**
  1. if you don't have already one, create a Google account
  2. go to https://console.developers.google.com and login with your Google account
  3. now you are in the developers console, there select "create a project"
    1. choose a project name, which used later as *your-project-ID*
    2. accept the terms of service

**Configure your repository and upload Wahlzeit**
  5. store your *your-project-ID* in your repository:
    1. open the file [/MigrateWahlzeitIntoTheCloud/src/main/webapp/WEB-INF/appengine-web.xml](/src/main/webapp/WEB-INF/appengine-web.xml)
    2. replace the project name with *your-project-ID*: \<application\>*your-project-ID*\</application\>
    3. save and close the appengine-web.xml
  6. ```./gradlew appengineUpdate```
  7. a browser window pops up and asks for permission, accept it
  8. copy the code from the following browser window to your gradle console

Open https://*your-project-ID*.appspot.com to use Wahlzeit on Google App Engine.

Done!



