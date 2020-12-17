package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.util.Util;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table( name = "workspaces" )
@JsonIdentityInfo(scope = Workspace.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Workspace {

	/**
	 * UUID representing this workspace.
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
	 * Workspace name. Visible in QuPath.
	 */
	private String name;

	/**
	 * UUID of the workspace owner, usually an organization. Only users which are part
	 * of this organization can edit the projects withing this workspace.
	 */
	@ManyToOne
	private Owner owner;

	/**
	 * Hidden workspaces are only visible to its owners and those with read access.
	 */
	private boolean hidden;

	@OneToMany(mappedBy = "workspace")
	@Cascade({ org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.REMOVE })
	private List<Subject> subjects = new ArrayList<>();

	public Workspace() {}

	public Workspace(String id, String name, Owner owner, List<Subject> subjects) {
		this.id = id;
		this.name = name;
		this.owner = owner;
		this.subjects = subjects;
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

	public void setId(UUID uuid) {
		setId(uuid.toString());
	}

	public void setId(String id) {
		this.id = id;
	}

	public Owner getOwner() {
		return owner;
	}

	public void setOwner(String id) {
		setOwner(Util.getOrganization(id));
	}

	public void setOwner(Owner owner) {
		this.owner = owner;
	}

	public List<Subject> getSubjects() {
		return subjects;
	}

	public void setSubjects(List<Subject> projects) {
		this.subjects = projects;
	}

	public boolean addProject(Subject subject, Project project) {
		return false; // TODO: implement
	}

	public boolean hasPermission(User user) {
		if (user.getRoles().contains(Roles.ADMIN)) {
			return true;
		}

		return owner.getId().equals(user.getOrganization().getId())
				&& user.getRoles().contains(Roles.MANAGE_PROJECTS);
	}

	@Override
	public String toString() {
		return "Workspace{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", owner=" + owner +
				", projects=" + subjects +
				'}';
	}
}
