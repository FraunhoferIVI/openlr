CREATE SCHEMA openlr; 

-- Functional Road Class -- 
CREATE TABLE openlr.functional_road_class (
frc_id smallint PRIMARY KEY, 
frc_desc varchar(5));

INSERT INTO openlr.functional_road_class (frc_id,frc_desc) VALUES 
(0,'FRC0')
,(1,'FRC1')
,(2,'FRC2')
,(3,'FRC3')
,(4,'FRC4')
,(5,'FRC5')
,(6,'FRC6')
,(7,'FRC7')
;

-- Form of Way -- 
CREATE TABLE openlr.form_of_way ( 
fow_id smallint PRIMARY KEY, 
fow_desc varchar(30) ); 

INSERT INTO openlr.form_of_way (fow_id,fow_desc) VALUES
(0,'Undefined')
,(1,'Motorway')
,(2,'Multiple Carriage Way')
,(3,'Single Carriage Way')
,(4,'Roundabout')
,(5,'Trafficsquare')
,(6,'Sliproad')
,(7,'Other')
;

-- ########### Clipping data to boundingbox if needed, bounding box must be in database ###############
CREATE TABLE planet_osm_line_clipped AS 
SELECT B.*
FROM "bbox" A, planet_osm_line B
WHERE st_intersects(A.geom, B.way) = true;

-- ######### FILTERD LINE TABLE #################### 
-- filterd table FROM plaent_osm_line 
CREATE TABLE filterd_lines as 
SELECT osm_id, highway, name, tags, oneway, junction, way FROM planet_osm_line_clipped polc WHERE 
highway = 'motorway' OR highway = 'trunk' OR highway = 'motorway_link' OR highway = 'trunk_link'
OR highway = 'primary' OR highway = 'primary_link' OR highway = 'secondary' OR highway = 'secondary_link'
OR highway = 'tertiary' OR highway = 'tertiary_link' OR highway = 'road' OR highway = 'road_link' OR 
highway = 'unclassified' OR highway = 'residential' OR highway = 'living_street';



UPDATE public.filterd_lines SET
    oneway = CASE
        WHEN oneway = 'yes' THEN 'true'
        WHEN oneway = 'no' THEN 'false'
        WHEN oneway is null THEN 'false'
        WHEN oneway = 'alternating' THEN 'false'
        WHEN oneway = 'reversible' THEN 'false'
        WHEN oneway = '-1' THEN '-1'
        ELSE 'false'
        end;


UPDATE filterd_lines set oneway='true' WHERE oneway='-1';


-- Add relevant columns fOR topology  
ALTER TABLE public.filterd_lines 
    add column source integer,
    add column target integer,
    add column frc smallint, 
    add column fow smallint,
    add column id bigserial;

-- Create road topology with pgRouting
SELECT pgr_createTopology('filterd_lines', 3, 'way', 'id', 'source', 'target');

-- Analyze road topology with pgRouting
SELECT  pgr_analyzeGraph('filterd_lines',3,the_geom:='way',id:='id',source:='source',target:='target');


ALTER TABLE public.filterd_lines
    rename column oneway to oneway_dir;
ALTER TABLE public.filterd_lines
    add column oneway boolean;

UPDATE public.filterd_lines SET
    oneway = CASE
        WHEN oneway_dir = 'true' THEN true
        WHEN oneway_dir = 'false' THEN false
        ELSE false
        end;

UPDATE public.filterd_lines SET
    frc = CASE
        WHEN highway = 'motorway' THEN 0
        WHEN highway = 'motorway_link' THEN 0 
        WHEN highway = 'trunk' THEN 1
        WHEN highway = 'trunk_link' THEN 1
        WHEN highway = 'primary' THEN 2
        WHEN highway = 'primary_link' THEN 2
        WHEN highway = 'secondary' THEN 3
        WHEN highway = 'secondary_link' THEN 3
        WHEN highway = 'tertiary' THEN 4
        WHEN highway = 'tertiary_link' THEN 4
        WHEN highway = 'road' THEN 5
        WHEN highway = 'road_link' THEN 5
        WHEN highway = 'unclassified' THEN 5
        WHEN highway = 'residential' THEN 5
        WHEN highway = 'living_street' THEN 6
        ELSE 7
        end;

-- Form Of Way Value --

UPDATE public.filterd_lines SET
    fow = CASE
        WHEN highway = 'motorway' THEN 1
        WHEN highway = 'motorway_link' THEN 6 
        WHEN highway = 'trunk' THEN 2
        WHEN highway = 'trunk_link' THEN 6
        WHEN highway = 'primary' THEN 3
        WHEN highway = 'primary_link' THEN 6
        WHEN highway = 'secondary' THEN 3
        WHEN highway = 'secondary_link' THEN 6
        WHEN highway = 'tertiary' THEN 3
        WHEN highway = 'tertiary_link' THEN 6
        WHEN highway = 'road' THEN 3
        WHEN highway = 'road_link' THEN 6  
        WHEN highway = 'residential' THEN 3
        WHEN highway = 'living_street' THEN 3
        WHEN highway = 'unclassified' THEN 0
        ELSE 0
        end;

UPDATE public.filterd_lines set 
    fow = 4 WHERE junction = 'roundabout';

CREATE TABLE openlr.knoten (
    node_id bigint PRIMARY KEY, 
    lat double precision, 
    lon double precision,
    geom geometry(POINT, 4326)
);

create unique index on openlr.knoten (node_id); 
CREATE INDEX knoten_geom_idx ON openlr.knoten USING GIST (geom);

INSERT INTO openlr.knoten (node_id, geom) 
SELECT v.id, st_transform(v.the_geom, 4326) FROM public.filterd_lines_vertices_pgr v;

UPDATE openlr.knoten set 
    lat = st_y(geom),
    lon = st_x(geom); 

-- Tabelle Kanten -- 
CREATE TABLE openlr.kanten (
    line_id bigint PRIMARY KEY, 
    start_node bigint, 
    end_node bigint, 
    name varchar(100), 
    name_langcode varchar(3) DEFAULT 'de', 
    fow smallint DEFAULT 3, 
    frc smallint DEFAULT 7, 
    oneway boolean,
    length_meter int, 
    min_lat double precision, 
    max_lat double precision, 
    min_lon double precision, 
    max_lon double precision, 
    geom geometry(LINESTRING, 4326),
    constraint frc_FK FOREIGN KEY(frc) 
        REFERENCES openlr.functional_road_class (frc_id),
    constraint fow_FK FOREIGN KEY(fow) 
        REFERENCES openlr.form_of_way (fow_id),
    constraint start_node_FK FOREIGN KEY(start_node) 
        REFERENCES openlr.knoten (node_id),
    constraint end_node_FK FOREIGN KEY(end_node) 
        REFERENCES openlr.knoten (node_id)
);

CREATE INDEX kanten_end_node_idx ON openlr.kanten USING btree (end_node);
CREATE UNIQUE INDEX kanten_line_id_idx ON openlr.kanten USING btree (line_id);
CREATE INDEX kanten_start_node_idx ON openlr.kanten USING btree (start_node);
CREATE INDEX kanten_geom_idx ON openlr.kanten USING GIST (geom);

-- Insert in Kanten von filterd_lines -- 
INSERT INTO openlr.kanten (line_id, start_node, end_node, name, fow, frc, oneway, geom)
SELECT fln.id, fln.source, fln.target, fln.name, fln.fow, fln.frc, fln.oneway, st_transform(fln.way, 4326) 
FROM public.filterd_lines fln;

UPDATE openlr.kanten set 
    name_langcode = 'de',
    length_meter = Round(ST_Length(geom::geography)), 
    max_lat = st_ymax(geom), 
    min_lat = st_ymin(geom), 
    max_lon = st_xmax(geom), 
    min_lon = st_xmin(geom); 

-- Tabelle Metadata -- 

CREATE TABLE openlr.metadata (
    map_name varchar(64), 
    map_compile_date date DEFAULT now(),
    map_owner varchar(25), 
    right_lat double precision,
    right_lon double precision, 
    left_lat double precision, 
    left_lon double precision, 
    bbox_height double precision, 
    bbox_width double precision, 
    bbox geometry(POLYGON, 0)
);
                                   
INSERT INTO openlr.metadata(map_name, map_owner) VALUES 
('map_name', 'map_owner'); 
                                   
UPDATE openlr.metadata set bbox = (SELECT ST_Extent(geom) FROM openlr.knoten);

UPDATE openlr.metadata set 
    left_lon = st_ymax(bbox) ,
    left_lat = st_xmin (bbox),
    right_lon = st_ymin(bbox) , 
    right_lat = st_xmax(bbox) ; 

UPDATE openlr.metadata set 
    bbox_height = left_lon - right_lon,
    bbox_width = right_lat - left_lat;

/* create table incidents_v7 */
CREATE TABLE openlr.incidents_v7 (
     incident_id varchar(64) PRIMARY KEY,
     original_id varchar(64),
     olr varchar(255),
     start_time TIMESTAMP,
     end_time TIMESTAMP,
     road_closed BOOLEAN,
     description TEXT,
     summary TEXT,
     junction_traversability varchar(32),
     posoff int,
     negoff int,
     last_updated TIMESTAMP
);

/* create table kanten_incidents_v7 */
CREATE TABLE openlr.kanten_incidents_v7 (
    incident_id varchar(32) PRIMARY KEY,
    line_id bigint NOT NULL,
    posoff int,
    negoff int
);

/* create table flow_v7 */
CREATE TABLE openlr.flow_v7 (
    name varchar(50),
    olr varchar(50) PRIMARY KEY,
    speed double precision NULL,
    speed_uncapped double precision NULL,
    free_flow_speed double precision NULL,
    jam_factor double precision NULL,
    confidence double precision NULL,
    traversability varchar(50) NULL,
    junction_traversability varchar(50) NULL,
    posoff int,
    negoff int,
    last_updated TIMESTAMP
);

/* create table kanten_flow_v7 */
CREATE TABLE openlr.kanten_flow_v7 (
   olr varchar(64) NOT NULL,
   line_id bigint NOT NULL,
   posoff int,
   negoff int
);
