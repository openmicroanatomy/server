package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Remember to update WorkspaceExpanded also when updating this class.
 *
 * @see WorkspaceExpanded
 */
@JsonIdentityInfo(scope = Workspace.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Workspace {

	/**
	 * UUID representing this workspace.
	 */
	private String id;

	/**
	 * Workspace name. Visible in QuPath.
	 */
	private String name;

	/**
	 * UUID of the workspace owner, usually an organization. Only users which are part
	 * of this organization can edit the projects withing this workspace.
	 */
	private String owner;

	@JsonIdentityReference(alwaysAsId = true)
	private List<Project> projects = new ArrayList<>();

	public Workspace() {
		setId(UUID.randomUUID().toString());
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

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
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

	@Override
	public String toString() {
		return "Workspace{" +
				"name='" + name + '\'' +
				", id='" + id + '\'' +
				", projects=" + projects +
				'}';
	}
}
