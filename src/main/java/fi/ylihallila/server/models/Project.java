package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.*;
import fi.ylihallila.remote.commons.Roles;
import fi.ylihallila.server.util.Database;
import fi.ylihallila.server.models.resolvers.ProjectIdResolver;
import org.hibernate.Session;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table( name = "projects" )
@JsonIdentityInfo(scope = Project.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = ProjectIdResolver.class)
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

	/**
	 * URL to project thumbnail. Visible in QuPath.
	 * @beta
	 */
	private String thumbnail;

	/**
	 * Users or Tenants GUID
	 */
	@ManyToOne
	@JsonIdentityReference
	private Owner owner;

	/**
	 * Unix timestamp as milliseconds. When was this project first created.
	 */
	private long createdAt;

	/**
	 * Unix timestamp as milliseconds. When this project was last modified.
	 */
	private long modifiedAt;

	public Project() {
		this.createdAt = System.currentTimeMillis();
		this.modifiedAt = System.currentTimeMillis();
	}

	public Project(String id, String name, String description, Owner owner) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.thumbnail = null;
		this.owner = owner;
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

	public Owner getOwner() {
		return owner;
	}

	public void setOwner(String id) {
		Session session = Database.getSession();
		session.beginTransaction();

		Organization organization = session.find(Organization.class, id);

		session.getTransaction().commit();
		session.close();

		setOwner(organization);
	}

	public void setOwner(Owner owner) {
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

	public boolean hasPermission(User user) {
		if (user.getRoles().contains(Roles.ADMIN)) {
			return true;
		}

		if (owner.getId().equals(user.getId())
				&& user.getRoles().contains(Roles.MANAGE_PERSONAL_PROJECTS)) {
			return true;
		} else if (owner.getId().equals(user.getOrganization().getId())
				&& user.getRoles().contains(Roles.MANAGE_PROJECTS)) {
			return true;
		} else {
			return false;
		}
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
