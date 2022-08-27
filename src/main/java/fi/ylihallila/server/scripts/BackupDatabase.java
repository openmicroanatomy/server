package fi.ylihallila.server.scripts;

import fi.ylihallila.server.util.Constants;
import fi.ylihallila.server.util.Database;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

public class BackupDatabase extends Script {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override String getDescription() {
        return "Backup database daily";
    }

    @Override long getInterval() {
        return TimeUnit.DAYS.toSeconds(1);
    }

    @Override public void run() {
        ZonedDateTime now = ZonedDateTime.now();
        String file = String.format(Constants.DATABASE_BACKUP_FORMAT, now.getDayOfMonth(), now.getMonthValue(), now.getYear(), now.toEpochSecond());

        logger.info("Starting database backup ...");

        try (Session session = Database.openSession()) {
            session.beginTransaction();

            session.createSQLQuery("BACKUP TO :file")
                    .setParameter("file", file)
                    .executeUpdate();

            session.getTransaction().commit();
        } catch (Exception e) {
            logger.error("Error while creating database backup", e);
        }

        logger.info("Database backed up successfully");
    }
}
