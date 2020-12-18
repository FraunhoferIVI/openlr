# How to set up routable OSM database

Instructions for setting up a routable PostgreSQL database

## Table of Contents
1. [General Info](#general-info)
2. [Programms and extensions needed](#programms)
3. [Get OSM data](#osmdata)
4. [Setup database](#database_setup)
5. [FAQs](#faqs)


### General Info
***
This guide shows you how to set up a usable PostgreSQL database for the reference implementation at hand.

### Programms and extensions needed
***
#### Programms
+ [PostgreSQL database](https://www.postgresql.org)
+ [OSM2PGSQL](https://osm2pgsql.org)
+ Database client, e.g. [DBeaver](https://dbeaver.com), [pgAdmin](https://www.pgadmin.org)
#### Extensions
+ [PostGIS](http://postgis.net)
+ [pgRouting](https://pgrouting.org)
+ [hstore](hstore)

### Get OSM data 
***
Load [OpenStreetMap](https://www.openstreetmap.org/#map=6/51.330/10.453) from the OSM Exporter or the [Geofabrik Downloader](https://download.geofabrik.de). The data must be a *.osm.pdf file. 

For example: hamburg-latest.osm.pbf

### Setup database
***
1. [Download](https://www.postgresql.org/download/) PostgreSQL and add PostGIS extension.
2. [Create](https://www.postgresql.org/docs/9.0/tutorial-createdb.html) database
3. Run the following commands in you database client: 

+ Create postgis extension
  ```sql 
  CREATE EXTENSION postgis;
  ```
+ Create pgRouting extension
  ```sql 
  CREATE EXTENSION pgrouting;
  ```
+ Create hstore extension
  ```sql 
  CREATE EXTENSION hstore;
  ```
4. Load OSM data in your databse using terminal
```bash
Osm2pgsql -d dbname -U username osmpbffilename.osm.pbf --hstore
```
If you have a larger file, e.g. OSM data for Germany use the following command: 
```bash
--Osm2pgsql -d dbname -U username osmpbffilename.osm.pbf --slim --hstore
```

After loading the OSM file to your databse it should look like this: 

![LoadedOSMData](src/main/resources/Screenshots/osm2pgsql.png)

1. Run one of the scripts in /src/main/resources/SQL in your database client to generate a routable OSM road network:
* If you want to use **all of the OSM** data use the [SQL_Script.sql](src/main/resources/SQL/SQL_Script.sql)
* if you want to **clip the data to an area** use the [SQL_Script_clip2bbox.sql](src/main/resources/SQL/SQL_Script.sql). The clipping area must be in the database in the public schema. The table containing the clipping area must be named **bbox**.
* Before running set the information in the metadata table, example for Hamburg: 
```sql
INSERT INTO openlr.metadata(map_name, map_owner) VALUES 
('Hamburg', 'OSM');
```

After running one of the SQL scripts the openlr schema should look like this: 

![OpenLRShema](src/main/resources/Screenshots/tables.png)





