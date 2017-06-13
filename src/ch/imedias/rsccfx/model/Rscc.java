package ch.imedias.rsccfx.model;

import ch.imedias.rsccfx.model.connectionutils.Rscccfp;
import ch.imedias.rsccfx.model.connectionutils.RunRudp;
import ch.imedias.rsccfx.model.util.KeyUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.function.UnaryOperator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


/**
 * Stores the key and keyserver connection details.
 * Handles communication with the keyserver.
 */
public class Rscc {

  public static final String PREFERENCES_KEY_SERVER_IP = "keyServerIp";
  public static final String DEFAULT_KEY_SERVER_IP = "86.119.39.89";
  public static final String PREFERENCES_KEY_SERVER_HTTP_PORT = "keyServerHttpPort";
  public static final String DEFAULT_KEY_SERVER_HTTP_PORT = "800";
  public static final String PREFERENCES_VNC_PORT = "vncPort";
  public static final int DEFAULT_VNC_PORT = 5900;
  public static final String PREFERENCES_ICE_PORT = "icePort";
  public static final int DEFAULT_ICE_PORT = 5050;
  public static final String PREFERENCES_UDP_PACKAGE_SIZE = "udpPackageSize";
  public static final int DEFAULT_UDP_PACKAGE_SIZE = 10000;
  public static final String PREFERENCES_PROXY_PORT = "proxyPort";
  public static final int DEFAULT_PROXY_PORT = 2601;
  public static final String PREFERENCES_STUN_SERVER_PORT = "stunServerPort";
  public static final int DEFAULT_STUN_SERVER_PORT = 3478;
  public static final String PREFERENCES_FORCING_SERVER_MODE = "forcingServerMode";
  public static final boolean DEFAULT_FORCING_SERVER_MODE = false;
  public static final String PREFERENCES_VNC_VIEW_ONLY = "vncViewOnly";
  public static final boolean DEFAULT_VNC_VIEW_ONLY = false;
  public static final String PREFERENCES_VNC_BGR_233 = "vncBgr233";
  public static final boolean DEFAULT_VNC_BGR_233 = false;
  public static final String PREFERENCES_VNC_COMPRESSION = "vncCompression";
  public static final int DEFAULT_VNC_COMPRESSION = 6;
  public static final String PREFERENCES_VNC_QUALITY = "vncQuality";
  public static final int DEFAULT_VNC_QUALITY = 6;
  public static final String PREFERENCES_STUN_SERVERS = "STUNServers";
  public static final String DEFAULT_STUN_SERVERS = "numb.viagenie.ca;"
      + "stun.ekiga.net;stun.gmx.net;stun.1und1.de";
  public static final String DELIMITER = ";";

  private static String[] STUN_SERVERS = {
      "numb.viagenie.ca", "stun.ekiga.net", "stun.gmx.net", "stun.1und1.de"};
  private static final int PACKAGE_SIZE = 10000; // needed, since a static method access it.
  // TODO: make access depend on current setting
  private static final Logger LOGGER =
      Logger.getLogger(Rscc.class.getName());
  /**
   * Points to the "docker-build_p2p" folder inside resources, relative to the build path.
   * Important: Make sure to NOT include a / in the beginning or the end.
   */
  private static final String DOCKER_FOLDER_NAME = "docker-build_p2p";
  public static final String DEFAULT_SUPPORTERS_FILE_NAME = "rscc-defaults-lernstick.xml";

  /**
   * sh files can not be executed in the JAR file and therefore must be extracted.
   * ".rscc" is a hidden folder in the user's home directory (e.g. /home/user)
   */
  private static final String RSCC_FOLDER_NAME = ".config/rscc";
  private static final String[] EXTRACTED_RESOURCES =
      {DOCKER_FOLDER_NAME, DEFAULT_SUPPORTERS_FILE_NAME};
  public static final UnaryOperator<String> REMOVE_FILE_IN_PATH =
      string -> string.replaceFirst("file:", "");
  private final SystemCommander systemCommander;

  private final StringProperty keyServerIp = new SimpleStringProperty();
  private final StringProperty keyServerHttpPort = new SimpleStringProperty();
  private final IntegerProperty vncPort = new SimpleIntegerProperty();
  private final IntegerProperty icePort = new SimpleIntegerProperty();
  private final BooleanProperty vncViewOnly = new SimpleBooleanProperty();
  private final DoubleProperty vncQuality = new SimpleDoubleProperty();
  private final DoubleProperty vncCompression = new SimpleDoubleProperty();
  private final BooleanProperty vncBgr233 = new SimpleBooleanProperty();
  private final StringProperty connectionStatusText = new SimpleStringProperty();
  private final StringProperty connectionStatusStyle = new SimpleStringProperty();
  private final IntegerProperty udpPackageSize = new SimpleIntegerProperty(
      getUdpPackageSizeStatic());
  private final IntegerProperty proxyPort = new SimpleIntegerProperty();
  private final IntegerProperty stunServerPort = new SimpleIntegerProperty();
  private final String[] connectionStatusStyles = {
      "statusBox", "statusBoxInitialize", "statusBoxSuccess", "statusBoxFail"};
  private final StringProperty terminalOutput = new SimpleStringProperty();

  private final BooleanProperty forcingServerMode = new SimpleBooleanProperty(false);
  private final BooleanProperty vncSessionRunning = new SimpleBooleanProperty(false);
  private final BooleanProperty vncServerProcessRunning
      = new SimpleBooleanProperty(false);
  private final BooleanProperty vncViewerProcessRunning
      = new SimpleBooleanProperty(false);
  private final BooleanProperty connectionEstablishmentRunning
      = new SimpleBooleanProperty(false);
  private final BooleanProperty rscccfpHasTalkedToOtherClient
      = new SimpleBooleanProperty(false);
  private final BooleanProperty isSshRunning = new SimpleBooleanProperty(false);

  private final Preferences preferences = Preferences.userNodeForPackage(Rscc.class);

  private final KeyUtil keyUtil;
  private String pathToResources;
  private String pathToResourceDocker;
  private String pathToDefaultSupporters;
  private boolean isLocalIceSuccessful = false;
  private boolean isRemoteIceSuccessful = false;
  private InetAddress remoteClientIpAddress;
  private int remoteClientPort;
  private RunRudp rudp;
  private VncViewerHandler vncViewer;
  private VncServerHandler vncServer;
  private Rscccfp rscccfp;

  /**
   * Initializes the Rscc model class.
   *
   * @param systemCommander a SystemComander-object that executes shell commands.
   * @param keyUtil         a KeyUtil-object which stores the key, validates and formats it.
   */
  public Rscc(SystemCommander systemCommander, KeyUtil keyUtil) {
    if (systemCommander == null) {
      LOGGER.info("Parameter SystemCommander is NULL");
      throw new IllegalArgumentException("Parameter SystemCommander is NULL");
    }
    if (keyUtil == null) {
      LOGGER.info("Parameter KeyUtil is NULL");
      throw new IllegalArgumentException("Parameter KeyUtil is NULL");
    }
    this.systemCommander = systemCommander;
    this.keyUtil = keyUtil;
    defineResourcePath();
    loadUserPreferences();
  }


  /**
   * Saves the UserPreferences.
   */
  private void loadUserPreferences() {
    setKeyServerIp(preferences.get(PREFERENCES_KEY_SERVER_IP, DEFAULT_KEY_SERVER_IP));
    setKeyServerHttpPort(preferences.get(PREFERENCES_KEY_SERVER_HTTP_PORT,
        DEFAULT_KEY_SERVER_HTTP_PORT));
    setVncPort(preferences.getInt(PREFERENCES_VNC_PORT, DEFAULT_VNC_PORT));
    setIcePort(preferences.getInt(PREFERENCES_ICE_PORT, DEFAULT_ICE_PORT));
    setUdpPackageSize(preferences.getInt(PREFERENCES_UDP_PACKAGE_SIZE, DEFAULT_UDP_PACKAGE_SIZE));
    setProxyPort(preferences.getInt(PREFERENCES_PROXY_PORT, DEFAULT_PROXY_PORT));
    setStunServerPort(preferences.getInt(PREFERENCES_STUN_SERVER_PORT, DEFAULT_STUN_SERVER_PORT));
    setVncViewOnly(preferences.getBoolean(PREFERENCES_VNC_VIEW_ONLY, DEFAULT_VNC_VIEW_ONLY));
    setVncBgr233(preferences.getBoolean(PREFERENCES_VNC_BGR_233, DEFAULT_VNC_BGR_233));
    setVncCompression(preferences.getDouble(PREFERENCES_VNC_COMPRESSION, DEFAULT_VNC_COMPRESSION));
    setVncQuality(preferences.getDouble(PREFERENCES_VNC_QUALITY, DEFAULT_VNC_QUALITY));

    String stunServers = preferences.get(PREFERENCES_STUN_SERVERS,
        DEFAULT_STUN_SERVERS);
    setStunServers(stunServers.split(DELIMITER));

    LOGGER.info("Loaded UserPrefs");
  }

  /**
   * Loads the UserPreferences.
   */
  public void saveUserPreferences() {
    preferences.put(PREFERENCES_KEY_SERVER_IP, getKeyServerIp());
    preferences.put(PREFERENCES_KEY_SERVER_HTTP_PORT, getKeyServerHttpPort());
    preferences.putInt(PREFERENCES_VNC_PORT, getVncPort());
    preferences.putInt(PREFERENCES_ICE_PORT, getIcePort());
    preferences.putInt(PREFERENCES_UDP_PACKAGE_SIZE, getUdpPackageSize());
    preferences.putInt(PREFERENCES_PROXY_PORT, getProxyPort());
    preferences.putInt(PREFERENCES_STUN_SERVERS, getStunServerPort());
    preferences.putBoolean(PREFERENCES_VNC_VIEW_ONLY, getVncViewOnly());
    preferences.putBoolean(PREFERENCES_VNC_BGR_233, getVncBgr233());
    preferences.putDouble(PREFERENCES_VNC_COMPRESSION, getVncCompression());
    preferences.putDouble(PREFERENCES_VNC_QUALITY, getVncQuality());

    preferences.put(PREFERENCES_STUN_SERVERS, String.join(DELIMITER, STUN_SERVERS));

    LOGGER.info("Saved UserPrefs");
  }

  public static int getUdpPackageSizeStatic() {
    return PACKAGE_SIZE;
  }

  /**
   * Sets resource path, according to the application running either as a JAR or in the IDE.
   */
  private void defineResourcePath() {
    String userHome = System.getProperty("user.home");
    LOGGER.fine("userHome " + userHome);
    URL theLocationOftheRunningClass = this.getClass().getProtectionDomain()
        .getCodeSource().getLocation();
    LOGGER.fine("Source Location: " + theLocationOftheRunningClass);
    File actualClass = new File(theLocationOftheRunningClass.getFile());
    if (actualClass.isDirectory()) {
      LOGGER.fine("Running in IDE");
      // set paths of the files
      pathToResourceDocker =
          REMOVE_FILE_IN_PATH.apply(
              getClass().getClassLoader().getResource(DOCKER_FOLDER_NAME).getFile()
          );
      pathToDefaultSupporters =
          REMOVE_FILE_IN_PATH.apply(
              getClass().getClassLoader().getResource(DEFAULT_SUPPORTERS_FILE_NAME).getFile()
          );
    } else {
      LOGGER.fine("Running in JAR");
      pathToResources = userHome + "/" + RSCC_FOLDER_NAME;
      // set paths of the files
      pathToResourceDocker = pathToResources + "/" + DOCKER_FOLDER_NAME;
      pathToDefaultSupporters = pathToResources + "/" + DEFAULT_SUPPORTERS_FILE_NAME;
      // extract all resources out of the JAR file
      Arrays.stream(EXTRACTED_RESOURCES).forEach(resource ->
          extractJarContents(theLocationOftheRunningClass, pathToResources, resource)
      );
    }
  }

  /**
   * Extracts files from running JAR to folder.
   *
   * @param filter filters the files that will be extracted by this string.
   */
  private void extractJarContents(URL sourceLocation, String destinationDirectory, String filter) {
    JarFile jarFile = null;
    LOGGER.fine("Extract Jar Contents");
    try {
      LOGGER.fine("sourceLocation: " + sourceLocation.getFile());
      jarFile = new JarFile(new File(sourceLocation.getFile()));
    } catch (IOException e) {
      LOGGER.severe("Exception thrown when trying to get file from: "
          + sourceLocation
          + "\n Exception Message: " + e.getMessage());
    }
    Enumeration<JarEntry> contentList = jarFile.entries();
    while (contentList.hasMoreElements()) {
      JarEntry item = contentList.nextElement();
      if (item.getName().contains(filter)) {
        LOGGER.fine("JarEntry: " + item.getName());
        File targetFile = new File(destinationDirectory, item.getName());
        if (!targetFile.exists()) {
          targetFile.getParentFile().mkdirs();
          targetFile = new File(destinationDirectory, item.getName());
        }
        if (item.isDirectory()) {
          LOGGER.fine("JarEntry: " + item.getName() + " is a directory");
          continue;
        }
        try (
            InputStream fromStream = jarFile.getInputStream(item);
            FileOutputStream toStream = new FileOutputStream(targetFile)
        ) {
          while (fromStream.available() > 0) {
            toStream.write(fromStream.read());
          }
        } catch (FileNotFoundException e) {
          LOGGER.severe("Exception thrown when reading from file: "
              + targetFile.getName()
              + "\n Exception Message: " + e.getMessage());
        } catch (IOException e) {
          LOGGER.severe("Exception thrown when trying to copy jar file contents to local"
              + "\n Exception Message: " + e.getMessage());
        }
        targetFile.setExecutable(true);
      }
    }
  }

  /**
   * Sets up the server with use.sh.
   */
  private void keyServerSetup() {
    String command = systemCommander.commandStringGenerator(
        pathToResourceDocker, "use.sh", getKeyServerIp(), getKeyServerHttpPort());
    SystemCommanderReturnValues returnValues = systemCommander.executeTerminalCommand(command);

    if (returnValues.getExitCode() != 0) {
      LOGGER.severe("Command failed: " + command + " ExitCode: " + returnValues.getExitCode());
      return;
    }
    isSshRunning.setValue(true);
  }

  /**
   * Kills the connection to the keyserver.
   */
  public void killConnection() {
    if (rscccfp != null) {
      LOGGER.info("RSCCFP not null. Close RSCCFP");
      rscccfp.closeConnection();
    }

    if (rudp != null) {
      LOGGER.info("Proxy not null. Try to close Proxy");
      rudp.closeRudpConnection();
    }

    if (vncServer != null && isVncServerProcessRunning()) {
      LOGGER.info("vncServer not null. Try to close VncServerProcess");
      vncServer.killVncServerProcess();
    }

    if (vncViewer != null && isVncViewerProcessRunning()) {
      LOGGER.info("Try to close VncViewer Process");
      vncViewer.killVncViewerProcess();
    }

    // Execute port_stop.sh with the generated key to kill the SSH connections
    LOGGER.info("SSH connection still active - try closing SSH connection");
    String command = systemCommander.commandStringGenerator(
        pathToResourceDocker, "port_stop.sh", keyUtil.getKey());
    systemCommander.executeTerminalCommand(command);
    keyUtil.setKey("");
    LOGGER.info("Everything should be closed");
  }

  /**
   * Requests a key from the key server.
   */
  public void requestKeyFromServer() {
    setConnectionEstablishmentRunning(true);
    setConnectionStatus("Setting keyserver...", 1);

    keyServerSetup();

    setConnectionStatus("Requesting key from server...", 1);

    String command = systemCommander.commandStringGenerator(
        pathToResourceDocker, "port_share.sh", Integer.toString(getVncPort()));

    SystemCommanderReturnValues returnValues = systemCommander.executeTerminalCommand(command);

    if (returnValues.getExitCode() != 0) {
      LOGGER.severe("Command failed: " + command + " ExitCode: " + returnValues.getExitCode());
      return;
    }

    keyUtil.setKey(returnValues.getOutputString()); // update key in model
    rscccfp = new Rscccfp(this, true);
    rscccfp.setDaemon(true);
    rscccfp.start();
    setConnectionStatus("Key successfully generated", 1);

    try {
      rscccfp.join();

      if (getRscccfpHasTalkedToOtherClient()) {
        LOGGER.info("RSCC: Starting VNCServer");

        vncServer = new VncServerHandler(this);
        vncServer.startVncServerListening();

        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        rudp = null;

        if (isLocalIceSuccessful && isRemoteIceSuccessful) {
          rudp = new RunRudp(this, true, false);
        } else if (isLocalIceSuccessful && !isRemoteIceSuccessful) {
          rudp = new RunRudp(this, false, false);
        } else if (!isLocalIceSuccessful && isRemoteIceSuccessful) {
          rudp = new RunRudp(this, true, false);
        }

        if (rudp != null) {
          LOGGER.info("RSCC: Starting rudp");

          rudp.start();
        }

        setConnectionStatus("VNC-Server waits for incoming connection", 2);
        setRscccfpHasTalkedToOtherClient(false);
      }

    } catch (Exception e) {
      e.printStackTrace();
      killConnection();
    }
    setConnectionEstablishmentRunning(false);
  }


  /**
   * Sets the Status of the connection establishment.
   *
   * @param text             Text to show for the connection status.
   * @param statusStyleIndex Index of the connectionStatusStyles.
   */
  public void setConnectionStatus(String text, int statusStyleIndex) {
    if (statusStyleIndex < 0 || statusStyleIndex >= connectionStatusStyles.length || text == null) {
      throw new IllegalArgumentException();
    }
    setConnectionStatusText(text);
    setConnectionStatusStyle(getConnectionStatusStyles(statusStyleIndex));
  }


  /**
   * Starts connection to the user.
   */
  public void connectToUser() {
    setConnectionEstablishmentRunning(true);

    setConnectionStatus("Get key from keyserver...", 1);

    keyServerSetup();

    String command = systemCommander.commandStringGenerator(pathToResourceDocker,
        "port_connect.sh", Integer.toString(getVncPort()), keyUtil.getKey());

    setConnectionStatus("Connected to keyserver.", 1);

    SystemCommanderReturnValues returnValues = systemCommander.executeTerminalCommand(command);

    if (returnValues.getExitCode() != 0) {
      LOGGER.severe("Command failed: " + command + " ExitCode: " + returnValues.getExitCode());
      setConnectionStatus(
          "Key " + getKeyUtil().getKey() + " could not be verified by the server.", 3);
      setConnectionEstablishmentRunning(false);
      return;
    }

    rscccfp = new Rscccfp(this, false);
    rscccfp.setDaemon(true);
    rscccfp.start();


    try {
      rscccfp.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    rudp = null;

    if (isLocalIceSuccessful) {
      rudp = new RunRudp(this, true, true);
    } else if (!isLocalIceSuccessful && isRemoteIceSuccessful) {
      rudp = new RunRudp(this, false, true);
    }
    vncViewer = new VncViewerHandler(this);

    if (rudp != null) {
      LOGGER.info("RSCC: Starting rudp");
      rudp.start();
    }

    LOGGER.info("RSCC: Starting VNCViewer");
    setConnectionStatus("Starting VNC Viewer...", 1);

    int i = 0;
    while (!isVncSessionRunning() && i < 10) {
      vncViewer.startVncViewerConnecting("localhost",
          (rudp != null) ? getProxyPort() : vncPort.getValue());
      i++;
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }


    if (isVncSessionRunning()) {
      if (rudp != null) {
        setConnectionStatus("VNC-Connection established using ICE", 2);
      } else {

        setConnectionStatus("VNC-Connection established over Server", 2);
      }

    }

    setConnectionEstablishmentRunning(false);
  }


  /**
   * Refreshes the key by killing the connection, requesting a new key and starting the server
   * again.
   */
  public void refreshKey() {
    setConnectionStatus("Refreshing key...", 1);
    killConnection();
    requestKeyFromServer();
  }


  /**
   * Starts VNCViewer in reverse mode (-listen).
   */
  public void startViewerReverse() {
    setConnectionEstablishmentRunning(true);
    if (vncViewer == null) {
      vncViewer = new VncViewerHandler(this);
    }
    vncViewer.startVncViewerListening();
    setConnectionEstablishmentRunning(false);
  }


  /**
   * Calls Supporter from addressbook (Starts VNC Server in Reverse mode).
   *
   * @param address     public reachable IP/Domain.
   * @param port        public reachable Port where vncViewer is listening.
   * @param isEncrypted sets if connection should be encrypted.
   */
  public void callSupporterDirect(String address, String port, boolean isEncrypted) {
    setConnectionEstablishmentRunning(true);
    setConnectionStatus("Connecting to " + address + ":" + port, 1);
    int portValue = -1;
    if (!port.equals("")) {
      portValue = Integer.valueOf(port);
    }
    vncServer = new VncServerHandler(this);
    boolean connectionSuccess = vncServer
        .startVncServerReverse(address, portValue > 0 ? portValue : 5500, isEncrypted);
    if (connectionSuccess) {
      setConnectionStatus("Connected", 2);
    } else {
      setConnectionStatus("Connection failed", 3);
    }
    setConnectionEstablishmentRunning(false);
  }

  /**
   * Starts the VNC Viewer as in listening mode.
   */
  public void startVncViewerAsService() {
    setConnectionEstablishmentRunning(true);
    setConnectionStatus("Starting VNC Viewer as service...", 1);
    vncViewer = new VncViewerHandler(this);
    vncViewer.startVncViewerListening();
    setConnectionStatus("VNC Viewer service is running", 2);

    setConnectionEstablishmentRunning(false);
  }

  /**
   * Stops the VNC Viewer.
   */
  public void stopVncViewerAsService() {
    setConnectionEstablishmentRunning(true);
    vncViewer.killVncViewerProcess();
    setConnectionStatus("VNC Viewer service is stopped", 1);

    setConnectionEstablishmentRunning(false);
  }


  public String getKeyServerIp() {
    return keyServerIp.get();
  }

  public void setKeyServerIp(String keyServerIp) {
    this.keyServerIp.set(keyServerIp);
  }

  public StringProperty keyServerIpProperty() {
    return keyServerIp;
  }

  public String getKeyServerHttpPort() {
    return keyServerHttpPort.get();
  }

  public void setKeyServerHttpPort(String keyServerHttpPort) {
    this.keyServerHttpPort.set(keyServerHttpPort);
  }

  public StringProperty keyServerHttpPortProperty() {
    return keyServerHttpPort;
  }

  public int getVncPort() {
    return vncPort.get();
  }

  public void setVncPort(int vncPort) {
    this.vncPort.set(vncPort);
  }

  public IntegerProperty vncPortProperty() {
    return vncPort;
  }

  public BooleanProperty vncViewOnlyProperty() {
    return vncViewOnly;
  }

  public double getVncQuality() {
    return vncQuality.get();
  }

  public void setVncQuality(int vncQuality) {
    this.vncQuality.set(vncQuality);
  }

  public void setVncQuality(double vncQuality) {
    this.vncQuality.set(vncQuality);
  }

  public DoubleProperty vncQualityProperty() {
    return vncQuality;
  }

  public boolean getVncViewOnly() {
    return vncViewOnly.get();
  }

  public void setVncViewOnly(boolean vncViewOnly) {
    this.vncViewOnly.set(vncViewOnly);
  }

  public double getVncCompression() {
    return vncCompression.get();
  }

  public void setVncCompression(double vncCompression) {
    this.vncCompression.set(vncCompression);
  }

  public DoubleProperty vncCompressionProperty() {
    return vncCompression;
  }

  public boolean getVncBgr233() {
    return vncBgr233.get();
  }

  public void setVncBgr233(boolean vncBgr233) {
    this.vncBgr233.set(vncBgr233);
  }

  public BooleanProperty vncBgr233Property() {
    return vncBgr233;
  }

  public KeyUtil getKeyUtil() {
    return keyUtil;
  }

  public String getConnectionStatusText() {
    return connectionStatusText.get();
  }

  public void setConnectionStatusText(String connectionStatusText) {
    this.connectionStatusText.set(connectionStatusText);
  }

  public StringProperty connectionStatusTextProperty() {
    return connectionStatusText;
  }

  public String getConnectionStatusStyle() {
    return connectionStatusStyle.get();
  }

  public void setConnectionStatusStyle(String connectionStatusStyle) {
    this.connectionStatusStyle.set(connectionStatusStyle);
  }

  public StringProperty connectionStatusStyleProperty() {
    return connectionStatusStyle;
  }

  public String getConnectionStatusStyles(int i) {
    return connectionStatusStyles[i];
  }

  public InetAddress getRemoteClientIpAddress() {
    return remoteClientIpAddress;
  }

  public void setRemoteClientIpAddress(InetAddress remoteClientIpAddress) {
    this.remoteClientIpAddress = remoteClientIpAddress;
  }

  public int getRemoteClientPort() {
    return remoteClientPort;
  }

  public void setRemoteClientPort(int remoteClientPort) {
    this.remoteClientPort = remoteClientPort;
  }

  public int getIcePort() {
    return icePort.get();
  }

  public void setIcePort(int icePort) {
    this.icePort.set(icePort);
  }

  public IntegerProperty icePortProperty() {
    return icePort;
  }

  public String[] getStunServers() {
    return STUN_SERVERS;
  }

  public void setStunServers(String[] servers) {
    STUN_SERVERS = servers;
  }

  public boolean isLocalIceSuccessful() {
    return isLocalIceSuccessful;
  }

  public void setLocalIceSuccessful(boolean localIceSuccessful) {
    isLocalIceSuccessful = localIceSuccessful;
  }

  public boolean isRemoteIceSuccessful() {
    return isRemoteIceSuccessful;
  }

  public void setRemoteIceSuccessful(boolean remoteIceSuccessful) {
    isRemoteIceSuccessful = remoteIceSuccessful;
  }

  public void setTerminalOutput(String terminalOutput) {
    this.terminalOutput.set(terminalOutput);
  }

  public boolean isForcingServerMode() {
    return forcingServerMode.get();
  }

  public void setForcingServerMode(boolean forcingServerMode) {
    this.forcingServerMode.set(forcingServerMode);
  }

  public BooleanProperty forcingServerModeProperty() {
    return forcingServerMode;
  }

  public SystemCommander getSystemCommander() {
    return systemCommander;
  }

  public boolean isVncSessionRunning() {
    return vncSessionRunning.get();
  }

  public void setVncSessionRunning(boolean vncSessionRunning) {
    this.vncSessionRunning.set(vncSessionRunning);
  }

  public BooleanProperty vncSessionRunningProperty() {
    return vncSessionRunning;
  }

  public VncServerHandler getVncServer() {
    return vncServer;
  }

  public void setVncServer(VncServerHandler vncServer) {
    this.vncServer = vncServer;
  }

  public boolean isVncServerProcessRunning() {
    return vncServerProcessRunning.get();
  }

  public void setVncServerProcessRunning(boolean vncServerProcessRunning) {
    this.vncServerProcessRunning.set(vncServerProcessRunning);
  }

  public BooleanProperty vncServerProcessRunningProperty() {
    return vncServerProcessRunning;
  }

  public boolean isVncViewerProcessRunning() {
    return vncViewerProcessRunning.get();
  }

  public void setVncViewerProcessRunning(boolean vncViewerProcessRunning) {
    this.vncViewerProcessRunning.set(vncViewerProcessRunning);
  }

  public BooleanProperty vncViewerProcessRunningProperty() {
    return vncViewerProcessRunning;
  }

  public boolean isConnectionEstablishmentRunning() {
    return connectionEstablishmentRunning.get();
  }

  public void setConnectionEstablishmentRunning(boolean connectionEstablishmentRunning) {
    this.connectionEstablishmentRunning.set(connectionEstablishmentRunning);
  }

  public BooleanProperty connectionEstablishmentRunningProperty() {
    return connectionEstablishmentRunning;
  }

  public boolean getRscccfpHasTalkedToOtherClient() {
    return rscccfpHasTalkedToOtherClient.get();
  }

  public void setRscccfpHasTalkedToOtherClient(boolean rscccfpHasTalkedToOtherClient) {
    this.rscccfpHasTalkedToOtherClient.set(rscccfpHasTalkedToOtherClient);
  }

  public BooleanProperty rscccfpHasTalkedToOtherClientProperty() {
    return rscccfpHasTalkedToOtherClient;
  }

  public int getUdpPackageSize() {
    return udpPackageSize.get();
  }

  public void setUdpPackageSize(int udpPackageSize) {
    this.udpPackageSize.set(udpPackageSize);
  }

  public IntegerProperty udpPackageSizeProperty() {
    return udpPackageSize;
  }

  public IntegerProperty proxyPortProperty() {
    return proxyPort;
  }

  public int getProxyPort() {
    return proxyPort.get();
  }

  public void setProxyPort(int proxyPort) {
    this.proxyPort.set(proxyPort);
  }

  public IntegerProperty stunServerPortProperty() {
    return stunServerPort;
  }

  public int getStunServerPort() {
    return stunServerPort.get();
  }

  public void setStunServerPort(int stunServerPort) {
    this.stunServerPort.set(stunServerPort);
  }

  public String getPathToDefaultSupporters() {
    return pathToDefaultSupporters;
  }

  public RunRudp getRudp() {
    return rudp;
  }
}
