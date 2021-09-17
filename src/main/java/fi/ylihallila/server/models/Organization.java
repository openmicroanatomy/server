package fi.ylihallila.server.models;

import javax.persistence.*;

import java.util.Objects;

import static fi.ylihallila.server.util.Config.Config;

@Entity
@DiscriminatorValue("Organization")
public class Organization extends Owner {

	public Organization() {}

	public Organization(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
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

	public String getLogoUrl() {
		return Config.getString("server.host") + ":" + Config.getString("server.port.insecure") + "/logos/" + id + ".png";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		Organization that = (Organization) o;
		return Objects.equals(id, that.id);
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

