package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import fi.ylihallila.server.Util;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table( name = "slides" )
//@JsonIdentityInfo(scope=Slide.class, generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
public class Slide {

    /**
     * UUID representing this slide.
     */
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    /**
     * Name of this slide.
     */
    private String name;

    /**
     * UUID of the slide owner. Can be either an organization or a specific user.
     */
    @ManyToOne
    private Owner owner;

    public Slide() {
    }

    public Slide(String id, String name, Owner owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
    }

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

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    /**
     * Converts the ID to a human readable format.
     * @return Organizations name, users name and if couldn't find ID: "Unknown (id)"
     */
    public String getOwnerReadable() {
        return owner.getName();
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
