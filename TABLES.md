# Database Tables
***
* [flow_v7](#flow_v7)
* [flow_affected_v7](#flow_affected_v7)
* [incidents_v7](#incidents_v7)
* [incident_affected_v7](#incident_affected_v7)
* [kanten_flow_v7](#kanten_flow_v7)
* [kanten_incidents_v7](#kanten_incidents_v7)

### flow_v7
***
* **name:** street name
* **olr:** open location reference
* **speed:** expected speed along the roadway; doesn't exceed the legal speed limit.
* **speed_uncapped:** expected speed along the roadway; may exceed the legal speed limit
* **free_flow_speed:** reference speed along the roadway when no traffic is present.
* **jam_factor:** value between 0 and 10; the higher, the more traffic. 10 stands for road closure.
* **confidence:** value between 0.0 and 1.0; the higher, the more real time data is included in the speed calculation
* **traversability:** open, closed or reversibleNotRoutable 
* **junction_traversability:** Used for road closures to indicate if the junctions along the closure can be crossed. Possible values:
    * ALL_OPEN
    * ALL_CLOSED
    * INTERMEDIATE_CLOSED_EDGE_OPEN
    * START_OPEN_OTHERS_CLOSED
    * END_OPEN_OTHERS_CLOSED
* **posoff:** for mapping
* **negoff:** for mapping
* **last_updated:** time the information is from

speed values presumably in m/s

### flow_affected_v7
***
* **line_id:** identifier for the map-line the information is corresponding to
* **name - junction_traversability:** see [above](#flow_v7)
* **geom:** for display in QGIS

### incidents_v7
***
* **incident_id:** identifier for this incident message (can change over time)
* **original_id:** identifier of the first message for this incident
* **olr:** open location reference
* **start_time:** time, at which the incident becomes relevant
* **end_time:** time, at which the incident expires
* **road_closed:** weather the incident prevents travel along the roadway
* **description:** a longer textual description of the incident, often with location information
* **summary:** a short textual description of the incident without location information
* **junction_traversability - last_updated:** see [flow_v7](#flow_v7)

### incident_affected_v7
***
* **line_id:** identifier for the map-line the information is corresponding to
* **name:** street name
* **incident_id - junction_traversability:** see [above](#incidents_v7)
* **geom:** for display in QGIS

### kanten_flow_v7
***
Key table. Combines olr, line_id, posoff and negoff of each FlowItem.

### kanten_incidents_v7
***
Key table. Combines incident_id, line_id, posoff and negoff of each IncidentItem.