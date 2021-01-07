package fi.ylihallila.server.models;

import javax.persistence.*;

import static fi.ylihallila.server.util.Config.Config;

@Entity
@DiscriminatorValue("Organization")
public class Organization extends Owner {

	@Id
	@Column(name = "id", updatable = false, nullable = false)
	private String id;

	private String name;

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
	public String toString() {
		return "Organization{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				'}';
	}
}

