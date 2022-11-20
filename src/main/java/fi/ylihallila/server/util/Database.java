package fi.ylihallila.server.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.type.UUIDCharType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class Database {

	private static final Logger logger = LoggerFactory.getLogger(Database.class);
	private static SessionFactory sessionFactory = null;

	static {
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
			.configure()
			.build();

		try {
			MetadataSources sources = new MetadataSources(registry);
			Metadata metadata = sources.getMetadataBuilder()
					.applyBasicType(UUIDCharType.INSTANCE, UUID.class.getName())
					.build();

			sessionFactory = metadata.buildSessionFactory();
			sessionFactory.getStatistics().setStatisticsEnabled(true);
		} catch (Exception e) {
			StandardServiceRegistryBuilder.destroy(registry);
			logger.error("Error while creating database", e);
			logger.error("Cannot continue without database, exiting ...");
			System.exit(0);
		}
	}

	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	/**
	 * Opens a new session. This session must be closed with {@link Session#close()}.
	 * @return A new session
	 */
	public static Session openSession() {
		return sessionFactory.openSession();
	}

	public static long getCurrentlyOpenSessionsCount() {
		return sessionFactory.getStatistics().getSessionOpenCount() - sessionFactory.getStatistics().getSessionCloseCount();
	}
}
