package fi.ylihallila.server.models;

import fi.ylihallila.remote.commons.Roles;

import javax.persistence.*;
import java.util.UUID;

/**
 * Slides are not tied to any workspaces or projects, instead they're just located
 * in QuPath project files. These slide objects do represent these slides and contain information
 * about each slide. Deleting a slide object won't delete it from any project mut makes
 * that slide inaccessible, as all the tiles will be deleted.
 */
@Entity
@Table( name = "slides" )
public class Slide {

    /**
     * UUID representing this slide.
     */
    @Id
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

    public Slide() {}

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

    public boolean hasPermission(User user) {
        if (user.getRoles().contains(Roles.ADMIN)) {
            return true;
        }

        return user.getRoles().contains(Roles.MANAGE_SLIDES) &&
                (owner.getId().equals(user.getOrganization().getId()) || owner.getId().equals(user.getId()));
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
