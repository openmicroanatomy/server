package fi.ylihallila.server.gson;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIdentityInfo(scope = Workspace.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Workspace {

	private String id;
	private String name;
	private String owner;
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
