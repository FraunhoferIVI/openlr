package HereDecoder;

/**
 * HERE implementation of the TPEG-OLR standard (ISO/TS 21219-22)
 * Original C# program translated to Java.
 */

public class TpegEnums {

    public enum LocationReferenceCode {
        TMC(2),
        ETL(5),
        OLR(8),
        HLR(0xFE);

        public final int id;

        private LocationReferenceCode(int id) {
            this.id = id;
        }
    }

    public enum EffectCode {
        Unknown(1),
        Free(2),
        Heavy(3),
        Slow(4),
        Quering(5),
        Stationary(6),
        NoTraffic(7);

        public final int id;

        private EffectCode(int id) {
            this.id = id;
        }
    }

    public enum CauseCodeTec {
        TrafficCongestion(1),
        Accident(2),
        Roadworks(3),
        NarrowLanes(4),
        Impassibility(5),
        SlipperyRoad(6),
        Aquaplaning(7),
        Fire(8),
        HazardousConditions(9),

        ObjectsRoad(10),
        AnimalsRoadway(11),
        PeopleRoadway(12),
        BrokenVehicles(13),
        VehicleWrongWay(14),
        RescueRecovery(15),
        RegulatoryMeasure(16),
        ExtremeWeather(17),
        VisibilityReduced(18),
        Precipitation(19),

        RecklessPersons(20),
        OverHeatWarning(21),
        RegulationsChanged(22),
        MajorEvent(23),
        ServiceNotOperating(24),
        ServiceNotUsable(25),
        SlowMovingVehicles(26),
        DangerousEndOfQueue(27),
        RiskOfLife(28),
        TimeDelay(29),

        PoliceCheckpoint(30),
        MalfRoadsideEquip(31),
        TestMessage(100),
        Closure(101),
        UndecodableCause(255);

        public final int id;

        private CauseCodeTec(int id) {
            this.id = id;
        }
    }

    public enum WarningLevel {
        Informative(1),
        Danger1(2),
        Danger2(3),
        Danger3(4);

        public final int id;

        private WarningLevel(int id) {
            this.id = id;
        }
    }

    public enum LaneRestrictionTec {
        Closed(1),
        Open(2),
        RightClosed(3),
        LeftClosed(4);

        public final int id;

        private LaneRestrictionTec(int id) {
            this.id = id;
        }

    }

    public enum Tendency {
        SlightlyIncreasing(1),
        Increasing(2),
        StronglyIncreasing(3),
        SlightlyDecreasing(4),
        Decreasing(5),
        StronglyDecreasing(6),
        Constant(7);

        public final int id;

        private Tendency(int id) {
            this.id = id;
        }

    }

    public enum DiversionRoadType {
        Bypass(1),
        Access(2),
        LimitedAccess(3),
        NotRecommended(4),
        Closed(5);

        public final int id;

        private DiversionRoadType(int id) {
            this.id = id;
        }

    }

    public enum VehicleType {
        Car(1),
        Lorry(2),
        Bus(3),
        Taxi(4),
        Train(5),
        MotorCycle(6),
        VehicleTrailer(7),
        MotorVehicles(8),
        DangerousGoods(9),
        AbnormalLoad(10),
        HeavyVehicle(11);

        public final int id;

        private VehicleType(int id) {
            this.id = id;
        }

    }

    public enum CauseCodeTfp {

    }

    /// <summary>
    /// Representation of TFP Section Types.
    /// Follow spec [TISA specification : TPEG2 TFP] section 8.6
    /// </summary>
    public enum SectionType {
        Unknown(0),
        Entry(1),
        Exit(2);

        public final int id;

        private SectionType(int id) {
            this.id = id;
        }
    }

    public enum FlowDataQuality {
        Unknown(0),
        VeryLow(1),
        Low(2),
        Moderate(3),
        Sufficient(4),
        High(5),
        VeryHigh(6);

        public final int id;

        private FlowDataQuality(int id) {
            this.id = id;
        }
    }

    public enum FormOfWay {
        Undefined(0),
        Motorway(1),
        MultipleCarriageway(2),
        SingleCarriageway(3),
        Roundabout(4),
        Trafficsquare(5),
        Sliproad(6),
        Other(7),
        BikePath(8),
        Footpath(9),
        PedestrianZone(10);

        public final int id;

        private FormOfWay(int id) {
            this.id = id;
        }
    }
}
