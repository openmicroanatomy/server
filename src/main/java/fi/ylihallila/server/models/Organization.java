package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

import java.util.Collection;
import java.util.Objects;

import static fi.ylihallila.server.util.Config.Config;

@Entity
@DiscriminatorValue("Organization")
public class Organization extends Owner {

	@OneToMany(mappedBy = "organization", orphanRemoval = true)
	@Transient
	@JsonIgnore
	private Collection<User> users;

	@OneToMany(mappedBy = "owner", orphanRemoval = true)
	@Transient
	@JsonIgnore
	private Collection<Workspace> workspaces;

	public Organization() {}

	public Organization(String id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	public String getLogoUrl() {
		return Config.getString("server.host") + "/logos/" + id + ".png";
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

