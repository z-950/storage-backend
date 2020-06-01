package pers.z950.cli;

import com.fasterxml.jackson.module.kotlin.KotlinModule;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Scanner;

import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;

public class Launcher extends io.vertx.core.Launcher {
  public static void main(String[] args) {
    // english message
    Locale.setDefault(Locale.US);

    // kotlin json support
    DatabindCodec.mapper().registerModules();
    KotlinModule kotlinModule = new KotlinModule();
    if (!(DatabindCodec.mapper().getRegisteredModuleIds().contains(kotlinModule.getTypeId()) && DatabindCodec.prettyMapper().getRegisteredModuleIds().contains(kotlinModule.getTypeId()))) {
      DatabindCodec.mapper().registerModule(new KotlinModule());
      DatabindCodec.prettyMapper().registerModule(new KotlinModule());
    }

    // logger settings (for vertx 3.x)
    String slf4JLogger = "io.vertx.core.logging.SLF4JLogDelegateFactory";
    System.setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, slf4JLogger);

    new Launcher().dispatch(args);
  }

  private final Logger log = LoggerFactory.getLogger(Launcher.class);

  @Override
  public void beforeStartingVertx(VertxOptions options) {
    // cluster settings
    // set in  cluster.xml
    // todo: how to set when serial hosts?
    final String host = "127.0.0.1";
    options.getEventBusOptions().setClustered(true).setHost(host);
  }

  @Override
  public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
    super.beforeDeployingVerticle(deploymentOptions);

    String mainVerticle = getMainVerticle();
    if (mainVerticle == null) mainVerticle = "unknown";

    log.info("---deploy [{}]", mainVerticle);

    JsonObject configuration = getConfiguration();

    JsonObject verticleConfig = configuration.getJsonObject("verticle");
    JsonObject config = configuration.getJsonObject("config", new JsonObject());

    if (verticleConfig != null) {
      // todo: set verticle config
      verticleConfig.getInteger("instances", 1);
    }

    if (deploymentOptions.getConfig() == null) {
      deploymentOptions.setConfig(new JsonObject());
    }
    deploymentOptions.getConfig().mergeIn(config);
  }

  private JsonObject doGetConfiguration(File config) {
    JsonObject conf = new JsonObject();
    if (config.isFile()) {
      log.info("reading config file: {}", config.getAbsolutePath());
      try (Scanner scanner = new Scanner(config).useDelimiter("\\A")) {
        String sconf = scanner.next();
        try {
          conf = new JsonObject(sconf);
        } catch (DecodeException e) {
          log.info("configuration file {} does not contain a valid JSON object", sconf);
        }
      } catch (FileNotFoundException e) {
        // Ignore it.
      }
    } else {
      log.info("invalid config file: {}", config.getAbsolutePath());
    }
    return conf;
  }

  /**
   * [configPath] is the path of config, work dir is where the .jar is
   */
  private final static String configPath = "conf/config.json";

  private JsonObject getConfiguration() {
    JsonObject conf;
    File deployConfig = new File(configPath);

    if (deployConfig.isFile()) {
      conf = doGetConfiguration(deployConfig);
    } else {
      log.info("config file not found: {}", deployConfig.getAbsolutePath());
      conf = new JsonObject();
    }

    return conf;
  }

  // todo: set debug log level command
}
