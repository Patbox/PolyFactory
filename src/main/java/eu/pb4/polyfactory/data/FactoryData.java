package eu.pb4.polyfactory.data;

public interface FactoryData {
    static FactoryData of(long count) {
        return new FactoryData() {
            @Override
            public String asString() {
                return "" + count;
            }

            @Override
            public long asLong() {
                return count;
            }

            @Override
            public double asDouble() {
                return count;
            }

            @Override
            public char padding() {
                return '0';
            }

            @Override
            public boolean forceRight() {
                return true;
            }
        };
    }

    String asString();
    long asLong();
    double asDouble();
    default char padding() {
        return ' ';
    }

    default boolean forceRight() {
        return false;
    };
}
