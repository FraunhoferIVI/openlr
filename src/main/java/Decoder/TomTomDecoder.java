package Decoder;

import Loader.RoutableOSMMapLoader;
import OpenLRImpl.MapDatabaseImpl;
import openlr.binary.ByteArray;
import openlr.binary.OpenLRBinaryDecoder;
import openlr.*;
import openlr.binary.impl.LocationReferenceBinaryImpl;
import openlr.decoder.OpenLRDecoderParameter;
import openlr.location.Location;
import openlr.map.MapDatabase;
import openlr.properties.OpenLRPropertiesReader;
import openlr.rawLocRef.RawLocationReference;
import openlr.decoder.OpenLRDecoder;

import org.apache.commons.configuration.FileConfiguration;

import java.io.File;

import java.util.Base64;

public class TomTomDecoder {

    /**
     * Method to decode base64 String to Byte Array.
     *
     * @param base64OpenLRString OpenLR String containing location references
     * @return Byte Array containing locations
     */
    public ByteArray openLR2byteArray(String base64OpenLRString) {
        // Base64 String to Byte Array
        return new ByteArray(Base64.getDecoder().decode(base64OpenLRString));
    }

    /**
     * Decodes byte array and outputs edges of the own routing network affected by the incident.
     *
     * @param byteArray Byte Array containing location references
     * @throws Exception Byte Array not valid
     */


    public Location decodeTomTom(ByteArray byteArray, RoutableOSMMapLoader osmMapLoader) throws Exception {
        // Byte array to location reference
        LocationReference lr = new LocationReferenceBinaryImpl("Incident", byteArray);

        // Decode Binary Array to raw location
        OpenLRBinaryDecoder binaryDecoder = new OpenLRBinaryDecoder();
        RawLocationReference rawLocationReference = binaryDecoder.decodeData(lr);

        // Initialize database
        MapDatabase mapDatabase = new MapDatabaseImpl(osmMapLoader);

        // DecoderHere parameter
        FileConfiguration decoderConfig = OpenLRPropertiesReader.loadPropertiesFromFile(new File(this.getClass().getClassLoader().getResource("OpenLR-Decoder-Properties.xml").getFile()));
        OpenLRDecoderParameter params = new OpenLRDecoderParameter.Builder().with(mapDatabase).with(decoderConfig).buildParameter();

        //Initialize the decoder
        OpenLRDecoder decoder = new openlr.decoder.OpenLRDecoder();

        //decode the location on own database

        return decoder.decodeRaw(params, rawLocationReference);
    }

}
