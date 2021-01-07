package fi.ylihallila.server.hibernate;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EnumSetTypeDescriptor extends AbstractTypeDescriptor<EnumSet> {

    private static final String SEPARATOR = ",";

    private Object[] enumConstants;

    protected EnumSetTypeDescriptor(Class enumClass) {
        super(EnumSet.class);
        this.enumConstants = enumClass.getEnumConstants();
    }

    @Override
    public String toString(EnumSet value) {
        return (String) value.stream()
                .map(Enum.class::cast)
                .map(v -> Integer.toString(((Enum) v).ordinal()))
                .collect(Collectors.joining(SEPARATOR));
    }

    @Override
    public EnumSet fromString(String string) {
        if (StringUtils.isEmpty(string))
            return null;

        List<Enum> list = Arrays.stream(StringUtils.split(string, SEPARATOR))
                .map(ordinal -> enumConstants[Integer.parseInt(ordinal)])
                .map(Enum.class::cast)
                .collect(Collectors.toList());

        if (list.isEmpty())
            return null;

        return EnumSet.copyOf(list);
    }

    @Override
    public int extractHashCode(EnumSet value) {
        return Objects.hashCode(value);
    }

    @Override
    public <X> X unwrap(EnumSet value, Class<X> type, WrapperOptions options) {
        if (value == null)
            return null;

        if (EnumSet.class.isAssignableFrom(type))
            return (X) value;

        if (String.class.isAssignableFrom(type))
            return (X) toString(value);

        throw unknownUnwrap(type);
    }

    @Override
    public <X> EnumSet wrap(X value, WrapperOptions options) {
        if (value == null)
            return null;

        if (EnumSet.class.isInstance(value))
            return (EnumSet) value;

        if (String.class.isInstance(value))
            return fromString((String) value);

        throw unknownWrap(value.getClass());
    }
}
