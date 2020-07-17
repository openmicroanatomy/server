package fi.ylihallila.server.models;

import javax.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Owner {

	@Id
	private String id;

	private String name;

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Owner{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				'}';
	}
}
