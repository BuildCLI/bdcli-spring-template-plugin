# BuildCLI Spring Template Plugin

This is a template plugin designed for the [BuildCLI](https://github.com/BuildCLI/BuildCLI) project. It streamlines the creation of Spring Boot projects by integrating with Spring Initializr. New programmers can use this plugin to quickly scaffold a Spring Boot application by simply answering a series of configuration questions.

## Overview

The plugin connects with the Spring Initializr API to retrieve available options (build system, packaging type, Java version, Spring Boot version, dependencies, etc.), then prompts the user for project configuration details. It builds a request URL, downloads a ZIP file containing the configured Spring Boot project, and extracts it to the specified output directory.

## Prerequisites

- **Java:** Version 21 or higher.
- **Maven:** Ensure Maven is installed to build the plugin.
- **BuildCLI:** This plugin is intended for use with BuildCLI. More details can be found at the link above.

## Adding the Plugin

There are two ways to add this plugin to BuildCLI:

- **Using a Local Path:**
    - If you have the plugin source directory, run:
      ```bash
      buildcli plugin add -f path/to/plugin
      ```
    - Or, if you have the packaged JAR file:
      ```bash
      buildcli plugin add -f path/to/plugin.jar
      ```

- **Using the Remote Repository URL:**
  ```bash
  buildcli plugin add -f https://github.com/BuildCLI/bdcli-spring-template-plugin.git
  ```

After adding the plugin, initialize a new Spring Boot project with:
```bash
buildcli project init -t
```

## Usage

Once the plugin is added, running the `buildcli project init -t` command will start an interactive process. You'll be prompted for project details such as the project name, group ID, artifact ID, Java version, Spring Boot version, and more. After confirming the configuration, the plugin will generate a new Spring Boot project based on your inputs.

## Contributing

Contributions to improve this plugin are welcome!
- Fork the repository, create a new branch, and submit a pull request with your changes.
- Follow the project's coding standards and include appropriate documentation for any new features.

## License

This project is licensed under the MIT License.

---

For more information about BuildCLI, please visit [BuildCLI on GitHub](https://github.com/BuildCLI/BuildCLI).