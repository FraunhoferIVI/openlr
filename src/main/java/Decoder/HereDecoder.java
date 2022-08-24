package Decoder;

import Exceptions.InvalidHereOLRException;
import HereDecoder.IntermediateReferencePoint;
import HereDecoder.LinearLocationReference;
import HereDecoder.OpenLocationReference;
import Loader.MapLoader;
import Loader.RoutableOSMMapLoader;
import OpenLRImpl.MapDatabaseImpl;
import openlr.LocationReferencePoint;
import openlr.Offsets;
import openlr.binary.impl.LocationReferencePointBinaryImpl;
import openlr.binary.impl.OffsetsBinaryImpl;
import openlr.decoder.OpenLRDecoder;
import openlr.decoder.OpenLRDecoderParameter;
import openlr.location.Location;
import openlr.map.FormOfWay;
import openlr.map.FunctionalRoadClass;
import openlr.map.MapDatabase;
import openlr.properties.OpenLRPropertiesReader;
import openlr.rawLocRef.RawLineLocRef;
import openlr.rawLocRef.RawLocationReference;
import org.apache.commons.configuration.FileConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * HERE implementation of the TPEG-OLR standard (ISO/TS 21219-22)
 * Original C# program translated to Java.
 * The TPEG-OLR standard differs from the TomTom OpenLR standard in the binary representation.
 * To overcome this issue HERE decoder decodes the TPEG-OLR location as a raw location reference in accordance
 * with the TomTom OpenLR standard. This makes it possible to decode the location with the TomTom decoder and
 * find the shortest path (affected lines) in the given road network.
 *
 * @author Emily Kast
 */

public class HereDecoder {

    private static final Logger logger = LoggerFactory.getLogger(HereDecoder.class);

    private MapLoader mapLoader;

    /**
     * @param mapLoader Loader for all Nodes and Lines in a specified bounding box
     */
    public HereDecoder(RoutableOSMMapLoader mapLoader)
    {
        this.mapLoader = mapLoader;
    }

    /**
     * Gets the OpenLR FOW Enum depending on the given FOW integer value
     *
     * @param fow integer value Form Of Way given in the HERE Location
     * @return OpenLR FOW value
     */
    private FormOfWay getFOWEnumOpenLR(int fow) {
        return FormOfWay.values()[fow];
    }

    /**
     * Gets the OpenLR FRC Enum depending on the given FRC integer value.
     *
     * @param frc integer value Functional Road Class given in the HERE Location.
     * @return OpenLR FRC value
     */
    private FunctionalRoadClass getFRCEnumOpenLR(int frc) {
        return FunctionalRoadClass.values()[frc];
    }

    /**
     * Writes HERE Line Location Reference to Raw Line Location Reference to make it readable for the OpenLR decoder.
     *
     * @param olr OpenLocationReference
     * @return OpenLR RawLineLocation Reference
     */
    public RawLineLocRef lineLocRefHere(OpenLocationReference olr) throws InvalidHereOLRException {
        if (!olr.isValid()) {
            // Exception
            System.out.println("HERE: Invalid OpenLR Data");
            throw new InvalidHereOLRException("HERE OLR is invalid!");
        } else {
            // switch statement since OpenLR offers more than line locations.
            switch (olr.getLocationReference().getType().id) {
                case OpenLocationReference.OLR_TYPE_LINEAR:
                    LinearLocationReference lr = (LinearLocationReference) olr.getLocationReference();
                    int seqNr = 0;
                    List<LocationReferencePoint> lrps = new ArrayList<>();
                    // First LRP
                    LocationReferencePointBinaryImpl firstRP = new LocationReferencePointBinaryImpl(
                            seqNr,
                            getFRCEnumOpenLR(lr.first.getLineProperties().frc),
                            getFOWEnumOpenLR(lr.first.getLineProperties().fow_id),
                            lr.first.coordinate.getLongitude(),
                            lr.first.coordinate.getLatitude(),
                            lr.first.lineProperties.bearing,
                            lr.first.pathProperties.dnp,
                            getFRCEnumOpenLR(lr.first.pathProperties.lfrcnp),
                            false);
                    seqNr++;
                    lrps.add(firstRP);
                    // Intermediate LRPs
                    boolean empty = (lr.intermediates == null);
                    if (!empty) {
                        for (IntermediateReferencePoint intermediateRP : lr.intermediates) {

                            LocationReferencePointBinaryImpl intermediateLRP = new LocationReferencePointBinaryImpl(
                                    seqNr,
                                    getFRCEnumOpenLR(intermediateRP.getLineProperties().frc),
                                    getFOWEnumOpenLR(intermediateRP.getLineProperties().fow_id),
                                    intermediateRP.coordinate.getLongitude(),
                                    intermediateRP.coordinate.getLatitude(),
                                    intermediateRP.lineProperties.bearing,
                                    intermediateRP.pathProperties.dnp,
                                    getFRCEnumOpenLR(intermediateRP.getPathProperties().lfrcnp),
                                    false);
                            seqNr++;
                        }
                    }
                    // Last LRP
                    LocationReferencePointBinaryImpl lastPoint = new LocationReferencePointBinaryImpl(
                            seqNr,
                            getFRCEnumOpenLR(lr.last.lineProperties.frc),
                            getFOWEnumOpenLR(lr.last.lineProperties.fow_id),
                            lr.last.coordinate.getLongitude(),
                            lr.last.coordinate.getLatitude(),
                            lr.last.lineProperties.bearing,
                            0,
                            getFRCEnumOpenLR(lr.first.pathProperties.lfrcnp),
                            true);
                    lrps.add(lastPoint);
                    // Negative and positive offsets
                    Offsets offsets = new OffsetsBinaryImpl(lr.getPosOff(), lr.getNegOff());
                    return new RawLineLocRef("1", lrps, offsets);
                default:
                    System.out.println("Unsupported OpenLR Type");
                    break;
            }
        }
        return null;
    }

    /**
     * HERE Decoder, decodes Base64 Strings to LineLocations by generating a raw location reference according
     * to the TomTom OpenLR standard.
     *
     * @param openLRCode OpenLR Base64 String
     * @return location
     * @throws Exception Invalid HERE Location
     */
    public Location decodeHere(String openLRCode) throws Exception {

        // Gets Open Location Reference from Base64 String
        OpenLocationReference olr = OpenLocationReference.fromBase64TpegOlr(openLRCode);

        // Creates Raw Line Location Reference from Here Location Reference
        RawLocationReference rawLocationReference;
        try {
            rawLocationReference = lineLocRefHere(olr);
        } catch (InvalidHereOLRException e) {
            return null;
        }

        // Initialize database
        MapDatabase mapDatabase = new MapDatabaseImpl(mapLoader);

        // Decoder parameter, properties for writing on map database
        FileConfiguration decoderConfig = OpenLRPropertiesReader.loadPropertiesFromFile(new File(
                this.getClass().getClassLoader().getResource("OpenLR-Decoder-Properties.xml").getFile()));
        OpenLRDecoderParameter params = new OpenLRDecoderParameter.Builder().with(mapDatabase)
                .with(decoderConfig).buildParameter();

        //Initialize the OpenLR decoder
        OpenLRDecoder decoder = new openlr.decoder.OpenLRDecoder();

        //decode the location on map database
        return decoder.decodeRaw(params, rawLocationReference);
    }


}
