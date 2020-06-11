package fi.ylihallila.server.gson;

import java.util.List;
import java.util.UUID;

public class Workspace {

	private String name;
	private String id;
	private List<Project> projects;

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

	public List<Project> getProjects() {
		return projects;
	}

	public void setProjects(List<Project> projects) {
		this.projects = projects;
	}

	public boolean addProject(Project project) {
		return this.projects.add(project);
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
