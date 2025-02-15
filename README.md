🔬 Open Microanatomy Server
==================== 

---

## 📜 Requirements

- Java 14 or newer

## 🔨 Building

Run `gradlew build -x test`

## 🏃‍♂️ Running

### Server

#### Docker

The preferred way to run OpenMicroanatomy Server is using Docker; see the [wiki](https://openmicroanatomy.github.io/docs/#_installation_docker) for more instructions.

#### Manually (development)

`java -jar <jar> [-port <port>]`

On your first startup, you'll be prompted to create the initial organization and administrator account.

After the initial setup, stop the server, edit the `application.conf` which was generated and restart the server.

See the [wiki](https://openmicroanatomy.github.io/docs/) for instructions for setting up the configuration, reverse proxies and more.

### Tiler

The tiler is a sub-process which runs automatically with the server. The tiler converts uploaded slides into smaller tiles.

**NOTICE:** The tiler fails if no OpenSlide binaries are available. To solve this issue either add the libraries to the same directory as the `server.jar` or to your `JAVA_HOME`. These binaries are available at `maven/org/openslide/openslide/3.4.1_2` in this repository. Extract the `.jar` file corresponding to your operating system.


## 📄 HTTP API Documentation

Available at [https://edu.qupath.yli-hallila.fi/docs/#/](https://edu.qupath.yli-hallila.fi/docs/#/).

This is updated manually and may be out of date. Navigate to `http://localhost:<port>/swagger` for the latest API docs on a development server.

## 🆘 See [wiki](https://openmicroanatomy.github.io/docs/) for additional information