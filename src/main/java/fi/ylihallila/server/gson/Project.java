package fi.ylihallila.server.gson;

import com.fasterxml.jackson.annotation.*;
import fi.ylihallila.server.gson.resolvers.ProjectIdResolver;

import java.util.UUID;

@JsonIdentityInfo(scope = Project.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = ProjectIdResolver.class)
public class Project {

	private String id;
	private String name;
	private String description;
	private String thumbnail;

	/**
	 * Users or Tenants GUID
	 */
	private String owner;

	private long createdAt;
	private long modifiedAt;

	public Project() {
		this.createdAt = System.currentTimeMillis();
		this.modifiedAt = System.currentTimeMillis();
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

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
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

	@Override
	public String toString() {
		return "Project{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", description='" + description + '\'' +
				", thumbnail='" + thumbnail + '\'' +
				", owner='" + owner + '\'' +
				", createdAt=" + createdAt +
				", modifiedAt=" + modifiedAt +
				'}';
	}
}
