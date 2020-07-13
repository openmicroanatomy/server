package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import fi.ylihallila.server.Util;
import java.util.UUID;

@JsonIdentityInfo(scope=Slide.class, generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
public class Slide {

    /**
     * UUID representing this slide.
     */
    private String id;

    /**
     * Name of this slide.
     */
    private String name;

    /**
     * UUID of the slide owner. Can be either an organization or a specific user.
     */
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
        return owner;
    }

    /**
     * Converts the ID to a human readable format.
     * @return Organizations name, users name and if couldn't find ID: "Unknown (id)"
     */
    public String getOwnerReadable() {
        return Util.getHumanReadableName(owner).orElse("Unknown (" + owner + ")");
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
