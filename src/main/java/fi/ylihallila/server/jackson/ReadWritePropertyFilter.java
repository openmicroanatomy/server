package fi.ylihallila.server.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.models.Workspace;

import java.lang.reflect.Field;

public class ReadWritePropertyFilter implements PropertyFilter {

    private final User user;

    public ReadWritePropertyFilter(User user) {
        this.user = user;
    }

    @Override
    public void serializeAsField(Object pojo, JsonGenerator gen, SerializerProvider prov, PropertyWriter writer) throws Exception {
        if (pojo instanceof Workspace) {
            Workspace workspace = (Workspace) pojo;

            try {
                Field field = pojo.getClass().getDeclaredField(writer.getName());

                if (field.isAnnotationPresent(Filters.VisibleToWriteOnly.class)) {
                    if (!(workspace.hasWritePermission(user))) {
                        return;
                    }
                }

                if (field.isAnnotationPresent(Filters.VisibleToReadOnly.class)) {
                    if (!(workspace.hasReadPermission(user))) {
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        writer.serializeAsField(pojo, gen, prov);
    }

    @Override
    public void serializeAsElement(Object elementValue, JsonGenerator gen, SerializerProvider prov, PropertyWriter writer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void depositSchemaProperty(PropertyWriter writer, ObjectNode propertiesNode, SerializerProvider provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void depositSchemaProperty(PropertyWriter writer, JsonObjectFormatVisitor objectVisitor, SerializerProvider provider) {
        throw new UnsupportedOperationException();
    }
}
