package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {

	/**
	 * UUID representing this project.
	 */
	@Id
	@Column(name = "id", updatable = false, nullable = false)
	private String id;

	/**
	 * Name of the project. Visible in QuPath.
	 */
	private String name;

	/**
	 * Description for this project. Optional. Visible in QuPath.
	 */
	private String description;

	@ManyToOne
	@JsonBackReference
	private Subject subject;

	/**
	 * Unix timestamp as milliseconds. When was this project first created.
	 */
	private long createdAt;

	/**
	 * Unix timestamp as milliseconds. When this project was last modified.
	 */
	private long modifiedAt;

	/**
	 * Hidden projects are only visible to users with write permissions.
	 */
	private boolean hidden;

	public Project() {
		this.createdAt = System.currentTimeMillis();
		this.modifiedAt = System.currentTimeMillis();
	}

	public Project(String id, String name, String description, Subject subject) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.subject = subject;
		this.createdAt = System.currentTimeMillis();
		this.modifiedAt = System.currentTimeMillis();
		this.hidden = false;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Subject getSubject() {
		return subject;
	}

	public void setSubject(Subject subject) {
		this.subject = subject;
	}

	@Transient
	public String getOwner() {
		return getSubject().getWorkspace().getOwner().getId();
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public long getModifiedAt() {
		return modifiedAt;
	}

	public void setModifiedAt(long modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public boolean hasWritePermission(User user) {
		return getSubject().getWorkspace().hasWritePermission(user);
	}

	@Override
	public String toString() {
		return "Project{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", description='" + description + '\'' +
				", subject='" + subject + '\'' +
				", createdAt=" + createdAt +
				", modifiedAt=" + modifiedAt +
				'}';
	}
}
