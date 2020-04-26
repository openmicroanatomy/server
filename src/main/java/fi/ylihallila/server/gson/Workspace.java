package fi.ylihallila.server.gson;

import java.util.List;

public class Workspace {

	private String name;
	private List<Project> projects;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
}
