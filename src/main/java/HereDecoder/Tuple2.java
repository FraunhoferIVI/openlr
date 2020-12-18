package HereDecoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * HERE implementation of the TPEG-OLR standard (ISO/TS 21219-22)
 * Original C# program translated to Java.
 */

public class Tuple2<A, B> implements Map.Entry<A, B> {
    private static final long serialVersionUID = 1L;

    /**
     * the first object
     */
    private A o1;

    /**
     * the second object
     */
    private B o2;

    public Tuple2() {

    }

    /**
     * Constructs a new pair of objects.
     *
     * @param o1
     *            the first object
     * @param o2
     *            the second object
     */
    public Tuple2(A o1, B o2) {
        this.o1 = o1;
        this.o2 = o2;
    }

    public int getElementCount() {
        return 2;
    }

    public Object getElement(int idx) {
        switch (idx) {
            case 0:
                return o1;
            case 1:
                return o2;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void setElement(int idx, Object v) {
        switch (idx) {
            case 0:
                setO1((A) v);
                break;
            case 1:
                setO2((B) v);
                break;
        }
    }

    /**
     * @return the o1
     * @category Getter
     */
    public A getO1() {
        return o1;
    }

    /**
     * @return the objectTwo
     * @category Getter
     */
    public B getO2() {
        return o2;
    }

    public void setO1(A objectOne) {
        this.o1 = objectOne;
    }

    public void setO2(B objectTwo) {
        this.o2 = objectTwo;
    }

    public void set(A o1, B o2) {
        this.o1 = o1;
        this.o2 = o2;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        string.append('[');
        if (o1 == null)
            string.append("<null>");
        else
            string.append(o1.toString());
        string.append(", ");
        if (o2 == null)
            string.append("<null>");
        else
            string.append(o2.toString());
        string.append(']');
        return string.toString();
    }

    @Override
    public A getKey() {
        return o1;
    }

    public A setKey(A value) {
        A old = o1;
        o1 = value;
        return old;
    }

    @Override
    public B getValue() {
        return o2;
    }

    @Override
    public B setValue(B value) {
        B old = o2;
        o2 = value;
        return old;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((o1 == null) ? 0 : o1.hashCode());
        result = prime * result + ((o2 == null) ? 0 : o2.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if ( !(obj instanceof Tuple2<?, ?>))
            return false;

        @SuppressWarnings("rawtypes")
        Tuple2 other = (Tuple2) obj;

        if (o1 == null) {
            if (other.o1 != null)
                return false;
        } else if ( !o1.equals(other.o1))
            return false;

        if (o2 == null) {
            if (other.o2 != null)
                return false;
        } else if ( !o2.equals(other.o2))
            return false;

        return true;
    }

    /**
     * Nimmt das erste Tupel mit "o1" als Wert für O1 und gibt den Wert des Tupel von O2 zurück.
     * null wenn keine Übereinstimmung.
     */
    public static <A, B, T extends Tuple2<A, B>> B getO2ForO1(Collection<T> c, A o1) {
        for (T t : c) {
            if (t.getO1().equals(o1))
                return t.getO2();
        }
        return null;
    }

    /**
     * Nimmt das erste Tupel mit "o1" als Wert für O1 und gibt den Wert des Tupel von O2 zurück.
     * null wenn keine Übereinstimmung.
     */
    public static <A, B, T extends Tuple2<A, B>> A getO1ForO2(Collection<T> c, B o2) {
        for (T t : c) {
            if (t.getO2().equals(o2))
                return t.getO1();
        }
        return null;
    }

    /**
     * Extrahiert die Werte (O1) die in der Sammlung sind.
     */
    public static <A, B, T extends Tuple2<A, B>> List<A> getO1FromCollection(Collection<T> c) {
        ArrayList<A> values = new ArrayList<>(c.size());
        for (Tuple2<A, B> t : c)
            values.add(t.getO1());

        return values;
    }

    /**
     * Extrahiert die Wert (o2) die nach den Schlüsseln (o1) in der Sammlung sind.
     */
    public static <A, B, T extends Tuple2<A, B>> List<B> getO2FromCollection(Collection<T> c) {
        ArrayList<B> values = new ArrayList<>(c.size());
        for (Tuple2<A, B> t : c) {
            values.add(t.getO2());
        }
        return values;
    }

    /**
     * Nimmt eine Sammlung von {@link Map.Entry}, erzeugt aus den Enties {@link Tuple2} und füllt
     * damit eine weiter Sammlung
     *
     * @param src
     *            Quell-Sammlung
     * @param dst
     *            Ziel-Sammlung
     */
    public static <A, B> void mapEntriesToTupel2(Collection<Map.Entry<A, B>> src,
                                                 Collection<Tuple2<A, B>> dst) {
        for (Map.Entry<A, B> e : src) {
            dst.add(new Tuple2<>(e.getKey(), e.getValue()));
        }
    }

    public static <A, B> List<Tuple2<A, B>> createTupelListOutOfMap(Map<A, B> map) {
        List<Tuple2<A, B>> l = new ArrayList<>();
        mapEntriesToTupel2(map.entrySet(), l);
        return l;
    }

    public static <A, B> Tuple2<A, B> createTupel(A o1, B o2) {
        return new Tuple2<>(o1, o2);
    }
}
