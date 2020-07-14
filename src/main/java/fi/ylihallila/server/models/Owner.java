package fi.ylihallila.server.models;

import fi.ylihallila.server.Util;

import javax.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Owner {

	@Id
	private String id;

	private String name;

//	private final boolean isOrganization = isOrganization();
//
//	public boolean isOrganization() {
//		return Util.getKnownTenants().containsKey(id);
//	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
