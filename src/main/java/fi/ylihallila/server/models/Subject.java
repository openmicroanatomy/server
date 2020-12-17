package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "subjects")
@JsonIdentityInfo(scope = Subject.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Subject {

    /**
     * UUID representing this class.
     */
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "subject_workspace")
    private Workspace workspace;

    @OneToMany(mappedBy = "subject")
    @Cascade({ org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.REMOVE })
    private List<Project> projects;

    public Subject() {

    }

    public Subject(String name, Workspace workspace) {
        this.name = name;
        this.workspace = workspace;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }
}
