# Setup

This setup guides you through the installations needed to run Here2OSM.
Examples are for Windows(x64).

## Index
1. [IDE](#IDE)
2. [QGIS](#QGIS)
3. [PostgreSQL](#PostgreSQL)
4. [DB client](#DB-client)
5. [HERE-Api-Key](#HERE-Api-Key)
6. [osm2pgsql](#osm2pgsql)
7. [OSM data](#OSM-data)
8. [DB setup](#DB-setup)
9. [Maven](#Maven)
10. [Build](#Build)


### 1. IDE
This code works best with [IntelliJ](https://www.jetbrains.com/de-de/idea/download/#section=windows), but feel free to use your favourite IDE

### 2. [QGIS](https://www.qgis.org/en/site/forusers/download.html) <- right click and open in new window
* Use the first Link under *Download for Windows*

### 3. [PostgreSQL](https://www.enterprisedb.com/downloads/postgres-postgresql-downloads)
* Click the first Link in the Windows column
* In the Application Stack Builder, choose your PostgreSQL > install the latest PostGIS Bundle (under *Spatial Extensions*)

### 4. DB client
This setup is written for [DBeaver](https://dbeaver.io/download/), but again you can choose which you want.
* Take the 64 bit installer under *Windows*

### 5. [HERE-Api-Key](https://developer.here.com/pricing)
* Sign up for Freemium Plan
* create a REST API Key

### 6. [osm2pgsql](https://osm2pgsql.org/doc/install.html#installing-on-windows)
* Download the prebuild binaries (*osm2pgsql-latest-x64.zip*)
* extract the zip and copy the Path to the bin folder
* Add the bin to your Path
    * type "View advanced system Settings" in the Start Menu and open it
    * click *Environment Variables* in the bottom right, than *Path* in the top  List and *Edit*
    * click *new* in the top right and paste the Path you copied earlyer

### 7. [OSM data](https://download.geofabrik.de/)
* Download a file from the Table (it is highly recomended to just get a City-file for starters)

### 8. DB setup
* Create DB
    * in DBeaver click *New Connection* (top left) and choose *PostgreSQL*, than *Next*
    * insert your PostgreSQL-Password and click *Done*
* Add extensions
    * under *postgres\DB's* doubleclick *postgres*
    * click *SQL* (next to *New Connection*) and write
    ```sql
    create extension postgis;
    create extension pgrouting;
    create extension hstore;
    ```
    * run the script with *run SQL script* (third button to the left)
* Transfer the osm data into DB
    * execute in Command Prompt:
    ```bash
    osm2pgsql -c -d postgres -U postgres -H localhost -W --hstore -S <Path to>\osm2pgsql-bin\default.style <Path to>\<filename>.osm.pbf
    ```
Now your database should look like this:

![LoadedOSMData](src/main/resources/Screenshots/osm2pgsql.png)

* Run one of these two scripts in your DB client to generate a routable OSM road network:
    * If you want to use **all of the OSM** data use [This](src/main/resources/SQL/SQL_Script.sql)
    * If you want to **clip the data to an area** use [That](src/main/resources/SQL/SQL_Script.sql)
        * The clipping area has to be in YourDB\schemas\public\tables and named **bbox**.
* **Before running** set the informations **map_name** and **map_owner** in the fourth last statement:
```sql
INSERT INTO openlr.metadata(map_name, map_owner) VALUES
('Hamburg', 'OSM');
```

When run your DB should look as follows:

![OpenLRShema](src/main/resources/Screenshots/tables.png)

### 9. [Maven](https://maven.apache.org/download.cgi)
* Under *Files* click the Binary zip archive Link and unzip the Downloaded archive
* Add to your Environment Variables:
    * *Variable name:* MAVEN_HOME *value:* **PathTo**\apache-maven-x.x.x
    * %MAVEN_HOME%\bin to the Path variable

### 10. Build
* Clone this project
    * Fork it **(optional)**
    * click *Code* (green button) and copy the HTTP-Adress
    * In a Command Prompt cd to a location you want the project in and execute
    ```bash
    git clone <HTTP-Adress>
    ```
 * Set your **dbname**, **user** and **password** at src\main\java\DataBase\DatasourceConfig.java (line 23+2) and pom.xml (line 195+2)
 * Set your HERE Api key at src\main\java\HereApi\ApiRequest.java (line 37)
 * go back to the Command Prompt and execute
```bash
cd Here2OSM
```
```bash
mvn clean install
```
```bash
java -jar target\here2osm-1.0-SNAPSHOT.jar
```
* supply the bounding box of your map as WGS84 coordinates *([NW latitude],[NW longitude];[SE latitude],[SE longitude])* for example 53.60,9.85,53.50,10.13 (hamburg)