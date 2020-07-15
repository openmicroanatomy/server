package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import fi.ylihallila.remote.commons.Roles;
import fi.ylihallila.server.Util;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Remember to update WorkspaceExpanded also when updating this class.
 *
 * @see WorkspaceExpanded
 */
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

	@JsonIdentityReference(alwaysAsId = false)
	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private List<Project> projects = new ArrayList<>();

	public Workspace() {}

	public Workspace(String id, String name, Owner owner, List<Project> projects) {
		this.id = id;
		this.name = name;
		this.owner = owner;
		this.projects = projects;
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

	public List<Project> getProjects() {
		return projects;
	}

	public void setProjects(List<Project> projects) {
		this.projects = projects;
	}

	public boolean addProject(Project project) {
		return this.projects.add(project);
	}

	public void removeProject(String id) {
		projects.removeIf(project -> project.getId().equalsIgnoreCase(id));
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
				", projects=" + projects +
				'}';
	}
}
