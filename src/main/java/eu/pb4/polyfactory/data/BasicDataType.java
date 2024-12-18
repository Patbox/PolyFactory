package eu.pb4.polyfactory.data;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

public enum BasicDataType implements StringIdentifiable {
    INTEGER("integer", x -> new LongData(Long.parseLong(x)),
            (a, b) -> new LongData(a.asLong() + b.asLong()),
            (a, b) -> new LongData(a.asLong() - b.asLong()),
            (a, b) -> new LongData(a.asLong() * b.asLong()),
            (a, b) -> new LongData(a.asLong() / b.asLong()),
            (a, b) -> new LongData(a.asLong() % b.asLong())
    ),
    DECIMAL("decimal", x -> new DoubleData(Double.parseDouble(x)),
            (a, b) -> new DoubleData(a.asDouble() + b.asDouble()),
            (a, b) -> new DoubleData(a.asDouble() - b.asDouble()),
            (a, b) -> new DoubleData(a.asDouble() * b.asDouble()),
            (a, b) -> new DoubleData(a.asDouble() / b.asDouble()),
            (a, b) -> new DoubleData(a.asDouble() % b.asDouble())
    ),
    BOOLEAN("boolean", x -> BoolData.of(Boolean.parseBoolean(x)),
            (a, b) -> BoolData.of(a.isTrue() || b.isTrue()),
            (a, b) -> BoolData.of(a.isTrue() && !b.isTrue()),
            (a, b) -> BoolData.of(a.isTrue() && b.isTrue()),
            (a, b) -> BoolData.of(!a.isTrue() && !b.isTrue()),
            (a, b) -> BoolData.of(!a.isTrue() || !b.isTrue())
    ),
    STRING("string", StringData::ofLimited,
            (a, b) -> StringData.ofLimited(a.asString() + b.asString()),
            (a, b) -> StringData.ofLimited(a.asString().replace(b.asString(), "")),
            (a, b) -> StringData.EMPTY,
            (a, b) -> StringData.EMPTY,
            (a, b) -> StringData.EMPTY
    );

    private final String name;
    private final Function<String, DataContainer> parser;
    private final Text text;
    private final BiFunction<DataContainer, DataContainer, DataContainer> add;
    private final BiFunction<DataContainer, DataContainer, DataContainer> subtract;
    private final BiFunction<DataContainer, DataContainer, DataContainer> multiply;
    private final BiFunction<DataContainer, DataContainer, DataContainer> divide;
    private final BiFunction<DataContainer, DataContainer, DataContainer> modulo;

    BasicDataType(String name,  Function<String, @Nullable DataContainer> parser,
                  BiFunction<DataContainer, DataContainer, DataContainer> add,
                  BiFunction<DataContainer, DataContainer, DataContainer> subtract,
                  BiFunction<DataContainer, DataContainer, DataContainer> multiply,
                  BiFunction<DataContainer, DataContainer, DataContainer> divide,
                  BiFunction<DataContainer, DataContainer, DataContainer> modulo
    ) {
        this.name = name;
        this.parser = parser;
        this.text = Text.translatable("item.polyfactory.wrench.action.mode.arithmetic." + name);
        this.add = add;
        this.subtract = subtract;
        this.multiply = multiply;
        this.divide = divide;
        this.modulo = modulo;
    }

    public DataContainer add(DataContainer left, DataContainer right) {
        return this.add.apply(left, right);
    }

    public DataContainer subtract(DataContainer left, DataContainer right) {
        return this.subtract.apply(left, right);
    }

    public DataContainer multiply(DataContainer left, DataContainer right) {
        return this.multiply.apply(left, right);
    }

    public DataContainer divide(DataContainer left, DataContainer right) {
        return this.divide.apply(left, right);
    }

    public DataContainer modulo(DataContainer left, DataContainer right) {
        return this.modulo.apply(left, right);
    }

    @Nullable
    public DataContainer parse(String input) {
        try {
            return this.parser.apply(input);
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public String asString() {
        return this.name;
    }

    public Text text() {
        return this.text;
    }
}
