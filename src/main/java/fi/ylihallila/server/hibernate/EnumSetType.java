package fi.ylihallila.server.hibernate;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;
import org.hibernate.usertype.DynamicParameterizedType;

import java.util.EnumSet;
import java.util.Properties;

public class EnumSetType extends AbstractSingleColumnStandardBasicType<EnumSet> implements DynamicParameterizedType {

    public EnumSetType() {
        super(VarcharTypeDescriptor.INSTANCE, null);
    }

    @Override
    public String getName() {
        return "enum-set";
    }

    @Override
    public String[] getRegistrationKeys() {
        return new String[] { getName(), "EnumSet", EnumSet.class.getName() };
    }

    @Override
    public void setParameterValues(Properties parameters) {
        String enumClassName = (String) parameters.get("enumClass");

        try {
            setJavaTypeDescriptor(new EnumSetTypeDescriptor(Class.forName(enumClassName)));
        }
        catch (ClassNotFoundException e) {}
    }
}
