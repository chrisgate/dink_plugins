package com.chrisgate.dink_plugins;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;

import com.uk.tsl.rfid.asciiprotocol.AsciiCommander;
import com.uk.tsl.rfid.asciiprotocol.commands.FactoryDefaultsCommand;
import com.uk.tsl.rfid.asciiprotocol.enumerations.TriState;
import com.uk.tsl.rfid.asciiprotocol.parameters.AntennaParameters;
import com.uk.tsl.rfid.asciiprotocol.responders.LoggerResponder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener;

/** DinkPluginsPlugin */
public class DinkPluginsPlugin implements MethodCallHandler, RequestPermissionsResultListener {
  // Debug control
  private static final boolean D = BuildConfig.DEBUG;

  //    private CallbackContext availableChangeCallbackContext;
  private Map<String, PresentationSession> sessions;
  private Map<Integer, SecondScreenPresentation> presentations;
  private DisplayManager displayManager;
  //  private Activity activity;
  private String defaultDisplay;

  private TSLChangeHandler customHandler;

  private static final String TAG = "tslplugins";
  private static final String NAMESPACE = "tslplugins.chrisgate.dev";
  private static final int REQUEST_COARSE_LOCATION_PERMISSIONS = 1451;
  private BluetoothAdapter mBluetoothAdapter;
  private final Registrar registrar;
  private BluetoothDevice mDevice = null;
  private List<String> mResults;
  private List<String> mBarcodeResults;

  private Result pendingResult;

  private EventSink readSink;
  private EventSink statusSink;
  private EventSink commandsSink;
  // The current setting of the power level
  private int mPowerLevel = AntennaParameters.MaximumCarrierPower;
  // The method result to complete with the Android permission request result.
  // This is null when not waiting for the Android permission request;
  private MethodChannel.Result permissionResult;

  // All of the reader inventory tasks are handled by this class
  private InventoryModel mModel;

  private static AsciiCommander commander = null;


  /// Returns the current AsciiCommander
  public AsciiCommander getCommander() {
    return commander;
  }

  /// Sets the current AsciiCommander
  public void setCommander(AsciiCommander _commander) {
    commander = _commander;
  }

  /**
   * Plugin registration.
   */
  public static void registerWith(Registrar registrar) {
    final DinkPluginsPlugin instance = new DinkPluginsPlugin(registrar);
    registrar.addRequestPermissionsResultListener(instance);
//    final MethodChannel channel = new MethodChannel(registrar.messenger(), "dink_plugins");
//    channel.setMethodCallHandler(new DinkPluginsPlugin());
  }

  DinkPluginsPlugin(Registrar registrar) {
    this.registrar = registrar;
    mResults = new ArrayList<>();
    mBarcodeResults = new ArrayList<>();


    MethodChannel channel = new MethodChannel(registrar.messenger(), NAMESPACE + "/methods");
    EventChannel stateChannel = new EventChannel(registrar.messenger(), NAMESPACE + "/state");
    EventChannel readChannel = new EventChannel(registrar.messenger(), NAMESPACE + "/read");
    EventChannel commandsChannel = new EventChannel(registrar.messenger(), NAMESPACE + "/commands");

    BluetoothManager mBluetoothManager = (BluetoothManager) registrar.activity().getSystemService(Context.BLUETOOTH_SERVICE);
    assert mBluetoothManager != null;
    this.mBluetoothAdapter = mBluetoothManager.getAdapter();

    channel.setMethodCallHandler(this);
    stateChannel.setStreamHandler(stateStreamHandler);
    readChannel.setStreamHandler(readResultsHandler);
    commandsChannel.setStreamHandler(tslCommanderMessageHandler);

    // If the adapter is null, then Bluetooth is not supported
    if (mBluetoothAdapter == null) {
      bluetoothNotAvailableError("Bluetooth is not available on this device\nApplication Quitting...");
      return;
    }
    // Create the AsciiCommander to talk to the reader (if it doesn't already exist)
    if (getCommander() == null) {
      try {
        AsciiCommander commander = new AsciiCommander(registrar.activity().getApplicationContext());
        setCommander(commander);

      } catch (Exception e) {
        fatalError("Unable to create AsciiCommander!");
      }
    }
    //
    // An AsciiCommander has been created by the base class
    //
    AsciiCommander commander = getCommander();

    // Add the LoggerResponder - this simply echoes all lines received from the reader to the log
    // and passes the line onto the next responder
    // This is added first so that no other responder can consume received lines before they are logged.
    commander.addResponder(new LoggerResponder());

    // Add a synchronous responder to handle synchronous commands
    commander.addSynchronousResponder();
    customHandler = new TSLChangeHandler((Registrar) this);
    //Create a (custom) model and configure its commander and handler
    mModel = new InventoryModel();
    mModel.setCommander(getCommander());
    mModel.setHandler(customHandler);
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (mBluetoothAdapter == null && !"isAvailable".equals(call.method)) {
      result.error("bluetooth_unavailable", "the device does not have bluetooth", null);
      return;
    }

    final Map<String, Object> arguments = call.arguments();

    switch (call.method) {

      case "isAvailable":
        result.success(mBluetoothAdapter != null);
        break;

      case "isOn":
        try {
          assert mBluetoothAdapter != null;
          result.success(mBluetoothAdapter.isEnabled());
        } catch (Exception ex) {
          result.error("Error", ex.getMessage(), exceptionToString(ex));
        }
        break;
      case "isConnected":
        result.success(getCommander().isConnected() != false);
        break;
      case "openSettings":
        ContextCompat.startActivity(registrar.activity(),
                new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS),
                null);
        result.success(true);
        break;

      case "getBondedDevices":
        try {

          if (ContextCompat.checkSelfPermission(registrar.activity(),
                  Manifest.permission.ACCESS_COARSE_LOCATION)
                  != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(registrar.activity(),
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION_PERMISSIONS);

            pendingResult = result;
            break;
          }
          sendPermissionResult(true);
          getBondedDevices(result);

        } catch (Exception ex) {
          result.error("Error", ex.getMessage(), exceptionToString(ex));
        }

        break;
      case "connectToDevice":
        if (arguments.containsKey("address")) {
          String address = (String) arguments.get("address");
          connectToDevice(result, address);
        } else {
          result.error("invalid_argument", "argument 'address' not found", null);
        }
        break;
      case "reconnectDevice":
        reconnectDevice(result);
        break;
      case "disconnectDevice":
        disconnectDevice(result);
        break;
      case "resetReader":
        resetReader(result);
        break;
      case "isReaderConnected":
        result.success(getCommander().isConnected());
        break;
      case "connectReader":
        result.success(getCommander().isConnected());
        break;

      case "setPowerLevel":
        if (arguments.containsKey("powerLevel")) {
          int pLevel = (int) arguments.get("powerLevel");
          setPowerLevel(pLevel);
        } else {
          result.error("invalid_argument", "argument 'powerLevel' not found", null);
        }
        break;
      case "scan":
        mModel.scan();
        break;
      case "isFastId":
        if (arguments.containsKey("isFastId")) {
          boolean isFastId = (boolean) arguments.get("isFastId");
          setUsefastId(isFastId);
        } else {
          result.error("invalid_argument", "argument 'isFastId' not found", null);
        }
        break;

      default:
        result.notImplemented();
        break;
    }
  }
  private Map<String, PresentationSession> getSessions() {
    if (sessions == null) {
      sessions = new HashMap<String, PresentationSession>();
    }
    return sessions;
  }
  private void addDisplay(final Display display) {
    if ((display.getFlags() & Display.FLAG_PRESENTATION) != 0) {
      this.registrar.activity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          int oldSize = getSessions().size();
          SecondScreenPresentation presentation = new SecondScreenPresentation(registrar.activity(),display,this.getDefaultDisplay());
          getPresentations().put(display.getDisplayId(), presentation);
          presentation.show();
          int newSize = getPresentations().size();
//                    CallbackContext callbackContext = getAvailableChangeCallbackContext();
//                    if (oldSize == 0 && newSize == 1 && callbackContext != null) {
          if (oldSize == 0 && newSize == 1 ) {
//                        sendAvailableChangeResult(callbackContext,getPresentations().size()>0);
          }
        }

          private String getDefaultDisplay() {
              return defaultDisplay;
          }
      });
    }
  }

  private DisplayManager getDisplayManager() {
    if (displayManager == null) {
      displayManager = (DisplayManager) registrar.activity().getSystemService(Activity.DISPLAY_SERVICE);
      for (Display display : displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)) {
        addDisplay(display);
      }
      displayManager.registerDisplayListener((DisplayManager.DisplayListener) this, null);
    }
    return displayManager;
  }
  private Map<Integer, SecondScreenPresentation> getPresentations() {
    if (presentations == null) {
      presentations = new HashMap<Integer, SecondScreenPresentation>();
    }
    return presentations;
  }

  //
  // Terminate the app with the given message
  //
  private void fatalError(String message){
    // Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    Timer t = new Timer();
    t.schedule(new TimerTask() {
      public void run() {
//        finish();
      }
    }, 1800);
  }
    private void setPowerLevel(int level) {
        mPowerLevel = level;
        // Update the reader's setting only after the user has finished changing the value
        // updatePowerSetting(getCommander().getDeviceProperties().getMinimumCarrierPower() + seekBar.getProgress());
        mModel.getCommand().setOutputPower(mPowerLevel);
        mModel.updateConfiguration();
    }


    private void setUsefastId(boolean isFastId) {
    mModel.getCommand().setUsefastId(isFastId ? TriState.YES : TriState.NO);
    mModel.updateConfiguration();
  }

  /**
   * @param requestCode  requestCode
   * @param permissions  permissions
   * @param grantResults grantResults
   * @return boolean
   */
  /**
   * @param result result
   */
  private void getBondedDevices(Result result) {

    List<Map<String, Object>> list = new ArrayList<>();

    for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
      Map<String, Object> ret = new HashMap<>();
      ret.put("address", device.getAddress());
      ret.put("name", device.getName());
      ret.put("type", device.getType());
      list.add(ret);
    }

    result.success(list);
  }

  private String exceptionToString(Exception ex) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ex.printStackTrace(pw);
    return sw.toString();
  }

  /**
   * Connect the current AsciiCommander to the given device
   *
   * @param result the device information received from the DeviceListActivity
   * @param address true if a secure connection should be requested
   */
  private void connectToDevice(Result result, String address) {

    AsyncTask.execute(() -> {
      try {
        mDevice = mBluetoothAdapter.getRemoteDevice(address);

        if (mDevice == null) {
          result.error("connect_error", "device not found", null);
          return;
        }
        // Get the BluetoothDevice object
        mDevice = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        if (mDevice != null) {
          getCommander().connect(mDevice);
          result.success(true);
        } else {
          if (D) Log.e(TAG, "Unable to obtain BluetoothDevice!");
          result.error("bluetooth error", "Unable to obtain BluetoothDevice!", null);
        }
      } catch (Exception ex) {
        Log.e(TAG, ex.getMessage(), ex);
        if(D) Log.e(TAG, "Unable to obtain BluetoothDevice!");
        result.error("connect_error", ex.getMessage(), exceptionToString(ex));
      }
    });
  }


  /**
   * @param result result
   */
  private void disconnectDevice(Result result) {

    AsyncTask.execute(() -> {
      try {
        getCommander().disconnect();
        result.success(true);
      } catch (Exception ex) {
        Log.e(TAG, ex.getMessage(), ex);
        result.error("disconnection_error", ex.getMessage(), exceptionToString(ex));
      }
    });
  }
  @Override
  public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//    if (requestCode == REQUEST_COARSE_LOCATION_PERMISSIONS) {
//      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//        getBondedDevices(pendingResult);
//      } else {
//        pendingResult.error("no_permissions",
//                "this plugin requires location permissions for scanning", null);
//        pendingResult = null;
//      }
//      sendPermissionResult(true);
//    }
//
//    sendPermissionResult(false) ;
//  }
    if (requestCode == REQUEST_COARSE_LOCATION_PERMISSIONS) {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        getBondedDevices(pendingResult);
      } else {
        pendingResult.error("no_permissions",
                "this plugin requires location permissions for scanning", null);
        pendingResult = null;
      }
      return true;
    }
    return false;
  }
  /**
   *
   */
  private final StreamHandler stateStreamHandler = new StreamHandler() {

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        Log.d(TAG, action);

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
          statusSink.success(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1));
        }else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
          statusSink.success(1);
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
          statusSink.success(0);
        }else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
          // Get the BluetoothDevice object from the Intent
          BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
          // If it's already paired, skip it, because it's been listed already
          if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            statusSink.success(20);
          }
          // When discovery is finished, change the Activity title
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
          statusSink.success(21);
        }
      }
    };

    @Override
    public void onListen(Object o, EventSink eventSink) {
      statusSink = eventSink;
      registrar.activity().registerReceiver(mReceiver,
              new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

      registrar.activity().registerReceiver(mReceiver,
              new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));

      registrar.activity().registerReceiver(mReceiver,
              new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
      registrar.activity().registerReceiver(mReceiver,
              new IntentFilter(BluetoothDevice.ACTION_FOUND));
      registrar.activity().registerReceiver(mReceiver,
              new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
    }

    @Override
    public void onCancel(Object o) {
      statusSink = null;
      registrar.activity().unregisterReceiver(mReceiver);
    }
  };
  /**
   *
   */
  private final StreamHandler tslCommanderMessageHandler = new StreamHandler() {

    //
    // Handle the messages broadcast from the AsciiCommander
    //
    private BroadcastReceiver mCommanderMessageReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if (D) { Log.d(getClass().getName(), "AsciiCommander state changed - isConnected: " + getCommander().isConnected()); }

        String connectionStateMsg = intent.getStringExtra(AsciiCommander.REASON_KEY);
        //Toast.makeText(context, connectionStateMsg, Toast.LENGTH_SHORT).show();
        //  toastMsg(connectionStateMsg);

        //  displayReaderState();
        if( getCommander().isConnected() )
        {
          // Update for any change in power limits
          //  setPowerBarLimits();
          // This may have changed the current power level setting if the new range is smaller than the old range
          // so update the model's inventory command for the new power value
          mModel.getCommand().setOutputPower(mPowerLevel);

          mModel.resetDevice();
          mModel.updateConfiguration();
        }

        //UpdateUI();
      }
    };

    @Override
    public void onListen(Object o, EventSink eventSink) {
      commandsSink = eventSink;
      mModel.setEnabled(true);
      registrar.activity().registerReceiver(mCommanderMessageReceiver,
              new IntentFilter(AsciiCommander.STATE_CHANGED_NOTIFICATION));
      Log.w(TAG, "adding commands listener");
    }

    @Override
    public void onCancel(Object o) {
      commandsSink = null;
      mModel.setEnabled(false);
      registrar.activity().unregisterReceiver(mCommanderMessageReceiver);
      Log.w(TAG, "cancelling listener");
    }
  };


  //
  // Handle reset controls
  //
  private void resetReader( Result result) {
    // Reset the reader
    FactoryDefaultsCommand fdCommand = FactoryDefaultsCommand.synchronousCommand();
    getCommander().executeCommand(fdCommand);
    String msg = "Reset " + (fdCommand.isSuccessful() ? "succeeded" : "failed");
    // Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    result.success(msg);
    //UpdateUI();
  }


  private String toastMsg(String connectionStateMsg) {
    return connectionStateMsg;
  }

  /**
   *
   */
  private final EventChannel.StreamHandler readResultsHandler = new EventChannel.StreamHandler() {
    @Override
    public void onListen(Object o, EventSink eventSink) {
      readSink = eventSink;
    }

    @Override
    public void onCancel(Object o) {
      readSink = null;
    }
  };
  private void sendPermissionResult(boolean result) {
    if (permissionResult == null)
      return;
    permissionResult.success(result);
    permissionResult = null;
  }



  /**
   *
   * @return the url of the default display
   */
  public String getDefaultDisplay() {
    return defaultDisplay;
  }
  //----------------------------------------------------------------------------------------------
  // Model notifications
  //----------------------------------------------------------------------------------------------
  private class TSLChangeHandler extends Handler {
    private final WeakReference activity;

    public TSLChangeHandler(Registrar activity) {
      this.activity = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
      Registrar activity = (Registrar) this.activity.get();
      if (activity == null) {
        Log.e(TAG, "Activity is null TSLChangeHandler.handleMessage()!");
        return;
      }
      try {
        switch (msg.what) {
          case ModelBase.BUSY_STATE_CHANGED_NOTIFICATION:
            //TODO: process change in model busy state
            break;

          case ModelBase.MESSAGE_NOTIFICATION:
            // Map<String,Object> message = (Map<String,Object>)msg.obj;
            Map<String,Object> message = (Map<String, Object>) msg.obj;
            commandsSink.success(message);
            // Examine the message for prefix
            // String message = (String)msg.obj;

            if( message.containsKey("ER")) {
              // mResultMessage = message.substring(3);
            }
            else if( message.containsKey("BC")) {
              // mBarcodeResults.add((String) message.get("BC"));
              // scrollBarcodeListViewToBottom();
            } else if( message.containsKey("EPC")) {
              commandsSink.success(message);

              // scrollBarcodeListViewToBottom();
            } else {
              Log.e(TAG,"Others" );
              // mResults.add(message);
              commandsSink.success(message);
              // scrollResultsListViewToBottom();
            }
//                      if( message.startsWith("ER:")) {
//                          // mResultMessage = message.substring(3);
//                      }
//                      else if( message.startsWith("BC:")) {
//                          mBarcodeResults.add(message);
//                          // scrollBarcodeListViewToBottom();
//                      } else if( message.startsWith("EPC:")) {
//                          commandsSink.success(message);
//
//                          // scrollBarcodeListViewToBottom();
//                      } else {
//                          Log.e(TAG,"Others" );
//                         // mResults.add(message);
//                          commandsSink.success(msg.obj);
//                          // scrollResultsListViewToBottom();
//                      }
//                      // UpdateUI();
            break;

          default:
            break;
        }
      } catch (Exception e) {

      }
    }
  }

  /**
   * Reconnects to the last successfully connected reader
   */
  public void reconnectDevice(Result result){
    getCommander().connect(null);
    result.success(null);
  }
  protected void bluetoothNotAvailableError(String message){
    fatalError(message);
  }
}