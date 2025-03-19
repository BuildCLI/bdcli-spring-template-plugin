package dev.buildcli.plugin.bdclispring;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.buildcli.core.utils.compress.FileExtractor;
import dev.buildcli.plugin.BuildCLITemplatePlugin;
import dev.buildcli.plugin.enums.TemplateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.buildcli.core.utils.input.InteractiveInputUtils.*;
import static dev.buildcli.plugin.bdclispring.constants.SpringInitializrConstants.SPRING_INITIALIZR_API;
import static dev.buildcli.plugin.bdclispring.constants.SpringInitializrConstants.SPRING_INITIALIZR_CATALOG;

public class BdclispringTemplate extends BuildCLITemplatePlugin {
  private static final HttpClient httpClient = HttpClient.newHttpClient();
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Gson gson = new Gson();

  // Metadados do Spring Initializr
  private JsonObject metadata;

  // Project metadata
  private String projectName;
  private String projectDescription;
  private String packageName;
  private String groupId;
  private String artifactId;
  private String javaVersion;
  private String springBootVersion;
  private String buildSystem;
  private String packaging;
  private List<String> dependencies;
  private String projectOutputDir;

  @Override
  public void execute() {
    logger.info("Creating Spring Boot project...");

    try {
      // Fetch metadata from Spring Initializr
      fetchMetadata();

      // Collect project information
      collectProjectInfo();

      // Choose build system
      chooseBuildSystem();

      // Choose packaging type
      choosePackaging();

      // Choose Java version
      chooseJavaVersion();

      // Choose Spring Boot version
      chooseSpringBootVersion();

      // Select dependencies
      selectDependencies();

      // Confirm and generate project
      if (confirmProjectCreation()) {
        generateProject();
      } else {
        System.out.println("Project creation canceled.");
      }
    } catch (Exception e) {
      logger.error("Error creating project", e);
      System.err.println("Error creating project: " + e.getMessage());
    }
  }

  private void fetchMetadata() throws IOException, InterruptedException {
    logger.info("Fetching metadata from Spring Initializr...");

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(SPRING_INITIALIZR_CATALOG))
        .header("Accept", "application/json")
        .GET()
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() == 200) {
      metadata = gson.fromJson(response.body(), JsonObject.class);
      logger.info("Metadata fetched successfully");
    } else {
      throw new IOException("Failed to fetch metadata. Status code: " + response.statusCode());
    }
  }

  private void collectProjectInfo() {
    projectName = question("Project name", "demo", true);
    projectDescription = question("Project description", "Spring Boot Demo Project", false);

    // Get default values from metadata if available
    String defaultGroupId = extractDefaultValue(metadata, "groupId.default");
    String defaultArtifactId = projectName;

    groupId = question("Group ID", defaultGroupId != null ? defaultGroupId : "com.example", true);
    artifactId = question("Artifact ID", defaultArtifactId, true);
    packageName = question("Package name", groupId + "." + artifactId.toLowerCase().replace("-", ""), true);
    projectOutputDir = question("Project output directory");
  }

  private void chooseBuildSystem() {
    List<String> buildSystems = new ArrayList<>();
    Map<String, String> buildSystemIdMap = new HashMap<>();

    JsonArray typeValues = metadata.getAsJsonObject("type").getAsJsonArray("values");
    for (JsonElement element : typeValues) {
      JsonObject buildSystemObj = element.getAsJsonObject();
      String name = buildSystemObj.get("name").getAsString();
      String id = buildSystemObj.get("id").getAsString();
      buildSystems.add(name);
      buildSystemIdMap.put(name, id);
    }

    String selectedBuildSystem = options("Choose build system", buildSystems);
    buildSystem = buildSystemIdMap.get(selectedBuildSystem);
  }

  private void choosePackaging() {
    List<String> packagingOptions = new ArrayList<>();
    Map<String, String> packagingIdMap = new HashMap<>();

    JsonArray packagingValues = metadata.getAsJsonObject("packaging").getAsJsonArray("values");
    for (JsonElement element : packagingValues) {
      JsonObject packagingObj = element.getAsJsonObject();
      String name = packagingObj.get("name").getAsString();
      String id = packagingObj.get("id").getAsString();
      packagingOptions.add(name);
      packagingIdMap.put(name, id);
    }

    String selectedPackaging = options("Choose packaging", packagingOptions);
    packaging = packagingIdMap.get(selectedPackaging);
  }

  private void chooseJavaVersion() {
    List<String> javaVersionOptions = new ArrayList<>();
    Map<String, String> javaVersionIdMap = new HashMap<>();

    JsonArray javaValues = metadata.getAsJsonObject("javaVersion").getAsJsonArray("values");
    for (JsonElement element : javaValues) {
      JsonObject javaObj = element.getAsJsonObject();
      String name = javaObj.get("name").getAsString();
      String id = javaObj.get("id").getAsString();
      javaVersionOptions.add(name);
      javaVersionIdMap.put(name, id);
    }

    String selectedJavaVersion = options("Choose Java version", javaVersionOptions);
    javaVersion = javaVersionIdMap.get(selectedJavaVersion);
  }

  private void chooseSpringBootVersion() {
    List<String> bootVersionOptions = new ArrayList<>();
    Map<String, String> bootVersionIdMap = new HashMap<>();

    JsonArray bootVersionValues = metadata.getAsJsonObject("bootVersion").getAsJsonArray("values");
    for (JsonElement element : bootVersionValues) {
      JsonObject versionObj = element.getAsJsonObject();
      String versionName = versionObj.get("name").getAsString();
      String versionId = versionObj.get("id").getAsString();
      bootVersionOptions.add(versionName);
      bootVersionIdMap.put(versionName, versionId);
    }

    String selectedBootVersion = options("Choose Spring Boot version", bootVersionOptions);
    springBootVersion = bootVersionIdMap.get(selectedBootVersion);
  }

  private void selectDependencies() {
    // Organize dependencies by category
    Map<String, Map<String, String>> dependenciesByCategory = new HashMap<>();

    JsonArray dependencyGroups = metadata.getAsJsonObject("dependencies").getAsJsonArray("values");
    for (JsonElement groupElement : dependencyGroups.getAsJsonArray()) {
      JsonObject group = groupElement.getAsJsonObject();
      String groupName = group.get("name").getAsString();

      Map<String, String> categoryDependencies = new HashMap<>();
      JsonArray values = group.getAsJsonArray("values");

      for (JsonElement dependencyElement : values) {
        JsonObject dependency = dependencyElement.getAsJsonObject();
        String dependencyName = dependency.get("name").getAsString();
        String dependencyId = dependency.get("id").getAsString();
        categoryDependencies.put(dependencyName, dependencyId);
      }

      dependenciesByCategory.put(groupName, categoryDependencies);
    }

    // Select dependencies by category
    dependencies = new ArrayList<>();
    for (Map.Entry<String, Map<String, String>> category : dependenciesByCategory.entrySet()) {
      String categoryName = category.getKey();
      Map<String, String> categoryDependencies = category.getValue();

      System.out.println("\nCategory: " + categoryName);

      List<String> dependencyNames = new ArrayList<>(categoryDependencies.keySet());
      List<String> selectedDependencies = checklist(
          "Select " + categoryName + " dependencies",
          dependencyNames
      );

      for (String selected : selectedDependencies) {
        dependencies.add(categoryDependencies.get(selected));
      }
    }
  }

  private boolean confirmProjectCreation() {
    System.out.println("\nProject Configuration Summary:");
    System.out.println("-----------------------------");
    System.out.println("Project Name: " + projectName);
    System.out.println("Description: " + projectDescription);
    System.out.println("Group ID: " + groupId);
    System.out.println("Artifact ID: " + artifactId);
    System.out.println("Package: " + packageName);
    System.out.println("Java Version: " + javaVersion);
    System.out.println("Spring Boot: " + springBootVersion);
    System.out.println("Build System: " + buildSystem);
    System.out.println("Packaging: " + packaging);
    System.out.println("Dependencies: " + dependencies);
    System.out.println("-----------------------------");

    return confirm("Do you want to create this project?");
  }

  private void generateProject() {
    try {
      // Build the Spring Initializr API URL
      String url = buildSpringInitializrUrl();

      System.out.println("Generating project from Spring Initializr...");
      System.out.println("URL: " + url);

      // Send request to Spring Initializr API
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Accept", "application/zip")
          .GET()
          .build();

      HttpResponse<Path> response = httpClient.send(
          request,
          HttpResponse.BodyHandlers.ofFile(Path.of(projectName + ".zip"))
      );

      if (response.statusCode() == 200) {
        System.out.println("Project created successfully!");
        System.out.println("Downloaded to: " + response.body().toAbsolutePath());

        // Unzip the project
        unzipProject(response.body());
      } else {
        System.out.println("Failed to create project. Status code: " + response.statusCode());
      }
    } catch (Exception e) {
      System.err.println("Error creating project: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private String buildSpringInitializrUrl() {
    try {
      StringBuilder urlBuilder = new StringBuilder(SPRING_INITIALIZR_API);
      urlBuilder.append("/starter.zip");
      urlBuilder.append("?type=").append(URLEncoder.encode(buildSystem, StandardCharsets.UTF_8));
      urlBuilder.append("&language=").append(URLEncoder.encode("java", StandardCharsets.UTF_8));
      urlBuilder.append("&bootVersion=").append(URLEncoder.encode(springBootVersion, StandardCharsets.UTF_8));
      urlBuilder.append("&baseDir=").append(URLEncoder.encode(projectName, StandardCharsets.UTF_8));
      urlBuilder.append("&groupId=").append(URLEncoder.encode(groupId, StandardCharsets.UTF_8));
      urlBuilder.append("&artifactId=").append(URLEncoder.encode(artifactId, StandardCharsets.UTF_8));
      urlBuilder.append("&name=").append(URLEncoder.encode(projectName, StandardCharsets.UTF_8));
      urlBuilder.append("&description=").append(URLEncoder.encode(projectDescription, StandardCharsets.UTF_8));
      urlBuilder.append("&packageName=").append(URLEncoder.encode(packageName, StandardCharsets.UTF_8));
      urlBuilder.append("&packaging=").append(URLEncoder.encode(packaging, StandardCharsets.UTF_8));
      urlBuilder.append("&javaVersion=").append(URLEncoder.encode(javaVersion, StandardCharsets.UTF_8));

      // Add dependencies
      for (String dependency : dependencies) {
        urlBuilder.append("&dependencies=").append(URLEncoder.encode(dependency, StandardCharsets.UTF_8));
      }

      return urlBuilder.toString();
    } catch (Exception e) {
      logger.error("Error building URL", e);
      throw new RuntimeException("Failed to build Spring Initializr URL", e);
    }
  }

  private void unzipProject(Path zipFile) {
    try {
      FileExtractor.extractFile(zipFile.toFile().getAbsolutePath(), projectOutputDir);
    } catch (IOException e) {
      System.err.println("Error unzipping project: " + e.getMessage());
    }
  }

  // Utility method to extract default value from metadata
  private String extractDefaultValue(JsonObject metadata, String path) {
    try {
      String[] parts = path.split("\\.");
      JsonObject current = metadata;

      for (int i = 0; i < parts.length - 1; i++) {
        if (current.has(parts[i])) {
          current = current.getAsJsonObject(parts[i]);
        } else {
          return null;
        }
      }

      if (current.has(parts[parts.length - 1])) {
        return current.get(parts[parts.length - 1]).getAsString();
      }
    } catch (Exception e) {
      logger.warn("Error extracting default value for " + path, e);
    }

    return null;
  }

  @Override
  public TemplateType type() {
    return TemplateType.PROJECT;
  }

  @Override
  public String version() {
    return "0.0.1-SNAPSHOT";
  }

  @Override
  public String name() {
    return "bdcli-spring";
  }

  @Override
  public String description() {
    return "Spring Boot Project Generator";
  }
}