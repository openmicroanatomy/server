package fi.ylihallila.server.gson;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import fi.ylihallila.server.Util;
import java.util.UUID;

@JsonIdentityInfo(scope=Slide.class, generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
public class Slide {

    private String name;
    private String id;
    private String owner;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id.toString();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwner() {
        return Util.idToName(owner).orElse("Unknown (" + owner + ")");
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "Slide{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", owner='" + owner + '\'' +
                '}';
    }
}
