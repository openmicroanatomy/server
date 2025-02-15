package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.ylihallila.server.util.Constants;

import javax.persistence.*;

import java.util.Collection;
import java.util.Objects;

import static fi.ylihallila.server.util.Config.Config;

@Entity
@DiscriminatorValue("Organization")
public class Organization extends Owner {

	@OneToMany(mappedBy = "organization", orphanRemoval = true)
	@JsonIgnore
	private Collection<User> users;

	@OneToMany(mappedBy = "owner", orphanRemoval = true)
	@JsonIgnore
	private Collection<Workspace> workspaces;

	public Organization() {}

	public Organization(String id, String name) {
		this.id = id;
		this.name = name;
	}

    @JsonProperty
	@Transient
	public String getLogoUrl() {
		return Config.getString("server.host") + "/" + String.format(Constants.ORGANIZATION_LOGOS, id);
	}

	public Collection<User> getUsers() {
		return users;
	}

	public void setUsers(Collection<User> users) {
		this.users = users;
	}

	public Collection<Workspace> getWorkspaces() {
		return workspaces;
	}

	public void setWorkspaces(Collection<Workspace> workspaces) {
		this.workspaces = workspaces;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), id);
	}

	@Override
	public String toString() {
		return "Organization{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				'}';
	}
}

